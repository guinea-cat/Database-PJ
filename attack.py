#!/usr/bin/env python3

import threading
import requests

# ============ 配置 ============
BASE_URL = "http://localhost:8080"
SEGMENT_ID = 1
FLIGHT_ID = 1
TOTAL = 10                    # 总人数
REFUND_COUNT = 5              # 前5个抢到即退，后5个抢到留着
# =============================

results = []
lock = threading.Lock()


def create_ticket(user_id):
    url = f"{BASE_URL}/api/ticket/create"
    body = {
        "userId": user_id,
        "flightId": FLIGHT_ID,
        "segmentId": SEGMENT_ID,
        "cabinClass": "ECONOMY",
        "passengerName": f"乘客{user_id:03d}",
        "passengerIdNumber": f"11010119900101{user_id:04d}",
        "passengerIdNumberDigest": "",
        "mealId": None
    }
    try:
        resp = requests.post(url, json=body, timeout=10)
        return resp.json()
    except Exception as e:
        return {"code": -1, "message": str(e)}


def pay_ticket(ticket_id):
    url = f"{BASE_URL}/api/ticket/pay"
    body = {"ticketId": ticket_id}
    try:
        resp = requests.post(url, json=body, timeout=10)
        return resp.json()
    except Exception as e:
        return {"code": -1, "message": str(e)}


def refund_ticket(ticket_id):
    url = f"{BASE_URL}/api/ticket/refund"
    body = {"ticketId": ticket_id, "remark": "攻击脚本退票"}
    try:
        resp = requests.post(url, json=body, timeout=10)
        return resp.json()
    except Exception as e:
        return {"code": -1, "message": str(e)}


def attack(user_id, should_refund):
   # """should_refund=True 表示抢到即退，False 表示抢到留着"""
    create_result = create_ticket(user_id)
    if create_result.get("code") != 0:
        with lock:
            results.append((user_id, "create_fail", create_result.get("message", "")))
        return

    data = create_result.get("data", {})
    ticket_id = data.get("ticketId") or data.get("id")
    if not ticket_id:
        with lock:
            results.append((user_id, "no_ticket_id", str(data)))
        return

    pay_result = pay_ticket(ticket_id)
    if pay_result.get("code") != 0:
        with lock:
            results.append((user_id, "pay_fail", pay_result.get("message", "")))
        return

    if should_refund:
        refund_result = refund_ticket(ticket_id)
        if refund_result.get("code") == 0:
            with lock:
                results.append((user_id, "refund", ticket_id))
        else:
            with lock:
                results.append((user_id, "refund_fail", refund_result.get("message", "")))
    else:
        with lock:
            results.append((user_id, "keep", ticket_id))


def main():
    print(f"攻击开始 - {TOTAL} 人抢 5 张票")
    print(f"  前 {REFUND_COUNT} 人: 抢到即退")
    print(f"  后 {TOTAL - REFUND_COUNT} 人: 抢到留着")
    print("-" * 50)

    barrier = threading.Barrier(TOTAL)

    def run(uid, should_refund):
        try:
            barrier.wait()
        except threading.BrokenBarrierError:
            pass
        attack(uid, should_refund)

    threads = []
    for i in range(TOTAL):
        uid = i + 1
        should_refund = (i < REFUND_COUNT)
        t = threading.Thread(target=run, args=(uid, should_refund))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    # 统计
    refund_list = []
    keep_list = []
    create_fail = 0
    pay_fail = 0

    for uid, status, detail in results:
        if status == "refund":
            print(f"  [退票] 用户{uid} 抢到并退票, ticketId={detail}")
            refund_list.append(uid)
        elif status == "keep":
            print(f"  [持有] 用户{uid} 抢到并持有, ticketId={detail}")
            keep_list.append(uid)
        elif status == "pay_fail":
            print(f"  [支付失败] 用户{uid}: {detail}")
            pay_fail += 1
        elif status == "create_fail":
            if "42001" in detail or "余票不足" in detail:
                print(f"  [售罄] 用户{uid}")
            else:
                print(f"  [创建失败] 用户{uid}: {detail}")
            create_fail += 1

    print("-" * 50)
    print(f"  退票人数: {len(refund_list)}")
    print(f"  持有票数: {len(keep_list)}")
    print(f"  支付失败: {pay_fail}")
    print(f"  创建失败: {create_fail}")

    if len(keep_list) > 5:
        print(f"  [结论] 超卖! 5 张票却有 {len(keep_list)} 人持有")
    elif len(keep_list) + len(refund_list) > 5:
        print(f"  [结论] 超卖! 成功支付 {len(keep_list) + len(refund_list)} 张")
    else:
        print(f"  [结论] 正常，未超卖")
    print("=" * 50)


if __name__ == "__main__":
    main()