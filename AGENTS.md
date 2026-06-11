# AGENTS.md

## Project Overview

本项目为大学数据库课程设计《航空票务系统》。

目标：

* 数据库设计规范（3NF）
* 核心业务闭环完整
* 本地稳定运行
* 演示过程零故障
* 获得课程设计高分

项目不是商业级航空平台。

任何实现优先考虑：

1. 数据一致性
2. 演示稳定性
3. 开发效率
4. 代码优雅性

---

## Fixed Technology Stack

### Backend

* Java 17
* Spring Boot 2.7.x
* Spring Data JPA
* Maven

架构：

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
MySQL
```

禁止跳层调用。

Controller 不允许直接访问 Repository。

---

### Database

* MySQL 9.6
* Port 3306

数据库结构必须严格遵循 ER 图。

禁止：

* 新增核心业务表
* 删除 ER 图中的字段
* 修改主外键关系

---

### Communication

Backend Port:

```text
8080
```

Communication:

```text
HTTP + JSON
```

Query Operations:

```text
GET
```

State Changing Operations:

```text
POST
```

---

## Core Business Rules

### User Roles

Only two roles:

```text
PASSENGER
ADMIN
```

### Member Levels

Only two levels:

```text
NORMAL
VIP
```

### VIP Rule

Upgrade Threshold:

```text
1000 Points
```

Ticket Reward:

```text
100 Points Per Ticket
```

VIP Discount:

```text
0.9
```

Database stores:

```text
Original Price
```

Settlement:

```text
PaymentAmount = PriceAmount × 0.9
```

for VIP users.

---

## Ticket Status

Allowed values:

```text
PENDING_PAYMENT
PAID
EXPIRED
REFUND_SUCCESS
CHANGE_SUCCESS
```

Do not invent additional statuses.

---

## Flight Status

Allowed values:

```text
NORMAL
DELAYED
CANCELLED
COMPLETED
DISABLED
```

Do not invent additional statuses.

---

## FlightSegment Rules

FlightSegment is the smallest sellable unit.

Example:

Flight:

A → B → C

Sellable Segments:

A → B
B → C
A → C

Ticket can only bind:

ONE Segment

Never multiple segments.

---

## Inventory Rules

Inventory belongs to:

```text
FlightSegment
```

Each segment has independent inventory.

Fields:

```text
FirstClassRemainingSeats
EconomyRemainingSeats
```

---

## Concurrency Rules

Before creating order:

```sql
SELECT ...
FOR UPDATE
```

must be used.

Inventory deduction and order creation must be inside one transaction.

Prevent overselling.

---

## Payment Timeout Rules

Payment Window:

```text
15 minutes
```

After timeout:

1. Order → EXPIRED
2. Inventory restored

Implemented by scheduled task.

---

## Refund Rules

Any paid ticket:

```text
Can Refund Anytime
```

Refund:

1. Update status
2. Restore inventory
3. Rollback points

All in one transaction.

---

## Change Ticket Rules

Support:

```text
Cross Flight Change
```

If price difference exists:

1. Create new pending order
2. 15-minute payment window
3. Pay difference
4. Old ticket invalid
5. New ticket active

Tracking:

```text
OriginalTicketId
```

points to previous ticket.

Chain structure:

A → B → C

Not:

A → C

---

## Security Rules

Password:

```text
BCrypt
```

Identity Number:

```text
SHA-256 Digest
```

Never store plaintext.

---

## Coding Rules

### Naming

Use:

```text
camelCase
```

for Java fields.

Database names must follow ER diagram.

### DTO

DTO fields must match API fields.

Avoid unnecessary field conversion.

### Response Format

All APIs return:

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-07-01T12:00:00"
}
```

---

## Database Change Rules

Before changing schema:

Must verify:

1. ER diagram compatibility
2. Existing API compatibility
3. Existing test data compatibility

Never change schema casually.

---

## Seed Data Requirements

Required:

* 1 Admin
* 2 Passenger

Passenger A:

```text
900 Points
```

Passenger B:

```text
0 Points
```

Meal Options:

* Normal Meal
* Halal Meal
* Vegetarian Meal

Minimum:

```text
20 Flights
```

Date Range:

```text
2026-06-28
~
2026-07-04
```

Every city pair should have available search results.

---

## Demonstration Requirements

Must successfully demonstrate:

### Admin

* Create Flight
* Query Flight
* Update Flight
* Disable Flight

### Passenger

* Register
* Login
* Search Flight
* Create Order
* Pay
* View Order

### Bonus Demonstration

User A:

900 Points

Buy First Ticket:

```text
900 → 1000
```

Auto Upgrade:

```text
VIP
```

Buy Second Ticket:

```text
9 Discount Applied
```

Must be visible in UI.

---

## Prohibited Actions

Do not:

* Introduce Docker
* Introduce Microservices
* Introduce MQ
* Introduce Redis
* Introduce OAuth
* Introduce Third-Party Payment SDK

Keep implementation simple.

Project goal is:

Stable Demo > Enterprise Complexity

---

## Completion Checklist

Before task completion verify:

* Project builds successfully
* No compilation errors
* API names unchanged
* Database fields unchanged
* Business rules unchanged
* Seed data still valid
* Demo workflow still executable
