# docs/backend-architecture.md

本文档定义后端 Spring Boot 项目的工程结构、类职责、包划分、调用链路和实现边界。它的目标不是限制开发创造力，而是让 Codex 和人工实现都按同一套结构输出，减少维护成本和联调错误。

## 1. 架构目标

后端只做三件事：接收请求、执行业务、操作数据库。所有界面展示、列表刷新、表单校验提示尽量放到前端，但核心合法性判断必须在后端完成。

后端设计要求：
- 单体应用
- 本地启动
- 三层架构
- 事务清晰
- 接口稳定
- 异常统一

## 2. 项目包结构建议

建议采用如下结构：

```text
com.example.airticket
├── AiticketApplication
├── config
├── controller
│   ├── auth
│   ├── passenger
│   ├── admin
│   └── common
├── service
│   ├── impl
│   ├── auth
│   ├── passenger
│   └── admin
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── enum
├── exception
├── aspect
├── scheduler
├── util
└── common
```

如果采用 MyBatis 或 JPA，Repository 层名称可以保持一致，但实现方式由项目组选定后统一。

## 3. 分层职责

### 3.1 Controller

Controller 只做以下事情：
- 接收 HTTP 请求
- 调用 Service
- 返回统一响应
- 不写复杂业务逻辑
- 不直接操作数据库

Controller 不应出现库存判断、会员升级判断、改签差价计算等逻辑。

### 3.2 Service

Service 是核心业务层，负责：
- 参数校验
- 权限判断
- 业务规则判断
- 事务控制
- 状态迁移
- 调用 Repository 完成数据库操作

涉及订单、库存、积分、改签、退票、支付超时等逻辑，必须在 Service 层完成。

### 3.3 Repository

Repository 只负责数据库读写。不要把复杂业务塞到 SQL 中，也不要把状态迁移逻辑写到 Repository 中。

Repository 负责的典型操作：
- 按主键查询
- 按条件分页查询
- 插入记录
- 更新库存
- 更新状态
- 统计数量

## 4. 模块划分

### 4.1 Auth 模块

负责注册、登录、退出、当前用户信息。

核心类：
- AuthController
- AuthService
- AuthRepository
- User entity
- RegisterRequest / LoginRequest / LoginResponse

### 4.2 Passenger 模块

负责航班查询、下单、支付、订单查询、退票、改签、餐食选择。

核心类：
- FlightController
- TicketController
- MealController
- FlightService
- TicketService
- MealService

### 4.3 Admin 模块

负责城市、机场、飞机、航班、航段、餐食、用户、订单管理。

核心类：
- AdminCityController
- AdminAirportController
- AdminAircraftController
- AdminFlightController
- AdminSegmentController
- AdminMealController
- AdminUserController
- AdminTicketController

### 4.4 Scheduler 模块

负责过期订单扫描与库存回补。

核心类：
- TicketExpireScheduler
- ExpiredOrderService

## 5. 核心实体类

实体类应与数据库表一一对应。

建议至少包括：
- User
- City
- Airport
- Aircraft
- Flight
- FlightSegment
- TicketSale
- MealOption
- MealReservation

实体类字段命名应与数据库字段语义保持一致，避免一张表在不同层有三套叫法。

## 6. DTO 设计原则

DTO 与 Entity 分离。

不要把数据库实体直接暴露给前端。

请求 DTO 与响应 DTO 分离。

DTO 中只保留接口真正需要的字段，不要把所有实体字段无差别透出。

## 7. 事务与锁实现位置

事务应主要放在 Service 层，通过 `@Transactional` 标注。

涉及 `SELECT ... FOR UPDATE` 的场景：
- 创建订单
- 支付订单
- 退票
- 改签
- 定时任务回补库存

事务边界必须覆盖：
- 查询锁定行
- 校验状态
- 更新库存
- 更新订单状态
- 发放或回退积分
- 更新会员等级

## 8. 异常体系

建议定义统一异常体系，例如：
- BusinessException
- AuthException
- ResourceNotFoundException
- PermissionDeniedException
- InventoryException
- OrderStateException
- TransactionException

异常统一由全局异常处理器转换为 ApiResponse。

不要在 Controller 里 try-catch 后手动拼大量错误字符串。

## 9. 枚举设计

建议建立以下枚举：
- UserType
- MemberLevel
- FlightStatus
- TicketStatus
- CabinClass
- MealType
- IsEnabled

枚举值必须和 `docs/state-machine.md` 保持一致。

## 10. 权限控制

管理员接口和乘客接口必须分离。

建议通过拦截器、过滤器或 Spring Security 的简化配置实现基础权限控制。

至少要保证：
- 未登录不能访问受保护接口
- 普通乘客不能访问管理员接口
- 管理员可查看数据，但不可修改积分

如果实现时间有限，可以采用轻量级会话或 Token 方案，不必引入复杂权限框架。

## 11. 调用链路标准

推荐链路如下：

前端请求 → Controller → Service → Repository → MySQL → Repository → Service → Controller → 前端响应

所有有副作用操作都必须保证从 Service 开始、在 Service 结束，不在 Controller 或 Repository 之间夹杂额外业务层。

## 12. 日志与调试

建议在关键业务节点打印必要日志：
- 登录成功或失败
- 下单开始与结束
- 支付成功或超时
- 退票开始与结束
- 改签开始与结束
- 定时任务扫描结果

日志不要打印明文身份证号和密码。

## 13. 测试优先级

后端实现时，应优先完成以下可测试链路：
1. 登录与注册
2. 航班查询
3. 下单锁库存
4. 支付成功与积分升级
5. 退票回补
6. 改签链追踪
7. 超时订单扫描
8. 管理员 CRUD
