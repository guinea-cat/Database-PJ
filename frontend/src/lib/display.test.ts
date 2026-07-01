import { describe, expect, it } from 'vitest';
import {
  airportLabel,
  airportCodeLabel,
  changeChainText,
  changeTargetPrice,
  deductSeatFromFlights,
  filterChangeTargets,
  formatForDateTimeLocal,
  formatMoney,
  maskUserName,
  orderRouteSummary,
  prependTicketIfMissing,
  replaceTicket,
  roleHome,
  sortTicketsNewestFirst,
  statusText,
  validateRegisterForm,
} from './display';

describe('display helpers', () => {
  it('formats money with two decimals for ticket settlement display', () => {
    expect(formatMoney(900)).toBe('¥900.00');
    expect(formatMoney('1260.5')).toBe('¥1260.50');
  });

  it('formats backend datetime for datetime-local inputs', () => {
    expect(formatForDateTimeLocal('2026-07-01T08:00:00')).toBe('2026-07-01T08:00');
    expect(formatForDateTimeLocal('2026-07-01 08:00:00')).toBe('2026-07-01T08:00');
  });

  it('routes users to different workspaces by backend userType', () => {
    expect(roleHome('ADMIN')).toBe('admin');
    expect(roleHome('PASSENGER')).toBe('passenger');
  });

  it('shows fixed ticket statuses without inventing extra states', () => {
    expect(statusText('PENDING_PAYMENT')).toBe('待支付');
    expect(statusText('CHANGE_SUCCESS')).toBe('已改签');
  });

  it('formats airports as city and readable airport short name', () => {
    expect(
      airportLabel({
        airportCode: 'PVG',
        airportName: '上海浦东国际机场',
        city: { cityId: 2, cityName: '上海', cityCode: 'SHA', country: '中国' },
        isInternational: true,
      }),
    ).toBe('上海（浦东）PVG');
  });

  it('maps airport code to readable label when an airport lookup is available', () => {
    const airports = {
      PVG: {
        airportCode: 'PVG',
        airportName: '上海浦东国际机场',
        city: { cityId: 2, cityName: '上海', cityCode: 'SHA', country: '中国' },
      },
    };
    expect(airportCodeLabel('PVG', airports)).toBe('上海（浦东）PVG');
    expect(airportCodeLabel('XYZ', airports)).toBe('XYZ');
  });

  it('validates passenger register form before POST submit', () => {
    expect(validateRegisterForm({
      loginAccount: 'demo',
      password: '12345',
      userName: '新乘客',
      phoneNumber: '123',
      email: 'bad',
      idNumber: '123',
    })).toEqual([
      '密码长度不能少于6位',
      '手机号必须是11位数字',
      '邮箱格式不正确',
      '身份证号格式不正确',
    ]);
  });

  it('builds readable order route summary from ticket and selected segment', () => {
    expect(orderRouteSummary(
      { ticketId: 1, orderNo: 'ORD1', ticketStatus: 'PAID', userId: 2, flightId: 21, segmentId: 99, cabinClass: 'ECONOMY', passengerName: 'A', priceAmount: 800, paymentAmount: 800 },
      { flightNumber: 'MU2001', flightDate: '2026-07-01', originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T08:00:00', plannedArrivalTime: '2026-07-01T10:00:00' },
    )).toBe('MU2001 · 2026-07-01 · PEK → SHA · 08:00-10:00');
  });

  it('builds order route summary with readable airport names when lookup exists', () => {
    const airports = {
      PEK: {
        airportCode: 'PEK',
        airportName: '北京首都国际机场',
        city: { cityId: 1, cityName: '北京', cityCode: 'BJS', country: '中国' },
      },
      SHA: {
        airportCode: 'SHA',
        airportName: '上海虹桥国际机场',
        city: { cityId: 2, cityName: '上海', cityCode: 'SHA', country: '中国' },
      },
    };
    expect(orderRouteSummary(
      { flightId: 21, segmentId: 99 },
      { flightNumber: 'MU2001', flightDate: '2026-07-01', originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T08:00:00', plannedArrivalTime: '2026-07-01T10:00:00' },
      airports,
    )).toBe('MU2001 · 2026-07-01 · 北京（首都）PEK → 上海（虹桥）SHA · 08:00-10:00');
  });
  it('masks user names for admin list display', () => {
    expect(maskUserName('王')).toBe('*');
    expect(maskUserName('张三')).toBe('张*');
    expect(maskUserName('演示乘客A')).toBe('演示***');
    expect(maskUserName('')).toBe('-');
  });

  it('filters change targets by selected cabin inventory and different segment', () => {
    const ticket = {
      ticketId: 1,
      orderNo: 'ORD1',
      ticketStatus: 'PAID' as const,
      userId: 2,
      flightId: 10,
      segmentId: 100,
      flightDate: '2026-07-01',
      originAirportCode: 'PEK',
      destinationAirportCode: 'SHA',
      cabinClass: 'ECONOMY' as const,
      passengerName: 'A',
      priceAmount: 800,
      paymentAmount: 800,
    };
    const candidates = [
      { flightId: 10, flightNumber: 'A', flightDate: '2026-07-01', flightStatus: 'NORMAL' as const, aircraftRegNo: 'B-1', departureAirportCode: 'PEK', arrivalAirportCode: 'SHA', segmentId: 100, originStopNo: 1, destinationStopNo: 2, originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T08:00:00', plannedArrivalTime: '2026-07-01T10:00:00', firstClassRemainingSeats: 1, economyRemainingSeats: 1, firstClassPrice: 1800, economyPrice: 800, isSpecialOffer: false, isAvailable: true },
      { flightId: 11, flightNumber: 'B', flightDate: '2026-07-01', flightStatus: 'NORMAL' as const, aircraftRegNo: 'B-2', departureAirportCode: 'PEK', arrivalAirportCode: 'SHA', segmentId: 101, originStopNo: 1, destinationStopNo: 2, originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T11:00:00', plannedArrivalTime: '2026-07-01T13:00:00', firstClassRemainingSeats: 0, economyRemainingSeats: 2, firstClassPrice: 1800, economyPrice: 800, isSpecialOffer: false, isAvailable: true },
      { flightId: 12, flightNumber: 'C', flightDate: '2026-07-01', flightStatus: 'NORMAL' as const, aircraftRegNo: 'B-3', departureAirportCode: 'PEK', arrivalAirportCode: 'SHA', segmentId: 102, originStopNo: 1, destinationStopNo: 2, originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T14:00:00', plannedArrivalTime: '2026-07-01T16:00:00', firstClassRemainingSeats: 1, economyRemainingSeats: 0, firstClassPrice: 1800, economyPrice: 800, isSpecialOffer: false, isAvailable: true },
      { flightId: 13, flightNumber: 'D', flightDate: '2026-07-02', flightStatus: 'NORMAL' as const, aircraftRegNo: 'B-4', departureAirportCode: 'PEK', arrivalAirportCode: 'SHA', segmentId: 103, originStopNo: 1, destinationStopNo: 2, originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-02T08:00:00', plannedArrivalTime: '2026-07-02T10:00:00', firstClassRemainingSeats: 1, economyRemainingSeats: 2, firstClassPrice: 1800, economyPrice: 800, isSpecialOffer: false, isAvailable: true },
    ];

    expect(filterChangeTargets(ticket, candidates).map((item) => item.segmentId)).toEqual([101]);
    expect(filterChangeTargets({ ...ticket, cabinClass: 'FIRST_CLASS' }, candidates).map((item) => item.segmentId)).toEqual([102]);
  });

  it('uses selected cabin price for change target display', () => {
    const target = { economyPrice: 800, firstClassPrice: 1800 };

    expect(changeTargetPrice(target, 'ECONOMY')).toBe(800);
    expect(changeTargetPrice(target, 'FIRST_CLASS')).toBe(1800);
  });

  it('prepends a newly created ticket without duplicating an existing one', () => {
    const existing = { ticketId: 1, orderNo: 'ORD1', ticketStatus: 'PENDING_PAYMENT' as const, userId: 2, flightId: 10, segmentId: 100, cabinClass: 'ECONOMY' as const, passengerName: 'A', priceAmount: 800, paymentAmount: 800 };
    const next = { ticketId: 2, orderNo: 'ORD2', ticketStatus: 'PENDING_PAYMENT' as const, userId: 2, flightId: 11, segmentId: 101, cabinClass: 'ECONOMY' as const, passengerName: 'A', priceAmount: 900, paymentAmount: 900 };

    expect(prependTicketIfMissing([existing], next).map((ticket) => ticket.ticketId)).toEqual([2, 1]);
    expect(prependTicketIfMissing([existing], existing)).toHaveLength(1);
  });

  it('deducts the selected cabin seat from the matching flight segment only', () => {
    const flights = [
      { flightId: 10, flightNumber: 'A', flightDate: '2026-07-01', flightStatus: 'NORMAL' as const, aircraftRegNo: 'B-1', departureAirportCode: 'PEK', arrivalAirportCode: 'SHA', segmentId: 100, originStopNo: 1, destinationStopNo: 2, originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T08:00:00', plannedArrivalTime: '2026-07-01T10:00:00', firstClassRemainingSeats: 1, economyRemainingSeats: 3, firstClassPrice: 1800, economyPrice: 800, isSpecialOffer: false, isAvailable: true },
      { flightId: 11, flightNumber: 'B', flightDate: '2026-07-01', flightStatus: 'NORMAL' as const, aircraftRegNo: 'B-2', departureAirportCode: 'PEK', arrivalAirportCode: 'SHA', segmentId: 101, originStopNo: 1, destinationStopNo: 2, originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T11:00:00', plannedArrivalTime: '2026-07-01T13:00:00', firstClassRemainingSeats: 2, economyRemainingSeats: 4, firstClassPrice: 1800, economyPrice: 800, isSpecialOffer: false, isAvailable: true },
    ];

    const afterEconomy = deductSeatFromFlights(flights, 100, 'ECONOMY');
    expect(afterEconomy[0].economyRemainingSeats).toBe(2);
    expect(afterEconomy[0].firstClassRemainingSeats).toBe(1);
    expect(afterEconomy[1].economyRemainingSeats).toBe(4);

    const afterFirst = deductSeatFromFlights(flights, 100, 'FIRST_CLASS');
    expect(afterFirst[0].firstClassRemainingSeats).toBe(0);
    expect(afterFirst[0].economyRemainingSeats).toBe(3);
  });

  it('restores a refunded seat by applying a negative deduction', () => {
    const flights = [
      { flightId: 10, flightNumber: 'A', flightDate: '2026-07-01', flightStatus: 'NORMAL' as const, aircraftRegNo: 'B-1', departureAirportCode: 'PEK', arrivalAirportCode: 'SHA', segmentId: 100, originStopNo: 1, destinationStopNo: 2, originAirportCode: 'PEK', destinationAirportCode: 'SHA', plannedDepartureTime: '2026-07-01T08:00:00', plannedArrivalTime: '2026-07-01T10:00:00', firstClassRemainingSeats: 1, economyRemainingSeats: 2, firstClassPrice: 1800, economyPrice: 800, isSpecialOffer: false, isAvailable: true },
    ];

    const restored = deductSeatFromFlights(flights, 100, 'ECONOMY', -1);

    expect(restored[0].economyRemainingSeats).toBe(3);
  });

  it('replaces an existing ticket with the latest backend version', () => {
    const pending = { ticketId: 1, orderNo: 'ORD1', ticketStatus: 'PAID' as const, userId: 2, flightId: 10, segmentId: 100, cabinClass: 'ECONOMY' as const, passengerName: 'A', priceAmount: 800, paymentAmount: 800 };
    const refunded = { ...pending, ticketStatus: 'REFUND_SUCCESS' as const, refundedAt: '2026-07-01T10:00:00' };

    expect(replaceTicket([pending], refunded)[0].ticketStatus).toBe('REFUND_SUCCESS');
  });

  it('sorts tickets newest first by ticket id', () => {
    const older = { ticketId: 1, orderNo: 'ORD1', ticketStatus: 'PAID' as const, userId: 2, flightId: 10, segmentId: 100, cabinClass: 'ECONOMY' as const, passengerName: 'A', priceAmount: 800, paymentAmount: 800 };
    const newer = { ...older, ticketId: 3, orderNo: 'ORD3' };
    const middle = { ...older, ticketId: 2, orderNo: 'ORD2' };

    expect(sortTicketsNewestFirst([older, newer, middle]).map((ticket) => ticket.ticketId)).toEqual([3, 2, 1]);
  });

  it('formats change history as a readable order chain', () => {
    expect(changeChainText({
      ticketId: 2,
      orderNo: 'TS202607010002',
      ticketStatus: 'PAID',
      userId: 1,
      flightId: 11,
      segmentId: 101,
      cabinClass: 'ECONOMY',
      passengerName: 'A',
      priceAmount: 800,
      paymentAmount: 100,
      originalTicketId: 1,
      originalOrderNo: 'TS202607010001',
    })).toBe('改签链路：原订单 TS202607010001 -> 新订单 TS202607010002');
  });

  it('falls back when change history has no original order number', () => {
    expect(changeChainText({
      ticketId: 2,
      orderNo: 'TS202607010002',
      ticketStatus: 'PAID',
      userId: 1,
      flightId: 11,
      segmentId: 101,
      cabinClass: 'ECONOMY',
      passengerName: 'A',
      priceAmount: 800,
      paymentAmount: 100,
      originalTicketId: 1,
    })).toBe('由原订单改签生成：新订单 TS202607010002');
  });
});
