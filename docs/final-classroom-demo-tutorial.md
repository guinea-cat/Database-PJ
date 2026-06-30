# 航空票务系统期末课堂演示完整教程

本文档用于现场演示。建议演示前完整走一遍，课堂上严格按顺序操作，避免临时改数据库或临时找数据。

## 0. 演示前准备

打开 VS Code：

```powershell
cd "D:\复旦大学\DS大二下\数据库及实现\Database-PJ"
code .
```

建议在 VS Code 里打开 3 个终端：

- 终端 1：数据库初始化和 SQL 演示
- 终端 2：后端 Spring Boot
- 终端 3：前端 Vite

演示账号：

| 角色 | 账号 | 密码 | 用途 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 后台资源、订单、航班、航段、餐食 |
| 演示乘客 A | `passengerA` | `pass123` | 初始 900 积分，演示 VIP 升级和 9 折 |
| 演示乘客 B | `passengerB` | `pass123` | 普通乘客备用 |

## 1. 重置数据库到默认状态

在 VS Code 的终端 1 执行：

```powershell
cd "D:\复旦大学\DS大二下\数据库及实现\Database-PJ"
```

删除旧数据库：

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS airticket;"'
```

重新建表：

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 < ".\database\schema.sql"'
```

导入默认演示数据：

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 < ".\database\seed_data.sql"'
```

可选检查默认账号是否存在：

```powershell
.\tools\run_demo_sql.ps1 -Demo users -Password 051130
```

如果 PowerShell 禁止运行脚本，改用：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\run_demo_sql.ps1 -Demo users -Password 051130
```

## 2. 启动后端和前端

终端 2 启动后端：

```powershell
cd "D:\复旦大学\DS大二下\数据库及实现\Database-PJ\backend"
mvn spring-boot:run
```

看到类似 `Tomcat started on port(s): 8080` 后，后端启动成功。这个终端不要关闭。

终端 3 启动前端：

```powershell
cd "D:\复旦大学\DS大二下\数据库及实现\Database-PJ\frontend"
npm run dev
```

浏览器打开：

```text
http://localhost:5173
```

## 3. 演示一：注册、完整性约束、User 表、安全隐私

### 3.1 展示注册完整性约束

在前端选择“乘客注册”。

先故意输入错误数据，展示系统会拒绝：

```text
登录账号：demoBad
密码：123
乘客姓名：测试用户
手机号：123
邮箱：bad
身份证号：123
```

讲解：

```text
注册时后端会校验手机号、邮箱、身份证号、密码长度。
这些是用户表和业务服务层共同保证的数据完整性约束。
```

再输入合法数据：

```text
登录账号：finaldemo001
密码：demo1234
乘客姓名：期末演示用户
手机号：13900009999
邮箱：finaldemo001@example.com
身份证号：110101200001019999
```

如果提示账号或身份证重复，把账号改成 `finaldemo002`，身份证最后四位改成其他数字。

注册成功后退出登录，或者先停留在页面，转到终端展示数据库。

### 3.2 终端查看 User 表新增用户

在终端 1 执行：

```powershell
cd "D:\复旦大学\DS大二下\数据库及实现\Database-PJ"
.\tools\run_demo_sql.ps1 -Demo users -Password 051130
```

讲解：

```text
这里直接查询 User 表，可以看到刚注册的用户已经写入数据库。
展示字段包括 UserId、LoginAccount、UserName、UserType、PhoneNumber、Email、Points、MemberLevel、CreatedAt。
```

### 3.3 展示密码和身份证隐私保护

继续在终端 1 执行：

```powershell
.\tools\run_demo_sql.ps1 -Demo privacy -Password 051130
```

讲解：

```text
PasswordHash 不是明文密码，而是 BCrypt 哈希，通常以 $2a$、$2b$ 或 $2y$ 开头。
PasswordHashLength 通常是 60，不等于原始密码长度。
IdNumberDigest 是 64 位 SHA-256 摘要。
数据库中看不到明文密码，也看不到明文身份证号。
```

## 4. 演示二：乘客 A 搜索、下单、支付、改签链

登录：

```text
passengerA / pass123
```

### 4.1 搜索航班

在乘客端航班搜索区域选择：

```text
出发城市：北京
到达城市：上海
出发日期：2026-07-01
```

点击搜索。

讲解：

```text
乘客看到的是可售航段 FlightSegment。
FlightSegment 是最小售票单位，库存也挂在航段上，而不是只挂在航班上。
```

注意：为了后面的 100 人并发抢票演示，前面普通购票不要购买 `MU2001` 的头等舱。可以选择经济舱，或者选择其他航班/特价票。

### 4.2 创建订单

选择一个搜索结果，填写：

```text
舱位：经济舱
餐食：任选一个，或不选
乘客姓名：演示乘客A
身份证号：110101199001010011
```

点击创建订单。

讲解：

```text
创建订单时，后端会在一个事务中锁定航段库存、扣减库存、创建 TicketSale 订单。
如果库存不足，下单会失败，避免超卖。
```

### 4.3 支付订单

点击支付。

展示：

```text
订单状态：待支付 -> 已支付
乘客 A 积分：900 -> 1000
会员等级：NORMAL -> VIP
```

讲解：

```text
支付成功后，系统给用户增加 100 积分。
乘客 A 初始 900 分，支付第一张票后达到 1000 分，自动升级为 VIP。
```

### 4.4 改签订单

在“我的订单”里选择刚支付的订单，点击改签。

在改签面板中：

```text
改签舱位：经济舱
改签餐食：任选
改签原因：课堂演示改签到其他航班
```

选择系统列出的可改签航班，提交改签。如果需要补差价，点击支付改签差价。

讲解：

```text
改签不是直接覆盖原订单。
系统会生成一张新票，并通过 OriginalTicketId 指向原票，形成可追溯的改签链。
旧票状态变为 CHANGE_SUCCESS，新票状态变为 PAID。
```

### 4.5 终端展示改签链

在终端 1 执行：

```powershell
.\tools\run_demo_sql.ps1 -Demo change -Password 051130
```

讲解：

```text
这里能看到“原订单 -> 新订单”的链路。
这说明系统保留了历史订单，不会破坏审计和追溯。
```

## 5. 演示三：管理员查看订单、新增杭州航线

退出乘客账号，登录管理员：

```text
admin / admin123
```

### 5.1 查看全部订单

进入管理员端“订单”模块。

展示：

```text
管理员可以查看全系统订单。
用户姓名已经做前端脱敏，不直接暴露完整真实姓名。
```

讲解：

```text
管理员需要看订单状态、订单号、金额，但不应该直接暴露乘客完整隐私信息。
```

### 5.2 新增城市杭州

进入管理员端“资源”模块，城市表单填写：

```text
城市名：杭州
城市代码：HGH
国家：中国
```

点击保存城市。

### 5.3 新增杭州萧山国际机场

机场表单填写：

```text
机场代码：HGH
机场名称：杭州萧山国际机场
城市 ID：选择刚才新增的杭州城市 ID
```

点击保存机场。

如果不确定杭州城市 ID，在终端 1 查询：

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 -e "USE airticket; SELECT CityId, CityName, CityCode FROM City ORDER BY CityId DESC LIMIT 5;"'
```

### 5.4 新增杭州直达上海虹桥航班

进入管理员端“航班”模块，填写：

```text
航班号：HU7001
日期：2026-07-01
飞机：任选一个可用飞机，例如 B-1001
状态：NORMAL
出发机场：HGH
到达机场：SHA
```

点击保存航班。

保存后在终端 1 查询刚新增航班的 `FlightId`：

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 -e "USE airticket; SELECT FlightId, FlightNumber, FlightDate, DepartureAirportCode, ArrivalAirportCode, FlightStatus FROM Flight WHERE FlightNumber = '\''HU7001'\'' ORDER BY FlightId DESC LIMIT 5;"'
```

记住输出里的 `FlightId`。

### 5.5 新增杭州萧山到上海虹桥航段

进入管理员端“航段”模块，填写：

```text
所属航班：选择刚才 HU7001 对应的 FlightId
起点站序：1
终点站序：2
起飞机场：HGH
到达机场：SHA
计划起飞：2026-07-01 09:00
计划到达：2026-07-01 11:00
经济舱余票：50
头等舱余票：5
经济舱价格：900
头等舱价格：1600
特价票：不勾选
```

点击保存航段。

讲解：

```text
必须先有 Flight，再有绑定该 Flight 的 FlightSegment。
乘客搜索查到的是 FlightSegment，所以只建航班、不建航段，乘客端搜不到票。
直飞航段的站序就是 1 -> 2。
```

### 5.6 解释为什么不允许删除航段

在航段列表点击“不删除”按钮或口头说明：

```text
FlightSegment 是售票和订单关联的核心对象。
如果物理删除航段，已存在的 TicketSale 订单仍然引用这个航段，会破坏外键完整性和历史订单可追溯。
技术上数据库可以设计级联删除，但这不符合票务业务逻辑。
所以系统限制删除航段，只允许保留历史数据。
```

### 5.7 演示停用和恢复航班

在航班列表中选择一条非关键航班，点击停用。

讲解：

```text
航班停用后状态变为 DISABLED。
旅客端不会继续售卖该航班机票。
管理员端可以再点击恢复，把状态恢复为 NORMAL。
```

建议不要停用刚新增的 `HU7001`，因为下一步要用乘客端搜索它。

## 6. 演示四：乘客查询刚新增的杭州到上海航班

退出管理员账号，登录普通乘客，例如：

```text
passengerB / pass123
```

搜索：

```text
出发城市：杭州
到达城市：上海
出发日期：2026-07-01
```

点击搜索，应能看到刚才新增的 `HU7001` 或对应杭州到上海航段。

讲解：

```text
管理员新增城市、机场、航班、航段后，乘客端可以立刻按城市和日期查询到新增的可售航段。
这证明后台资源维护和前台售票查询打通。
```

## 7. 演示五：100 人并发抢票，展示锁和事务

并发演示目标：

```text
航班：MU2001
日期：2026-07-01
航段：北京首都 PEK -> 上海虹桥 SHA
舱位：头等舱
头等舱余票：5
并发用户：100
预期结果：5 个订单成功，95 个订单失败
```

注意：前面演示不要购买 `MU2001 PEK -> SHA` 的头等舱，否则库存不是 5，脚本会提示警告。

在终端 1 执行：

```powershell
cd "D:\复旦大学\DS大二下\数据库及实现\Database-PJ"
python ".\attack(1).py"
```

脚本会自动：

```text
1. 注册或登录 lockdemo001 到 lockdemo100 共 100 个乘客账号
2. 搜索 MU2001 在 2026-07-01 的 PEK -> SHA 航段
3. 100 人同时创建头等舱订单
4. 成功的 5 个订单自动支付
5. 等待 30 秒，方便切到管理员订单页查看
6. 自动退票，恢复库存
```

30 秒等待期间，切到管理员端订单模块，展示 5 个已支付订单。

讲解：

```text
后端下单时使用 FlightSegmentRepository.findByIdForUpdate，对目标航段执行悲观写锁。
库存扣减和订单创建在同一个事务中完成。
所以即使 100 个请求同时到达，也只有 5 个请求能扣到库存并创建订单。
其余 95 个请求会被余票不足校验拒绝，不会发生超卖。
```

脚本退票后讲解：

```text
本系统没有自动候补队列。
退票后余票释放，后续乘客需要重新查询并下单。
这次演示的是并发库存控制和退票库存恢复。
```

## 8. 演示六：VIP 积分升级和九折优惠

使用乘客 A：

```text
passengerA / pass123
```

如果前面已经用乘客 A 支付过一张票，此时积分应为：

```text
1000 分
会员等级：VIP
```

再次搜索航班并创建第二张订单。

重点观察订单金额：

```text
PriceAmount：原票价
PaymentAmount：VIP 九折后的实付金额
```

讲解：

```text
系统保留原价 PriceAmount。
结算时根据会员等级计算 PaymentAmount。
VIP 用户按 0.9 折扣支付。
这样既能保留原始票价，也能清晰展示会员优惠。
```

可选用终端查看特价票和 VIP 结算：

```powershell
.\tools\run_demo_sql.ps1 -Demo tickets -Password 051130
```

## 9. 演示七：管理员新增餐食

退出乘客账号，登录管理员：

```text
admin / admin123
```

进入“资源”模块，餐食表单填写：

```text
餐食名：低脂餐
类型：LOW_FAT
描述：课堂演示新增餐食
```

点击保存。

讲解：

```text
MealOption 表维护餐食选项。
乘客下单时可以选择餐食，系统会通过 MealReservation 记录订单和餐食的关联。
```

## 10. 额外加分演示建议

### 10.1 特价票

乘客搜索北京到上海时，如果结果中出现“特价票”标识，可以说明：

```text
特价票是航段级别属性，因为 FlightSegment 是最小售票单位。
特价票按原价五折计算；如果用户同时是 VIP，则在五折基础上继续按 0.9 结算。
```

终端查看：

```powershell
.\tools\run_demo_sql.ps1 -Demo tickets -Password 051130
```

### 10.2 后端日志

展示后端日志：

```powershell
Get-Content ".\backend\logs\airticket-backend.log" -Tail 80
```

讲解：

```text
后端记录注册、登录、下单、支付、退票、改签、航班启停等关键操作。
日志不记录明文密码和明文身份证号，只记录必要的业务 ID、订单号、状态和金额。
```

### 10.3 一次性 SQL 总展示

如果时间充足，可以运行：

```powershell
.\tools\run_demo_sql.ps1 -Demo all -Password 051130
```

它会展示：

```text
1. 最新用户
2. 密码哈希和身份证摘要
3. 特价票订单结算
4. 改签链
```

## 11. 课堂收尾总结话术

可以这样总结：

```text
本系统围绕航空票务数据库 ER 图实现。
管理员端维护城市、机场、飞机、航班、航段、餐食等基础数据。
乘客端完成注册、登录、航班搜索、下单、支付、退票、改签。
数据库层面通过外键、唯一约束、Check 约束维护数据完整性。
业务层面通过事务和行级锁保证库存一致性，避免并发超卖。
安全隐私方面，密码使用 BCrypt，身份证号只保存 SHA-256 摘要。
改签通过 OriginalTicketId 形成链式追踪，保留历史订单。
VIP 会员、特价票、日志和并发演示是本项目的加分点。
```

## 12. 现场风险控制

- 每次正式演示前先执行第 1 节数据库重置。
- 后端启动后不要关闭终端 2。
- 前端启动后不要关闭终端 3。
- 并发脚本会真实注册 100 个用户、创建订单、支付、退票，建议只在并发演示环节运行。
- 如果并发脚本提示头等舱余票不是 5，说明前面操作消耗了目标库存；可以重新重置数据库后再演示并发。
- 如果杭州到上海搜不到，确认是否已经保存了航班和航段；只保存航班不保存航段，乘客端搜不到可售票。
- 如果 SQL 脚本不能运行，使用 `powershell -ExecutionPolicy Bypass -File ...` 方式运行。
