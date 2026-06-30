USE airticket;

SELECT '1. Latest users after registration' AS DemoStep;

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

SELECT '2. Password hash and ID-number digest privacy check' AS DemoStep;

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

SELECT '3. Special ticket settlement check' AS DemoStep;

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
