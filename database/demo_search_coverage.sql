USE airticket;

SELECT COUNT(*) AS FlightCount
FROM Flight;

SELECT COUNT(*) AS SameCitySegmentCount
FROM FlightSegment s
JOIN Airport originAirport ON s.OriginAirportCode = originAirport.AirportCode
JOIN Airport destinationAirport ON s.DestinationAirportCode = destinationAirport.AirportCode
WHERE originAirport.CityId = destinationAirport.CityId;

SELECT
  originCity.CityName AS OriginCity,
  destinationCity.CityName AS DestinationCity,
  f.FlightDate,
  SUM(s.IsSpecialOffer = FALSE) AS NormalSegmentCount,
  SUM(s.IsSpecialOffer = TRUE) AS SpecialSegmentCount,
  CASE
    WHEN SUM(s.IsSpecialOffer = FALSE) > 0 AND SUM(s.IsSpecialOffer = TRUE) > 0
    THEN 'OK'
    ELSE 'MISSING'
  END AS CoverageStatus
FROM FlightSegment s
JOIN Flight f ON s.FlightId = f.FlightId
JOIN Airport originAirport ON s.OriginAirportCode = originAirport.AirportCode
JOIN Airport destinationAirport ON s.DestinationAirportCode = destinationAirport.AirportCode
JOIN City originCity ON originAirport.CityId = originCity.CityId
JOIN City destinationCity ON destinationAirport.CityId = destinationCity.CityId
WHERE f.FlightDate BETWEEN '2026-06-28' AND '2026-07-04'
  AND originAirport.CityId <> destinationAirport.CityId
  AND f.FlightStatus IN ('NORMAL', 'DELAYED')
GROUP BY originCity.CityId, destinationCity.CityId, originCity.CityName, destinationCity.CityName, f.FlightDate
HAVING CoverageStatus <> 'OK'
ORDER BY originCity.CityId, destinationCity.CityId, f.FlightDate;

SELECT
  COUNT(*) AS CoveredCityDateCount,
  SUM(NormalSegmentCount > 0 AND SpecialSegmentCount > 0) AS ReadyCityDateCount
FROM (
  SELECT
    originCity.CityId AS OriginCityId,
    destinationCity.CityId AS DestinationCityId,
    f.FlightDate,
    SUM(s.IsSpecialOffer = FALSE) AS NormalSegmentCount,
    SUM(s.IsSpecialOffer = TRUE) AS SpecialSegmentCount
  FROM FlightSegment s
  JOIN Flight f ON s.FlightId = f.FlightId
  JOIN Airport originAirport ON s.OriginAirportCode = originAirport.AirportCode
  JOIN Airport destinationAirport ON s.DestinationAirportCode = destinationAirport.AirportCode
  JOIN City originCity ON originAirport.CityId = originCity.CityId
  JOIN City destinationCity ON destinationAirport.CityId = destinationCity.CityId
  WHERE f.FlightDate BETWEEN '2026-06-28' AND '2026-07-04'
    AND originAirport.CityId <> destinationAirport.CityId
    AND f.FlightStatus IN ('NORMAL', 'DELAYED')
  GROUP BY originCity.CityId, destinationCity.CityId, f.FlightDate
) coverage;
