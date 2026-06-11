# docs/transaction-spec.md

# 1. Overview

本文档定义航空票务系统所有事务边界、锁机制、库存控制规则、状态流转规则以及定时任务行为。

目标：

* 防止超售
* 防止库存丢失
* 防止订单状态错乱
* 保证支付、退票、改签的一致性
* 保证演示过程稳定

数据库：

```text
MySQL 9.6
```

事务隔离级别：

```text
READ COMMITTED
```

对于库存相关操作：

必须使用

```sql
SELECT ... FOR UPDATE
```

实现行级锁。

---

# 2. Inventory Model

## 2.1 Inventory Location

库存位于：

```text
FlightSegment
```

字段：

```text
FirstClassRemainingSeats
EconomyRemainingSeats
```

每个可售区间独立维护库存。

示例：

```text
Flight MU1001

A -> B
B -> C
A -> C
```

库存互不影响：

```text
A->B : 20

B->C : 20

A->C : 20
```

购买 A->C

不会影响

```text
A->B

B->C
```

库存。

---

# 3. Order Lifecycle

订单状态：

```text
PENDING_PAYMENT
PAID
EXPIRED
REFUND_SUCCESS
CHANGE_SUCCESS
```

状态流转：

```text
PENDING_PAYMENT
        |
        |
        v
      PAID
      /  \
     /    \
    v      v

REFUND_SUCCESS
CHANGE_SUCCESS
```

超时：

```text
PENDING_PAYMENT
       |
       v
    EXPIRED
```

EXPIRED 为终态。

REFUND_SUCCESS 为终态。

CHANGE_SUCCESS 为终态。

---

# 4. Create Ticket Transaction

## API

```http
POST /api/ticket/create
```

## Purpose

创建订单

锁定库存

进入支付窗口

## Transaction Boundary

必须在一个事务中完成：

```text
BEGIN

锁定航段库存

校验库存

扣减库存

生成订单

写入餐食关联

提交事务

COMMIT
```

---

## Step 1

锁定目标航段

```sql
SELECT *
FROM FlightSegment
WHERE SegmentId = ?
FOR UPDATE;
```

目的：

防止多个用户同时购买最后一张票。

---

## Step 2

检查库存

经济舱：

```text
EconomyRemainingSeats > 0
```

头等舱：

```text
FirstClassRemainingSeats > 0
```

否则：

```text
INSUFFICIENT_SEATS
```

事务回滚。

---

## Step 3

扣减库存

经济舱：

```sql
UPDATE FlightSegment
SET EconomyRemainingSeats =
EconomyRemainingSeats - 1
WHERE SegmentId = ?;
```

头等舱：

```sql
UPDATE FlightSegment
SET FirstClassRemainingSeats =
FirstClassRemainingSeats - 1
WHERE SegmentId = ?;
```

---

## Step 4

生成订单

状态：

```text
PENDING_PAYMENT
```

写入：

```text
BookedAt
ExpiredAt
```

其中：

```text
ExpiredAt =
BookedAt + 15分钟
```

---

## Step 5

写入餐食选择

MealReservation

---

## Commit

```text
COMMIT
```

---

# 5. Payment Transaction

## API

```http
POST /api/ticket/pay
```

## Purpose

支付订单

确认出票

发放积分

升级会员

---

## Transaction Boundary

```text
BEGIN

校验订单

校验超时

更新订单状态

发放积分

升级会员

COMMIT
```

---

## Step 1

锁定订单

```sql
SELECT *
FROM TicketSale
WHERE TicketId = ?
FOR UPDATE;
```

---

## Step 2

检查状态

必须：

```text
PENDING_PAYMENT
```

否则：

```text
INVALID_ORDER_STATUS
```

---

## Step 3

检查超时

```text
NOW() <= ExpiredAt
```

否则：

```text
ORDER_EXPIRED
```

---

## Step 4

修改状态

```text
PAID
```

更新：

```text
PaidAt
IssuedAt
```

---

## Step 5

积分发放

规则：

```text
+100 points
```

SQL：

```sql
UPDATE User
SET Points = Points + 100
WHERE UserId = ?;
```

---

## Step 6

VIP升级

升级阈值：

```text
1000
```

判断：

```text
Points >= 1000
```

更新：

```text
MemberLevel = VIP
```

---

## Commit

```text
COMMIT
```

---

# 6. Refund Transaction

## API

```http
POST /api/ticket/refund
```

## Purpose

退票

恢复库存

回退积分

---

## Transaction Boundary

```text
BEGIN

锁定订单

恢复库存

回退积分

更新状态

COMMIT
```

---

## Step 1

锁定订单

```sql
SELECT *
FROM TicketSale
WHERE TicketId = ?
FOR UPDATE;
```

---

## Step 2

状态校验

必须：

```text
PAID
```

否则拒绝退票。

---

## Step 3

锁定航段

```sql
SELECT *
FROM FlightSegment
WHERE SegmentId = ?
FOR UPDATE;
```

---

## Step 4

恢复库存

经济舱：

```sql
UPDATE FlightSegment
SET EconomyRemainingSeats =
EconomyRemainingSeats + 1;
```

头等舱：

```sql
UPDATE FlightSegment
SET FirstClassRemainingSeats =
FirstClassRemainingSeats + 1;
```

---

## Step 5

积分回退

```sql
UPDATE User
SET Points = Points - 100
WHERE UserId = ?;
```

---

## Step 6

重新判断VIP

如果：

```text
Points < 1000
```

则：

```text
MemberLevel = NORMAL
```

---

## Step 7

订单状态

```text
REFUND_SUCCESS
```

写入：

```text
RefundedAt
```

---

# 7. Change Ticket Transaction

## API

```http
POST /api/ticket/change/apply
```

## Purpose

生成改签订单

---

## Transaction Boundary

```text
BEGIN

校验原票

锁定新航段

扣减新库存

生成新票

COMMIT
```

---

## Step 1

原票状态必须：

```text
PAID
```

---

## Step 2

锁定目标航段

```sql
SELECT *
FROM FlightSegment
WHERE SegmentId = ?
FOR UPDATE;
```

---

## Step 3

检查库存

库存不足：

```text
CHANGE_FAILED
```

---

## Step 4

扣减库存

同购票逻辑。

---

## Step 5

生成新票

状态：

```text
PENDING_PAYMENT
```

字段：

```text
OriginalTicketId
```

指向：

```text
上一张票
```

不是最初票。

---

## Step 6

计算差价

```text
Difference =
NewPrice - OldPrice
```

若：

```text
Difference < 0
```

统一按：

```text
0
```

处理。

课程设计不演示退款差价。

---

# 8. Change Payment Transaction

## API

```http
POST /api/ticket/change/pay
```

## Purpose

完成改签

---

## Transaction Boundary

```text
BEGIN

锁定新票

锁定旧票

完成支付

更新状态

COMMIT
```

---

## Step 1

新票：

```text
PENDING_PAYMENT
```

---

## Step 2

旧票：

```text
PAID
```

---

## Step 3

新票状态

```text
PAID
```

---

## Step 4

旧票状态

```text
CHANGE_SUCCESS
```

---

## Commit

```text
COMMIT
```

---

# 9. Expired Order Scheduler

## Schedule

每分钟执行一次。

Spring：

```java
@Scheduled(fixedDelay = 60000)
```

---

## Query

```sql
SELECT *
FROM TicketSale
WHERE TicketStatus='PENDING_PAYMENT'
AND ExpiredAt < NOW();
```

---

## Transaction Boundary

每张订单单独事务。

```text
BEGIN

锁定订单

锁定库存

恢复库存

更新状态

COMMIT
```

---

## Step 1

锁定订单

```sql
SELECT *
FROM TicketSale
WHERE TicketId = ?
FOR UPDATE;
```

---

## Step 2

再次确认状态

必须：

```text
PENDING_PAYMENT
```

否则跳过。

---

## Step 3

恢复库存

根据舱位：

```text
Economy
```

或：

```text
FirstClass
```

回补库存。

---

## Step 4

状态修改

```text
EXPIRED
```

写入：

```text
ExpiredAt
```

---

# 10. Locking Strategy

## Must Use FOR UPDATE

以下场景必须使用：

### Create Ticket

```sql
FlightSegment
FOR UPDATE
```

### Pay Ticket

```sql
TicketSale
FOR UPDATE
```

### Refund

```sql
TicketSale
FOR UPDATE

FlightSegment
FOR UPDATE
```

### Change Ticket

```sql
FlightSegment
FOR UPDATE
```

### Expired Scheduler

```sql
TicketSale
FOR UPDATE

FlightSegment
FOR UPDATE
```

---

# 11. Deadlock Prevention

统一锁顺序：

```text
TicketSale
    ↓
FlightSegment
    ↓
User
```

所有事务必须遵守同样顺序。

禁止：

```text
Transaction A:
FlightSegment -> User

Transaction B:
User -> FlightSegment
```

否则可能产生死锁。

---

# 12. Consistency Rules

任何情况下：

库存变化

必须伴随：

```text
订单状态变化
```

同时提交。

禁止：

库存已扣减

但订单未生成。

禁止：

订单已支付

但积分未发放。

禁止：

退票成功

但库存未恢复。

所有业务必须满足：

```text
Atomicity
Consistency
Isolation
Durability
```

即 ACID 原则。
