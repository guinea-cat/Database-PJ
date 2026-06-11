## 一、异常基类

```java
BusinessException
```

所有业务异常统一继承。

---

## 二、认证异常

```java
AuthException
```

对应：

```text
用户名不存在
密码错误
账号被禁用
未登录
```

---

## 三、权限异常

```java
PermissionException
```

对应：

```text
无管理员权限
访问越权资源
```

---

## 四、库存异常

```java
InventoryException
```

对应：

```text
余票不足
库存锁定失败
库存回补失败
```

---

## 五、订单异常

```java
OrderException
```

对应：

```text
订单不存在
订单已支付
订单已退款
订单已过期
订单状态非法
```

---

## 六、航班异常

```java
FlightException
```

对应：

```text
航班不存在
航班已停用
航班已取消
```

---

## 七、改签异常

```java
ChangeTicketException
```

对应：

```text
改签超时
改签差价异常
原票状态异常
```

---

## 八、退款异常

```java
RefundException
```

对应：

```text
重复退款
退款失败
```