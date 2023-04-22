# 支付超时。

前置逻辑，为了保证支付可能出现丢失的情况，设置了一个redis进行延迟执行，对订单支付状态进行检查。

```
zset数据结构
add orderNo + timestamp
pay order过程
更新状态
del orderNo + timestamp
```

其中zset用来储存状态，使用的是轮询redis的zscore进行访问。

但是如果支付超时，那么无法在确认一次，如果此时支付成功，但是保存失败，讲无法补偿此次订单。
