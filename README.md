# 航空票务数据库系统运行说明

这是一个大学数据库课程设计项目：航空票务数据库系统。

项目分为三部分：

- `database`：数据库建表脚本和初始化数据
- `backend`：后端 Spring Boot 服务，端口 `8080`
- `frontend`：前端 React 页面，端口 `5173`

本项目严格以 ER 图为准，数据库只使用 9 张核心表。

## 1. 运行前确认

请先确认你电脑上已经安装并配置好：

- MySQL 9.6
- Java 17
- Maven
- Node.js
- npm

本项目使用的 MySQL 账号密码是：

```text
账号：root
密码：051130
端口：3306
数据库名：airticket
```

## 2. 初始化数据库

如果你刚刚清空过数据库，或者第一次运行项目，请先初始化数据库。

打开 PowerShell，进入项目根目录：

```powershell
cd C:\Users\19588\Desktop\ds_study\database_introduction\Database-PJ
```

然后依次执行下面两条命令：

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 < "C:\Users\19588\Desktop\ds_study\database_introduction\Database-PJ\database\schema.sql"'
```

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 < "C:\Users\19588\Desktop\ds_study\database_introduction\Database-PJ\database\seed_data.sql"'
```

第一条命令用于建表，第二条命令用于导入演示数据。

初始化成功后，数据库中会有：

- 1 个管理员账号
- 2 个乘客账号
- 城市、机场、飞机、航班、航段、餐食等演示数据

## 3. 启动后端

新开一个 PowerShell 窗口，执行：

```powershell
cd C:\Users\19588\Desktop\ds_study\database_introduction\Database-PJ\backend
mvn spring-boot:run
```

看到类似下面的信息，说明后端启动成功：

```text
Tomcat started on port(s): 8080
Started AirticketApplication
```

后端地址是：

```text
http://localhost:8080
```

这个窗口不要关闭，保持运行。

## 4. 启动前端

再新开一个 PowerShell 窗口，执行：

```powershell
cd C:\Users\19588\Desktop\ds_study\database_introduction\Database-PJ\frontend
npm run dev
```

看到类似下面的信息，说明前端启动成功：

```text
Local: http://localhost:5173/
```

然后用浏览器打开：

```text
http://localhost:5173
```

## 5. 演示账号

管理员账号：

```text
账号：admin
密码：admin123
```

900 分乘客账号：

```text
账号：passengerA
密码：pass123
```

普通乘客账号：

```text
账号：passengerB
密码：pass123
```

## 6. 推荐演示流程

普通乘客可以演示：

1. 登录
2. 查询航班
3. 下单
4. 支付
5. 查看我的订单
6. 退票
7. 改签
8. 注销账号

管理员可以演示：

1. 登录管理员账号
2. 查看城市、机场、飞机、航班、航段、订单
3. 新增或修改航班
4. 停用航班
5. 恢复启用航班
6. 新增或编辑航段

VIP 演示：

1. 使用 `passengerA / pass123` 登录
2. passengerA 初始是 `900` 分，会员等级是 `NORMAL`
3. 支付第一张票后，积分变为 `1000`，自动升级为 `VIP`
4. 再下第二张票，支付金额会显示 9 折

## 7. 如何关闭项目

关闭项目不是关闭浏览器，而是停止前端和后端服务。

### 方法一：最简单

找到运行后端的 PowerShell 窗口，按：

```text
Ctrl + C
```

找到运行前端的 PowerShell 窗口，也按：

```text
Ctrl + C
```

如果提示是否终止批处理操作，输入：

```text
Y
```

然后回车。

### 方法二：端口强制关闭

如果你找不到窗口，可以在新的 PowerShell 中执行：

```powershell
$ports = @(8080,5173)
$listeners = Get-NetTCPConnection -LocalPort $ports -ErrorAction SilentlyContinue | Where-Object { $_.State -eq 'Listen' } | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($processId in $listeners) { Stop-Process -Id $processId -Force }
```

这会关闭：

- 后端端口 `8080`
- 前端端口 `5173`

不会关闭 MySQL 服务。

## 8. 如何清空数据库

如果你想把整个项目数据库清掉，执行：

```powershell
& "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS airticket;"
```

清空后，下次运行前需要重新执行“初始化数据库”的两条命令。

## 9. 常见问题

### 1. 打不开页面

先确认前端是否启动成功。

浏览器访问：

```text
http://localhost:5173
```

不是访问 `8080`。

### 2. 登录失败或查不到数据

可能是数据库没有初始化。

请重新执行：

```powershell
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 < "C:\Users\19588\Desktop\ds_study\database_introduction\Database-PJ\database\schema.sql"'
cmd.exe /c '"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p051130 --default-character-set=utf8mb4 < "C:\Users\19588\Desktop\ds_study\database_introduction\Database-PJ\database\seed_data.sql"'
```

### 3. 后端启动失败

先确认 MySQL 服务正在运行。

PowerShell 执行：

```powershell
Get-Service -Name MySQL96
```

如果状态不是 `Running`，说明 MySQL 没启动。

### 4. 端口被占用

如果提示 `8080` 或 `5173` 被占用，先执行关闭命令：

```powershell
$ports = @(8080,5173)
$listeners = Get-NetTCPConnection -LocalPort $ports -ErrorAction SilentlyContinue | Where-Object { $_.State -eq 'Listen' } | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($processId in $listeners) { Stop-Process -Id $processId -Force }
```

然后重新启动后端和前端。

## 10. 一句话运行顺序

第一次运行或清空数据库后：

```text
初始化数据库 -> 启动后端 -> 启动前端 -> 浏览器打开 http://localhost:5173
```

平时数据库已经初始化过：

```text
启动后端 -> 启动前端 -> 浏览器打开 http://localhost:5173
```

