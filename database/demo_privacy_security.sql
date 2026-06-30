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
