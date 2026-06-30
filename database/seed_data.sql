SET FOREIGN_KEY_CHECKS = 0;
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

INSERT INTO City (CityId, CityName, CityCode, Country) VALUES
(1, '北京', 'BJS', '中国'),
(2, '上海', 'SHA', '中国'),
(3, '广州', 'CAN', '中国'),
(4, '深圳', 'SZX', '中国'),
(5, '成都', 'CTU', '中国')
ON DUPLICATE KEY UPDATE CityName = VALUES(CityName), CityCode = VALUES(CityCode), Country = VALUES(Country);

INSERT INTO Airport (AirportCode, AirportName, CityId, IsInternational) VALUES
('PEK', '北京首都国际机场', 1, TRUE),
('PKX', '北京大兴国际机场', 1, TRUE),
('SHA', '上海虹桥国际机场', 2, TRUE),
('PVG', '上海浦东国际机场', 2, TRUE),
('CAN', '广州白云国际机场', 3, TRUE),
('SZX', '深圳宝安国际机场', 4, TRUE),
('TFU', '成都天府国际机场', 5, TRUE),
('CTU', '成都双流国际机场', 5, TRUE)
ON DUPLICATE KEY UPDATE AirportName = VALUES(AirportName), CityId = VALUES(CityId), IsInternational = VALUES(IsInternational);

INSERT INTO Aircraft (AircraftRegNo, AircraftType, Manufacturer, TotalFirstClassSeats, TotalEconomySeats, Status, Remark) VALUES
('B-1001', 'A320', 'Airbus', 8, 150, 'NORMAL', '演示飞机1'),
('B-1002', 'A321', 'Airbus', 12, 170, 'NORMAL', '演示飞机2'),
('B-1003', 'B737-800', 'Boeing', 8, 160, 'NORMAL', '演示飞机3'),
('B-1004', 'B787-9', 'Boeing', 16, 180, 'NORMAL', '演示飞机4'),
('B-1005', 'C919', 'COMAC', 8, 156, 'NORMAL', '演示飞机5'),
('B-1006', 'ARJ21', 'COMAC', 4, 90, 'NORMAL', '演示飞机6')
ON DUPLICATE KEY UPDATE AircraftType = VALUES(AircraftType), Manufacturer = VALUES(Manufacturer), TotalFirstClassSeats = VALUES(TotalFirstClassSeats), TotalEconomySeats = VALUES(TotalEconomySeats), Status = VALUES(Status), Remark = VALUES(Remark);

INSERT INTO `User` (UserId, LoginAccount, UserName, IdNumberDigest, PasswordHash, UserType, PhoneNumber, Email, Points, MemberLevel, CreatedAt, UpdatedAt) VALUES
(1, 'admin', '系统管理员', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '$2a$10$dCCOusBIvYH49nHrpWSN8uLY0Eur55J2DO4l4on6a1J6Q4p8UgVLy', 'ADMIN', '13800000000', 'admin@example.com', 0, 'NORMAL', NOW(), NOW()),
(2, 'passengerA', '演示乘客A', 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '$2a$10$59MqfMIxXrZ3ZXSXq6qZB.TuKJ3Q.6OgvFd4v8td6AVhQv4I4.Zpi', 'PASSENGER', '13800000001', 'passengerA@example.com', 900, 'NORMAL', NOW(), NOW()),
(3, 'passengerB', '演示乘客B', 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', '$2a$10$59MqfMIxXrZ3ZXSXq6qZB.TuKJ3Q.6OgvFd4v8td6AVhQv4I4.Zpi', 'PASSENGER', '13800000002', 'passengerB@example.com', 0, 'NORMAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE UserName = VALUES(UserName), PasswordHash = VALUES(PasswordHash), UserType = VALUES(UserType), Points = VALUES(Points), MemberLevel = VALUES(MemberLevel), UpdatedAt = NOW();

INSERT INTO MealOption (MealId, MealName, MealType, IsAvailable, Description) VALUES
(1, '普通餐', 'NORMAL', TRUE, '标准航空餐'),
(2, '清真餐', 'HALAL', TRUE, '清真餐食'),
(3, '素食餐', 'VEGETARIAN', TRUE, '素食餐食')
ON DUPLICATE KEY UPDATE MealName = VALUES(MealName), MealType = VALUES(MealType), IsAvailable = VALUES(IsAvailable), Description = VALUES(Description);

INSERT INTO Flight (FlightId, FlightNumber, FlightDate, AircraftRegNo, FlightStatus, DepartureAirportCode, ArrivalAirportCode, Remark) VALUES
(1, 'MU1001', '2026-06-28', 'B-1001', 'NORMAL', 'PEK', 'SHA', '北京到上海'),
(2, 'MU1002', '2026-06-28', 'B-1002', 'NORMAL', 'PEK', 'CAN', '北京到广州'),
(3, 'MU1003', '2026-06-29', 'B-1003', 'NORMAL', 'PEK', 'SZX', '北京到深圳'),
(4, 'MU1004', '2026-06-29', 'B-1004', 'NORMAL', 'PEK', 'TFU', '北京到成都'),
(5, 'MU1005', '2026-06-30', 'B-1005', 'NORMAL', 'PVG', 'PEK', '上海到北京'),
(6, 'MU1006', '2026-06-30', 'B-1006', 'NORMAL', 'PVG', 'CAN', '上海到广州'),
(7, 'MU1007', '2026-07-01', 'B-1001', 'NORMAL', 'SHA', 'SZX', '上海到深圳'),
(8, 'MU1008', '2026-07-01', 'B-1002', 'NORMAL', 'PVG', 'CTU', '上海到成都'),
(9, 'MU1009', '2026-07-02', 'B-1003', 'NORMAL', 'CAN', 'PEK', '广州到北京'),
(10, 'MU1010', '2026-07-02', 'B-1004', 'NORMAL', 'CAN', 'SHA', '广州到上海'),
(11, 'MU1011', '2026-07-03', 'B-1005', 'NORMAL', 'CAN', 'SZX', '广州到深圳'),
(12, 'MU1012', '2026-07-03', 'B-1006', 'NORMAL', 'CAN', 'TFU', '广州到成都'),
(13, 'MU1013', '2026-07-04', 'B-1001', 'NORMAL', 'SZX', 'PEK', '深圳到北京'),
(14, 'MU1014', '2026-07-04', 'B-1002', 'NORMAL', 'SZX', 'PVG', '深圳到上海'),
(15, 'MU1015', '2026-06-28', 'B-1003', 'NORMAL', 'SZX', 'CAN', '深圳到广州'),
(16, 'MU1016', '2026-06-29', 'B-1004', 'NORMAL', 'SZX', 'CTU', '深圳到成都'),
(17, 'MU1017', '2026-06-30', 'B-1005', 'NORMAL', 'TFU', 'PEK', '成都到北京'),
(18, 'MU1018', '2026-07-01', 'B-1006', 'NORMAL', 'CTU', 'SHA', '成都到上海'),
(19, 'MU1019', '2026-07-02', 'B-1001', 'NORMAL', 'TFU', 'CAN', '成都到广州'),
(20, 'MU1020', '2026-07-03', 'B-1002', 'NORMAL', 'CTU', 'SZX', '成都到深圳'),
(21, 'MU2001', '2026-07-01', 'B-1004', 'NORMAL', 'PEK', 'TFU', '北京经上海到成都'),
(22, 'MU2002', '2026-07-02', 'B-1005', 'NORMAL', 'CAN', 'PEK', '广州经深圳到北京')
ON DUPLICATE KEY UPDATE FlightStatus = VALUES(FlightStatus), Remark = VALUES(Remark);

INSERT INTO FlightSegment (FlightId, OriginStopNo, DestinationStopNo, OriginAirportCode, DestinationAirportCode, PlannedDepartureTime, PlannedArrivalTime, FirstClassRemainingSeats, EconomyRemainingSeats, FirstClassPrice, EconomyPrice, Remark)
SELECT FlightId, 1, 2, DepartureAirportCode, ArrivalAirportCode,
       TIMESTAMP(FlightDate, '08:00:00'), TIMESTAMP(FlightDate, '10:00:00'),
       8, 50, 1800.00, 1000.00, '直达可售区间'
FROM Flight
WHERE FlightId BETWEEN 1 AND 20
ON DUPLICATE KEY UPDATE FirstClassRemainingSeats = VALUES(FirstClassRemainingSeats), EconomyRemainingSeats = VALUES(EconomyRemainingSeats), FirstClassPrice = VALUES(FirstClassPrice), EconomyPrice = VALUES(EconomyPrice), Remark = VALUES(Remark);

INSERT INTO FlightSegment (FlightId, OriginStopNo, DestinationStopNo, OriginAirportCode, DestinationAirportCode, PlannedDepartureTime, PlannedArrivalTime, FirstClassRemainingSeats, EconomyRemainingSeats, FirstClassPrice, EconomyPrice, Remark) VALUES
(21, 1, 2, 'PEK', 'SHA', '2026-07-01 08:00:00', '2026-07-01 10:00:00', 5, 50, 1500.00, 800.00, '北京到上海区间'),
(21, 2, 3, 'SHA', 'TFU', '2026-07-01 11:30:00', '2026-07-01 14:00:00', 8, 50, 1700.00, 900.00, '上海到成都区间'),
(21, 1, 3, 'PEK', 'TFU', '2026-07-01 08:00:00', '2026-07-01 14:00:00', 8, 50, 2600.00, 1400.00, '北京到成都全程区间'),
(22, 1, 2, 'CAN', 'SZX', '2026-07-02 08:00:00', '2026-07-02 09:00:00', 8, 50, 900.00, 500.00, '广州到深圳区间'),
(22, 2, 3, 'SZX', 'PEK', '2026-07-02 10:30:00', '2026-07-02 13:30:00', 8, 50, 1800.00, 1000.00, '深圳到北京区间'),
(22, 1, 3, 'CAN', 'PEK', '2026-07-02 08:00:00', '2026-07-02 13:30:00', 8, 50, 2600.00, 1400.00, '广州到北京全程区间')
ON DUPLICATE KEY UPDATE FirstClassRemainingSeats = VALUES(FirstClassRemainingSeats), EconomyRemainingSeats = VALUES(EconomyRemainingSeats), FirstClassPrice = VALUES(FirstClassPrice), EconomyPrice = VALUES(EconomyPrice), Remark = VALUES(Remark);

INSERT INTO Flight (FlightId, FlightNumber, FlightDate, AircraftRegNo, FlightStatus, DepartureAirportCode, ArrivalAirportCode, Remark) VALUES
(31, 'CA3001', '2026-06-28', 'B-1001', 'NORMAL', 'PEK', 'PVG', '补充航线 北京首都到上海浦东'),
(32, 'CA3002', '2026-06-28', 'B-1002', 'NORMAL', 'PVG', 'PEK', '补充航线 上海浦东到北京首都'),
(33, 'CA3003', '2026-06-28', 'B-1003', 'NORMAL', 'CAN', 'SZX', '补充航线 广州到深圳'),
(34, 'CA3004', '2026-06-28', 'B-1004', 'NORMAL', 'SZX', 'CAN', '补充航线 深圳到广州'),
(35, 'CA3005', '2026-06-28', 'B-1005', 'NORMAL', 'PEK', 'TFU', '补充航线 北京首都到成都天府'),
(36, 'CA3006', '2026-06-28', 'B-1006', 'NORMAL', 'TFU', 'PEK', '补充航线 成都天府到北京首都'),
(37, 'CA3007', '2026-06-29', 'B-1001', 'NORMAL', 'PEK', 'CAN', '补充航线 北京首都到广州'),
(38, 'CA3008', '2026-06-29', 'B-1002', 'NORMAL', 'CAN', 'PEK', '补充航线 广州到北京首都'),
(39, 'CA3009', '2026-06-29', 'B-1003', 'NORMAL', 'SHA', 'SZX', '补充航线 上海虹桥到深圳'),
(40, 'CA3010', '2026-06-29', 'B-1004', 'NORMAL', 'SZX', 'SHA', '补充航线 深圳到上海虹桥'),
(41, 'CA3011', '2026-06-29', 'B-1005', 'NORMAL', 'PVG', 'TFU', '补充航线 上海浦东到成都天府'),
(42, 'CA3012', '2026-06-29', 'B-1006', 'NORMAL', 'TFU', 'PVG', '补充航线 成都天府到上海浦东'),
(43, 'CA3013', '2026-06-30', 'B-1001', 'NORMAL', 'PEK', 'SZX', '补充航线 北京首都到深圳'),
(44, 'CA3014', '2026-06-30', 'B-1002', 'NORMAL', 'SZX', 'PEK', '补充航线 深圳到北京首都'),
(45, 'CA3015', '2026-06-30', 'B-1003', 'NORMAL', 'SHA', 'CAN', '补充航线 上海虹桥到广州'),
(46, 'CA3016', '2026-06-30', 'B-1004', 'NORMAL', 'CAN', 'SHA', '补充航线 广州到上海虹桥'),
(47, 'CA3017', '2026-06-30', 'B-1005', 'NORMAL', 'PKX', 'CTU', '补充航线 北京大兴到成都双流'),
(48, 'CA3018', '2026-06-30', 'B-1006', 'NORMAL', 'CTU', 'PKX', '补充航线 成都双流到北京大兴'),
(49, 'CA3019', '2026-07-01', 'B-1001', 'NORMAL', 'PEK', 'SHA', '补充航线 北京首都到上海虹桥'),
(50, 'CA3020', '2026-07-01', 'B-1002', 'NORMAL', 'SHA', 'PEK', '补充航线 上海虹桥到北京首都'),
(51, 'CA3021', '2026-07-01', 'B-1003', 'NORMAL', 'CAN', 'TFU', '补充航线 广州到成都天府'),
(52, 'CA3022', '2026-07-01', 'B-1004', 'NORMAL', 'TFU', 'CAN', '补充航线 成都天府到广州'),
(53, 'CA3023', '2026-07-01', 'B-1005', 'NORMAL', 'SZX', 'PVG', '补充航线 深圳到上海浦东'),
(54, 'CA3024', '2026-07-01', 'B-1006', 'NORMAL', 'PVG', 'SZX', '补充航线 上海浦东到深圳'),
(55, 'CA3025', '2026-07-02', 'B-1001', 'NORMAL', 'PEK', 'CAN', '补充航线 北京首都到广州'),
(56, 'CA3026', '2026-07-02', 'B-1002', 'NORMAL', 'CAN', 'PEK', '补充航线 广州到北京首都'),
(57, 'CA3027', '2026-07-02', 'B-1003', 'NORMAL', 'SHA', 'TFU', '补充航线 上海虹桥到成都天府'),
(58, 'CA3028', '2026-07-02', 'B-1004', 'NORMAL', 'TFU', 'SHA', '补充航线 成都天府到上海虹桥'),
(59, 'CA3029', '2026-07-02', 'B-1005', 'NORMAL', 'SZX', 'CTU', '补充航线 深圳到成都双流'),
(60, 'CA3030', '2026-07-02', 'B-1006', 'NORMAL', 'CTU', 'SZX', '补充航线 成都双流到深圳'),
(61, 'CA3031', '2026-07-03', 'B-1001', 'NORMAL', 'PEK', 'SZX', '补充航线 北京首都到深圳'),
(62, 'CA3032', '2026-07-03', 'B-1002', 'NORMAL', 'SZX', 'PEK', '补充航线 深圳到北京首都'),
(63, 'CA3033', '2026-07-03', 'B-1003', 'NORMAL', 'PVG', 'CAN', '补充航线 上海浦东到广州'),
(64, 'CA3034', '2026-07-03', 'B-1004', 'NORMAL', 'CAN', 'PVG', '补充航线 广州到上海浦东'),
(65, 'CA3035', '2026-07-03', 'B-1005', 'NORMAL', 'PKX', 'TFU', '补充航线 北京大兴到成都天府'),
(66, 'CA3036', '2026-07-03', 'B-1006', 'NORMAL', 'TFU', 'PKX', '补充航线 成都天府到北京大兴'),
(67, 'CA3037', '2026-07-04', 'B-1001', 'NORMAL', 'PEK', 'PVG', '补充航线 北京首都到上海浦东'),
(68, 'CA3038', '2026-07-04', 'B-1002', 'NORMAL', 'PVG', 'PEK', '补充航线 上海浦东到北京首都'),
(69, 'CA3039', '2026-07-04', 'B-1003', 'NORMAL', 'SHA', 'CAN', '补充航线 上海虹桥到广州'),
(70, 'CA3040', '2026-07-04', 'B-1004', 'NORMAL', 'CAN', 'SHA', '补充航线 广州到上海虹桥'),
(71, 'CA3041', '2026-07-04', 'B-1005', 'NORMAL', 'SZX', 'TFU', '补充航线 深圳到成都天府'),
(72, 'CA3042', '2026-07-04', 'B-1006', 'NORMAL', 'TFU', 'SZX', '补充航线 成都天府到深圳')
ON DUPLICATE KEY UPDATE FlightStatus = VALUES(FlightStatus), Remark = VALUES(Remark);

INSERT INTO FlightSegment (FlightId, OriginStopNo, DestinationStopNo, OriginAirportCode, DestinationAirportCode, PlannedDepartureTime, PlannedArrivalTime, FirstClassRemainingSeats, EconomyRemainingSeats, FirstClassPrice, EconomyPrice, Remark)
SELECT FlightId, 1, 2, DepartureAirportCode, ArrivalAirportCode,
       TIMESTAMP(FlightDate, '09:30:00'), TIMESTAMP(FlightDate, '12:00:00'),
       10, 72, 1880.00, 980.00, '补充直达可售区间'
FROM Flight
WHERE FlightId BETWEEN 31 AND 72
ON DUPLICATE KEY UPDATE FirstClassRemainingSeats = VALUES(FirstClassRemainingSeats), EconomyRemainingSeats = VALUES(EconomyRemainingSeats), FirstClassPrice = VALUES(FirstClassPrice), EconomyPrice = VALUES(EconomyPrice), Remark = VALUES(Remark);
UPDATE FlightSegment
SET IsSpecialOffer = FALSE;

UPDATE FlightSegment
SET IsSpecialOffer = TRUE
WHERE (FlightId = 21 AND OriginStopNo = 1 AND DestinationStopNo = 2)
   OR (FlightId = 49 AND OriginStopNo = 1 AND DestinationStopNo = 2);

DELETE mr
FROM MealReservation mr
JOIN TicketSale t ON mr.TicketId = t.TicketId
WHERE t.FlightId >= 10000
   OR t.SegmentId IN (SELECT SegmentId FROM FlightSegment WHERE FlightId >= 10000);

DELETE FROM TicketSale
WHERE FlightId >= 10000
   OR SegmentId IN (SELECT SegmentId FROM FlightSegment WHERE FlightId >= 10000);

DELETE FROM FlightSegment
WHERE FlightId >= 10000;

DELETE FROM Flight
WHERE FlightId >= 10000;

DROP TEMPORARY TABLE IF EXISTS seed_demo_dates;
CREATE TEMPORARY TABLE seed_demo_dates (
  DateIndex INT PRIMARY KEY,
  FlightDate DATE NOT NULL
);

INSERT INTO seed_demo_dates (DateIndex, FlightDate) VALUES
(0, '2026-06-28'),
(1, '2026-06-29'),
(2, '2026-06-30'),
(3, '2026-07-01'),
(4, '2026-07-02'),
(5, '2026-07-03'),
(6, '2026-07-04');

DROP TEMPORARY TABLE IF EXISTS seed_demo_routes;
CREATE TEMPORARY TABLE seed_demo_routes (
  RouteIndex INT NOT NULL,
  OfferIndex INT NOT NULL,
  FlightNumber VARCHAR(20) NOT NULL,
  IsSpecialOffer BOOLEAN NOT NULL,
  BaseDepartureTime TIME NOT NULL,
  Remark VARCHAR(255) NOT NULL,
  PRIMARY KEY (RouteIndex, OfferIndex)
);

INSERT INTO seed_demo_routes (RouteIndex, OfferIndex, FlightNumber, IsSpecialOffer, BaseDepartureTime, Remark) VALUES
(1, 0, 'DM101', FALSE, '08:00:00', 'demo city coverage normal clockwise'),
(2, 0, 'DM102', FALSE, '09:00:00', 'demo city coverage normal reverse'),
(1, 1, 'DM201', TRUE, '14:00:00', 'demo city coverage special clockwise'),
(2, 1, 'DM202', TRUE, '15:00:00', 'demo city coverage special reverse');

DROP TEMPORARY TABLE IF EXISTS seed_demo_route_stops;
CREATE TEMPORARY TABLE seed_demo_route_stops (
  RouteIndex INT NOT NULL,
  StopNo INT NOT NULL,
  AirportCode VARCHAR(20) NOT NULL,
  PRIMARY KEY (RouteIndex, StopNo)
);

INSERT INTO seed_demo_route_stops (RouteIndex, StopNo, AirportCode) VALUES
(1, 1, 'PEK'),
(1, 2, 'SHA'),
(1, 3, 'CAN'),
(1, 4, 'SZX'),
(1, 5, 'TFU'),
(2, 1, 'TFU'),
(2, 2, 'SZX'),
(2, 3, 'CAN'),
(2, 4, 'SHA'),
(2, 5, 'PEK');

DROP TEMPORARY TABLE IF EXISTS seed_demo_destination_stops;
CREATE TEMPORARY TABLE seed_demo_destination_stops (
  RouteIndex INT NOT NULL,
  StopNo INT NOT NULL,
  AirportCode VARCHAR(20) NOT NULL,
  PRIMARY KEY (RouteIndex, StopNo)
);

INSERT INTO seed_demo_destination_stops (RouteIndex, StopNo, AirportCode)
SELECT RouteIndex, StopNo, AirportCode
FROM seed_demo_route_stops;

INSERT INTO Flight (FlightId, FlightNumber, FlightDate, AircraftRegNo, FlightStatus, DepartureAirportCode, ArrivalAirportCode, Remark)
SELECT
  10000 + d.DateIndex * 100 + r.OfferIndex * 10 + r.RouteIndex AS FlightId,
  r.FlightNumber,
  d.FlightDate,
  CONCAT('B-100', MOD(d.DateIndex + r.OfferIndex + r.RouteIndex, 6) + 1) AS AircraftRegNo,
  'NORMAL' AS FlightStatus,
  CASE r.RouteIndex WHEN 1 THEN 'PEK' ELSE 'TFU' END AS DepartureAirportCode,
  CASE r.RouteIndex WHEN 1 THEN 'TFU' ELSE 'PEK' END AS ArrivalAirportCode,
  r.Remark
FROM seed_demo_dates d
JOIN seed_demo_routes r
WHERE TRUE
  AND r.RouteIndex IN (1, 2)
ON DUPLICATE KEY UPDATE
  AircraftRegNo = VALUES(AircraftRegNo),
  FlightStatus = VALUES(FlightStatus),
  DepartureAirportCode = VALUES(DepartureAirportCode),
  ArrivalAirportCode = VALUES(ArrivalAirportCode),
  Remark = VALUES(Remark);

INSERT INTO FlightSegment (
  FlightId,
  OriginStopNo,
  DestinationStopNo,
  OriginAirportCode,
  DestinationAirportCode,
  PlannedDepartureTime,
  PlannedArrivalTime,
  FirstClassRemainingSeats,
  EconomyRemainingSeats,
  FirstClassPrice,
  EconomyPrice,
  IsSpecialOffer,
  Remark
)
SELECT
  10000 + d.DateIndex * 100 + r.OfferIndex * 10 + r.RouteIndex AS FlightId,
  originStop.StopNo AS OriginStopNo,
  destinationStop.StopNo AS DestinationStopNo,
  originStop.AirportCode AS OriginAirportCode,
  destinationStop.AirportCode AS DestinationAirportCode,
  TIMESTAMP(d.FlightDate, ADDTIME(r.BaseDepartureTime, SEC_TO_TIME((originStop.StopNo - 1) * 5400))) AS PlannedDepartureTime,
  TIMESTAMP(d.FlightDate, ADDTIME(r.BaseDepartureTime, SEC_TO_TIME((destinationStop.StopNo - 1) * 5400))) AS PlannedArrivalTime,
  8 AS FirstClassRemainingSeats,
  80 AS EconomyRemainingSeats,
  1600.00 + (destinationStop.StopNo - originStop.StopNo) * 200 AS FirstClassPrice,
  900.00 + (destinationStop.StopNo - originStop.StopNo) * 120 AS EconomyPrice,
  r.IsSpecialOffer,
  r.Remark
FROM seed_demo_dates d
JOIN seed_demo_routes r
JOIN seed_demo_route_stops originStop ON originStop.RouteIndex = r.RouteIndex
JOIN seed_demo_destination_stops destinationStop ON destinationStop.RouteIndex = r.RouteIndex
JOIN Airport originAirport ON originAirport.AirportCode = originStop.AirportCode
JOIN Airport destinationAirport ON destinationAirport.AirportCode = destinationStop.AirportCode
WHERE TRUE
  AND originStop.StopNo < destinationStop.StopNo
  AND originAirport.CityId <> destinationAirport.CityId
ON DUPLICATE KEY UPDATE
  OriginAirportCode = VALUES(OriginAirportCode),
  DestinationAirportCode = VALUES(DestinationAirportCode),
  PlannedDepartureTime = VALUES(PlannedDepartureTime),
  PlannedArrivalTime = VALUES(PlannedArrivalTime),
  FirstClassRemainingSeats = VALUES(FirstClassRemainingSeats),
  EconomyRemainingSeats = VALUES(EconomyRemainingSeats),
  FirstClassPrice = VALUES(FirstClassPrice),
  EconomyPrice = VALUES(EconomyPrice),
  IsSpecialOffer = VALUES(IsSpecialOffer),
  Remark = VALUES(Remark);

DROP TEMPORARY TABLE IF EXISTS seed_demo_route_stops;
DROP TEMPORARY TABLE IF EXISTS seed_demo_destination_stops;
DROP TEMPORARY TABLE IF EXISTS seed_demo_routes;
DROP TEMPORARY TABLE IF EXISTS seed_demo_dates;
SET FOREIGN_KEY_CHECKS = 1;
