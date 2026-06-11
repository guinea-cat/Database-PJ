# docs/sql-schema-spec.md

本文档定义数据库建表、主外键、索引、字段类型、约束与默认值的工程化规范。它的目标是让数据库队友可以直接按文档建表，不需要再猜字段、猜状态、猜关联。

本项目必须严格遵循已确认 ER 图，不擅自新增破坏结构的表，也不把业务逻辑拆散到无关实体中。数据库设计要服务于后端事务和前端演示，重点是清晰、稳定、可追溯。

## 1. 命名规范

- 表名建议使用单数或统一风格，不要混用。
- 主键字段建议固定为 `xxxId` 或业务唯一编码字段。
- 外键字段名与被引用主键保持语义一致。
- 时间字段统一使用 `DATETIME`。
- 金额字段统一使用 `DECIMAL(10,2)` 或更高精度。
- 布尔状态建议使用 `TINYINT(1)` 或枚举字符串，但整套系统必须保持一致。
- 不要把业务含义强的字段写成模糊缩写。

## 2. 表设计清单

### 2.1 User

核心字段建议包括：
- userId，主键
- loginAccount，唯一
- passwordHash，非空
- userName，非空
- userType，非空
- memberLevel，非空
- points，非空，默认 0
- phoneNumber，唯一或高选择性索引
- email，唯一或高选择性索引
- idNumberDigest，唯一
- createdAt，非空
- updatedAt，非空
- isEnabled，非空，默认 1
- remark

说明：
密码用 BCrypt 哈希后存储。身份证号不允许明文入库，只能存摘要。

### 2.2 City

核心字段建议包括：
- cityId，主键
- cityCode，唯一
- cityName，非空
- countryName，非空
- isEnabled，默认 1
- createdAt
- updatedAt
- remark

### 2.3 Airport

核心字段建议包括：
- airportId，主键
- airportCode，唯一
- airportName，非空
- cityId，外键
- isInternational
- isEnabled，默认 1
- createdAt
- updatedAt
- remark

索引建议：`cityId` 建索引。

### 2.4 Aircraft

核心字段建议包括：
- aircraftRegNo，主键或唯一键
- aircraftModel，非空
- manufacturer，非空
- firstClassSeatCount，非空
- economySeatCount，非空
- isEnabled，默认 1
- createdAt
- updatedAt
- remark

### 2.5 Flight

核心字段建议包括：
- flightId，主键
- flightNumber，非空
- flightDate，非空
- aircraftRegNo，外键
- departureAirportCode，外键语义字段
- arrivalAirportCode，外键语义字段
- flightStatus，非空
- isEnabled，默认 1
- createdAt
- updatedAt
- remark

建议建立组合唯一约束：`flightNumber + flightDate`。

说明：
若存在票务记录，不允许物理删除，只允许停用。

### 2.6 FlightSegment

这是最重要的库存表。

核心字段建议包括：
- segmentId，主键
- flightId，外键
- originStopNo，非空
- destinationStopNo，非空
- originAirportCode，非空
- destinationAirportCode，非空
- plannedDepartureTime，非空或按 ER 图要求
- plannedArrivalTime，非空或按 ER 图要求
- actualDepartureTime
- actualArrivalTime
- delayMinutes
- delayReason
- firstClassRemainingSeats，非空，默认 0
- economyRemainingSeats，非空，默认 0
- firstClassPriceAmount，非空
- economyPriceAmount，非空
- isEnabled，默认 1
- createdAt
- updatedAt
- remark

建议索引：
- `flightId`
- `originAirportCode`
- `destinationAirportCode`
- `originStopNo + destinationStopNo`

建议约束：
- 同一航班下 `originStopNo < destinationStopNo`
- 同一航班下同一可售区间不能重复
- 库存不得为负数

### 2.7 TicketSale

核心字段建议包括：
- ticketId，主键
- orderNo，唯一
- userId，外键
- flightId，外键
- segmentId，外键
- cabinClass，非空
- ticketStatus，非空
- passengerName，非空
- passengerIdNumberDigest，非空
- priceAmount，非空
- paymentAmount，非空
- originalTicketId，自引用外键
- bookedAt，非空
- paidAt
- issuedAt
- expiredAt
- refundedAt
- changedAt
- changeReason
- remark

建议索引：
- `userId`
- `flightId`
- `segmentId`
- `originalTicketId`
- `ticketStatus`
- `expiredAt`

### 2.8 MealOption

核心字段建议包括：
- mealId，主键
- mealName，唯一
- mealType
- isAvailable，默认 1
- createdAt
- updatedAt
- remark

### 2.9 MealReservation

这是 TicketSale 与 MealOption 的关联表。

核心字段建议包括：
- mealReservationId，主键
- ticketId，外键
- mealId，外键
- createdAt
- remark

建议唯一约束：`ticketId + mealId`，防止重复绑定。

## 3. 外键关系

建议保持以下关系：

- Airport.cityId → City.cityId
- Flight.aircraftRegNo → Aircraft.aircraftRegNo
- Flight.departureAirportCode → Airport.airportCode
- Flight.arrivalAirportCode → Airport.airportCode
- FlightSegment.flightId → Flight.flightId
- FlightSegment.originAirportCode → Airport.airportCode
- FlightSegment.destinationAirportCode → Airport.airportCode
- TicketSale.userId → User.userId
- TicketSale.flightId → Flight.flightId
- TicketSale.segmentId → FlightSegment.segmentId
- TicketSale.originalTicketId → TicketSale.ticketId
- MealReservation.ticketId → TicketSale.ticketId
- MealReservation.mealId → MealOption.mealId

删除策略建议：
- 主数据优先逻辑删除或停用。
- 已被业务事实表引用的数据禁止物理删除。

## 4. 索引设计原则

索引设计要服务于查询、下单、支付和演示。

必须重点考虑以下查询：
- 按城市查航班
- 按日期查航班
- 按航班号查航段
- 按用户查订单
- 按订单号查详情
- 按过期时间扫描待支付订单
- 按状态统计订单数量

不要过度建索引。课程设计中只要覆盖核心查询即可。

## 5. 约束设计原则

### 5.1 实体完整性

所有主键非空且唯一。

### 5.2 参照完整性

所有外键必须能找到对应主表记录。

### 5.3 业务完整性

- 余票不得为负。
- 已支付订单不可重复支付。
- 已过期订单不可再支付。
- 已退款订单不可再次退票。
- 已改签成功的旧票不可再次改签。
- 航班已停用后不可继续售卖。
- 管理员不可修改用户积分。

## 6. 默认值建议

- `points = 0`
- `isEnabled = 1`
- `firstClassRemainingSeats` 与 `economyRemainingSeats` 由初始化数据决定
- `ticketStatus = PENDING_PAYMENT`
- `memberLevel = NORMAL`
- `userType = PASSENGER`（注册默认）

## 7. 初始化 SQL 输出要求

数据库队友最终应提供以下 SQL 文件：

1. `schema.sql`：建表语句
2. `seed_data.sql`：初始化数据
3. `indexes.sql`：若索引未写入建表语句，可单独提供
4. `truncate.sql`：用于重置演示环境

若条件允许，建议再提供一个 `reset_all.sql`，用于一键清库后重建。

## 8. 与后端对接时必须固定的字段

后端与数据库协作时，以下字段必须提前统一：
- `loginAccount`
- `passwordHash`
- `idNumberDigest`
- `memberLevel`
- `userType`
- `flightStatus`
- `ticketStatus`
- `cabinClass`
- `priceAmount`
- `paymentAmount`
- `firstClassRemainingSeats`
- `economyRemainingSeats`
- `originalTicketId`
- `expiredAt`

这些字段一旦落库，后端 DTO 与接口都要同步保持一致。
