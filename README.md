# 航空票务数据库系统

> **复旦大学 2024-2025 学年 · 数据库及其实现课程项目 · Group 22**

这是复旦大学2025-2026学年第一学期《数据库及实现》课程Group 22的Project——航空票务数据库系统，覆盖航班检索、票务交易（下单/支付/退票/改签）、会员积分与管理员资源维护。

成员：杜诚俊 何韵琳 吴沁远 杨子卿

---

## 技术栈

| 层次   | 技术                              |
| ------ | --------------------------------- |
| 前端   | React 19 + TypeScript + Vite 7    |
| 后端   | Spring Boot 2.7.18 + JPA + Maven  |
| 数据库 | MySQL 9.x                         |
| Java   | 17                                |
| 测试   | Vitest (前端) / JUnit + H2 (后端) |

---

## 项目结构

```
airline-ticketing/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/          # Java 源码
│   │   ├── controller/         # REST 控制器
│   │   ├── service/            # 业务逻辑层
│   │   ├── repository/         # JPA 数据访问层
│   │   ├── entity/             # JPA 实体映射
│   │   ├── dto/                # 请求/响应数据传输对象
│   │   ├── enums/              # 枚举定义
│   │   ├── exception/          # 统一异常处理
│   │   ├── config/             # 配置（CORS 等）
│   │   ├── scheduler/          # 定时任务
│   │   ├── common/             # 通用工具
│   │   └── util/               # 工具类
│   ├── src/main/resources/
│   │   └── application.properties  # 后端配置
│   ├── src/test/               # 单元测试
│   └── pom.xml                 # Maven 构建
│
├── frontend/                   # React 前端
│   ├── src/
│   │   ├── App.tsx             # 主组件（含全部页面逻辑）
│   │   ├── main.tsx            # 入口
│   │   ├── styles.css          # 全局样式
│   │   ├── types.ts            # TypeScript 类型定义
│   │   ├── api/                # API 请求封装
│   │   └── lib/                # 工具函数
│   ├── index.html
│   ├── package.json
│   └── vite.config.ts
│
├── database/                   # 数据库脚本
│   ├── schema.sql              # 建库建表（9 张表）
│   ├── seed_data.sql           # 种子数据（城市、机场、航班、航段、用户）
│   ├── truncate.sql            # 清空所有表数据
│   ├── demo_all.sql            # 综合演示查询
│   ├── demo_change_chain.sql   # 改签链查询演示
│   ├── demo_privacy_security.sql
│   ├── demo_search_coverage.sql
│   ├── demo_special_ticket_orders.sql
│   └── demo_user_table.sql
│
├── docs/                       # 项目文档
│   ├── api-spec.md             # API 接口规范（共 58 个接口）
│   ├── backend-architecture.md # 后端架构说明
│   ├── error-code.md           # 统一错误码规范
│   ├── transaction-spec.md     # 事务与库存控制规范
│   ├── frontend-integration-guide.md
│   └── classroom-demo-guide.md # 课堂展示指南
│
├── attack(1).py                # 并发抢票演示脚本
└── README.md
```

---

## 数据库设计

### ER 图

详见项目根目录 `ER图（end）.png`。

### 核心表（共 9 张）

| 表名                | 说明                                                                                             |
| ------------------- | ------------------------------------------------------------------------------------------------ |
| `City`            | 城市 (CityId, CityName, CityCode, Country)                                                       |
| `Airport`         | 机场 (AirportCode, AirportName, CityId, IsInternational)                                         |
| `Aircraft`        | 飞机 (AircraftRegNo, AircraftType, Manufacturer, 座位数, Status)                                 |
| `User`            | 用户 (UserId, LoginAccount, PasswordHash, IdNumberDigest, Points, MemberLevel)                   |
| `MealOption`      | 餐食选项 (MealId, MealName, MealType, IsAvailable)                                               |
| `Flight`          | 航班 (FlightId, FlightNumber, FlightDate, FlightStatus)                                          |
| `FlightSegment`   | **航段 —— 最小售卖单元** (SegmentId, FlightId, 起止机场/时间/价格/余票, IsSpecialOffer)  |
| `TicketSale`      | 售票记录 (TicketId, OrderNo, 乘客信息, CabinClass, PriceAmount, PaymentAmount, OriginalTicketId) |
| `MealReservation` | 餐食预订 (关联 TicketId 与 MealId)                                                               |

### 设计亮点

- **库存位于航段（FlightSegment）**，每个航段独立管理经济舱/头等舱余票和价格，支持多点经停航线分段售卖
- **支持多航段航班**，如 PEK→SHA→TFU 可售卖 PEK→SHA、SHA→TFU、PEK→TFU 三个独立航段
- **改签链追溯**，通过 `OriginalTicketId` 自引用形成完整改签历史链
- **隐私保护**，密码使用 BCrypt 哈希，身份证号保存 SHA-256 摘要

---

## 系统安装与部署

### 前置条件

- **Java 17+** 和 Maven（后端编译）
- **Node.js 18+** 和 npm（前端构建）
- **MySQL 9.x** 数据库

### 1. 创建数据库并初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行建库建表脚本
source database/schema.sql;

# 导入种子数据
source database/seed_data.sql;

# 验证数据
USE airticket;
SELECT COUNT(*) FROM City;
SELECT COUNT(*) FROM Airport;
SELECT COUNT(*) FROM Flight;
SELECT COUNT(*) FROM FlightSegment;
```

### 2. 配置后端

编辑 `backend/src/main/resources/application.properties`，修改数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/airticket?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
spring.datasource.username=你的数据库用户名
spring.datasource.password=你的数据库密码
server.port=8080
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

看到 `Tomcat started on port(s): 8080` 表示启动成功。

### 4. 配置前端

前端默认通过 `http://localhost:8080` 访问后端 API。如需修改，编辑 `frontend/src/api/client.ts` 中的 `BASE_URL`。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`。

---

## 使用说明

### 演示账号

| 账号           | 密码         | 角色   | 说明                                         |
| -------------- | ------------ | ------ | -------------------------------------------- |
| `admin`      | `admin123` | 管理员 | 管理城市、机场、飞机、航班、航段、餐食、订单 |
| `passengerA` | `pass123`  | 乘客   | 900 积分，用于 VIP 升级演示                  |
| `passengerB` | `pass123`  | 乘客   | 普通乘客，用于购票/退票/改签演示             |

### 旅客端功能

1. **航班搜索** — 选择出发/到达城市和日期，查询可售航段
2. **下单支付** — 选择航段和舱位，创建订单后 15 分钟内支付
3. **查看订单** — 浏览本人所有订单及状态
4. **退票** — 已支付订单可退票，库存与积分自动回滚
5. **改签** — 已支付订单可改签至同日同航线其他航班，通过 `OriginalTicketId` 形成改签链
6. **会员升级** — 每支付一单 +100 积分，满 1000 分自动升级 VIP（享 9 折优惠）
7. **账号注销** — 旅客可注销账号（历史订单保留保持外键完整性）

### 管理员端功能

1. **概览 Dashboard** — 查看用户数、航班数、航段数、订单数统计
2. **航班管理** — 新增/修改/停用/恢复航班
3. **航段管理** — 新增/编辑航段（含价格、余票、特价标记）
4. **资源管理** — 城市、机场、飞机、餐食的 CRUD
5. **订单管理** — 浏览所有订单，自动定时刷新
6. **演示辅助** — 触发过期订单扫描、重置演示数据

---

## 核心业务规则

| 业务               | 规则                                                                        |
| ------------------ | --------------------------------------------------------------------------- |
| **下单**     | 使用`SELECT ... FOR UPDATE` 行级锁锁定航段库存，事务内扣减库存并创建订单  |
| **支付**     | 同一事务内更新订单状态、增加用户积分、重算会员等级                          |
| **退票**     | 同一事务内更新状态为`REFUND_SUCCESS`、回补库存、回滚积分、重算等级        |
| **改签**     | 生成新订单（非修改旧票），`OriginalTicketId` 指向原订单，形成可追溯改签链 |
| **支付超时** | 定时任务每分钟扫描过期未支付订单，自动标记`EXPIRED` 并回补库存            |
| **VIP 折扣** | VIP 会员支付时`PaymentAmount = PriceAmount × 0.9`，数据库保留原价        |
| **特价票**   | `FlightSegment.IsSpecialOffer = TRUE` 时价格按 5 折计算                   |

---

## 测试

### 前端测试

```bash
cd frontend
npm test
```

使用 Vitest + jsdom 运行。

### 后端测试

```bash
cd backend
mvn test
```

使用 JUnit + H2 内存数据库运行。

### 并发抢票测试

使用 `attack(1).py` 脚本模拟 50 个用户并发抢购同一航段（5 张余票），验证行级锁防超卖：

```bash
python attack.py --users 50 --stock 5
```

---

## API 概览

完整 API 规范见 [`docs/api-spec.md`](docs/api-spec.md)，共 **58 个接口**。

| 分类   | 接口数 | 说明                                            |
| ------ | ------ | ----------------------------------------------- |
| 认证   | 4      | 注册、登录、登出、当前用户                      |
| 旅客   | 11     | 搜索航班、下单、支付、退票、改签、订单列表      |
| 餐食   | 2      | 餐食列表与详情                                  |
| 会员   | 2      | 会员资料、积分                                  |
| 管理员 | 39     | 城市/机场/飞机/航班/航段/餐食的 CRUD + 订单管理 |

统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-07-01T12:00:00"
}
```

---

## 常见问题

**Q: 启动后端时报数据库连接失败？**
A: 确认 MySQL 已启动，并已在 `application.properties` 中配置正确的用户名和密码。

**Q: 前端页面空白或接口 404？**
A: 确保后端已成功启动（端口 8080），前端 npm install 已完成。

**Q: 搜索不到航班？**
A: 先用管理员账号登录，在概览页点击「重置演示数据」，然后选择出发/到达城市尝试搜索。

**Q: 如何重置所有数据？**
A: 管理员登录后，在管理端概览页点击「重置演示数据」按钮；或手动执行 `database/truncate.sql` 后重新导入 `schema.sql` 和 `seed_data.sql`。

---

## 许可

本项目为复旦大学数据库课程教学项目，仅供学习交流使用。