# docs/api-spec.md

# 1. API Design Rules

Base URL

```http
http://localhost:8080/api
```

Content-Type

```http
application/json
```

Success Response

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-07-01T12:00:00"
}
```

Failure Response

```json
{
  "code": 40001,
  "message": "invalid parameter",
  "data": null,
  "timestamp": "2026-07-01T12:00:00"
}
```

---

# 2. Authentication APIs

## 2.1 User Register

POST

```http
/api/auth/register
```

Request

```json
{
  "loginAccount": "",
  "password": "",
  "userName": "",
  "phoneNumber": "",
  "email": "",
  "idNumber": ""
}
```

Response

```json
{
  "userId": 1
}
```

---

## 2.2 User Login

POST

```http
/api/auth/login
```

Request

```json
{
  "loginAccount": "",
  "password": ""
}
```

Response

```json
{
  "userId": 1,
  "userType": "PASSENGER",
  "memberLevel": "NORMAL",
  "points": 900,
  "token": "xxxxx"
}
```

---

## 2.3 Logout

POST

```http
/api/auth/logout
```

---

## 2.4 Current User

GET

```http
/api/auth/me
```

---

# 3. Passenger APIs

## 3.1 Search Flights

GET

```http
/api/flight/search
```

Parameters

```text
departureCityId
arrivalCityId
flightDate
cabinClass
```

---

## 3.2 Flight Detail

GET

```http
/api/flight/detail
```

Parameters

```text
flightId
segmentId
```

---

## 3.3 Available Segments

GET

```http
/api/flight/segments
```

Parameters

```text
flightId
```

---

## 3.4 Create Ticket

POST

```http
/api/ticket/create
```

Request

```json
{
  "userId": 1,
  "flightId": 1,
  "segmentId": 1,
  "cabinClass": "ECONOMY",
  "passengerName": "",
  "passengerIdNumber": "",
  "mealId": 1
}
```

Response

```json
{
  "ticketId": 10001,
  "orderNo": "ORD202607010001",
  "expiredAt": "2026-07-01T12:15:00"
}
```

---

## 3.5 Pay Ticket

POST

```http
/api/ticket/pay
```

Request

```json
{
  "ticketId": 10001
}
```

---

## 3.6 Ticket Detail

GET

```http
/api/ticket/detail
```

Parameters

```text
ticketId
```

---

## 3.7 My Ticket List

GET

```http
/api/ticket/my
```

Parameters

```text
userId
pageNum
pageSize
ticketStatus
```

---

## 3.8 Refund Ticket

POST

```http
/api/ticket/refund
```

Request

```json
{
  "ticketId": 10001
}
```

---

## 3.9 Apply Change Ticket

POST

```http
/api/ticket/change/apply
```

Request

```json
{
  "ticketId": 10001,
  "targetFlightId": 2,
  "targetSegmentId": 3
}
```

---

## 3.10 Pay Change Ticket

POST

```http
/api/ticket/change/pay
```

Request

```json
{
  "ticketId": 10002
}
```

---

## 3.11 Change History

GET

```http
/api/ticket/change/history
```

Parameters

```text
ticketId
```

---

# 4. Meal APIs

## 4.1 Meal List

GET

```http
/api/meal/list
```

---

## 4.2 Meal Detail

GET

```http
/api/meal/detail
```

Parameters

```text
mealId
```

---

# 5. Member APIs

## 5.1 Member Profile

GET

```http
/api/member/profile
```

---

## 5.2 Member Point History

GET

```http
/api/member/points
```

备注：

当前 ER 图没有积分流水表。

此接口仅返回当前积分与等级。

---

# 6. Admin User APIs

## 6.1 User List

GET

```http
/api/admin/user/list
```

---

## 6.2 User Detail

GET

```http
/api/admin/user/detail
```

Parameters

```text
userId
```

---

# 7. Admin City APIs

## 7.1 City List

GET

```http
/api/admin/city/list
```

---

## 7.2 City Detail

GET

```http
/api/admin/city/detail
```

---

## 7.3 Add City

POST

```http
/api/admin/city/add
```

---

## 7.4 Update City

POST

```http
/api/admin/city/update
```

---

## 7.5 Disable City

POST

```http
/api/admin/city/disable
```

---

# 8. Admin Airport APIs

## 8.1 Airport List

GET

```http
/api/admin/airport/list
```

---

## 8.2 Airport Detail

GET

```http
/api/admin/airport/detail
```

---

## 8.3 Add Airport

POST

```http
/api/admin/airport/add
```

---

## 8.4 Update Airport

POST

```http
/api/admin/airport/update
```

---

## 8.5 Disable Airport

POST

```http
/api/admin/airport/disable
```

---

# 9. Admin Aircraft APIs

## 9.1 Aircraft List

GET

```http
/api/admin/aircraft/list
```

---

## 9.2 Aircraft Detail

GET

```http
/api/admin/aircraft/detail
```

---

## 9.3 Add Aircraft

POST

```http
/api/admin/aircraft/add
```

---

## 9.4 Update Aircraft

POST

```http
/api/admin/aircraft/update
```

---

## 9.5 Disable Aircraft

POST

```http
/api/admin/aircraft/disable
```

---

# 10. Admin Flight APIs

## 10.1 Flight List

GET

```http
/api/admin/flight/list
```

---

## 10.2 Flight Detail

GET

```http
/api/admin/flight/detail
```

---

## 10.3 Add Flight

POST

```http
/api/admin/flight/add
```

---

## 10.4 Update Flight

POST

```http
/api/admin/flight/update
```

---

## 10.5 Disable Flight

POST

```http
/api/admin/flight/disable
```

---

# 11. Admin Flight Segment APIs

## 11.1 Segment List

GET

```http
/api/admin/segment/list
```

---

## 11.2 Segment Detail

GET

```http
/api/admin/segment/detail
```

---

## 11.3 Add Segment

POST

```http
/api/admin/segment/add
```

---

## 11.4 Update Segment

POST

```http
/api/admin/segment/update
```

---

## 11.5 Disable Segment

POST

```http
/api/admin/segment/disable
```

---

# 12. Admin Meal APIs

## 12.1 Meal List

GET

```http
/api/admin/meal/list
```

---

## 12.2 Meal Detail

GET

```http
/api/admin/meal/detail
```

---

## 12.3 Add Meal

POST

```http
/api/admin/meal/add
```

---

## 12.4 Update Meal

POST

```http
/api/admin/meal/update
```

---

## 12.5 Disable Meal

POST

```http
/api/admin/meal/disable
```

---

# 13. Admin Ticket APIs

## 13.1 Ticket List

GET

```http
/api/admin/ticket/list
```

---

## 13.2 Ticket Detail

GET

```http
/api/admin/ticket/detail
```

---

## 13.3 Refund Record List

GET

```http
/api/admin/refund/list
```

---

## 13.4 Change Ticket Record List

GET

```http
/api/admin/change/list
```

---

# 14. Dashboard APIs

## 14.1 Dashboard Summary

GET

```http
/api/admin/dashboard/summary
```

Response

```json
{
  "userCount": 100,
  "flightCount": 20,
  "ticketCount": 300,
  "vipCount": 12
}
```

---

## 14.2 Sales Statistics

GET

```http
/api/admin/dashboard/statistics
```

---

# 15. Scheduled Job APIs

仅供测试环境调用。

## 15.1 Execute Expired Order Scan

POST

```http
/api/admin/job/expire-order
```

作用：

立即触发：

待支付订单扫描

库存回补

订单状态更新

用于演示支付超时功能。

---

# Total APIs

Authentication:
4

Passenger:
11

Meal:
2

Member:
2

Admin User:
2

Admin City:
5

Admin Airport:
5

Admin Aircraft:
5

Admin Flight:
5

Admin Segment:
5

Admin Meal:
5

Admin Ticket:
4

Dashboard:
2

Job:
1

Total:
58 APIs
