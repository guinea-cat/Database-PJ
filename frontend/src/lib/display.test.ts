import { describe, expect, it } from 'vitest';
import {
  airportLabel,
  airportCodeLabel,
  formatForDateTimeLocal,
  formatMoney,
  orderRouteSummary,
  roleHome,
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
});
