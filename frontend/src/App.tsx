import {
  BadgeCheck,
  ClipboardList,
  Coffee,
  CreditCard,
  Edit,
  Gauge,
  LogOut,
  Plane,
  PlaneLanding,
  PlaneTakeoff,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  Ticket as TicketIcon,
  Undo2,
  UserX,
  UserPlus,
} from 'lucide-react';
import { FormEvent, useEffect, useState } from 'react';
import {
  applyChange,
  cancelAccount,
  createTicket,
  disableAircraft,
  disableFlight,
  disableMeal,
  enableFlight,
  getChangeHistory,
  getDashboardSummary,
  getMemberProfile,
  listAdminMeals,
  listAircraft,
  listAirports,
  listCities,
  listFlights,
  listMeals,
  listMyTickets,
  listSegments,
  listTickets,
  listUsers,
  login,
  payChangeTicket,
  payTicket,
  processExpiredOrders,
  refundTicket,
  registerPassenger,
  saveAircraft,
  saveAirport,
  saveCity,
  saveFlight,
  saveMeal,
  saveSegment,
  searchFlights,
} from './api/airticket';
import {
  airportCodeLabel,
  airportLabel,
  cabinText,
  formatDateTime,
  formatForDateTimeLocal,
  formatMoney,
  orderRouteSummary,
  roleHome,
  shortTime,
  statusText,
  validateRegisterForm,
} from './lib/display';
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
} from './types';

type Toast = { kind: 'success' | 'error' | 'info'; message: string };
type Workspace = 'passenger' | 'admin';
type AdminTab = 'overview' | 'flights' | 'segments' | 'resources' | 'tickets';

const demoAccounts = [
  { label: '管理员', loginAccount: 'admin', password: 'admin123' },
  { label: '900 分乘客', loginAccount: 'passengerA', password: 'pass123' },
  { label: '普通乘客', loginAccount: 'passengerB', password: 'pass123' },
];

const defaultRegister: RegisterForm = {
  loginAccount: '',
  password: '',
  userName: '',
  phoneNumber: '',
  email: '',
  idNumber: '',
};

function App() {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem('airticket-user');
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  });
  const [workspace, setWorkspace] = useState<Workspace>(() =>
    currentUser ? roleHome(currentUser.userType) : 'passenger',
  );
  const [toast, setToast] = useState<Toast | null>(null);

  useEffect(() => {
    if (currentUser) {
      localStorage.setItem('airticket-user', JSON.stringify(currentUser));
      setWorkspace(roleHome(currentUser.userType));
    } else {
      localStorage.removeItem('airticket-user');
    }
  }, [currentUser]);

  const notify = (message: string, kind: Toast['kind'] = 'info') => {
    setToast({ message, kind });
    window.setTimeout(() => setToast(null), 3200);
  };

  const logout = async () => {
    setCurrentUser(null);
    notify('已退出登录', 'info');
  };

  const cancelCurrentAccount = async () => {
    if (!currentUser || currentUser.userType === 'ADMIN') {
      return;
    }
    const ok = window.confirm('注销后账号将匿名化失效，历史订单保留用于数据库外键完整性。确认注销吗？');
    if (!ok) {
      return;
    }
    try {
      await cancelAccount(currentUser.userId);
      setCurrentUser(null);
      notify('账号已注销，历史订单外键未被破坏', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '注销账号失败', 'error');
    }
  };

  return (
    <div className="app-shell">
      <div className="skyline" aria-hidden="true" />
      {toast && <div className={`toast toast-${toast.kind}`}>{toast.message}</div>}
      {!currentUser ? (
        <AuthScreen onLogin={setCurrentUser} notify={notify} />
      ) : (
        <>
          <header className="topbar">
            <div className="brand-lockup">
              <div className="brand-mark">
                <Plane size={24} />
              </div>
              <div>
                <p className="eyebrow">AIR TICKET CONTROL</p>
                <h1>航空票务数据库系统</h1>
              </div>
            </div>
            <div className="topbar-actions">
              <div className="user-chip">
                <span>{currentUser.userName}</span>
                <strong>{currentUser.userType === 'ADMIN' ? '管理员' : currentUser.memberLevel}</strong>
              </div>
              {currentUser.userType === 'ADMIN' && (
                <button
                  className={`icon-text ${workspace === 'admin' ? 'active' : ''}`}
                  onClick={() => setWorkspace('admin')}
                >
                  <ShieldCheck size={16} />
                  管理台
                </button>
              )}
              <button
                className={`icon-text ${workspace === 'passenger' ? 'active' : ''}`}
                onClick={() => setWorkspace('passenger')}
              >
                <TicketIcon size={16} />
                旅客端
              </button>
              {currentUser.userType === 'PASSENGER' && (
                <button className="icon-text danger" onClick={cancelCurrentAccount}>
                  <UserX size={16} />
                  注销账号
                </button>
              )}
              <button className="icon-button" onClick={logout} title="退出登录">
                <LogOut size={18} />
              </button>
            </div>
          </header>
          {workspace === 'admin' && currentUser.userType === 'ADMIN' ? (
            <AdminWorkspace notify={notify} />
          ) : (
            <PassengerWorkspace user={currentUser} onUserRefresh={setCurrentUser} notify={notify} />
          )}
        </>
      )}
    </div>
  );
}

function AuthScreen({
  onLogin,
  notify,
}: {
  onLogin: (user: AuthUser) => void;
  notify: (message: string, kind?: Toast['kind']) => void;
}) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [loginAccount, setLoginAccount] = useState('passengerA');
  const [password, setPassword] = useState('pass123');
  const [registerForm, setRegisterForm] = useState<RegisterForm>(defaultRegister);
  const [loading, setLoading] = useState(false);

  const submitLogin = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      const user = await login(loginAccount, password);
      onLogin(user);
      notify(`欢迎回来，${user.userName}`, 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '登录失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const submitRegister = async (event: FormEvent) => {
    event.preventDefault();
    const errors = validateRegisterForm(registerForm);
    if (errors.length > 0) {
      notify(errors.join('；'), 'error');
      return;
    }
    setLoading(true);
    try {
      const user = await registerPassenger(registerForm);
      onLogin(user);
      notify('注册成功，已进入旅客端', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '注册失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="hero-panel">
        <div className="hero-copy">
          <p className="eyebrow">LOCAL DEMO · ER FIRST</p>
          <h1>航空票务数据库系统</h1>
          <p>
            面向课程演示的本地航空票务平台：航班检索、票务交易、会员积分与管理员维护都严格运行在 ER 图规定的 9 张表内。
          </p>
        </div>
        <div className="route-strip">
          <div>
            <span>PEK</span>
            <small>北京</small>
          </div>
          <div className="route-line">
            <Plane size={18} />
          </div>
          <div>
            <span>SHA</span>
            <small>上海</small>
          </div>
        </div>
      </section>
      <section className="auth-card">
        <div className="segment-control">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>
            账号登录
          </button>
          <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
            乘客注册
          </button>
        </div>
        {mode === 'login' ? (
          <form onSubmit={submitLogin} className="form-grid">
            <label>
              登录账号
              <input value={loginAccount} onChange={(event) => setLoginAccount(event.target.value)} required />
            </label>
            <label>
              密码
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </label>
            <div className="quick-row">
              {demoAccounts.map((account) => (
                <button
                  key={account.loginAccount}
                  type="button"
                  className="text-button"
                  onClick={() => {
                    setLoginAccount(account.loginAccount);
                    setPassword(account.password);
                  }}
                >
                  {account.label}
                </button>
              ))}
            </div>
            <button className="primary-action" disabled={loading}>
              <PlaneTakeoff size={18} />
              登录进入系统
            </button>
          </form>
        ) : (
          <form onSubmit={submitRegister} className="form-grid two-col">
            {[
              ['loginAccount', '登录账号'],
              ['password', '密码'],
              ['userName', '乘客姓名'],
              ['phoneNumber', '手机号'],
              ['email', '邮箱'],
              ['idNumber', '身份证号'],
            ].map(([key, label]) => (
              <label key={key}>
                {label}
                <input
                  type={key === 'password' ? 'password' : 'text'}
                  value={registerForm[key as keyof RegisterForm]}
                  onChange={(event) =>
                    setRegisterForm((form) => ({ ...form, [key]: event.target.value }))
                  }
                  required
                />
              </label>
            ))}
            <button className="primary-action wide" disabled={loading}>
              <UserPlus size={18} />
              注册并登录
            </button>
          </form>
        )}
      </section>
    </main>
  );
}

function PassengerWorkspace({
  user,
  onUserRefresh,
  notify,
}: {
  user: AuthUser;
  onUserRefresh: (user: AuthUser) => void;
  notify: (message: string, kind?: Toast['kind']) => void;
}) {
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [airports, setAirports] = useState<Airport[]>([]);
  const [meals, setMeals] = useState<MealOption[]>([]);
  const [flights, setFlights] = useState<FlightSearchItem[]>([]);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [selectedFlight, setSelectedFlight] = useState<FlightSearchItem | null>(null);
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [changeTargets, setChangeTargets] = useState<FlightSearchItem[]>([]);
  const [changeHistory, setChangeHistory] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchForm, setSearchForm] = useState({
    departureAirportCode: 'PEK',
    arrivalAirportCode: 'SHA',
    flightDate: '2026-07-01',
  });
  const [orderForm, setOrderForm] = useState({
    cabinClass: 'ECONOMY' as CabinClass,
    passengerName: user.userName,
    passengerIdNumber: '110101199001010011',
    mealId: '',
  });
  const [changeReason, setChangeReason] = useState('演示改签到更合适的航班');

  const paidTickets = tickets.filter((ticket) => ticket.ticketStatus === 'PAID');
  const pendingTickets = tickets.filter((ticket) => ticket.ticketStatus === 'PENDING_PAYMENT');
  const vipProgress = profile ? Math.min(100, Math.round((profile.points / profile.vipThreshold) * 100)) : 0;
  const airportOptions = airports.map((airport) => ({
    value: airport.airportCode,
    label: airportLabel(airport),
  }));
  const airportLookup = Object.fromEntries(airports.map((airport) => [airport.airportCode, airport]));

  const refreshProfile = async () => {
    const nextProfile = await getMemberProfile(user.userId);
    setProfile(nextProfile);
    onUserRefresh({
      ...user,
      memberLevel: nextProfile.memberLevel,
      points: nextProfile.points,
      userName: nextProfile.userName,
    });
  };

  const refreshTickets = async () => {
    setTickets(await listMyTickets(user.userId));
  };

  const bootstrap = async () => {
    try {
      const [nextProfile, nextMeals, nextTickets, nextAirports] = await Promise.all([
        getMemberProfile(user.userId),
        listMeals(),
        listMyTickets(user.userId),
        listAirports(),
      ]);
      setProfile(nextProfile);
      setMeals(nextMeals.filter((meal) => meal.isAvailable));
      setTickets(nextTickets);
      setAirports(nextAirports);
    } catch (error) {
      notify(error instanceof Error ? error.message : '旅客数据加载失败', 'error');
    }
  };

  useEffect(() => {
    void bootstrap();
  }, [user.userId]);

  const runSearch = async (event?: FormEvent) => {
    event?.preventDefault();
    setLoading(true);
    try {
      const data = await searchFlights(searchForm);
      setFlights(data);
      setSelectedFlight(data[0] ?? null);
      notify(`查询到 ${data.length} 个可售航段`, 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '航班查询失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const submitOrder = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedFlight) {
      notify('请先选择一个航段', 'error');
      return;
    }
    setLoading(true);
    try {
      const ticket = await createTicket({
        userId: user.userId,
        flightId: selectedFlight.flightId,
        segmentId: selectedFlight.segmentId,
        cabinClass: orderForm.cabinClass,
        passengerName: orderForm.passengerName,
        passengerIdNumber: orderForm.passengerIdNumber,
        mealId: orderForm.mealId ? Number(orderForm.mealId) : undefined,
      });
      setSelectedTicket(ticket);
      await refreshTickets();
      notify(`订单 ${ticket.orderNo} 已创建，请在 15 分钟内支付`, 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '下单失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const doPay = async (ticketId: number, change = false) => {
    setLoading(true);
    try {
      const ticket = change ? await payChangeTicket(ticketId) : await payTicket(ticketId);
      setSelectedTicket(ticket);
      await Promise.all([refreshProfile(), refreshTickets()]);
      notify(change ? '改签支付完成' : '支付成功，积分已更新', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '支付失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const doRefund = async (ticketId: number) => {
    setLoading(true);
    try {
      await refundTicket(ticketId, '前端演示退票');
      await Promise.all([refreshProfile(), refreshTickets()]);
      notify('退票成功，库存与积分已回补', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '退票失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadChangeTargets = async (ticket: Ticket) => {
    setSelectedTicket(ticket);
    setLoading(true);
    try {
      const current = flights.find((item) => item.segmentId !== ticket.segmentId);
      if (current) {
        setChangeTargets(flights.filter((item) => item.segmentId !== ticket.segmentId));
      } else {
        setChangeTargets(await searchFlights({ departureAirportCode: 'PEK', arrivalAirportCode: 'TFU' }));
      }
      setChangeHistory(await getChangeHistory(ticket.ticketId));
    } catch (error) {
      notify(error instanceof Error ? error.message : '改签航班加载失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const doApplyChange = async (target: FlightSearchItem) => {
    if (!selectedTicket) {
      return;
    }
    setLoading(true);
    try {
      const ticket = await applyChange({
        ticketId: selectedTicket.ticketId,
        targetFlightId: target.flightId,
        targetSegmentId: target.segmentId,
        cabinClass: selectedTicket.cabinClass,
        changeReason,
      });
      setSelectedTicket(ticket);
      await refreshTickets();
      setChangeHistory(await getChangeHistory(selectedTicket.ticketId));
      notify(`改签单 ${ticket.orderNo} 已创建`, 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '申请改签失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="workspace passenger-grid">
      <section className="control-panel search-panel">
        <div className="panel-title">
          <PlaneTakeoff size={20} />
          <div>
            <h2>航班搜索</h2>
            <p>GET /flight/search</p>
          </div>
        </div>
        <form onSubmit={runSearch} className="form-grid route-form">
          <label>
            出发机场
            <select
              value={searchForm.departureAirportCode}
              onChange={(event) => setSearchForm((form) => ({ ...form, departureAirportCode: event.target.value }))}
            >
              {airportOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            到达机场
            <select
              value={searchForm.arrivalAirportCode}
              onChange={(event) => setSearchForm((form) => ({ ...form, arrivalAirportCode: event.target.value }))}
            >
              {airportOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            日期
            <input
              type="date"
              value={searchForm.flightDate}
              onChange={(event) => setSearchForm((form) => ({ ...form, flightDate: event.target.value }))}
            />
          </label>
          <button className="primary-action" disabled={loading}>
            <Search size={18} />
            查询航班
          </button>
        </form>
      </section>

      <section className="member-board">
        <div className="member-copy">
          <p className="eyebrow">MEMBER STATUS</p>
          <h2>{profile?.memberLevel === 'VIP' ? 'VIP 会员' : '普通会员'}</h2>
          <p>{profile?.points ?? user.points} / 1000 积分</p>
        </div>
        <div className="radar-ring">
          <span>{vipProgress}%</span>
        </div>
        <div className="vip-bar">
          <span style={{ width: `${vipProgress}%` }} />
        </div>
        <p className="small-note">VIP 购票自动按 9 折计算 paymentAmount。</p>
      </section>

      <section className="results-lane">
        {flights.length === 0 ? (
          <div className="empty-state">
            <PlaneLanding size={36} />
            <strong>选择出发地、到达地和日期</strong>
            <p>航班结果会以机场代码、时间轴、余票和价格卡片展示。</p>
          </div>
        ) : (
          flights.map((flight) => (
            <FlightCard
              key={flight.segmentId}
              flight={flight}
              airportLookup={airportLookup}
              selected={selectedFlight?.segmentId === flight.segmentId}
              onSelect={() => setSelectedFlight(flight)}
            />
          ))
        )}
      </section>

      <section className="control-panel order-panel">
        <div className="panel-title">
          <CreditCard size={20} />
          <div>
            <h2>下单与支付</h2>
            <p>POST /ticket/create · POST /ticket/pay</p>
          </div>
        </div>
        {selectedFlight && (
          <div className="route-summary">
            <div>
              <strong>{selectedFlight.originAirportCode}</strong>
              <small>{airportCodeLabel(selectedFlight.originAirportCode, airportLookup)}</small>
            </div>
            <span>{selectedFlight.flightNumber}</span>
            <div>
              <strong>{selectedFlight.destinationAirportCode}</strong>
              <small>{airportCodeLabel(selectedFlight.destinationAirportCode, airportLookup)}</small>
            </div>
          </div>
        )}
        <form onSubmit={submitOrder} className="form-grid two-col">
          <label>
            舱位
            <select
              value={orderForm.cabinClass}
              onChange={(event) =>
                setOrderForm((form) => ({ ...form, cabinClass: event.target.value as CabinClass }))
              }
            >
              <option value="ECONOMY">经济舱</option>
              <option value="FIRST_CLASS">头等舱</option>
            </select>
          </label>
          <label>
            餐食
            <select
              value={orderForm.mealId}
              onChange={(event) => setOrderForm((form) => ({ ...form, mealId: event.target.value }))}
            >
              <option value="">不预订餐食</option>
              {meals.map((meal) => (
                <option key={meal.mealId} value={meal.mealId}>
                  {meal.mealName}
                </option>
              ))}
            </select>
          </label>
          <label>
            乘机人
            <input
              value={orderForm.passengerName}
              onChange={(event) => setOrderForm((form) => ({ ...form, passengerName: event.target.value }))}
            />
          </label>
          <label>
            证件号
            <input
              value={orderForm.passengerIdNumber}
              onChange={(event) => setOrderForm((form) => ({ ...form, passengerIdNumber: event.target.value }))}
            />
          </label>
          <button className="primary-action wide" disabled={loading || !selectedFlight}>
            <TicketIcon size={18} />
            创建订单
          </button>
        </form>
        {selectedTicket && (
          <div className="ticket-slip">
            <span>{selectedTicket.orderNo}</span>
            <strong>{statusText(selectedTicket.ticketStatus)}</strong>
            <p>
              原价 {formatMoney(selectedTicket.priceAmount)} · 应付 {formatMoney(selectedTicket.paymentAmount)}
            </p>
            {selectedTicket.ticketStatus === 'PENDING_PAYMENT' && (
              <button className="secondary-action" onClick={() => doPay(selectedTicket.ticketId, Boolean(selectedTicket.originalTicketId))}>
                <CreditCard size={16} />
                {selectedTicket.originalTicketId ? '支付改签单' : '立即支付'}
              </button>
            )}
          </div>
        )}
      </section>

      <section className="control-panel tickets-panel">
        <div className="panel-title">
          <ClipboardList size={20} />
          <div>
            <h2>我的订单</h2>
            <p>GET /ticket/my · 退票/改签均为 POST</p>
          </div>
        </div>
        <div className="ticket-list">
          {tickets.map((ticket) => (
            <div className="ticket-row" key={ticket.ticketId}>
              <div className="ticket-main">
                <strong>#{ticket.ticketId} {statusText(ticket.ticketStatus)} · {ticket.orderNo}</strong>
                <span>{orderRouteSummary(ticket, ticket, airportLookup)} · {cabinText(ticket.cabinClass)}</span>
                <em>原价 {formatMoney(ticket.priceAmount)} · 应付 {formatMoney(ticket.paymentAmount)}</em>
              </div>
              <div className="row-actions">
                {ticket.ticketStatus === 'PENDING_PAYMENT' && (
                  <button className="mini-button" onClick={() => doPay(ticket.ticketId, Boolean(ticket.originalTicketId))}>
                    支付
                  </button>
                )}
                {ticket.ticketStatus === 'PAID' && (
                  <>
                    <button className="mini-button" onClick={() => doRefund(ticket.ticketId)}>退票</button>
                    <button className="mini-button" onClick={() => loadChangeTargets(ticket)}>改签</button>
                  </>
                )}
              </div>
            </div>
          ))}
          {tickets.length === 0 && <p className="small-note">暂无订单。</p>}
        </div>
      </section>

      <section className="control-panel change-panel">
        <div className="panel-title">
          <RotateCcw size={20} />
          <div>
            <h2>改签工作台</h2>
            <p>链式 OriginalTicketId 指向上一张票</p>
          </div>
        </div>
        <label>
          改签原因
          <input value={changeReason} onChange={(event) => setChangeReason(event.target.value)} />
        </label>
        <div className="compact-list">
          {changeTargets.slice(0, 4).map((target) => (
            <button key={target.segmentId} className="change-target" onClick={() => doApplyChange(target)}>
              <span>{airportCodeLabel(target.originAirportCode, airportLookup)} → {airportCodeLabel(target.destinationAirportCode, airportLookup)}</span>
              <strong>{target.flightNumber}</strong>
              <em>{target.flightDate} · {shortTime(target.plannedDepartureTime)}-{shortTime(target.plannedArrivalTime)} · {formatMoney(target.economyPrice)}</em>
            </button>
          ))}
          {changeTargets.length === 0 && <p className="small-note">选择一个已支付订单后加载可改签航段。</p>}
        </div>
        <div className="history-stack">
          {changeHistory.map((ticket) => (
            <span key={ticket.ticketId}>#{ticket.ticketId} ← 原票 #{ticket.originalTicketId}</span>
          ))}
        </div>
      </section>

      <section className="kpi-ribbon">
        <Kpi label="待支付" value={pendingTickets.length} />
        <Kpi label="已出票" value={paidTickets.length} />
        <Kpi label="餐食选项" value={meals.length} />
      </section>
    </main>
  );
}

function FlightCard({
  flight,
  airportLookup,
  selected,
  onSelect,
}: {
  flight: FlightSearchItem;
  airportLookup: Record<string, Airport>;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button className={`flight-card ${selected ? 'selected' : ''}`} onClick={onSelect}>
      <div className="flight-card-head">
        <span>{flight.flightNumber}</span>
        <em>{statusText(flight.flightStatus)}</em>
      </div>
      <div className="airport-pair">
        <div>
          <strong>{flight.originAirportCode}</strong>
          <small>{airportCodeLabel(flight.originAirportCode, airportLookup)}</small>
          <time>{shortTime(flight.plannedDepartureTime)}</time>
        </div>
        <div className="flight-path">
          <Plane size={18} />
        </div>
        <div>
          <strong>{flight.destinationAirportCode}</strong>
          <small>{airportCodeLabel(flight.destinationAirportCode, airportLookup)}</small>
          <time>{shortTime(flight.plannedArrivalTime)}</time>
        </div>
      </div>
      <div className="seat-grid">
        <span>经济舱 {flight.economyRemainingSeats} · {formatMoney(flight.economyPrice)}</span>
        <span>头等舱 {flight.firstClassRemainingSeats} · {formatMoney(flight.firstClassPrice)}</span>
      </div>
    </button>
  );
}

function AdminWorkspace({ notify }: { notify: (message: string, kind?: Toast['kind']) => void }) {
  const [tab, setTab] = useState<AdminTab>('overview');
  const [dashboard, setDashboard] = useState<DashboardSummary>({});
  const [cities, setCities] = useState<City[]>([]);
  const [airports, setAirports] = useState<Airport[]>([]);
  const [aircraft, setAircraft] = useState<Aircraft[]>([]);
  const [flights, setFlights] = useState<Flight[]>([]);
  const [segments, setSegments] = useState<FlightSegment[]>([]);
  const [meals, setMeals] = useState<MealOption[]>([]);
  const [users, setUsers] = useState<AuthUser[]>([]);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [editingSegmentId, setEditingSegmentId] = useState<number | null>(null);
  const [flightForm, setFlightForm] = useState({
    flightNumber: 'MU9001',
    flightDate: '2026-07-01',
    aircraftRegNo: 'B-1001',
    flightStatus: 'NORMAL',
    departureAirportCode: 'PEK',
    arrivalAirportCode: 'SHA',
    remark: '管理员演示航班',
  });
  const [segmentForm, setSegmentForm] = useState({
    segmentId: undefined as number | undefined,
    flightId: '',
    originStopNo: 2,
    destinationStopNo: 3,
    originAirportCode: 'PEK',
    destinationAirportCode: 'SHA',
    plannedDepartureTime: '2026-07-01T11:00',
    plannedArrivalTime: '2026-07-01T13:00',
    firstClassRemainingSeats: 8,
    economyRemainingSeats: 50,
    firstClassPrice: 1800,
    economyPrice: 1000,
    remark: '管理员演示航段',
  });
  const [mealForm, setMealForm] = useState({ mealName: '低脂餐', mealType: 'LOW_FAT', isAvailable: true, description: '演示餐食' });
  const [resourceForm, setResourceForm] = useState({
    cityName: '杭州',
    cityCode: 'HGH',
    country: '中国',
    airportCode: 'HGH',
    airportName: '杭州萧山国际机场',
    cityId: '',
    aircraftRegNo: 'B-9901',
    aircraftType: 'C919',
    manufacturer: 'COMAC',
    totalFirstClassSeats: 8,
    totalEconomySeats: 156,
  });

  const refreshAll = async () => {
    try {
      const [nextDashboard, nextCities, nextAirports, nextAircraft, nextFlights, nextSegments, nextMeals, nextUsers, nextTickets] =
        await Promise.all([
          getDashboardSummary(),
          listCities(),
          listAirports(),
          listAircraft(),
          listFlights(),
          listSegments(),
          listAdminMeals(),
          listUsers(),
          listTickets(),
        ]);
      setDashboard(nextDashboard);
      setCities(nextCities);
      setAirports(nextAirports);
      setAircraft(nextAircraft);
      setFlights(nextFlights);
      setSegments(nextSegments);
      setMeals(nextMeals);
      setUsers(nextUsers);
      setTickets(nextTickets);
      setSegmentForm((form) => ({ ...form, flightId: form.flightId || String(nextFlights[0]?.flightId ?? '') }));
      setResourceForm((form) => ({ ...form, cityId: form.cityId || String(nextCities[0]?.cityId ?? '') }));
    } catch (error) {
      notify(error instanceof Error ? error.message : '管理员数据加载失败', 'error');
    }
  };

  const airportOptions = airports.map((airport) => ({
    value: airport.airportCode,
    label: airportLabel(airport),
  }));

  useEffect(() => {
    void refreshAll();
  }, []);

  const submitFlight = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await saveFlight(flightForm);
      await refreshAll();
      notify('航班已保存，使用 POST /admin/flight/update', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '航班保存失败', 'error');
    }
  };

  const submitSegment = async (event: FormEvent) => {
    event.preventDefault();
    if (!segmentForm.flightId) {
      notify('请先选择所属航班', 'error');
      return;
    }
    try {
      await saveSegment({
        ...segmentForm,
        segmentId: editingSegmentId ?? undefined,
        flightId: Number(segmentForm.flightId),
        plannedDepartureTime: segmentForm.plannedDepartureTime,
        plannedArrivalTime: segmentForm.plannedArrivalTime,
      });
      setEditingSegmentId(null);
      setSegmentForm((form) => ({ ...form, segmentId: undefined }));
      await refreshAll();
      notify('航段已保存，库存仍属于 FlightSegment', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '航段保存失败', 'error');
    }
  };

  const submitMeal = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await saveMeal(mealForm);
      await refreshAll();
      notify('餐食已保存', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '餐食保存失败', 'error');
    }
  };

  const submitCity = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const city = await saveCity({
        cityName: resourceForm.cityName,
        cityCode: resourceForm.cityCode,
        country: resourceForm.country,
      });
      setResourceForm((form) => ({ ...form, cityId: String(city.cityId) }));
      await refreshAll();
      notify('城市已保存', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '城市保存失败', 'error');
    }
  };

  const submitAirport = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await saveAirport({
        airportCode: resourceForm.airportCode,
        airportName: resourceForm.airportName,
        cityId: Number(resourceForm.cityId),
        isInternational: true,
      });
      await refreshAll();
      notify('机场已保存', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '机场保存失败', 'error');
    }
  };

  const submitAircraft = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await saveAircraft({
        aircraftRegNo: resourceForm.aircraftRegNo,
        aircraftType: resourceForm.aircraftType,
        manufacturer: resourceForm.manufacturer,
        totalFirstClassSeats: resourceForm.totalFirstClassSeats,
        totalEconomySeats: resourceForm.totalEconomySeats,
        status: 'NORMAL',
        remark: '管理员演示飞机',
      });
      await refreshAll();
      notify('飞机已保存', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '飞机保存失败', 'error');
    }
  };

  const expireOrders = async () => {
    try {
      const count = await processExpiredOrders();
      await refreshAll();
      notify(`已扫描过期订单 ${count} 单`, 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '过期扫描失败', 'error');
    }
  };

  const safeDisableNotice = (name: string) => {
    notify(`${name} 没有 ER 图停用字段，前端不执行破坏性删除`, 'info');
  };

  const doDisableFlight = async (flightId: number) => {
    const ok = window.confirm('确认停用该航班吗？停用后旅客端不可继续售卖，可在管理员端恢复为 NORMAL。');
    if (!ok) {
      return;
    }
    try {
      await disableFlight(flightId);
      await refreshAll();
      notify('航班已停用', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '停用失败', 'error');
    }
  };

  const doEnableFlight = async (flightId: number) => {
    try {
      await enableFlight(flightId);
      await refreshAll();
      notify('航班已恢复为 NORMAL', 'success');
    } catch (error) {
      notify(error instanceof Error ? error.message : '恢复失败', 'error');
    }
  };

  const editSegment = (segment: FlightSegment) => {
    setEditingSegmentId(segment.segmentId);
    setSegmentForm({
      segmentId: segment.segmentId,
      flightId: String(segment.flight?.flightId ?? ''),
      originStopNo: segment.originStopNo,
      destinationStopNo: segment.destinationStopNo,
      originAirportCode: segment.originAirportCode,
      destinationAirportCode: segment.destinationAirportCode,
      plannedDepartureTime: formatForDateTimeLocal(segment.plannedDepartureTime),
      plannedArrivalTime: formatForDateTimeLocal(segment.plannedArrivalTime),
      firstClassRemainingSeats: segment.firstClassRemainingSeats,
      economyRemainingSeats: segment.economyRemainingSeats,
      firstClassPrice: Number(segment.firstClassPrice),
      economyPrice: Number(segment.economyPrice),
      remark: segment.remark ?? '',
    });
  };

  const resetSegmentForm = () => {
    setEditingSegmentId(null);
    setSegmentForm({
      segmentId: undefined,
      flightId: String(flights[0]?.flightId ?? ''),
      originStopNo: 1,
      destinationStopNo: 2,
      originAirportCode: airports[0]?.airportCode ?? 'PEK',
      destinationAirportCode: airports[1]?.airportCode ?? 'SHA',
      plannedDepartureTime: '2026-07-01T08:00',
      plannedArrivalTime: '2026-07-01T10:00',
      firstClassRemainingSeats: 8,
      economyRemainingSeats: 50,
      firstClassPrice: 1800,
      economyPrice: 1000,
      remark: '管理员演示航段',
    });
  };

  return (
    <main className="workspace admin-grid">
      <aside className="admin-nav">
        {[
          ['overview', Gauge, '概览'],
          ['flights', PlaneTakeoff, '航班'],
          ['segments', PlaneLanding, '航段'],
          ['resources', BadgeCheck, '资源'],
          ['tickets', ClipboardList, '订单'],
        ].map(([id, Icon, label]) => (
          <button key={id as string} className={tab === id ? 'active' : ''} onClick={() => setTab(id as AdminTab)}>
            <Icon size={18} />
            {label as string}
          </button>
        ))}
      </aside>

      {tab === 'overview' && (
        <section className="admin-content">
          <div className="kpi-ribbon admin-kpis">
            <Kpi label="用户" value={dashboard.userCount ?? 0} />
            <Kpi label="航班" value={dashboard.flightCount ?? 0} />
            <Kpi label="航段" value={dashboard.segmentCount ?? 0} />
            <Kpi label="订单" value={dashboard.ticketCount ?? 0} />
          </div>
          <div className="control-panel">
            <div className="panel-title">
              <RefreshCw size={20} />
              <div>
                <h2>演示辅助</h2>
                <p>POST /admin/job/expire-order</p>
              </div>
            </div>
            <button className="primary-action" onClick={expireOrders}>
              <RefreshCw size={18} />
              触发过期订单扫描
            </button>
          </div>
        </section>
      )}

      {tab === 'flights' && (
        <section className="admin-content two-panel">
          <div className="control-panel">
            <div className="panel-title">
              <PlaneTakeoff size={20} />
              <div>
                <h2>新增/修改航班</h2>
                <p>POST /admin/flight/update</p>
              </div>
            </div>
            <form onSubmit={submitFlight} className="form-grid two-col">
              <TextInput label="航班号" value={flightForm.flightNumber} onChange={(value) => setFlightForm((form) => ({ ...form, flightNumber: value }))} />
              <label>
                日期
                <input type="date" value={flightForm.flightDate} onChange={(event) => setFlightForm((form) => ({ ...form, flightDate: event.target.value }))} />
              </label>
              <SelectInput label="飞机" value={flightForm.aircraftRegNo} options={aircraft.map((item) => item.aircraftRegNo)} onChange={(value) => setFlightForm((form) => ({ ...form, aircraftRegNo: value }))} />
              <SelectInput label="状态" value={flightForm.flightStatus} options={['NORMAL', 'DELAYED', 'CANCELLED', 'COMPLETED', 'DISABLED']} onChange={(value) => setFlightForm((form) => ({ ...form, flightStatus: value }))} />
              <SelectInput label="出发机场" value={flightForm.departureAirportCode} options={airportOptions} onChange={(value) => setFlightForm((form) => ({ ...form, departureAirportCode: value }))} />
              <SelectInput label="到达机场" value={flightForm.arrivalAirportCode} options={airportOptions} onChange={(value) => setFlightForm((form) => ({ ...form, arrivalAirportCode: value }))} />
              <button className="primary-action wide">
                <Plus size={18} />
                保存航班
              </button>
            </form>
          </div>
          <DataTable
            title="航班列表 GET /admin/flight/list"
            rows={flights.slice(0, 10).map((flight) => ({
              key: String(flight.flightId),
              main: `${flight.flightNumber} · ${flight.departureAirportCode} → ${flight.arrivalAirportCode}`,
              meta: `${flight.flightDate} · ${statusText(flight.flightStatus)}`,
              action: (
                flight.flightStatus === 'DISABLED' ? (
                  <button className="mini-button success" onClick={() => void doEnableFlight(flight.flightId)}>
                    <Undo2 size={14} />
                    恢复
                  </button>
                ) : (
                  <button className="mini-button danger" onClick={() => void doDisableFlight(flight.flightId)}>
                    停用
                  </button>
                )
              ),
            }))}
          />
        </section>
      )}

      {tab === 'segments' && (
        <section className="admin-content two-panel">
          <div className="control-panel">
            <div className="panel-title">
              <PlaneLanding size={20} />
              <div>
                <h2>{editingSegmentId ? `编辑航段 #${editingSegmentId}` : '新增航段'}</h2>
                <p>POST /admin/segment/update</p>
              </div>
            </div>
            <form onSubmit={submitSegment} className="form-grid two-col">
              <SelectInput label="所属航班" value={segmentForm.flightId} options={flights.map((item) => String(item.flightId))} onChange={(value) => setSegmentForm((form) => ({ ...form, flightId: value }))} />
              <NumberInput label="起点站序" value={segmentForm.originStopNo} onChange={(value) => setSegmentForm((form) => ({ ...form, originStopNo: value }))} />
              <NumberInput label="终点站序" value={segmentForm.destinationStopNo} onChange={(value) => setSegmentForm((form) => ({ ...form, destinationStopNo: value }))} />
              <SelectInput label="起飞机场" value={segmentForm.originAirportCode} options={airportOptions} onChange={(value) => setSegmentForm((form) => ({ ...form, originAirportCode: value }))} />
              <SelectInput label="到达机场" value={segmentForm.destinationAirportCode} options={airportOptions} onChange={(value) => setSegmentForm((form) => ({ ...form, destinationAirportCode: value }))} />
              <label>
                计划起飞
                <input type="datetime-local" value={segmentForm.plannedDepartureTime} onChange={(event) => setSegmentForm((form) => ({ ...form, plannedDepartureTime: event.target.value }))} />
              </label>
              <label>
                计划到达
                <input type="datetime-local" value={segmentForm.plannedArrivalTime} onChange={(event) => setSegmentForm((form) => ({ ...form, plannedArrivalTime: event.target.value }))} />
              </label>
              <NumberInput label="经济舱余票" value={segmentForm.economyRemainingSeats} onChange={(value) => setSegmentForm((form) => ({ ...form, economyRemainingSeats: value }))} />
              <NumberInput label="头等舱余票" value={segmentForm.firstClassRemainingSeats} onChange={(value) => setSegmentForm((form) => ({ ...form, firstClassRemainingSeats: value }))} />
              <NumberInput label="经济舱价格" value={segmentForm.economyPrice} onChange={(value) => setSegmentForm((form) => ({ ...form, economyPrice: value }))} />
              <NumberInput label="头等舱价格" value={segmentForm.firstClassPrice} onChange={(value) => setSegmentForm((form) => ({ ...form, firstClassPrice: value }))} />
              <button className="primary-action wide">
                <Plus size={18} />
                保存航段
              </button>
              {editingSegmentId && (
                <button type="button" className="secondary-action wide" onClick={resetSegmentForm}>
                  取消编辑，改为新增
                </button>
              )}
            </form>
          </div>
          <DataTable
            title="航段列表 GET /admin/segment/list"
            rows={segments.slice(0, 12).map((segment) => ({
              key: String(segment.segmentId),
              main: `#${segment.segmentId} ${segment.originAirportCode} → ${segment.destinationAirportCode}`,
              meta: `${formatDateTime(segment.plannedDepartureTime)} · 经济舱 ${segment.economyRemainingSeats}`,
              action: (
                <div className="row-actions">
                  <button className="mini-button" onClick={() => editSegment(segment)}>
                    <Edit size={14} />
                    编辑
                  </button>
                  <button className="mini-button ghost" onClick={() => safeDisableNotice('FlightSegment')}>不删除</button>
                </div>
              ),
            }))}
          />
        </section>
      )}

      {tab === 'resources' && (
        <section className="admin-content resources-grid">
          <ResourceCard title="城市 POST /admin/city/update" onSubmit={submitCity}>
            <TextInput label="城市名" value={resourceForm.cityName} onChange={(value) => setResourceForm((form) => ({ ...form, cityName: value }))} />
            <TextInput label="城市代码" value={resourceForm.cityCode} onChange={(value) => setResourceForm((form) => ({ ...form, cityCode: value.toUpperCase() }))} />
            <TextInput label="国家" value={resourceForm.country} onChange={(value) => setResourceForm((form) => ({ ...form, country: value }))} />
            <button type="button" className="mini-button ghost" onClick={() => safeDisableNotice('City')}>不删除</button>
          </ResourceCard>
          <ResourceCard title="机场 POST /admin/airport/update" onSubmit={submitAirport}>
            <TextInput label="机场代码" value={resourceForm.airportCode} onChange={(value) => setResourceForm((form) => ({ ...form, airportCode: value.toUpperCase() }))} />
            <TextInput label="机场名" value={resourceForm.airportName} onChange={(value) => setResourceForm((form) => ({ ...form, airportName: value }))} />
            <SelectInput label="城市 ID" value={resourceForm.cityId} options={cities.map((city) => String(city.cityId))} onChange={(value) => setResourceForm((form) => ({ ...form, cityId: value }))} />
            <button type="button" className="mini-button ghost" onClick={() => safeDisableNotice('Airport')}>不删除</button>
          </ResourceCard>
          <ResourceCard title="飞机 POST /admin/aircraft/update" onSubmit={submitAircraft}>
            <TextInput label="注册号" value={resourceForm.aircraftRegNo} onChange={(value) => setResourceForm((form) => ({ ...form, aircraftRegNo: value.toUpperCase() }))} />
            <TextInput label="机型" value={resourceForm.aircraftType} onChange={(value) => setResourceForm((form) => ({ ...form, aircraftType: value }))} />
            <TextInput label="制造商" value={resourceForm.manufacturer} onChange={(value) => setResourceForm((form) => ({ ...form, manufacturer: value }))} />
            <NumberInput label="头等舱座位" value={resourceForm.totalFirstClassSeats} onChange={(value) => setResourceForm((form) => ({ ...form, totalFirstClassSeats: value }))} />
            <NumberInput label="经济舱座位" value={resourceForm.totalEconomySeats} onChange={(value) => setResourceForm((form) => ({ ...form, totalEconomySeats: value }))} />
            <button type="button" className="mini-button" onClick={() => void disableAircraft(resourceForm.aircraftRegNo).then(refreshAll).then(() => notify('飞机已停用', 'success')).catch((error) => notify(error instanceof Error ? error.message : '停用失败', 'error'))}>停用</button>
          </ResourceCard>
          <ResourceCard title="餐食 POST /admin/meal/update" onSubmit={submitMeal}>
            <TextInput label="餐食名" value={mealForm.mealName} onChange={(value) => setMealForm((form) => ({ ...form, mealName: value }))} />
            <TextInput label="类型" value={mealForm.mealType} onChange={(value) => setMealForm((form) => ({ ...form, mealType: value.toUpperCase() }))} />
            <TextInput label="描述" value={mealForm.description} onChange={(value) => setMealForm((form) => ({ ...form, description: value }))} />
            <button type="button" className="mini-button" onClick={() => meals[0] && void disableMeal(meals[0].mealId).then(refreshAll).then(() => notify('餐食已停用', 'success')).catch((error) => notify(error instanceof Error ? error.message : '停用失败', 'error'))}>停用第一项</button>
          </ResourceCard>
          <DataTable title="资源快照" rows={[
            ...cities.slice(0, 3).map((city) => ({ key: `city-${city.cityId}`, main: `${city.cityName} ${city.cityCode}`, meta: city.country })),
            ...airports.slice(0, 3).map((airport) => ({ key: `airport-${airport.airportCode}`, main: `${airport.airportCode} ${airport.airportName}`, meta: airport.city?.cityName ?? '-' })),
            ...aircraft.slice(0, 3).map((item) => ({ key: `aircraft-${item.aircraftRegNo}`, main: `${item.aircraftRegNo} ${item.aircraftType}`, meta: item.status })),
          ]} />
        </section>
      )}

      {tab === 'tickets' && (
        <section className="admin-content two-panel">
          <DataTable
            title="用户列表 GET /admin/user/list"
            rows={users.slice(0, 10).map((item) => ({
              key: String(item.userId),
              main: `${item.userName} · ${item.loginAccount}`,
              meta: `${item.userType} · ${item.memberLevel} · ${item.points} 分`,
            }))}
          />
          <DataTable
            title="订单列表 GET /admin/ticket/list"
            rows={tickets.slice(0, 14).map((ticket) => ({
              key: String(ticket.ticketId),
              main: `#${ticket.ticketId} ${ticket.orderNo}`,
              meta: `${statusText(ticket.ticketStatus)} · ${formatMoney(ticket.paymentAmount)} · 原票 ${ticket.originalTicketId ?? '-'}`,
            }))}
          />
        </section>
      )}
    </main>
  );
}

function Kpi({ label, value }: { label: string; value: number }) {
  return (
    <div className="kpi-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function TextInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label>
      {label}
      <input value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function NumberInput({ label, value, onChange }: { label: string; value: number; onChange: (value: number) => void }) {
  return (
    <label>
      {label}
      <input type="number" value={value} onChange={(event) => onChange(Number(event.target.value))} />
    </label>
  );
}

type SelectOption = string | { value: string; label: string };

function SelectInput({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: SelectOption[];
  onChange: (value: string) => void;
}) {
  return (
    <label>
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => {
          const value = typeof option === 'string' ? option : option.value;
          const label = typeof option === 'string' ? option : option.label;
          return (
          <option key={value} value={value}>
            {label}
          </option>
          );
        })}
      </select>
    </label>
  );
}

function ResourceCard({
  title,
  onSubmit,
  children,
}: {
  title: string;
  onSubmit: (event: FormEvent) => void;
  children: React.ReactNode;
}) {
  return (
    <form className="control-panel resource-card" onSubmit={onSubmit}>
      <div className="panel-title">
        <BadgeCheck size={20} />
        <div>
          <h2>{title}</h2>
          <p>变更操作使用 POST</p>
        </div>
      </div>
      <div className="form-grid">{children}</div>
      <button className="primary-action">
        <Plus size={18} />
        保存
      </button>
    </form>
  );
}

function DataTable({
  title,
  rows,
}: {
  title: string;
  rows: { key: string; main: string; meta: string; action?: React.ReactNode }[];
}) {
  return (
    <section className="control-panel data-panel">
      <div className="panel-title">
        <Coffee size={20} />
        <div>
          <h2>{title}</h2>
          <p>查询操作使用 GET</p>
        </div>
      </div>
      <div className="data-list">
        {rows.map((row) => (
          <div className="data-row" key={row.key}>
            <div>
              <strong>{row.main}</strong>
              <span>{row.meta}</span>
            </div>
            {row.action}
          </div>
        ))}
        {rows.length === 0 && <p className="small-note">暂无数据。</p>}
      </div>
    </section>
  );
}

export default App;
