# 期末现场演示 SQL：用户表、隐私保护、日志、特价票

## 1. 注册后查看 User 表新增用户

先在前端注册一个新乘客账号，然后在 MySQL 客户端运行：

```sql
USE airticket;

SELECT
  UserId,
  LoginAccount,
  UserName,
  UserType,
  PhoneNumber,
  Email,
  Points,
  MemberLevel,
  CreatedAt,
  UpdatedAt
FROM `User`
ORDER BY UserId DESC
LIMIT 10;
```

讲解点：最新注册的账号会出现在最上面，说明注册接口成功写入了 `User` 表。

## 2. 展示密码和身份证隐私保护

```sql
USE airticket;

SELECT
  UserId,
  LoginAccount,
  UserName,
  PasswordHash,
  LEFT(PasswordHash, 7) AS PasswordHashPrefix,
  LENGTH(PasswordHash) AS PasswordHashLength,
  IdNumberDigest,
  LENGTH(IdNumberDigest) AS IdNumberDigestLength
FROM `User`
ORDER BY UserId DESC
LIMIT 10;
```

讲解点：

- `PasswordHash` 是 BCrypt 哈希，通常以 `$2a$10$` 开头。
- `PasswordHashLength` 通常是 `60`，不是原始密码长度。
- `IdNumberDigestLength` 是 `64`，说明身份证号保存为 SHA-256 摘要。
- 数据库里看不到明文密码，也看不到明文身份证号。

也可以运行这个对比 SQL：

```sql
USE airticket;

SELECT
  LoginAccount,
  CASE
    WHEN PasswordHash LIKE '$2a$%' OR PasswordHash LIKE '$2b$%' OR PasswordHash LIKE '$2y$%'
    THEN 'BCrypt password hash'
    ELSE 'NOT BCrypt'
  END AS PasswordStorage,
  CASE
    WHEN CHAR_LENGTH(IdNumberDigest) = 64
    THEN 'SHA-256 digest'
    ELSE 'NOT SHA-256 digest'
  END AS IdNumberStorage
FROM `User`
ORDER BY UserId DESC
LIMIT 10;
```

## 3. 查看后端日志

后端运行后，日志文件位置：

```text
backend/logs/airticket-backend.log
```

PowerShell 查看最近日志：

```powershell
Get-Content .\backend\logs\airticket-backend.log -Tail 80
```

PowerShell 持续观察日志：

```powershell
Get-Content .\backend\logs\airticket-backend.log -Wait
```

讲解点：日志记录注册、登录、下单、支付、退款、改签、航班启停、航段保存和过期订单扫描，但不会记录明文密码或明文身份证号。

## 4. 展示特价票航段和订单结算

如果是旧数据库，不确定是否已经有特价票字段，先运行一次：

```sql
USE airticket;

SET @add_is_special_offer = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE FlightSegment ADD COLUMN IsSpecialOffer BOOLEAN NOT NULL DEFAULT FALSE AFTER EconomyPrice',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'FlightSegment'
    AND COLUMN_NAME = 'IsSpecialOffer'
);
PREPARE add_is_special_offer_stmt FROM @add_is_special_offer;
EXECUTE add_is_special_offer_stmt;
DEALLOCATE PREPARE add_is_special_offer_stmt;
```

查看哪些航段是特价票：

```sql
USE airticket;

SELECT
  SegmentId,
  FlightId,
  OriginAirportCode,
  DestinationAirportCode,
  IsSpecialOffer,
  EconomyPrice AS OriginalEconomyPrice,
  ROUND(EconomyPrice * 0.5, 2) AS SpecialEconomyPrice,
  FirstClassPrice AS OriginalFirstClassPrice,
  ROUND(FirstClassPrice * 0.5, 2) AS SpecialFirstClassPrice
FROM FlightSegment
WHERE IsSpecialOffer = TRUE
ORDER BY SegmentId
LIMIT 10;
```

创建特价票订单后查看 `TicketSale`：

```sql
USE airticket;

SELECT
  t.TicketId,
  t.OrderNo,
  t.UserId,
  t.SegmentId,
  s.IsSpecialOffer,
  t.CabinClass,
  t.TicketStatus,
  t.PriceAmount,
  t.PaymentAmount,
  u.MemberLevel
FROM TicketSale t
JOIN FlightSegment s ON t.SegmentId = s.SegmentId
JOIN `User` u ON t.UserId = u.UserId
ORDER BY t.TicketId DESC
LIMIT 10;
```

讲解点：特价票航段 `IsSpecialOffer = 1`。订单 `PriceAmount` 是五折后的票价；如果用户是 VIP，`PaymentAmount = PriceAmount * 0.9`。
