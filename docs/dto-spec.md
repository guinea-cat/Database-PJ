docs/dto-spec.md
本文档定义后端所有接口请求体与响应体的数据结构。它的作用是把字段名、字段类型、必填性、默认值、枚举值、嵌套对象结构和分页规则一次定死，避免后续前后端联调时出现字段缺失、字段名不一致、状态值乱传或 DTO 映射混乱的问题。
本项目坚持一个原则：接口字段命名尽量与数据库字段命名保持一致。对于前端提交的 JSON，后端应直接按本规范解析，不应在 Controller 中临时发明新的字段名，也不应在 Service 中做复杂字段转换。
1. 通用 DTO 规范
1.1 ApiResponse
所有接口统一返回 ApiResponse 包装对象。
字段定义如下：code，message，data，timestamp。
code 为业务状态码，0 表示成功，其余表示失败。message 为人类可读的提示信息。data 为业务数据主体，可为对象、数组、分页对象或 null。timestamp 为后端返回时间，格式建议为 ISO 8601 字符串。
推荐 JSON 结构如下：
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-07-01T12:00:00"
}
1.2 PageRequest
分页请求统一使用 pageNum 与 pageSize。
字段说明：pageNum 为页码，从 1 开始；pageSize 为每页条数，建议默认 10，最大可限制为 100。
1.3 PageResult
分页响应统一使用 total、pageNum、pageSize、records。
total 表示总记录数，records 表示当前页数据数组。
1.4 IdRequest
对于只需要一个主键的接口，统一使用单字段对象，字段名为 id、userId、flightId、segmentId、ticketId、mealId 等，不要强行复用一个无意义的 id 字段导致歧义。
2. Authentication DTOs
2.1 RegisterRequest
用于用户注册。字段如下：loginAccount、password、userName、phoneNumber、email、idNumber。
其中 loginAccount、password、userName、phoneNumber、idNumber 为必填字段，email 可选但建议前端提供。idNumber 为身份证号明文输入，后端必须在落库前做不可逆摘要处理，不允许前端自行加密。
2.2 RegisterResponse
字段如下：userId、loginAccount、userType、memberLevel、points。
注册成功后默认 userType=PASSENGER，memberLevel=NORMAL，points=0。
2.3 LoginRequest
字段如下：loginAccount、password。
2.4 LoginResponse
字段如下：userId、loginAccount、userName、userType、memberLevel、points、token、createdAt、updatedAt。
如果使用 Session 而不是 Token，token 字段可保留但返回 null，或者由前端通过 Cookie 维持登录态。为了接口统一，建议保留 token 字段。
2.5 CurrentUserResponse
用于 GET /api/auth/me。
字段如下：userId、loginAccount、userName、userType、memberLevel、points、phoneNumber、email、createdAt、updatedAt。
3. Passenger Query DTOs
3.1 FlightSearchRequest
字段如下：departureCityId、arrivalCityId、flightDateStart、flightDateEnd、cabinClass、pageNum、pageSize。
其中 departureCityId 与 arrivalCityId 至少传一种筛选维度所需字段。为了演示稳定，前端通常传城市 ID；若实现中使用机场编码，也应在后端做兼容转换，但对外字段名保持不变。
cabinClass 建议使用枚举字符串：ECONOMY、FIRST_CLASS。若不传，则默认查询全部舱位可售情况。
3.2 FlightSearchItemResponse
字段如下：flightId、flightNumber、flightDate、flightStatus、aircraftRegNo、departureAirportCode、departureAirportName、departureCityId、departureCityName、arrivalAirportCode、arrivalAirportName、arrivalCityId、arrivalCityName、segmentId、originStopNo、destinationStopNo、originAirportCode、destinationAirportCode、originAirportName、destinationAirportName、firstClassRemainingSeats、economyRemainingSeats、firstClassPriceAmount、economyPriceAmount、plannedDepartureTime、plannedArrivalTime、isAvailable。
isAvailable 为布尔值，用于前端快速判断是否可售。
3.3 FlightDetailResponse
字段与搜索项类似，但应补充 remark、createdAt、updatedAt、delayReason、actualDepartureTime、actualArrivalTime 等更完整信息。如果你的 ER 图最终没有这些字段，则该对象中不返回这些字段，避免接口与数据库不一致。
3.4 AvailableSegmentsResponse
用于返回某航班下可售航段列表。字段为数组，每项结构同 FlightSearchItemResponse 中的航段部分。
3.5 MealListResponseItem
字段如下：mealId、mealName、mealType、isAvailable、description。
3.6 MemberProfileResponse
字段如下：userId、loginAccount、userName、memberLevel、points、vipThreshold、vipDiscountRate。
其中 vipThreshold=1000，vipDiscountRate=0.9 为固定业务参数。
4. Ticket DTOs
4.1 CreateTicketRequest
字段如下：userId、flightId、segmentId、cabinClass、passengerName、passengerIdNumber、mealId。
userId 为下单人 ID。passengerName 与 passengerIdNumber 为乘机人信息，后端应将身份证号做摘要处理后存储。
4.2 CreateTicketResponse
字段如下：ticketId、orderNo、ticketStatus、priceAmount、paymentAmount、expiredAt、bookedAt、memberLevel、pointsAfterPurchase。
若用户是 VIP，则 paymentAmount 为折后金额；若为普通用户，则 paymentAmount=priceAmount。
4.3 PayTicketRequest
字段如下：ticketId。
支付接口只允许通过订单 ID 发起，不建议再次携带价格或用户输入金额，避免前端篡改。
4.4 PayTicketResponse
字段如下：ticketId、ticketStatus、paidAt、issuedAt、paymentAmount、pointsAfterPurchase、memberLevel。
4.5 TicketDetailResponse
字段如下：ticketId、orderNo、ticketStatus、userId、loginAccount、userName、flightId、flightNumber、flightDate、segmentId、originAirportCode、destinationAirportCode、passengerName、passengerIdNumberMask、cabinClass、priceAmount、paymentAmount、mealId、mealName、originalTicketId、bookedAt、paidAt、issuedAt、expiredAt、refundedAt、changedAt、remark。
这里的 passengerIdNumberMask 仅用于前端展示脱敏值，不能返回明文身份证。
4.6 TicketListItemResponse
字段如下：ticketId、orderNo、ticketStatus、flightNumber、flightDate、originAirportName、destinationAirportName、passengerName、cabinClass、priceAmount、paymentAmount、bookedAt、expiredAt。
4.7 TicketListResponse
字段如下：total、pageNum、pageSize、records。
records 的元素类型为 TicketListItemResponse。
4.8 RefundTicketRequest
字段如下：ticketId。
4.9 ChangeApplyRequest
字段如下：ticketId、targetFlightId、targetSegmentId。
若改签只允许传一个新航段，也可以只保留 targetSegmentId，但文档应与实现保持一致。推荐保留两个字段，便于前端展示跨航班改签。
4.10 ChangeApplyResponse
字段如下：oldTicketId、newTicketId、orderNo、ticketStatus、oldPriceAmount、newPriceAmount、differenceAmount、expiredAt。
若差价为 0，differenceAmount=0。
4.11 ChangePayRequest
字段如下：ticketId。
这里的 ticketId 建议指向改签生成的新票记录。
4.12 ChangeHistoryItemResponse
字段如下：ticketId、originalTicketId、orderNo、ticketStatus、bookedAt、paidAt、changedAt、refundedAt、passengerName、flightNumber、segmentId。
5. User and Member DTOs
5.1 UserListItemResponse
字段如下：userId、loginAccount、userName、userType、memberLevel、points、phoneNumber、email、createdAt、updatedAt、isEnabled。
5.2 UserDetailResponse
字段如下：userId、loginAccount、userName、userType、memberLevel、points、phoneNumber、email、createdAt、updatedAt、remark。
管理员只能查看积分，不得修改积分，因此 points 只出现在响应中，不应在管理员修改接口中作为可写字段。
6. City DTOs
6.1 CityListItemResponse
字段如下：cityId、cityCode、cityName、countryName、isEnabled、remark。
6.2 CityDetailResponse
字段如下：cityId、cityCode、cityName、countryName、isEnabled、createdAt、updatedAt、remark。
6.3 CityAddRequest / CityUpdateRequest
字段如下：cityCode、cityName、countryName、remark。
更新时额外带 cityId。
6.4 CityDisableRequest
字段如下：cityId。
7. Airport DTOs
7.1 AirportListItemResponse
字段如下：airportId、airportCode、airportName、cityId、cityName、countryName、isInternational、isEnabled、remark。
7.2 AirportDetailResponse
字段如下：airportId、airportCode、airportName、cityId、cityName、countryName、isInternational、isEnabled、createdAt、updatedAt、remark。
7.3 AirportAddRequest / AirportUpdateRequest
字段如下：airportCode、airportName、cityId、isInternational、remark。
更新时额外带 airportId。
7.4 AirportDisableRequest
字段如下：airportId。
8. Aircraft DTOs
8.1 AircraftListItemResponse
字段如下：aircraftRegNo、aircraftModel、manufacturer、firstClassSeatCount、economySeatCount、isEnabled、remark。
8.2 AircraftDetailResponse
字段如下：aircraftRegNo、aircraftModel、manufacturer、firstClassSeatCount、economySeatCount、isEnabled、createdAt、updatedAt、remark。
8.3 AircraftAddRequest / AircraftUpdateRequest
字段如下：aircraftRegNo、aircraftModel、manufacturer、firstClassSeatCount、economySeatCount、remark。
更新时如果主键不可改，则新增字段 oldAircraftRegNo 或 aircraftId，以实现定位。具体以数据库主键设计为准。
8.4 AircraftDisableRequest
字段如下：aircraftRegNo。
9. Flight DTOs
9.1 FlightListItemResponse
字段如下：flightId、flightNumber、flightDate、flightStatus、aircraftRegNo、departureAirportCode、departureAirportName、arrivalAirportCode、arrivalAirportName、isEnabled、remark。
9.2 FlightDetailResponse
字段如下：flightId、flightNumber、flightDate、flightStatus、aircraftRegNo、departureAirportCode、departureAirportName、arrivalAirportCode、arrivalAirportName、createdAt、updatedAt、isEnabled、remark。
9.3 FlightAddRequest / FlightUpdateRequest
字段如下：flightNumber、flightDate、aircraftRegNo、departureAirportCode、arrivalAirportCode、flightStatus、remark。
更新时额外带 flightId。
9.4 FlightDisableRequest
字段如下：flightId。
10. FlightSegment DTOs
10.1 SegmentListItemResponse
字段如下：segmentId、flightId、flightNumber、originStopNo、destinationStopNo、originAirportCode、originAirportName、destinationAirportCode、destinationAirportName、plannedDepartureTime、plannedArrivalTime、firstClassRemainingSeats、economyRemainingSeats、firstClassPriceAmount、economyPriceAmount、isEnabled、remark。
10.2 SegmentDetailResponse
字段如下：segmentId、flightId、flightNumber、originStopNo、destinationStopNo、originAirportCode、originAirportName、destinationAirportCode、destinationAirportName、plannedDepartureTime、plannedArrivalTime、actualDepartureTime、actualArrivalTime、delayMinutes、firstClassRemainingSeats、economyRemainingSeats、firstClassPriceAmount、economyPriceAmount、isEnabled、createdAt、updatedAt、remark。
10.3 SegmentAddRequest / SegmentUpdateRequest
字段如下：flightId、originStopNo、destinationStopNo、originAirportCode、destinationAirportCode、plannedDepartureTime、plannedArrivalTime、actualDepartureTime、actualArrivalTime、delayMinutes、firstClassRemainingSeats、economyRemainingSeats、firstClassPriceAmount、economyPriceAmount、remark。
更新时额外带 segmentId。
10.4 SegmentDisableRequest
字段如下：segmentId。
11. Meal DTOs
11.1 MealListItemResponse
字段如下：mealId、mealName、mealType、isAvailable、remark。
11.2 MealDetailResponse
字段如下：mealId、mealName、mealType、isAvailable、createdAt、updatedAt、remark。
11.3 MealAddRequest / MealUpdateRequest
字段如下：mealName、mealType、remark。
更新时额外带 mealId。
11.4 MealDisableRequest
字段如下：mealId。
12. Dashboard DTOs
12.1 DashboardSummaryResponse
字段如下：userCount、passengerCount、adminCount、flightCount、segmentCount、ticketCount、vipCount、availableSeatCount、expiredTicketCount。
12.2 DashboardStatisticsResponse
字段如下：date、ticketSoldCount、refundCount、changeCount、vipUpgradeCount。
13. DTO Implementation Rules
DTO 类不直接承担复杂业务逻辑。它们只承担参数承载、字段声明和少量必要校验，例如非空、长度、格式。
DTO 字段命名必须稳定。若某个字段在 API 文档中出现，就必须在 DTO 中出现；若 API 文档中未出现，就不要在 DTO 中随意加字段。
请求 DTO 与响应 DTO 应分离，不建议一个类同时承担请求和响应职责，除非字段完全相同且不会造成歧义。
所有金额字段使用 BigDecimal。所有时间字段使用 LocalDateTime 或等价类型。所有状态字段使用枚举或字符串枚举映射，不要用魔法数字。
所有身份证号原始输入只出现在请求 DTO 中，响应 DTO 仅返回脱敏后的展示值或不返回。