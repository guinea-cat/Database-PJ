import { apiGet, apiPost } from './client';
import type {
  Aircraft,
  Airport,
  AuthUser,
  CabinClass,
  City,
  DashboardSummary,
  Flight,
  FlightSearchItem,
  FlightSegment,
  MealOption,
  MemberProfile,
  RegisterForm,
  Ticket,
} from '../types';

export function login(loginAccount: string, password: string) {
  return apiPost<AuthUser>('/auth/login', { loginAccount, password });
}

export function registerPassenger(form: RegisterForm) {
  return apiPost<AuthUser, RegisterForm>('/auth/register', form);
}

export function cancelAccount(userId: number) {
  return apiPost<void>('/auth/cancel', { userId });
}

export function getMemberProfile(userId: number) {
  return apiGet<MemberProfile>('/member/profile', { userId });
}

export function searchFlights(params: {
  departureCityId?: number;
  arrivalCityId?: number;
  departureAirportCode?: string;
  arrivalAirportCode?: string;
  flightDate?: string;
}) {
  return apiGet<FlightSearchItem[]>('/flight/search', params);
}

export function listMeals() {
  return apiGet<MealOption[]>('/meal/list');
}

export function createTicket(body: {
  userId: number;
  flightId: number;
  segmentId: number;
  cabinClass: CabinClass;
  passengerName: string;
  passengerIdNumber: string;
  mealId?: number;
}) {
  return apiPost<Ticket>('/ticket/create', body);
}

export function payTicket(ticketId: number) {
  return apiPost<Ticket>('/ticket/pay', { ticketId });
}

export function listMyTickets(userId: number) {
  return apiGet<Ticket[]>('/ticket/my', { userId });
}

export function refundTicket(ticketId: number, remark?: string) {
  return apiPost<Ticket>('/ticket/refund', { ticketId, remark });
}

export function applyChange(body: {
  ticketId: number;
  targetFlightId: number;
  targetSegmentId: number;
  cabinClass: CabinClass;
  changeReason: string;
  mealId?: number;
}) {
  return apiPost<Ticket>('/ticket/change/apply', body);
}

export function payChangeTicket(ticketId: number) {
  return apiPost<Ticket>('/ticket/change/pay', { ticketId });
}

export function getChangeHistory(ticketId: number) {
  return apiGet<Ticket[]>('/ticket/change/history', { ticketId });
}

export function listCities() {
  return apiGet<City[]>('/admin/city/list');
}

export function saveCity(city: Partial<City>) {
  return apiPost<City>('/admin/city/update', city);
}

export function listAirports(cityId?: number) {
  return apiGet<Airport[]>('/admin/airport/list', { cityId });
}

export function saveAirport(airport: {
  airportCode: string;
  airportName: string;
  cityId: number;
  isInternational: boolean;
}) {
  return apiPost<Airport>('/admin/airport/update', airport);
}

export function listAircraft() {
  return apiGet<Aircraft[]>('/admin/aircraft/list');
}

export function saveAircraft(aircraft: Partial<Aircraft>) {
  return apiPost<Aircraft>('/admin/aircraft/update', aircraft);
}

export function disableAircraft(aircraftRegNo: string) {
  return apiPost<Aircraft>('/admin/aircraft/disable', { aircraftRegNo });
}

export function listFlights() {
  return apiGet<Flight[]>('/admin/flight/list');
}

export function saveFlight(flight: {
  flightId?: number;
  flightNumber: string;
  flightDate: string;
  aircraftRegNo: string;
  flightStatus: string;
  departureAirportCode: string;
  arrivalAirportCode: string;
  remark?: string;
}) {
  return apiPost<Flight>('/admin/flight/update', flight);
}

export function disableFlight(flightId: number) {
  return apiPost<Flight>('/admin/flight/disable', { flightId });
}

export function enableFlight(flightId: number) {
  return apiPost<Flight>('/admin/flight/enable', { flightId });
}

export function listSegments(flightId?: number) {
  return apiGet<FlightSegment[]>('/admin/segment/list', { flightId });
}

export function saveSegment(segment: Partial<FlightSegment> & { flightId: number }) {
  return apiPost<FlightSegment>('/admin/segment/update', segment);
}

export function listAdminMeals() {
  return apiGet<MealOption[]>('/admin/meal/list');
}

export function saveMeal(meal: Partial<MealOption>) {
  return apiPost<MealOption>('/admin/meal/update', meal);
}

export function disableMeal(mealId: number) {
  return apiPost<MealOption>('/admin/meal/disable', { mealId });
}

export function listUsers() {
  return apiGet<AuthUser[]>('/admin/user/list');
}

export function listTickets() {
  return apiGet<Ticket[]>('/admin/ticket/list');
}

export function getDashboardSummary() {
  return apiGet<DashboardSummary>('/admin/dashboard/summary');
}

export function processExpiredOrders() {
  return apiPost<number>('/admin/job/expire-order', {});
}

export function resetDemoData() {
  return apiPost<void>('/admin/job/reset-demo-data', {});
}
