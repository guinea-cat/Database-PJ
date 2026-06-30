import type { Airport, CabinClass, FlightSearchItem, FlightStatus, RegisterForm, Ticket, TicketStatus, UserType } from '../types';

type AirportLike = Airport | Pick<Airport, 'airportCode' | 'airportName' | 'city'>;
type AirportLookup = Record<string, AirportLike>;

export function formatMoney(value: number | string | undefined) {
  const amount = Number(value ?? 0);
  return `¥${amount.toFixed(2)}`;
}

export function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

export function formatForDateTimeLocal(value?: string) {
  if (!value) {
    return '';
  }
  return value.replace(' ', 'T').slice(0, 16);
}

export function roleHome(userType: UserType) {
  return userType === 'ADMIN' ? 'admin' : 'passenger';
}

export function cabinText(value?: string) {
  return value === 'FIRST_CLASS' ? '头等舱' : '经济舱';
}

export function statusText(status: TicketStatus | FlightStatus | string) {
  const map: Record<string, string> = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    EXPIRED: '已过期',
    REFUND_SUCCESS: '已退票',
    CHANGE_SUCCESS: '已改签',
    NORMAL: '正常',
    DELAYED: '延误',
    CANCELLED: '取消',
    COMPLETED: '完成',
    DISABLED: '停用',
  };
  return map[status] ?? status;
}

export function shortTime(value?: string) {
  if (!value) {
    return '--:--';
  }
  return value.replace('T', ' ').slice(11, 16);
}

export function airportLabel(airport?: AirportLike) {
  if (!airport) {
    return '';
  }
  const cityName = airport.city?.cityName ?? airport.airportCode;
  const shortName = airport.airportName
    .replace(cityName, '')
    .replace('国际机场', '')
    .replace('机场', '')
    .trim();
  return `${cityName}（${shortName || airport.airportName}）${airport.airportCode}`;
}

export function airportCodeLabel(code?: string, airports?: AirportLookup) {
  if (!code) {
    return '-';
  }
  return airports?.[code] ? airportLabel(airports[code]) : code;
}

export function validateRegisterForm(form: RegisterForm) {
  const errors: string[] = [];
  if (!form.loginAccount.trim()) {
    errors.push('登录账号不能为空');
  }
  if (form.password.trim().length < 6) {
    errors.push('密码长度不能少于6位');
  }
  if (!form.userName.trim()) {
    errors.push('乘客姓名不能为空');
  }
  if (!/^\d{11}$/.test(form.phoneNumber.trim())) {
    errors.push('手机号必须是11位数字');
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.push('邮箱格式不正确');
  }
  if (!/^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/.test(form.idNumber.trim())) {
    errors.push('身份证号格式不正确');
  }
  return errors;
}

export function orderRouteSummary(
  ticket: Ticket | Pick<Ticket, 'flightId' | 'segmentId'>,
  segment?: {
    flightNumber?: string;
    flightDate?: string;
    originAirportCode?: string;
    destinationAirportCode?: string;
    plannedDepartureTime?: string;
    plannedArrivalTime?: string;
  },
  airports?: AirportLookup,
) {
  if (!segment) {
    return `航班 #${ticket.flightId} · 航段 #${ticket.segmentId}`;
  }
  const time = `${shortTime(segment.plannedDepartureTime)}-${shortTime(segment.plannedArrivalTime)}`;
  const origin = airportCodeLabel(segment.originAirportCode, airports);
  const destination = airportCodeLabel(segment.destinationAirportCode, airports);
  return `${segment.flightNumber ?? `航班 #${ticket.flightId}`} · ${segment.flightDate ?? '-'} · ${origin} → ${destination} · ${time}`;
}
export function maskUserName(userName?: string) {
  const chars = Array.from(userName?.trim() ?? '');
  if (chars.length === 0) {
    return '-';
  }
  if (chars.length === 1) {
    return '*';
  }
  const keep = chars.length >= 4 ? 2 : 1;
  return `${chars.slice(0, keep).join('')}${'*'.repeat(chars.length - keep)}`;
}

export function changeChainText(ticket: Ticket | Pick<Ticket, 'orderNo' | 'originalOrderNo'>) {
  if (ticket.originalOrderNo) {
    return `改签链路：原订单 ${ticket.originalOrderNo} -> 新订单 ${ticket.orderNo}`;
  }
  return `由原订单改签生成：新订单 ${ticket.orderNo}`;
}

export function filterChangeTargets(
  ticket: Pick<Ticket, 'segmentId' | 'flightDate' | 'originAirportCode' | 'destinationAirportCode' | 'cabinClass'>,
  candidates: FlightSearchItem[],
) {
  if (!ticket.flightDate || !ticket.originAirportCode || !ticket.destinationAirportCode) {
    return [];
  }
  return candidates.filter((item) => {
    const hasCabinInventory = ticket.cabinClass === 'FIRST_CLASS'
      ? item.firstClassRemainingSeats > 0
      : item.economyRemainingSeats > 0;
    return item.segmentId !== ticket.segmentId
      && item.originAirportCode === ticket.originAirportCode
      && item.destinationAirportCode === ticket.destinationAirportCode
      && item.flightDate === ticket.flightDate
      && (item.flightStatus === 'NORMAL' || item.flightStatus === 'DELAYED')
      && hasCabinInventory;
  });
}

export function changeTargetPrice(
  target: Pick<FlightSearchItem, 'economyPrice' | 'firstClassPrice'>,
  cabinClass: CabinClass,
) {
  return cabinClass === 'FIRST_CLASS' ? target.firstClassPrice : target.economyPrice;
}
