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
