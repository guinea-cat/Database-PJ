#!/usr/bin/env python3
"""Concurrent booking demo for the airline ticketing project.

Demo goal:
1. Prepare 50 real passenger accounts.
2. Make them concurrently book the same MU2001 PEK -> SHA first-class segment.
3. Show that only the 5 available seats can create orders.
4. Pay the 5 successful orders, wait 15 seconds, then refund them to restore stock.

The script uses only Python standard library modules so it works on classroom
machines without installing requests.
"""

from __future__ import annotations

import argparse
import json
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


DEFAULT_BASE_URL = "http://localhost:8080"
DEFAULT_TOTAL_USERS = 50
DEFAULT_STOCK = 5
DEFAULT_REFUND_DELAY = 15
DEFAULT_PASSWORD = "pass123"


@dataclass(frozen=True)
class DemoUser:
    index: int
    user_id: int
    login_account: str
    user_name: str
    passenger_id_number: str


@dataclass(frozen=True)
class BookingResult:
    user: DemoUser
    ok: bool
    ticket_id: int | None = None
    order_no: str | None = None
    message: str = ""


def api_url(base_url: str, path: str, params: dict[str, Any] | None = None) -> str:
    url = f"{base_url.rstrip('/')}/api{path}"
    if params:
        query = urllib.parse.urlencode({key: value for key, value in params.items() if value not in (None, "")})
        if query:
            url = f"{url}?{query}"
    return url


def api_request(base_url: str, method: str, path: str, payload: dict[str, Any] | None = None,
                params: dict[str, Any] | None = None, timeout: float = 20.0) -> dict[str, Any]:
    data = None
    headers: dict[str, str] = {}
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(api_url(base_url, path, params), data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return {"code": exc.code, "message": raw or f"HTTP {exc.code}", "data": None}
    except urllib.error.URLError as exc:
        return {"code": -1, "message": f"Cannot connect to backend: {exc.reason}", "data": None}
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return {"code": -2, "message": f"Invalid JSON response: {raw[:200]}", "data": None}


def require_success(response: dict[str, Any], action: str) -> Any:
    if response.get("code") != 0:
        raise RuntimeError(f"{action} failed: code={response.get('code')} message={response.get('message')}")
    return response.get("data")


def id_number_for(index: int) -> str:
    # Valid 18-character demo ID number. Birth date is 1990-01-01, unique sequence is 001-50.
    return f"11010119900101{index:04d}"


def user_payload(index: int) -> dict[str, Any]:
    return {
        "loginAccount": f"lockdemo{index:03d}",
        "password": DEFAULT_PASSWORD,
        "userName": f"并发演示乘客{index:03d}",
        "phoneNumber": f"13988{index:06d}",
        "email": f"lockdemo{index:03d}@example.com",
        "idNumber": id_number_for(index),
    }


def register_or_login_user(base_url: str, index: int) -> DemoUser:
    payload = user_payload(index)
    register_response = api_request(base_url, "POST", "/auth/register", payload)
    if register_response.get("code") == 0:
        data = register_response["data"]
    else:
        login_response = api_request(base_url, "POST", "/auth/login", {
            "loginAccount": payload["loginAccount"],
            "password": DEFAULT_PASSWORD,
        })
        data = require_success(login_response, f"login {payload['loginAccount']}")
    return DemoUser(
        index=index,
        user_id=int(data["userId"]),
        login_account=payload["loginAccount"],
        user_name=payload["userName"],
        passenger_id_number=payload["idNumber"],
    )


def prepare_users(base_url: str, total_users: int) -> list[DemoUser]:
    users: list[DemoUser] = []
    for index in range(1, total_users + 1):
        users.append(register_or_login_user(base_url, index))
        if index % 10 == 0:
            print(f"已准备乘客账号：{index}/{total_users}")
    return users


def search_target_segment(base_url: str, flight_number: str, flight_date: str, origin: str, destination: str) -> dict[str, Any]:
    data = require_success(api_request(base_url, "GET", "/flight/search", params={
        "departureAirportCode": origin,
        "arrivalAirportCode": destination,
        "flightDate": flight_date,
    }), "search target flight")
    for item in data:
        if item.get("flightNumber") == flight_number and item.get("originAirportCode") == origin and item.get("destinationAirportCode") == destination:
            return item
    available = ", ".join(f"{item.get('flightNumber')}#{item.get('segmentId')}" for item in data[:10])
    raise RuntimeError(f"Cannot find target segment {flight_number} {flight_date} {origin}->{destination}. Search returned: {available or 'none'}")


def create_ticket(base_url: str, user: DemoUser, flight_id: int, segment_id: int, cabin_class: str) -> BookingResult:
    response = api_request(base_url, "POST", "/ticket/create", {
        "userId": user.user_id,
        "flightId": flight_id,
        "segmentId": segment_id,
        "cabinClass": cabin_class,
        "passengerName": user.user_name,
        "passengerIdNumber": user.passenger_id_number,
    }, timeout=30.0)
    if response.get("code") != 0:
        return BookingResult(user=user, ok=False, message=f"code={response.get('code')} {response.get('message')}")
    data = response.get("data") or {}
    return BookingResult(
        user=user,
        ok=True,
        ticket_id=int(data["ticketId"]),
        order_no=data.get("orderNo"),
        message="created",
    )


def run_concurrent_booking(base_url: str, users: list[DemoUser], flight_id: int, segment_id: int, cabin_class: str) -> list[BookingResult]:
    barrier = threading.Barrier(len(users))
    results: list[BookingResult] = []
    results_lock = threading.Lock()

    def worker(user: DemoUser) -> None:
        try:
            barrier.wait()
        except threading.BrokenBarrierError:
            pass
        result = create_ticket(base_url, user, flight_id, segment_id, cabin_class)
        with results_lock:
            results.append(result)

    threads = [threading.Thread(target=worker, args=(user,), daemon=True) for user in users]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return results


def pay_ticket(base_url: str, ticket_id: int) -> bool:
    response = api_request(base_url, "POST", "/ticket/pay", {"ticketId": ticket_id}, timeout=20.0)
    if response.get("code") != 0:
        print(f"支付失败 ticketId={ticket_id}: code={response.get('code')} message={response.get('message')}")
        return False
    return True


def refund_ticket(base_url: str, ticket_id: int) -> bool:
    response = api_request(base_url, "POST", "/ticket/refund", {
        "ticketId": ticket_id,
        "remark": "并发控制课堂演示自动退票",
    }, timeout=20.0)
    if response.get("code") != 0:
        print(f"退票失败 ticketId={ticket_id}: code={response.get('code')} message={response.get('message')}")
        return False
    return True


def first_class_stock(base_url: str, segment_id: int) -> int:
    data = require_success(api_request(base_url, "GET", "/flight/detail", params={"segmentId": segment_id}), "load segment detail")
    return int(data["firstClassRemainingSeats"])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="航空票务系统并发抢票演示脚本")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="Backend base URL, default: http://localhost:8080")
    parser.add_argument("--total", type=int, default=DEFAULT_TOTAL_USERS, help="Concurrent passenger count, default: 50")
    parser.add_argument("--expected-stock", type=int, default=DEFAULT_STOCK, help="Expected first-class stock, default: 5")
    parser.add_argument("--refund-delay", type=int, default=DEFAULT_REFUND_DELAY, help="Seconds before refunding successful tickets, default: 30")
    parser.add_argument("--flight-number", default="MU2001", help="Target flight number, default: MU2001")
    parser.add_argument("--flight-date", default="2026-07-01", help="Target flight date, default: 2026-07-01")
    parser.add_argument("--origin", default="PEK", help="Origin airport code, default: PEK")
    parser.add_argument("--destination", default="SHA", help="Destination airport code, default: SHA")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    cabin_class = "FIRST_CLASS"

    print("=" * 72)
    print("航空票务系统并发控制演示")
    print(f"目标：{args.total} 个乘客同时抢 {args.expected_stock} 张头等舱票")
    print(f"航段：{args.flight_number} {args.flight_date} {args.origin} -> {args.destination}")
    print("=" * 72)

    try:
        target = search_target_segment(args.base_url, args.flight_number, args.flight_date, args.origin, args.destination)
        flight_id = int(target["flightId"])
        segment_id = int(target["segmentId"])
        current_stock = int(target["firstClassRemainingSeats"])
        print(f"目标 flightId={flight_id}, segmentId={segment_id}, 当前头等舱余票={current_stock}")
        if current_stock != args.expected_stock:
            print(f"警告：当前头等舱余票是 {current_stock}，不是预期的 {args.expected_stock}。")
            print("建议先重置演示数据并重新执行 seed_data.sql，再运行本脚本。")

        print("-" * 72)
        print("准备 50 个合法乘客账号，不存在则注册，已存在则登录复用。")
        users = prepare_users(args.base_url, args.total)
        print(f"乘客账号准备完成：{len(users)}")

        print("-" * 72)
        print("开始并发创建订单。")
        started = time.perf_counter()
        results = run_concurrent_booking(args.base_url, users, flight_id, segment_id, cabin_class)
        elapsed = time.perf_counter() - started

        success = [result for result in results if result.ok]
        failed = [result for result in results if not result.ok]
        print(f"并发创建完成，用时 {elapsed:.2f} 秒")
        print(f"创建成功：{len(success)}")
        print(f"创建失败：{len(failed)}")
        print("成功订单：")
        for result in sorted(success, key=lambda item: item.user.index):
            print(f"  {result.user.login_account}: ticketId={result.ticket_id}, orderNo={result.order_no}")
        if failed:
            print("失败样例：")
            for result in sorted(failed, key=lambda item: item.user.index)[:8]:
                print(f"  {result.user.login_account}: {result.message}")

        print("-" * 72)
        paid_ticket_ids: list[int] = []
        for result in success:
            if result.ticket_id is not None and pay_ticket(args.base_url, result.ticket_id):
                paid_ticket_ids.append(result.ticket_id)
        print(f"支付成功：{len(paid_ticket_ids)}")
        print(f"支付后头等舱余票：{first_class_stock(args.base_url, segment_id)}")

        if len(success) == args.expected_stock and len(failed) == args.total - args.expected_stock:
            print("结论：并发控制生效，未超卖。")
        else:
            print("结论：结果与预期不一致，请检查目标库存、历史订单或后端状态。")

        print("-" * 72)
        print(f"现在可以切到管理员订单页查看 {len(paid_ticket_ids)} 个已支付订单。")
        print(f"{args.refund_delay} 秒后脚本会自动退票并恢复库存。")
        time.sleep(args.refund_delay)

        refunded = 0
        for ticket_id in paid_ticket_ids:
            if refund_ticket(args.base_url, ticket_id):
                refunded += 1
        print(f"退票成功：{refunded}")
        print(f"退票后头等舱余票：{first_class_stock(args.base_url, segment_id)}")
        print("=" * 72)
        return 0
    except KeyboardInterrupt:
        print("\n用户中断脚本。")
        return 130
    except Exception as exc:
        print(f"脚本执行失败：{exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
