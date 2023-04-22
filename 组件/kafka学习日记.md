# 学习日记

以下是本科的记录，先汇总下。

[1](http://42.193.170.22/2021/01/27/kafka1/kafka/)

[2](http://42.193.170.22/2021/01/27/kafka2/kafka/)

[3](http://42.193.170.22/2021/01/27/kafka3/kafka/)

[4](http://42.193.170.22/2021/01/27/kafka4/kafka/)

[5](http://42.193.170.22/2021/01/27/kafka5/kafka/)

[中文文档](https://kafka.apachecn.org/documentation.html#majordesignelements)

## 备份日志

`选取机制不同`：leader的选取不是通过raft类似的投票机制，而是从ISR队伍中选取。

`ISR`：（a set of in-sync replicas）这个列表中的队列，和leader完全一直，只有这个集合成员才有资格成为leader，一条消息必须被这个集合所有节点追加到日志，才视为提交。



## unclean leader选举

以上的政策基于isr列表没有挂的情况才可以实施，如果全部节点都挂了，有2种选择。

1. 等待ISR的副本恢复服务
2. 选择任意一个副本恢复服务作为leader。

只要是quorum-based规则中，都存在这个问题。在`一致性`和`可用性`之间妥协。