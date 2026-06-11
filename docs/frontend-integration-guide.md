# 航空票务系统前端对接文档

本文档供前端同学参考，内容严格基于当前项目后端实际代码生成。

后端代码基准：

- 控制器目录：`backend/src/main/java/com/example/airticket/controller`
- DTO 目录：`backend/src/main/java/com/example/airticket/dto`
- 前端现有 API 封装：`frontend/src/api/airticket.ts`
- 统一响应类：`backend/src/main/java/com/example/airticket/common/ApiResponse.java`

重要原则：

- 后端地址固定为 `http://localhost:8080/api`。
- 查询类接口全部使用 `GET`。
- 登录、注册、下单、支付、退票、改签、管理员保存、停用、恢复、注销等状态变更全部使用 `POST`。
- 前端字段名使用 `camelCase`，必须和本文档一致。
- 数据库和业务规则永远以 ER 图为准，前端不要要求新增表或新增落库字段。

---

## 1. 前端页面和组件清单

当前项目已有一个 React 前端示例，核心页面结构在 `frontend/src/App.tsx`。如果同学 A 重新实现，也建议按下面组件拆分。

### 1.1 应用外壳 App

功能：

- 负责保存当前登录用户 `currentUser`。
- 根据后端返回的 `userType` 判断进入旅客端还是管理员端。
- 保存登录状态到 `localStorage`，刷新页面后可恢复。
- 展示顶部导航、当前用户、退出登录、旅客注销按钮。
- 统一展示操作提示，例如登录成功、支付失败、航班停用成功。

关键数据：

| 状态名 | 类型 | 说明 |
|---|---|---|
| `currentUser` | `AuthUser | null` | 当前登录用户，未登录时为空 |
| `workspace` | `"passenger" | "admin"` | 当前显示旅客端还是管理员端 |
| `toast` | `{ kind, message }` | 右上角提示信息 |

页面跳转规则：

| 后端返回字段 | 值 | 前端进入页面 |
|---|---|---|
| `userType` | `PASSENGER` | 旅客端 |
| `userType` | `ADMIN` | 管理员端 |

---

### 1.2 登录注册页 AuthScreen

必须包含：

| 组件 | 功能 |
|---|---|
| 登录/注册切换按钮 | 在账号登录和乘客注册之间切换 |
| 登录账号输入框 | 填 `loginAccount` |
| 密码输入框 | 填 `password` |
| 登录按钮 | 调用 `POST /api/auth/login` |
| 注册账号输入框 | 填 `loginAccount` |
| 注册密码输入框 | 填 `password`，至少 6 位 |
| 乘客姓名输入框 | 填 `userName` |
| 手机号输入框 | 填 `phoneNumber`，必须 11 位数字 |
| 邮箱输入框 | 填 `email`，必须是邮箱格式 |
| 身份证号输入框 | 填 `idNumber`，必须是合法中国身份证格式 |
| 注册按钮 | 调用 `POST /api/auth/register` |
| 演示账号快捷按钮 | 可自动填入 `admin/admin123`、`passengerA/pass123`、`passengerB/pass123` |

注册前端校验建议：

| 字段 | 规则 |
|---|---|
| `loginAccount` | 非空 |
| `password` | 非空，长度至少 6 |
| `userName` | 非空 |
| `phoneNumber` | `^\d{11}$` |
| `email` | `^[^\s@]+@[^\s@]+\.[^\s@]+$` |
| `idNumber` | `^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$` |

注意：后端也会做同样校验，前端校验是为了让用户更早看到提示。

---

### 1.3 旅客端 PassengerWorkspace

旅客端用于普通乘客完成完整业务闭环。

必须包含：

| 区域 | 组件 | 功能 | 对应接口 |
|---|---|---|---|
| 会员信息区 | 会员卡片 | 显示姓名、积分、会员等级、VIP 进度 | `GET /api/member/profile` |
| 航班搜索区 | 出发机场下拉框 | 选择 `departureAirportCode`，不要让用户手输代码 | `GET /api/admin/airport/list` |
| 航班搜索区 | 到达机场下拉框 | 选择 `arrivalAirportCode` | `GET /api/admin/airport/list` |
| 航班搜索区 | 日期选择器 | 选择 `flightDate`，格式 `yyyy-MM-dd` | `GET /api/flight/search` |
| 航班结果区 | 航班卡片列表 | 展示航班号、日期、起降机场、起降时间、余票、价格 | `GET /api/flight/search` |
| 下单区 | 舱位选择 | `ECONOMY` 或 `FIRST_CLASS` | `POST /api/ticket/create` |
| 下单区 | 乘机人姓名 | `passengerName` | `POST /api/ticket/create` |
| 下单区 | 乘机人身份证 | `passengerIdNumber`，后端会摘要后入库 | `POST /api/ticket/create` |
| 下单区 | 餐食选择 | 可选 `mealId` | `GET /api/meal/list` |
| 下单区 | 创建订单按钮 | 锁库存并生成待支付订单 | `POST /api/ticket/create` |
| 订单区 | 我的订单列表 | 展示航班号、日期、路线、时间、舱位、状态、金额 | `GET /api/ticket/my` |
| 订单区 | 支付按钮 | 支付普通订单或改签订单 | `POST /api/ticket/pay`、`POST /api/ticket/change/pay` |
| 订单区 | 退票按钮 | 仅对 `PAID` 订单显示 | `POST /api/ticket/refund` |
| 改签区 | 改签目标航班列表 | 从搜索结果中选新航段 | `POST /api/ticket/change/apply` |
| 改签区 | 改签历史 | 展示某张旧票产生的新票 | `GET /api/ticket/change/history` |
| 账号区 | 注销账号按钮 | 注销乘客账号，不物理删除历史订单 | `POST /api/auth/cancel` |

机场显示格式建议：

```text
城市名（机场简称）机场代码
例如：上海（浦东）PVG、北京（首都）PEK
```

当前前端工具函数 `airportLabel` 已经按这个格式实现。

---

### 1.4 管理员端 AdminWorkspace

管理员端用于演示基础数据维护和订单查看。所有删除都不要做物理删除。

建议分成 5 个 Tab：

| Tab | 功能 | 常用接口 |
|---|---|---|
| 概览 | 展示用户、航班、航段、订单、餐食数量；手动触发过期订单扫描 | `GET /api/admin/dashboard/summary`、`POST /api/admin/job/expire-order` |
| 航班 | 新增/修改航班、停用航班、恢复航班 | `GET /api/admin/flight/list`、`POST /api/admin/flight/update`、`POST /api/admin/flight/disable`、`POST /api/admin/flight/enable` |
| 航段 | 新增/修改航段，编辑时必须带 `segmentId` | `GET /api/admin/segment/list`、`POST /api/admin/segment/update` |
| 资源 | 城市、机场、飞机、餐食维护 | 城市/机场/飞机/餐食接口 |
| 订单 | 查看用户列表和订单列表 | `GET /api/admin/user/list`、`GET /api/admin/ticket/list` |

管理员端安全删除规则：

| 对象 | 后端实际能力 | 前端按钮建议 |
|---|---|---|
| `Flight` | 可 `DISABLED`，也可恢复 `NORMAL` | 停用前二次确认；停用后显示“恢复” |
| `Aircraft` | 可把 `Status` 改成 `DISABLED` | 显示“停用飞机” |
| `MealOption` | 可把 `IsAvailable=false` | 显示“停用餐食” |
| `City` | ER 图无停用字段 | 不删除，只提示“不执行破坏性删除” |
| `Airport` | ER 图无停用字段 | 不删除，只提示“不执行破坏性删除” |
| `FlightSegment` | ER 图无停用字段 | 不删除，只提示“不执行破坏性删除” |

---

## 2. 前后端通信链路

### 2.1 基本链路

```text
浏览器页面
  ↓ fetch / axios
React API 封装
  ↓ HTTP + JSON
Spring Boot Controller
  ↓ DTO 参数
Service 业务事务
  ↓ Repository / JPA
MySQL 9.6
```

前端不直接连接数据库。前端只调用后端接口。

---

### 2.2 后端基础地址

```ts
const API_BASE_URL = "http://localhost:8080/api";
```

例如：

| 相对路径 | 完整地址 |
|---|---|
| `/auth/login` | `http://localhost:8080/api/auth/login` |
| `/flight/search` | `http://localhost:8080/api/flight/search` |
| `/ticket/create` | `http://localhost:8080/api/ticket/create` |

---

### 2.3 GET 调用规范

GET 用于查询，不要放 JSON body，参数拼在 URL 后面。

示例：

```ts
fetch("http://localhost:8080/api/flight/search?departureAirportCode=PEK&arrivalAirportCode=SHA&flightDate=2026-07-01", {
  method: "GET"
});
```

---

### 2.4 POST 调用规范

POST 用于状态变更，必须发送 JSON。

示例：

```ts
fetch("http://localhost:8080/api/ticket/pay", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ ticketId: 1 })
});
```

---

### 2.5 统一响应格式

所有接口都返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-06-10T12:00:00"
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | number | 业务状态码，`0` 表示成功，非 0 表示失败 |
| `message` | string | 成功时通常是 `success`，失败时是错误原因 |
| `data` | any | 真正业务数据，失败时通常是 `null` |
| `timestamp` | string | 后端生成响应时间 |

前端统一处理规则：

```ts
if (payload.code !== 0) {
  throw new Error(payload.message || "请求失败");
}
return payload.data;
```

注意：后端业务错误通常仍然返回 JSON，所以前端不能只看 HTTP 状态码，必须判断 `code`。

---

## 3. 公共数据类型

### 3.1 枚举值

前端必须严格使用这些字符串。

| 类型 | 可选值 |
|---|---|
| `UserType` | `PASSENGER`、`ADMIN` |
| `MemberLevel` | `NORMAL`、`VIP` |
| `CabinClass` | `ECONOMY`、`FIRST_CLASS` |
| `TicketStatus` | `PENDING_PAYMENT`、`PAID`、`EXPIRED`、`REFUND_SUCCESS`、`CHANGE_SUCCESS` |
| `FlightStatus` | `NORMAL`、`DELAYED`、`CANCELLED`、`COMPLETED`、`DISABLED` |

---

### 3.2 AuthUser

登录、注册、获取当前用户返回。

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | number | 用户 ID |
| `loginAccount` | string | 登录账号 |
| `userName` | string | 用户姓名 |
| `userType` | `PASSENGER` 或 `ADMIN` | 用户角色 |
| `memberLevel` | `NORMAL` 或 `VIP` | 会员等级 |
| `points` | number | 积分 |
| `token` | string | 本地演示 token，格式类似 `LOCAL-2` |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

示例：

```json
{
  "userId": 2,
  "loginAccount": "passengerA",
  "userName": "演示乘客A",
  "userType": "PASSENGER",
  "memberLevel": "NORMAL",
  "points": 900,
  "token": "LOCAL-2",
  "createdAt": "2026-06-10T10:00:00",
  "updatedAt": "2026-06-10T10:00:00"
}
```

---

### 3.3 FlightSearchItem

航班搜索返回的是“可售航段”，不是单纯航班。

| 字段 | 类型 | 说明 |
|---|---|---|
| `flightId` | number | 航班 ID |
| `flightNumber` | string | 航班号 |
| `flightDate` | string | 航班日期，`yyyy-MM-dd` |
| `flightStatus` | string | 航班状态 |
| `aircraftRegNo` | string | 飞机注册号 |
| `departureAirportCode` | string | 航班整体出发机场代码 |
| `arrivalAirportCode` | string | 航班整体到达机场代码 |
| `segmentId` | number | 航段 ID，下单必须传这个 |
| `originStopNo` | number | 航段起点站序 |
| `destinationStopNo` | number | 航段终点站序 |
| `originAirportCode` | string | 航段出发机场代码 |
| `destinationAirportCode` | string | 航段到达机场代码 |
| `plannedDepartureTime` | string | 计划起飞时间 |
| `plannedArrivalTime` | string | 计划到达时间 |
| `firstClassRemainingSeats` | number | 头等舱剩余座位 |
| `economyRemainingSeats` | number | 经济舱剩余座位 |
| `firstClassPrice` | number | 头等舱原价 |
| `economyPrice` | number | 经济舱原价 |
| `isAvailable` | boolean | 是否至少还有一种舱位有余票 |

---

### 3.4 Ticket

订单相关接口返回。

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticketId` | number | 订单/机票 ID |
| `orderNo` | string | 订单号 |
| `ticketStatus` | string | 订单状态 |
| `userId` | number | 用户 ID |
| `flightId` | number | 航班 ID |
| `segmentId` | number | 航段 ID |
| `flightNumber` | string | 航班号 |
| `flightDate` | string | 航班日期 |
| `originAirportCode` | string | 航段出发机场 |
| `destinationAirportCode` | string | 航段到达机场 |
| `plannedDepartureTime` | string | 计划起飞 |
| `plannedArrivalTime` | string | 计划到达 |
| `cabinClass` | string | 舱位 |
| `passengerName` | string | 乘机人姓名 |
| `priceAmount` | number | 原价金额 |
| `paymentAmount` | number | 实付金额；VIP 会打 9 折，改签时可能是差价 |
| `originalTicketId` | number/null | 改签新票指向旧票 |
| `changeReason` | string/null | 改签原因 |
| `bookedAt` | string | 下单时间 |
| `paidAt` | string/null | 支付时间 |
| `issuedAt` | string/null | 出票时间 |
| `changedAt` | string/null | 改签完成时间 |
| `refundedAt` | string/null | 退票时间 |
| `expiredAt` | string/null | 支付截止时间 |
| `remark` | string/null | 备注 |

---

## 4. 接口详细规范

以下所有接口的完整前缀都是：

```text
http://localhost:8080/api
```

---

## 4.1 认证接口 Auth

### 4.1.1 乘客注册

| 项目 | 内容 |
|---|---|
| 地址 | `/auth/register` |
| 方法 | `POST` |
| 说明 | 注册新乘客，注册成功后直接返回登录用户信息 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `loginAccount` | string | 是 | `"newPassenger"` | 登录账号，不能重复 |
| `password` | string | 是 | `"pass123"` | 密码，至少 6 位 |
| `userName` | string | 是 | `"张三"` | 乘客姓名 |
| `phoneNumber` | string | 是 | `"13900000000"` | 11 位手机号 |
| `email` | string | 是 | `"zhangsan@example.com"` | 邮箱 |
| `idNumber` | string | 是 | `"110101199001010011"` | 身份证号，后端只保存摘要 |
| `idNumberDigest` | string | 否 | 不建议传 | 兼容字段，前端正常传 `idNumber` 即可 |

请求示例：

```json
{
  "loginAccount": "newPassenger",
  "password": "pass123",
  "userName": "张三",
  "phoneNumber": "13900000000",
  "email": "zhangsan@example.com",
  "idNumber": "110101199001010011"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 4,
    "loginAccount": "newPassenger",
    "userName": "张三",
    "userType": "PASSENGER",
    "memberLevel": "NORMAL",
    "points": 0,
    "token": "LOCAL-4",
    "createdAt": "2026-06-10T12:00:00",
    "updatedAt": "2026-06-10T12:00:00"
  },
  "timestamp": "2026-06-10T12:00:00"
}
```

常见失败：

| `code` | 含义 |
|---|---|
| `40901` | 登录账号已存在 |
| `40904` | 身份证号已存在 |
| `45003` | 密码为空 |
| `45006` | 密码少于 6 位 |
| `45008` | 手机号不是 11 位数字 |
| `45011` | 邮箱格式不正确 |
| `45012` | 身份证号格式不正确 |

---

### 4.1.2 登录

| 项目 | 内容 |
|---|---|
| 地址 | `/auth/login` |
| 方法 | `POST` |
| 说明 | 使用账号密码登录 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `loginAccount` | string | 是 | `"passengerA"` |
| `password` | string | 是 | `"pass123"` |

成功响应 `data` 类型：`AuthUser`。

演示账号：

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 900 分乘客 | `passengerA` | `pass123` |
| 普通乘客 | `passengerB` | `pass123` |

前端登录后必须判断：

```ts
if (user.userType === "ADMIN") {
  // 进入管理员页面
} else {
  // 进入旅客页面
}
```

---

### 4.1.3 退出登录

| 项目 | 内容 |
|---|---|
| 地址 | `/auth/logout` |
| 方法 | `POST` |
| 请求体 | `{}` 或空 JSON |
| 说明 | 本地演示版后端不维护 session，前端清空 localStorage 即可 |

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": null,
  "timestamp": "2026-06-10T12:00:00"
}
```

---

### 4.1.4 注销账号

| 项目 | 内容 |
|---|---|
| 地址 | `/auth/cancel` |
| 方法 | `POST` |
| 说明 | 注销乘客账号。后端不会物理删除用户，而是匿名化账号，避免破坏历史订单外键 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `userId` | number | 是 | `2` |

成功后前端必须：

- 清空 `localStorage` 中的用户信息。
- 回到登录页。
- 不再用旧账号自动登录。

常见失败：

| `code` | 含义 |
|---|---|
| `40401` | 用户不存在 |
| `45010` | 管理员账号不允许注销 |

---

### 4.1.5 获取当前用户

| 项目 | 内容 |
|---|---|
| 地址 | `/auth/me` |
| 方法 | `GET` |
| 说明 | 根据用户 ID 获取用户信息 |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `userId` | number | 是 | `2` |

示例：

```text
GET /api/auth/me?userId=2
```

---

## 4.2 会员接口 Member

### 4.2.1 会员资料

| 项目 | 内容 |
|---|---|
| 地址 | `/member/profile` |
| 兼容地址 | `/member/points` |
| 方法 | `GET` |
| 说明 | 查询会员积分、等级、VIP 门槛、折扣 |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `userId` | number | 是 | `2` |

成功响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | number | 用户 ID |
| `loginAccount` | string | 登录账号 |
| `userName` | string | 用户名 |
| `memberLevel` | string | `NORMAL` 或 `VIP` |
| `points` | number | 当前积分 |
| `vipThreshold` | number | 固定为 `1000` |
| `vipDiscountRate` | number | 固定为 `0.9` |

示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 2,
    "loginAccount": "passengerA",
    "userName": "演示乘客A",
    "memberLevel": "NORMAL",
    "points": 900,
    "vipThreshold": 1000,
    "vipDiscountRate": 0.9
  },
  "timestamp": "2026-06-10T12:00:00"
}
```

---

## 4.3 航班查询接口 Flight

### 4.3.1 搜索可售航段

| 项目 | 内容 |
|---|---|
| 地址 | `/flight/search` |
| 方法 | `GET` |
| 说明 | 查询可售航段。后端只返回 `NORMAL`、`DELAYED` 航班的航段 |

查询参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `departureCityId` | number | 否 | `1` | 出发城市 ID |
| `arrivalCityId` | number | 否 | `2` | 到达城市 ID |
| `departureAirportCode` | string | 否 | `"PEK"` | 出发机场代码 |
| `arrivalAirportCode` | string | 否 | `"SHA"` | 到达机场代码 |
| `flightDate` | string | 否 | `"2026-07-01"` | 指定单日查询 |
| `flightDateStart` | string | 否 | `"2026-06-28"` | 日期范围开始 |
| `flightDateEnd` | string | 否 | `"2026-07-04"` | 日期范围结束 |

推荐前端调用：

```text
GET /api/flight/search?departureAirportCode=PEK&arrivalAirportCode=SHA&flightDate=2026-07-01
```

注意：

- `flightDate` 优先级高于 `flightDateStart` 和 `flightDateEnd`。
- 如果不传日期，后端默认查询 `2026-06-28` 到 `2026-07-04`。
- 前端最好至少传出发和到达机场，否则可能查不到结果。

成功响应 `data` 类型：`FlightSearchItem[]`。

---

### 4.3.2 航段详情

| 项目 | 内容 |
|---|---|
| 地址 | `/flight/detail` |
| 方法 | `GET` |
| 说明 | 根据航段 ID 查询一个可售航段详情 |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `segmentId` | number | 是 | `1` |

---

### 4.3.3 查询某航班全部航段

| 项目 | 内容 |
|---|---|
| 地址 | `/flight/segments` |
| 方法 | `GET` |
| 说明 | 根据航班 ID 查询该航班的所有航段 |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `flightId` | number | 是 | `21` |

---

## 4.4 餐食接口 Meal

### 4.4.1 餐食列表

| 项目 | 内容 |
|---|---|
| 地址 | `/meal/list` |
| 方法 | `GET` |
| 说明 | 查询全部餐食 |

返回 `data` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `mealId` | number | 餐食 ID |
| `mealName` | string | 餐食名 |
| `mealType` | string | 餐食类型 |
| `isAvailable` | boolean | 是否可用 |
| `description` | string | 描述 |

前端下单时建议只展示 `isAvailable=true` 的餐食。

---

### 4.4.2 餐食详情

| 项目 | 内容 |
|---|---|
| 地址 | `/meal/detail` |
| 方法 | `GET` |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `mealId` | number | 是 | `1` |

---

## 4.5 订单接口 Ticket

### 4.5.1 创建订单

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/create` |
| 兼容地址 | `/ticket/book` |
| 方法 | `POST` |
| 说明 | 创建待支付订单，后端会锁定航段库存并扣减 1 个座位 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `userId` | number | 是 | `2` | 当前登录用户 ID |
| `flightId` | number | 是 | `49` | 搜索结果返回的航班 ID |
| `segmentId` | number | 是 | `49` | 搜索结果返回的航段 ID |
| `cabinClass` | string | 是 | `"ECONOMY"` | `ECONOMY` 或 `FIRST_CLASS` |
| `passengerName` | string | 是 | `"演示乘客A"` | 乘机人姓名 |
| `passengerIdNumber` | string | 是 | `"110101199001010011"` | 乘机人身份证，后端摘要后入库 |
| `passengerIdNumberDigest` | string | 否 | 不建议传 | 兼容字段 |
| `mealId` | number | 否 | `1` | 餐食 ID |

成功后：

- 订单状态是 `PENDING_PAYMENT`。
- `expiredAt` 是下单后 15 分钟。
- 库存已经扣减，所以前端应提示用户及时支付。

成功响应 `data` 类型：`Ticket`。

常见失败：

| `code` | 含义 |
|---|---|
| `40401` | 用户不存在 |
| `40405` | 航班不存在 |
| `40406` | 航段不存在 |
| `41001` | 航班已停用 |
| `41002` | 航班已取消 |
| `41003` | 航班已完成 |
| `41004` | 航段不属于该航班 |
| `41009` | 舱位类型非法 |
| `42001` | 当前航段余票不足 |
| `40408` | 餐食不存在 |
| `44001` | 餐食已停用 |

---

### 4.5.2 支付普通订单

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/pay` |
| 方法 | `POST` |
| 说明 | 支付普通待支付订单 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `ticketId` | number | 是 | `1` |

成功后：

- 订单状态从 `PENDING_PAYMENT` 变为 `PAID`。
- 设置 `paidAt` 和 `issuedAt`。
- 用户积分增加 `100`。
- 积分达到 `1000` 后自动升级为 `VIP`。

VIP 规则：

- 普通用户买票时，`paymentAmount = priceAmount`。
- VIP 用户买票时，`paymentAmount = priceAmount * 0.9`。
- `passengerA` 初始 900 分，第一张票支付后变 1000 分并升级 VIP；第二张票应显示 9 折实付。

---

### 4.5.3 查询订单详情

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/detail` |
| 方法 | `GET` |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `ticketId` | number | 是 | `1` |

---

### 4.5.4 查询我的订单

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/my` |
| 兼容地址 | `/ticket/list-by-user` |
| 方法 | `GET` |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `userId` | number | 是 | `2` |

前端订单列表必须展示：

- 订单号 `orderNo`
- 航班号 `flightNumber`
- 日期 `flightDate`
- 出发地 `originAirportCode`
- 目的地 `destinationAirportCode`
- 起降时间
- 舱位 `cabinClass`
- 状态 `ticketStatus`
- 原价 `priceAmount`
- 实付 `paymentAmount`

---

### 4.5.5 退票

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/refund` |
| 方法 | `POST` |
| 说明 | 退一张已支付订单 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `ticketId` | number | 是 | `1` |
| `remark` | string | 否 | `"前端演示退票"` |

成功后：

- 订单状态变为 `REFUND_SUCCESS`。
- 航段库存恢复 1 个座位。
- 用户积分减少 `100`，最低不低于 0。
- 如果积分低于 1000，会员等级会回到 `NORMAL`。

常见失败：

| `code` | 含义 |
|---|---|
| `42019` | 只有 `PAID` 状态订单允许退票 |

---

### 4.5.6 申请改签

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/change/apply` |
| 兼容地址 | `/ticket/change` |
| 方法 | `POST` |
| 说明 | 给一张已支付旧票创建一张新待支付改签单 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `ticketId` | number | 是 | `1` | 旧票 ID，必须是 `PAID` |
| `targetFlightId` | number | 推荐 | `50` | 新航班 ID |
| `targetSegmentId` | number | 推荐 | `50` | 新航段 ID |
| `flightId` | number | 否 | `50` | 兼容字段，等价于 `targetFlightId` |
| `segmentId` | number | 否 | `50` | 兼容字段，等价于 `targetSegmentId` |
| `cabinClass` | string | 否 | `"ECONOMY"` | 不传则沿用旧票舱位 |
| `changeReason` | string | 否 | `"改到更合适的航班"` | 改签原因 |

成功后：

- 创建一张新订单，状态为 `PENDING_PAYMENT`。
- 新订单 `originalTicketId` 指向旧票。
- 新订单 `paymentAmount` 是改签差价。如果新票更便宜，差价按 `0` 处理。
- 新航段库存会先扣减。
- 如果差价为 `0`，后端会立即完成改签：旧票 `CHANGE_SUCCESS`，新票 `PAID`。

---

### 4.5.7 支付改签单

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/change/pay` |
| 方法 | `POST` |
| 说明 | 支付改签差价 |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `ticketId` | number | 是 | `10` |

成功后：

- 旧票状态变为 `CHANGE_SUCCESS`。
- 新票状态变为 `PAID`。
- 新票 `originalTicketId` 仍然指向旧票。
- 改签链是 A → B → C，不是 A → C。

常见失败：

| `code` | 含义 |
|---|---|
| `42014` | 当前订单不是改签订单 |
| `42017` | 改签订单已过期 |
| `42018` | 旧票状态不允许改签 |

---

### 4.5.8 查询改签历史

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/change/history` |
| 方法 | `GET` |

查询参数：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `ticketId` | number | 是 | `1` |

说明：

- 返回所有 `originalTicketId = ticketId` 的新票。
- 用于展示某张票后续改签产生了哪些订单。

---

### 4.5.9 手动处理过期订单

| 项目 | 内容 |
|---|---|
| 地址 | `/ticket/expire-process` |
| 方法 | `POST` |
| 请求体 | `{}` |
| 说明 | 手动扫描过期待支付订单，返回处理数量 |

管理员端也有同等接口：`POST /api/admin/job/expire-order`。

---

## 4.6 管理员接口 Admin

管理员接口统一前缀：`/admin`。

### 4.6.1 用户管理

#### 查询用户列表

| 项目 | 内容 |
|---|---|
| 地址 | `/admin/user/list` |
| 方法 | `GET` |

返回实际是后端 `User` 实体列表，字段包括：

| 字段 | 说明 |
|---|---|
| `userId` | 用户 ID |
| `loginAccount` | 登录账号 |
| `userName` | 用户名 |
| `idNumberDigest` | 身份证摘要 |
| `passwordHash` | 密码哈希 |
| `userType` | 用户角色 |
| `phoneNumber` | 手机号 |
| `email` | 邮箱 |
| `points` | 积分 |
| `memberLevel` | 会员等级 |
| `createdAt` | 创建时间 |
| `updatedAt` | 更新时间 |

前端注意：`passwordHash` 和 `idNumberDigest` 是敏感字段，管理员页面不要展示给普通观众看。

#### 查询用户详情

| 地址 | 方法 | 参数 |
|---|---|---|
| `/admin/user/detail` | `GET` | `userId` |

---

### 4.6.2 城市管理

#### 查询城市列表

| 地址 | 方法 |
|---|---|
| `/admin/city/list` | `GET` |

返回字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `cityId` | number | 城市 ID |
| `cityName` | string | 城市名 |
| `cityCode` | string | 城市代码 |
| `country` | string | 国家 |

#### 查询城市详情

| 地址 | 方法 | 参数 |
|---|---|---|
| `/admin/city/detail` | `GET` | `cityId` |

#### 新增或修改城市

| 地址 | 方法 | 说明 |
|---|---|---|
| `/admin/city/add` | `POST` | 新增城市 |
| `/admin/city/update` | `POST` | 修改城市，也可新增 |

请求 JSON：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `cityId` | number | 修改时必填 | 城市 ID；新增可不传 |
| `cityName` | string | 是 | 城市名 |
| `cityCode` | string | 是 | 城市代码 |
| `country` | string | 是 | 国家 |

#### 停用城市

| 地址 | 方法 | 结果 |
|---|---|---|
| `/admin/city/disable` | `POST` | 固定返回业务错误，因为 `City` 表没有停用字段 |

前端不要做城市删除。

---

### 4.6.3 机场管理

#### 查询机场列表

| 地址 | 方法 | 参数 |
|---|---|---|
| `/admin/airport/list` | `GET` | 可选 `cityId` |

返回字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `airportCode` | string | 机场代码，主键 |
| `airportName` | string | 机场名 |
| `city` | object | 所属城市对象 |
| `isInternational` | boolean | 是否国际机场 |

#### 查询机场详情

| 地址 | 方法 | 参数 |
|---|---|---|
| `/admin/airport/detail` | `GET` | `airportCode` |

#### 新增或修改机场

| 地址 | 方法 |
|---|---|
| `/admin/airport/add` | `POST` |
| `/admin/airport/update` | `POST` |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `airportCode` | string | 是 | `"PVG"` |
| `airportName` | string | 是 | `"上海浦东国际机场"` |
| `cityId` | number | 是 | `2` |
| `isInternational` | boolean | 是 | `true` |

#### 停用机场

| 地址 | 方法 | 结果 |
|---|---|---|
| `/admin/airport/disable` | `POST` | 固定返回业务错误，因为 `Airport` 表没有停用字段 |

---

### 4.6.4 飞机管理

#### 查询飞机列表和详情

| 功能 | 地址 | 方法 | 参数 |
|---|---|---|---|
| 列表 | `/admin/aircraft/list` | `GET` | 无 |
| 详情 | `/admin/aircraft/detail` | `GET` | `aircraftRegNo` |

#### 新增或修改飞机

| 地址 | 方法 |
|---|---|
| `/admin/aircraft/add` | `POST` |
| `/admin/aircraft/update` | `POST` |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `aircraftRegNo` | string | 是 | `"B-1001"` |
| `aircraftType` | string | 是 | `"A320"` |
| `manufacturer` | string | 是 | `"Airbus"` |
| `totalFirstClassSeats` | number | 是 | `8` |
| `totalEconomySeats` | number | 是 | `150` |
| `status` | string | 是 | `"NORMAL"` |
| `remark` | string | 否 | `"演示飞机"` |

#### 停用飞机

| 地址 | 方法 |
|---|---|
| `/admin/aircraft/disable` | `POST` |

请求 JSON：

| 参数 | 类型 | 必填 |
|---|---|---|
| `aircraftRegNo` | string | 是 |

说明：后端会把飞机 `status` 保存为 `DISABLED`。

---

### 4.6.5 航班管理

#### 查询航班列表和详情

| 功能 | 地址 | 方法 | 参数 |
|---|---|---|---|
| 列表 | `/admin/flight/list` | `GET` | 无 |
| 详情 | `/admin/flight/detail` | `GET` | `flightId` |

#### 新增或修改航班

| 地址 | 方法 |
|---|---|
| `/admin/flight/add` | `POST` |
| `/admin/flight/update` | `POST` |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `flightId` | number | 修改时必填 | `49` |
| `flightNumber` | string | 是 | `"CA3019"` |
| `flightDate` | string | 是 | `"2026-07-01"` |
| `aircraftRegNo` | string | 是 | `"B-1001"` |
| `flightStatus` | string | 否 | `"NORMAL"` |
| `departureAirportCode` | string | 是 | `"PEK"` |
| `arrivalAirportCode` | string | 是 | `"SHA"` |
| `remark` | string | 否 | `"管理员演示航班"` |

特殊规则：

- 如果某个航班已经有订单，后端只允许修改 `flightStatus` 和 `remark`，不会改航班号、日期、飞机、出发到达机场。
- `flightStatus` 不传时默认 `NORMAL`。
- 同一 `FlightNumber + FlightDate` 不能重复。

#### 停用航班

| 地址 | 方法 |
|---|---|
| `/admin/flight/disable` | `POST` |

请求 JSON：

```json
{ "flightId": 49 }
```

成功后 `flightStatus = "DISABLED"`。

#### 恢复航班

| 地址 | 方法 |
|---|---|
| `/admin/flight/enable` | `POST` |

请求 JSON：

```json
{ "flightId": 49 }
```

成功后 `flightStatus = "NORMAL"`。

---

### 4.6.6 航段管理

#### 查询航段列表和详情

| 功能 | 地址 | 方法 | 参数 |
|---|---|---|---|
| 列表 | `/admin/segment/list` | `GET` | 可选 `flightId` |
| 详情 | `/admin/segment/detail` | `GET` | `segmentId` |

#### 新增或修改航段

| 地址 | 方法 |
|---|---|
| `/admin/segment/add` | `POST` |
| `/admin/segment/update` | `POST` |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `segmentId` | number | 修改时必填 | `1` |
| `flightId` | number | 是 | `49` |
| `originStopNo` | number | 是 | `1` |
| `destinationStopNo` | number | 是 | `2` |
| `originAirportCode` | string | 是 | `"PEK"` |
| `destinationAirportCode` | string | 是 | `"SHA"` |
| `plannedDepartureTime` | string | 是 | `"2026-07-01T09:30:00"` |
| `plannedArrivalTime` | string | 是 | `"2026-07-01T12:00:00"` |
| `actualDepartureTime` | string | 否 | `"2026-07-01T09:35:00"` |
| `actualArrivalTime` | string | 否 | `"2026-07-01T12:05:00"` |
| `delayMinutes` | number | 否 | `5` |
| `delayReason` | string | 否 | `"天气原因"` |
| `firstClassRemainingSeats` | number | 是 | `8` |
| `economyRemainingSeats` | number | 是 | `50` |
| `firstClassPrice` | number | 是 | `1800.00` |
| `economyPrice` | number | 是 | `1000.00` |
| `remark` | string | 否 | `"管理员演示航段"` |

关键规则：

- `originStopNo` 必须小于 `destinationStopNo`。
- 同一航班内 `(flightId, originStopNo, destinationStopNo)` 不能重复。
- 新增时不要传 `segmentId`。
- 编辑时必须传已有 `segmentId`，否则后端会按新增处理，可能触发重复站序错误。

常见失败：

| `code` | 含义 |
|---|---|
| `41007` | 起止站序错误 |
| `41010` | 航段站序已存在 |
| `40409` | 起飞机场不存在 |
| `40410` | 到达机场不存在 |

#### 停用航段

| 地址 | 方法 | 结果 |
|---|---|---|
| `/admin/segment/disable` | `POST` | 固定返回业务错误，因为 `FlightSegment` 没有停用字段 |

---

### 4.6.7 餐食管理

#### 查询餐食列表和详情

| 功能 | 地址 | 方法 | 参数 |
|---|---|---|---|
| 列表 | `/admin/meal/list` | `GET` | 无 |
| 详情 | `/admin/meal/detail` | `GET` | `mealId` |

#### 新增或修改餐食

| 地址 | 方法 |
|---|---|
| `/admin/meal/add` | `POST` |
| `/admin/meal/update` | `POST` |

请求 JSON：

| 参数 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `mealId` | number | 修改时必填 | `1` |
| `mealName` | string | 是 | `"普通餐"` |
| `mealType` | string | 是 | `"NORMAL"` |
| `isAvailable` | boolean | 否 | `true` |
| `description` | string | 否 | `"标准航空餐"` |

#### 停用餐食

| 地址 | 方法 |
|---|---|
| `/admin/meal/disable` | `POST` |

请求 JSON：

```json
{ "mealId": 1 }
```

成功后 `isAvailable=false`。

---

### 4.6.8 管理员订单、退票、改签记录

| 功能 | 地址 | 方法 | 参数 | 返回 |
|---|---|---|---|---|
| 订单列表 | `/admin/ticket/list` | `GET` | 无 | `Ticket[]` |
| 订单详情 | `/admin/ticket/detail` | `GET` | `ticketId` | `Ticket` |
| 退票记录 | `/admin/refund/list` | `GET` | 无 | 当前实现返回全部订单 |
| 改签记录 | `/admin/change/list` | `GET` | 无 | 当前实现返回全部订单 |

注意：`/admin/refund/list` 和 `/admin/change/list` 当前代码没有过滤状态，返回的是全部订单。前端如果想只显示退票或改签记录，需要自己按 `ticketStatus` 过滤。

---

### 4.6.9 Dashboard 和过期订单

#### Dashboard 汇总

| 地址 | 方法 |
|---|---|
| `/admin/dashboard/summary` | `GET` |
| `/admin/dashboard/statistics` | `GET` |

返回 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `userCount` | number | 用户数量 |
| `flightCount` | number | 航班数量 |
| `segmentCount` | number | 航段数量 |
| `ticketCount` | number | 订单数量 |
| `mealCount` | number | 餐食数量 |

#### 手动触发过期订单扫描

| 地址 | 方法 | 请求体 | 返回 |
|---|---|---|---|
| `/admin/job/expire-order` | `POST` | `{}` | 处理的过期订单数量 |

---

## 5. 前端调用接口的具体步骤

### 5.1 建议封装一个统一客户端

```ts
const API_BASE_URL = "http://localhost:8080/api";

async function apiGet(path: string, params?: Record<string, string | number | boolean | undefined>) {
  const url = new URL(`${API_BASE_URL}${path}`);
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, String(value));
    }
  });
  const response = await fetch(url.toString(), { method: "GET" });
  const payload = await response.json();
  if (payload.code !== 0) throw new Error(payload.message || "请求失败");
  return payload.data;
}

async function apiPost(path: string, body: object) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  const payload = await response.json();
  if (payload.code !== 0) throw new Error(payload.message || "请求失败");
  return payload.data;
}
```

---

### 5.2 旅客完整流程

1. 登录：

```ts
const user = await apiPost("/auth/login", {
  loginAccount: "passengerA",
  password: "pass123"
});
```

2. 加载基础数据：

```ts
const airports = await apiGet("/admin/airport/list");
const meals = await apiGet("/meal/list");
const profile = await apiGet("/member/profile", { userId: user.userId });
const tickets = await apiGet("/ticket/my", { userId: user.userId });
```

3. 查询航班：

```ts
const flights = await apiGet("/flight/search", {
  departureAirportCode: "PEK",
  arrivalAirportCode: "SHA",
  flightDate: "2026-07-01"
});
```

4. 选择一个 `FlightSearchItem`，创建订单：

```ts
const selected = flights[0];
const ticket = await apiPost("/ticket/create", {
  userId: user.userId,
  flightId: selected.flightId,
  segmentId: selected.segmentId,
  cabinClass: "ECONOMY",
  passengerName: user.userName,
  passengerIdNumber: "110101199001010011",
  mealId: 1
});
```

5. 支付：

```ts
const paidTicket = await apiPost("/ticket/pay", {
  ticketId: ticket.ticketId
});
```

6. 支付成功后刷新：

```ts
const nextProfile = await apiGet("/member/profile", { userId: user.userId });
const nextTickets = await apiGet("/ticket/my", { userId: user.userId });
```

7. 退票：

```ts
await apiPost("/ticket/refund", {
  ticketId: paidTicket.ticketId,
  remark: "前端演示退票"
});
```

8. 改签：

```ts
const changeTicket = await apiPost("/ticket/change/apply", {
  ticketId: paidTicket.ticketId,
  targetFlightId: selectedNew.flightId,
  targetSegmentId: selectedNew.segmentId,
  cabinClass: paidTicket.cabinClass,
  changeReason: "改到更合适的航班"
});

if (changeTicket.ticketStatus === "PENDING_PAYMENT") {
  await apiPost("/ticket/change/pay", { ticketId: changeTicket.ticketId });
}
```

---

### 5.3 管理员完整流程

1. 管理员登录：

```ts
const admin = await apiPost("/auth/login", {
  loginAccount: "admin",
  password: "admin123"
});
```

2. 加载管理台数据：

```ts
const dashboard = await apiGet("/admin/dashboard/summary");
const cities = await apiGet("/admin/city/list");
const airports = await apiGet("/admin/airport/list");
const aircraft = await apiGet("/admin/aircraft/list");
const flights = await apiGet("/admin/flight/list");
const segments = await apiGet("/admin/segment/list");
const meals = await apiGet("/admin/meal/list");
const users = await apiGet("/admin/user/list");
const tickets = await apiGet("/admin/ticket/list");
```

3. 保存航班：

```ts
await apiPost("/admin/flight/update", {
  flightId: 49,
  flightNumber: "CA3019",
  flightDate: "2026-07-01",
  aircraftRegNo: "B-1001",
  flightStatus: "NORMAL",
  departureAirportCode: "PEK",
  arrivalAirportCode: "SHA",
  remark: "管理员演示航班"
});
```

4. 停用航班：

```ts
await apiPost("/admin/flight/disable", { flightId: 49 });
```

5. 恢复航班：

```ts
await apiPost("/admin/flight/enable", { flightId: 49 });
```

6. 保存航段：

```ts
await apiPost("/admin/segment/update", {
  segmentId: 49,
  flightId: 49,
  originStopNo: 1,
  destinationStopNo: 2,
  originAirportCode: "PEK",
  destinationAirportCode: "SHA",
  plannedDepartureTime: "2026-07-01T09:30:00",
  plannedArrivalTime: "2026-07-01T12:00:00",
  firstClassRemainingSeats: 10,
  economyRemainingSeats: 72,
  firstClassPrice: 1880,
  economyPrice: 980,
  remark: "管理员演示航段"
});
```

---

## 6. 前后端联调流程

### 6.1 启动顺序

1. 确认 MySQL 已启动。
2. 导入数据库结构和种子数据。
3. 启动后端 Spring Boot，端口 `8080`。
4. 启动前端 Vite，端口 `5173`。
5. 浏览器打开 `http://localhost:5173`。

### 6.2 建议先测接口

可以直接在浏览器访问 GET：

```text
http://localhost:8080/api/admin/airport/list
http://localhost:8080/api/flight/search?departureAirportCode=PEK&arrivalAirportCode=SHA&flightDate=2026-07-01
```

如果能看到 JSON，说明后端正在运行。

### 6.3 演示验收路径

#### 普通用户路径

1. 用 `passengerA/pass123` 登录。
2. 查看会员积分，应为 `900`，等级 `NORMAL`。
3. 查 `PEK → SHA`，日期 `2026-07-01`。
4. 选择航班，创建经济舱订单。
5. 支付订单。
6. 刷新会员信息，应变为 `1000`，等级 `VIP`。
7. 再下第二张票，应看到 `paymentAmount = priceAmount * 0.9`。
8. 对已支付订单点击退票，应变为 `REFUND_SUCCESS`。
9. 对另一张已支付订单申请改签并支付，旧票应变为 `CHANGE_SUCCESS`，新票应变为 `PAID`。

#### 管理员路径

1. 用 `admin/admin123` 登录。
2. 进入管理员端。
3. 查看 Dashboard。
4. 查询航班列表。
5. 保存一个航班或修改航班状态。
6. 停用航班，确认旅客端不再售卖。
7. 点击恢复，航班恢复 `NORMAL`。
8. 新增或编辑航段，确认没有 500 错误。
9. 手动触发过期订单扫描。

---

## 7. 常见错误排查

### 7.1 浏览器报 Failed to fetch

可能原因：

- 后端没启动。
- 后端不是 `8080` 端口。
- 前端 API 地址写错。
- 浏览器跨域失败。

当前后端已配置 CORS，允许 `/api/**` 的 `GET`、`POST`、`OPTIONS`。

排查：

```text
浏览器访问 http://localhost:8080/api/admin/airport/list
```

如果打不开，先处理后端启动问题。

---

### 7.2 接口返回 code 非 0

前端应该展示 `message`。常见业务错误：

| 场景 | 可能原因 |
|---|---|
| 登录失败 | 账号或密码错误 |
| 注册失败 | 账号重复、身份证重复、手机号/邮箱/身份证格式不对 |
| 下单失败 | 航段无余票、航班停用/取消/完成、航段不属于航班 |
| 支付失败 | 订单不是待支付、订单已过期 |
| 退票失败 | 订单不是已支付状态 |
| 改签失败 | 旧票不是已支付状态、新航段无余票 |
| 航段保存失败 | 起点站序大于等于终点站序，或同航班站序重复 |

---

### 7.3 查不到航班

检查：

- 日期是否在种子数据范围 `2026-06-28` 到 `2026-07-04`。
- 出发机场和到达机场是否选择正确。
- 是否把城市 ID 当成机场代码传了。
- 是否传了停用或取消航班对应的线路。

推荐测试：

```text
PEK → SHA，2026-07-01
PEK → PVG，2026-07-04
CAN → PEK，2026-07-02
SZX → TFU，2026-07-04
```

---

### 7.4 下单成功但库存减少

这是正常行为。创建订单时后端已经锁定并扣减库存，订单 15 分钟内未支付会被定时任务恢复库存。

如果想立即恢复过期订单，可调用：

```text
POST /api/admin/job/expire-order
```

---

### 7.5 改签后为什么有两张票

这是后端设计：

- 旧票保留历史记录。
- 新票通过 `originalTicketId` 指向旧票。
- 改签支付后旧票状态为 `CHANGE_SUCCESS`。
- 新票状态为 `PAID`。

前端订单列表可以把 `originalTicketId` 展示为“来自订单 #xxx 的改签单”。

---

### 7.6 管理员用户列表返回 passwordHash

当前后端 `/admin/user/list` 返回的是 `User` 实体，所以会包含 `passwordHash` 和 `idNumberDigest`。

前端处理建议：

- 不展示这两个字段。
- 不把这两个字段复制到表单里。
- 只展示账号、姓名、角色、积分、会员等级、手机号、邮箱即可。

---

## 8. 前端实现注意事项

1. 所有接口返回都要先判断 `code`。
2. 不要自己编造状态，状态只能用后端枚举。
3. 下单必须使用搜索结果里的 `flightId` 和 `segmentId`。
4. 改签必须对 `PAID` 状态订单操作。
5. 支付普通订单用 `/ticket/pay`。
6. 支付改签订单用 `/ticket/change/pay`。
7. 管理员删除操作不要用 `DELETE`，本项目没有 `DELETE` 接口。
8. 城市、机场、航段不要物理删除，也不要要求后端新增停用字段。
9. 机场选择使用下拉框，不要让用户手动输入机场代码。
10. 订单列表必须展示路线、日期、时间、状态、金额，不要只展示订单号。

