CREATE DATABASE IF NOT EXISTS airticket
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE airticket;

CREATE TABLE IF NOT EXISTS City (
  CityId INT PRIMARY KEY AUTO_INCREMENT,
  CityName VARCHAR(50) NOT NULL UNIQUE,
  CityCode VARCHAR(20) NOT NULL,
  Country VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS Airport (
  AirportCode VARCHAR(20) PRIMARY KEY,
  AirportName VARCHAR(100) NOT NULL,
  CityId INT NOT NULL,
  IsInternational BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_airport_city FOREIGN KEY (CityId) REFERENCES City (CityId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS Aircraft (
  AircraftRegNo VARCHAR(20) PRIMARY KEY,
  AircraftType VARCHAR(50) NOT NULL,
  Manufacturer VARCHAR(50) NOT NULL,
  TotalFirstClassSeats INT NOT NULL,
  TotalEconomySeats INT NOT NULL,
  Status VARCHAR(20) NOT NULL,
  Remark VARCHAR(255),
  CONSTRAINT ck_aircraft_first_seats CHECK (TotalFirstClassSeats >= 0),
  CONSTRAINT ck_aircraft_economy_seats CHECK (TotalEconomySeats >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `User` (
  UserId INT PRIMARY KEY AUTO_INCREMENT,
  LoginAccount VARCHAR(50) NOT NULL UNIQUE,
  UserName VARCHAR(50) NOT NULL,
  IdNumberDigest VARCHAR(64) NOT NULL UNIQUE,
  PasswordHash VARCHAR(100) NOT NULL,
  UserType VARCHAR(20) NOT NULL,
  PhoneNumber VARCHAR(20),
  Email VARCHAR(100),
  Points INT NOT NULL DEFAULT 0,
  MemberLevel VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  CreatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UpdatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_user_type CHECK (UserType IN ('PASSENGER', 'ADMIN')),
  CONSTRAINT ck_member_level CHECK (MemberLevel IN ('NORMAL', 'VIP')),
  CONSTRAINT ck_points CHECK (Points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS MealOption (
  MealId INT PRIMARY KEY AUTO_INCREMENT,
  MealName VARCHAR(50) NOT NULL UNIQUE,
  MealType VARCHAR(50) NOT NULL,
  IsAvailable BOOLEAN NOT NULL DEFAULT TRUE,
  Description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS Flight (
  FlightId INT PRIMARY KEY AUTO_INCREMENT,
  FlightNumber VARCHAR(20) NOT NULL,
  FlightDate DATE NOT NULL,
  AircraftRegNo VARCHAR(20) NOT NULL,
  FlightStatus VARCHAR(20) NOT NULL,
  DepartureAirportCode VARCHAR(20) NOT NULL,
  ArrivalAirportCode VARCHAR(20) NOT NULL,
  Remark VARCHAR(255),
  CONSTRAINT fk_flight_aircraft FOREIGN KEY (AircraftRegNo) REFERENCES Aircraft (AircraftRegNo),
  CONSTRAINT fk_flight_departure_airport FOREIGN KEY (DepartureAirportCode) REFERENCES Airport (AirportCode),
  CONSTRAINT fk_flight_arrival_airport FOREIGN KEY (ArrivalAirportCode) REFERENCES Airport (AirportCode),
  CONSTRAINT ck_flight_status CHECK (FlightStatus IN ('NORMAL', 'DELAYED', 'CANCELLED', 'COMPLETED', 'DISABLED')),
  UNIQUE KEY uk_flight_number_date (FlightNumber, FlightDate),
  KEY idx_flight_date (FlightDate),
  KEY idx_flight_status (FlightStatus)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS FlightSegment (
  SegmentId INT PRIMARY KEY AUTO_INCREMENT,
  FlightId INT NOT NULL,
  OriginStopNo INT NOT NULL,
  DestinationStopNo INT NOT NULL,
  OriginAirportCode VARCHAR(20) NOT NULL,
  DestinationAirportCode VARCHAR(20) NOT NULL,
  PlannedDepartureTime DATETIME NOT NULL,
  PlannedArrivalTime DATETIME NOT NULL,
  ActualDepartureTime DATETIME,
  ActualArrivalTime DATETIME,
  DelayMinutes INT DEFAULT 0,
  DelayReason VARCHAR(255),
  FirstClassRemainingSeats INT NOT NULL,
  EconomyRemainingSeats INT NOT NULL,
  FirstClassPrice DECIMAL(10,2) NOT NULL,
  EconomyPrice DECIMAL(10,2) NOT NULL,
  IsSpecialOffer BOOLEAN NOT NULL DEFAULT FALSE,
  Remark VARCHAR(255),
  CONSTRAINT fk_segment_flight FOREIGN KEY (FlightId) REFERENCES Flight (FlightId),
  CONSTRAINT fk_segment_origin_airport FOREIGN KEY (OriginAirportCode) REFERENCES Airport (AirportCode),
  CONSTRAINT fk_segment_destination_airport FOREIGN KEY (DestinationAirportCode) REFERENCES Airport (AirportCode),
  CONSTRAINT ck_segment_stop_order CHECK (OriginStopNo < DestinationStopNo),
  CONSTRAINT ck_segment_delay CHECK (DelayMinutes >= 0),
  CONSTRAINT ck_segment_first_inventory CHECK (FirstClassRemainingSeats >= 0),
  CONSTRAINT ck_segment_economy_inventory CHECK (EconomyRemainingSeats >= 0),
  CONSTRAINT ck_segment_first_price CHECK (FirstClassPrice >= 0),
  CONSTRAINT ck_segment_economy_price CHECK (EconomyPrice >= 0),
  UNIQUE KEY uk_segment_route (FlightId, OriginStopNo, DestinationStopNo),
  KEY idx_segment_origin_destination (OriginAirportCode, DestinationAirportCode),
  KEY idx_segment_departure_time (PlannedDepartureTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS TicketSale (
  TicketId INT PRIMARY KEY AUTO_INCREMENT,
  OrderNo VARCHAR(32) NOT NULL UNIQUE,
  UserId INT NOT NULL,
  FlightId INT NOT NULL,
  SegmentId INT NOT NULL,
  CabinClass VARCHAR(20) NOT NULL,
  TicketStatus VARCHAR(20) NOT NULL,
  PassengerName VARCHAR(50) NOT NULL,
  PassengerIdNumberDigest VARCHAR(64) NOT NULL,
  PriceAmount DECIMAL(10,2) NOT NULL,
  PaymentAmount DECIMAL(10,2) NOT NULL,
  OriginalTicketId INT,
  ChangeReason VARCHAR(255),
  BookedAt DATETIME NOT NULL,
  PaidAt DATETIME,
  IssuedAt DATETIME,
  ChangedAt DATETIME,
  RefundedAt DATETIME,
  ExpiredAt DATETIME,
  Remark VARCHAR(255),
  CONSTRAINT fk_ticket_user FOREIGN KEY (UserId) REFERENCES `User` (UserId),
  CONSTRAINT fk_ticket_flight FOREIGN KEY (FlightId) REFERENCES Flight (FlightId),
  CONSTRAINT fk_ticket_segment FOREIGN KEY (SegmentId) REFERENCES FlightSegment (SegmentId),
  CONSTRAINT fk_ticket_original FOREIGN KEY (OriginalTicketId) REFERENCES TicketSale (TicketId),
  CONSTRAINT ck_cabin_class CHECK (CabinClass IN ('ECONOMY', 'FIRST_CLASS')),
  CONSTRAINT ck_ticket_status CHECK (TicketStatus IN ('PENDING_PAYMENT', 'PAID', 'EXPIRED', 'REFUND_SUCCESS', 'CHANGE_SUCCESS')),
  CONSTRAINT ck_ticket_price CHECK (PriceAmount >= 0),
  CONSTRAINT ck_ticket_payment CHECK (PaymentAmount >= 0),
  KEY idx_ticket_user (UserId),
  KEY idx_ticket_flight (FlightId),
  KEY idx_ticket_segment (SegmentId),
  KEY idx_ticket_status_expired (TicketStatus, ExpiredAt),
  KEY idx_ticket_original (OriginalTicketId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS MealReservation (
  MealReservationId INT PRIMARY KEY AUTO_INCREMENT,
  TicketId INT NOT NULL,
  MealId INT NOT NULL,
  Quantity INT NOT NULL DEFAULT 1,
  CONSTRAINT fk_meal_reservation_ticket FOREIGN KEY (TicketId) REFERENCES TicketSale (TicketId),
  CONSTRAINT fk_meal_reservation_meal FOREIGN KEY (MealId) REFERENCES MealOption (MealId),
  CONSTRAINT ck_meal_quantity CHECK (Quantity > 0),
  UNIQUE KEY uk_ticket_meal (TicketId, MealId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
