export type UserType = 'PASSENGER' | 'ADMIN';
export type MemberLevel = 'NORMAL' | 'VIP';
export type CabinClass = 'ECONOMY' | 'FIRST_CLASS';
export type TicketStatus = 'PENDING_PAYMENT' | 'PAID' | 'EXPIRED' | 'REFUND_SUCCESS' | 'CHANGE_SUCCESS';
export type FlightStatus = 'NORMAL' | 'DELAYED' | 'CANCELLED' | 'COMPLETED' | 'DISABLED';

export type AuthUser = {
  userId: number;
  loginAccount: string;
  userName: string;
  userType: UserType;
  memberLevel: MemberLevel;
  points: number;
  token: string;
};

export type RegisterForm = {
  loginAccount: string;
  password: string;
  userName: string;
  phoneNumber: string;
  email: string;
  idNumber: string;
};

export type FlightSearchItem = {
  flightId: number;
  flightNumber: string;
  flightDate: string;
  flightStatus: FlightStatus;
  aircraftRegNo: string;
  departureAirportCode: string;
  arrivalAirportCode: string;
  segmentId: number;
  originStopNo: number;
  destinationStopNo: number;
  originAirportCode: string;
  destinationAirportCode: string;
  plannedDepartureTime: string;
  plannedArrivalTime: string;
  firstClassRemainingSeats: number;
  economyRemainingSeats: number;
  firstClassPrice: number;
  economyPrice: number;
  isAvailable: boolean;
};

export type MealOption = {
  mealId: number;
  mealName: string;
  mealType: string;
  isAvailable: boolean;
  description?: string;
};

export type Ticket = {
  ticketId: number;
  orderNo: string;
  ticketStatus: TicketStatus;
  userId: number;
  flightId: number;
  segmentId: number;
  flightNumber?: string;
  flightDate?: string;
  originAirportCode?: string;
  destinationAirportCode?: string;
  plannedDepartureTime?: string;
  plannedArrivalTime?: string;
  cabinClass: CabinClass;
  passengerName: string;
  priceAmount: number;
  paymentAmount: number;
  originalTicketId?: number;
  changeReason?: string;
  bookedAt?: string;
  paidAt?: string;
  issuedAt?: string;
  changedAt?: string;
  refundedAt?: string;
  expiredAt?: string;
  remark?: string;
};

export type MemberProfile = {
  userId: number;
  loginAccount: string;
  userName: string;
  memberLevel: MemberLevel;
  points: number;
  vipThreshold: number;
  vipDiscountRate: number;
};

export type City = {
  cityId: number;
  cityName: string;
  cityCode: string;
  country: string;
};

export type Airport = {
  airportCode: string;
  airportName: string;
  city?: City;
  isInternational: boolean;
};

export type Aircraft = {
  aircraftRegNo: string;
  aircraftType: string;
  manufacturer: string;
  totalFirstClassSeats: number;
  totalEconomySeats: number;
  status: string;
  remark?: string;
};

export type Flight = {
  flightId: number;
  flightNumber: string;
  flightDate: string;
  aircraftRegNo?: string;
  aircraft?: Aircraft;
  flightStatus: FlightStatus;
  departureAirportCode: string;
  arrivalAirportCode: string;
  remark?: string;
};

export type FlightSegment = {
  segmentId: number;
  flight?: Flight;
  originStopNo: number;
  destinationStopNo: number;
  originAirportCode: string;
  destinationAirportCode: string;
  plannedDepartureTime: string;
  plannedArrivalTime: string;
  actualDepartureTime?: string;
  actualArrivalTime?: string;
  delayMinutes?: number;
  delayReason?: string;
  firstClassRemainingSeats: number;
  economyRemainingSeats: number;
  firstClassPrice: number;
  economyPrice: number;
  remark?: string;
};

export type DashboardSummary = {
  userCount?: number;
  flightCount?: number;
  segmentCount?: number;
  ticketCount?: number;
  mealCount?: number;
  vipCount?: number;
};
