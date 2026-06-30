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
