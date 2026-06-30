USE airticket;

SELECT 'Change ticket chain: original order -> new order' AS DemoStep;

SELECT
  CONCAT('改签链路：原订单 ', oldTicket.OrderNo, ' -> 新订单 ', newTicket.OrderNo) AS ChangeChain,
  oldTicket.OrderNo AS OriginalOrderNo,
  oldTicket.TicketStatus AS OriginalTicketStatus,
  newTicket.OrderNo AS NewOrderNo,
  newTicket.TicketStatus AS NewTicketStatus,
  newTicket.CabinClass,
  newTicket.PriceAmount,
  newTicket.PaymentAmount,
  newTicket.ChangeReason,
  newTicket.BookedAt,
  newTicket.PaidAt
FROM TicketSale newTicket
JOIN TicketSale oldTicket
  ON newTicket.OriginalTicketId = oldTicket.TicketId
ORDER BY newTicket.TicketId DESC
LIMIT 10;
