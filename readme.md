# 订单系统

做了好久，好累啊，每天看日志，改bug，重新设计，gggg。



- [x] 高并发
- [x] 幂等性
- [x] 一致性
- [x] 限流保护
- [x] RPC服务
- [ ] 注册中心
- [ ] 动态限流
- [ ] 高可用
- [ ] 可扩展
- [ ] LSM-Tree 作为库存系统持久化。

[toc]





## 对订单系统的理解

经过一个月开发订单系统，阅读了大量的相关成熟的技术文章，对于订单系统有了自己一定的理解。订单系统的整体下来最难的点在于订单系统库存、支付、订单之间的关系（这里没有加入优惠卷部分），也就是订单的创建过程，需要考虑到非常多的问题，如果是同一个服务下，使用MySQL出现问题的可能性很小，但并发情况下，使用缓存和调用服务的过程，存在`一致性问题`与`事务性`问题，使用了消息队列以及超时问题造成了`幂等性`问题，这也是造成订单系统开发的难点。

### **订单系统与支付系统关系**

其实订单系统需要维护自身的正确性，需要多个组件进行保证，而对于支付系统而言，肯定更加的复杂。在测试时间的时候，会发现`订单系统`所花费的时间其实整体过程和`支付系统`相差不多，虽然我的设计会根据将订单的创建与支付在同一个流程2个事务中完成。但这样子很大程度浪费将近一半的时间在支付系统上，所以应该创建往后，将支付时间直接浪费在用户上，才是最合理的。

支付系统的`优化`,支付系统之前需要花费0.8s才能完成一次请求，这个时间够创建4~6订单了，这个是没办法容忍的，优化思路从2个反面，优化netty的**rpc服务**，优化**数据库**。这个部分其实没怎么用上缓存，因为创建支付过程没几个状态，需要修改部分很少，大部分是读取，而读取则只需要一个到2个缓存就够了。对于netty的优化，之前的`rpc过程`使用的是jdk的反射，之后全部换成了`cglib`的进行方法反射。而对于连接问题来说，每次服务都会创建一个客户端，但是使用了连接池后发现其实效果差不过。而且连接池也会存在超时问题。对于数据库的优化，主要是加了几个索引，对主键生成修改，之前使用uuid，当订单数目到达了一定数目后，插入明显慢了，也能理解。uuid是字符串，且是完全随机，这插入过程索引频繁的修改，对性能不好，所以之后id生成使用了`雪花算法`索引是一个bigint，而且基于雪花算法的生成，总是有序的。最后总体性能在本地运行，远程组件的情况下，是0.4s的延迟。将服务部署到服务器上，由于物理上靠近，整体的速度惊人，比本机快**30倍**(物理距离看来还是很重要的)。

### **订单系统设计原则**

整体设计上应该凸显`早失败`的原则，`有序驱动`，`补偿`。订单系统，很多时候有库存限制，如果库存不够，根本没必要走到消息队列哪一个步骤，应该在创建订单的消息之前的时候，直接噶掉，还有如果服务压根处理不了那么多请求，就不要接受了要不然直接g了不太好，或者采取动态扩容，我目前还没实现（库存系统的原因）。其实早失败对整体服务感觉维护起来很重要，之后的部分为了一致性采取了太多了限制于补偿，我第一步就g那完全可以剩下很多的事情。

`有序驱动`也就是一个，对过程容忍行为吧。比如创建订单可以容忍支付失败，但是绝不能容易扣除库存失败。支付失败必须要在有订单的基础上才行。也就是过程必须是有序的。订单一旦创建成功，必须是在库存扣除成功的前提，而支付一旦成功，必须在订单创建入库的原则下。也就是`事务性`吧,但这些操作写不到一个事务里面，也就是有序的事务性，但是为了完整事务，但即使失败也必须将失败的事务补偿回来。

`补偿`。如果过程是完整，也就不会这么多事情，但是肯定会出现订单创建失败，或者支付失败或者丢失的情况。这种情况下需要判断下一个操作是否成功，再来决定返还事宜。如果创建订单操作失败了，必须将订单的库存返还回来才行。支付由于超时或者其他的异常错误，导致没有`明确`的返回结果，从而导致事务回滚或者未生效，这是不被允许的，应该尽快查询支付状态，修改订单状态。

`强迫进行`。比如说，用户选择创建订单，那么订单除去库存不足的情况，就应该被成功创建（组件故障导致的需要紧急处理），也就是最终成功性。不能说系统除了故障就创建失败了吧，再比如用户既然提出了支付，那么最好只有2种结果，支付前不够导致的失败，和最终的成功。用户进行重试，理论上来说并不是一个很好的体验，目前来说只能通过redis和mq结合补偿才能完成这个。

## 高并发

前一个版本是同步处理消息，这个版本采用消息队列处理订单生成消息。

此时做的事情有3件。

1. 限流
2. 预判断库存
3. 生成订单号

由于是异步处理，必须要有一个回调查询过程，所以此时的依据则是订单号了。

订单号此时必须生成，用于前端的回调查询，此后的订单号必须保持相同。

对于订单生成，会按照一次提交的商品，按照商铺进行归类，有`几个商铺`生成`几个订单`。



消息队列的异步操作处理，虽然失去了同步，但是在请求量很高的情况下，可以达到晓峰的效果，也能保证消耗的持续与稳定。但必须预防消息的积压问题，消息积压，最好的办法是多几个消费者，实在不行就把不能处理的订单量给拒绝算了，代码优化感觉只会占很小的一部分，架构的设计才是核心。

**demo**

**request body**

```json
{
    "uid" : 1,
    "to_uid": 2,
    "origin_price": 29.99,
    "actual_price": 29.99,
    "pay_at_once": true,
    "commodity_details": [
         {
                "actual_price": 29.99,
                "commodity_id": 1,
                "good_type": 1,
                "page_type": 0,
                "prices": 29.99,
                "user_id": 100001,
                "num": 1
            },
            {
                "actual_price": 49.99,
                "commodity_id": 2,
                "good_type": 1,
                "page_type": 0,
                "prices": 49.99,
                "user_id": 100002,
                "num": 1
            }
    ]
}
```

**response**

```json
{
    "data": {
        "order_no": [
            "14401939662076689831000011679206719",
            "14401939662076689841000011679206719"
        ]
    },
    "message": "OK",
    "status": 200
}
```

## 库存系统

这里的库存系统，是和商品系统写在一起了，按道理应该将库存和商品分两张表，比较合理。因为商品表总的来说都是读操作，但是库存表较多的是写操作，所以最好单独分一个库存表。

### **需要用到缓存？**

库存系统，对库存进行操作，如果都在库中进行是一个很可怕的事情，如果请求率和修改率能达到1：1，其实压根不会出问题。如果一但，请求率`高于`能够修改的速率，1.1:1那么1s就是0.1，2s就是0.2，越往后后续等待的窗口越长，所以提高修改效率非常重要，也就是会使用到`缓存`。我看了许多关于`缓存一致性`的方案，但这些方案并不适用于库存，库存是一个修改频繁的键，如果每次修改都去库中同步，那还不如直接读取库算了。订单查询这种操作使用的就是`延迟双删`。

需要使用缓存另一个重要原因是redis处理是`单线程`,如果缓存的扣除由订单生成的时候，直接操作数据库，如果只使用一个消息队列单线程，其实没啥关系，但是如果使用了多线程，对于同一个商品的修改，一个时间内，只有一个线程能修改库存成功，如果有**2个商品在同一个事务**内`修改库存`，很容易陷入事务大量失效的场景,多线程此时就是噩梦般存在，此时设计上对于订单创建上，商品代码的`侵入性`会很高。所以将整体库存消耗交给redis，再通过有限状态自动机中的action部分，通知数据库修改库存。

### **库存初始化**

使用lua脚本先get key，返回一个list，然后将缺少的list查库，然后setnx设置key，设置过期时间60s（这个时间是允许数据库最终一致性的时间，也就是消息队列消耗最大时间）。

###  **如何防止超卖**

超卖在库存中肯定不被允许，使用数据库肯定不需要考虑这个问题，使用缓存的时候，需要考虑到原子性问题，使用decs和incs可以保证库存的原子性，然后将值返回，如果小于0就说明不够了。

### **如何保证库存系统的原子性？**

很多时候，商品都会有多件，需要保证这些商品同时足够。第一个版本的时候，我是直接使用redis的ops来操作，遇到不够的则依次反向操作一次，但是如果此时发生故障，库存系统肯定不在准确，后续学习了一下午lua脚本，之后的部分redis有多个操作需要保证原子性的时候，都会使用lua脚本。例如商品库存扣除脚本如下。

```lua
local inventory = KEYS
local consumer = cjson.decode(ARGV[1])
local orderNo = cjson.decode(ARGV[2])

redis.log(redis.LOG_DEBUG, consumer)
-- return consumer[1]
--
for i = 1, #inventory
do
    --     print(inventory[i])
    local invent = tonumber(redis.call('get', inventory[i]))
    redis.log(redis.LOG_DEBUG, invent)
    if (invent < consumer[i])
    then
        return -1;
    end
end

-- 扣减库存
for i = 1, #inventory
do
    redis.call('DECRBY', inventory[i], consumer[i])
end
for i = 1, #orderNo
do
    redis.call('sadd', 'orderInventory', orderNo[i])
    redis.call('sadd', 'inventorySuccess', orderNo[i])
    -- 延迟消息逻辑比较复杂，
    -- 首先发送一个mq，用来之后核对库存。
    -- 扣减库存 执行当前lua脚本
    -- 执行保存订单操作。
    -- 如果执行成功，删除 orderInventory,对应值。
    -- 如果保存成功后，但是删除过程或者其他过程出了问题，可以由延迟消息补救一次。

    -- 延迟消息主要是判断3点，第一点如果订单没有创建，那么需要返回库存。
    -- 如果创建成功。则不做任何操作。

    -- case 1.非常特殊的情况，订单插入的时候，由于主键约束，不会出现幂等状况，由于是先删除的库存，所以无法保证
    -- 2次相同的请求，都进行扣除了库存操作，所以需要将o和i的值设置为orderNo+时间戳，保证创建失败的时候，将重复消息返回。

    -- orderInventory是用来确认订单执行成库存的，如果订单直接保存成功，肯定是能扣减库存成功的。
    -- inventorySuccess用来确认，订单是否成功扣减。
end
return 1;

```

### **库存补偿**

其实这个问题就涉及到了分布式事务一致性问题了。不再是缓存一致性，但好消息是库存只与**订单是否生成**有关系，与支付状态无关。订单生成必须扣除库存，订单未生成不能扣除库存。

如果订单没创建成功，库存是必须返回的，这个时候需要用到消息队列来维护库存的补偿系统。这里使用消息队列的死信交换机+队列完成延迟消息。每一个库存操作都需要在10s后检查订单是否生成成功。没有生成成功则返回库存到cache，删除orderInventory键。这个是一个set类型，里面是orderNo+timestamp。主要作用是确保库存确实倍扣除了，如果这个时候库存压根没扣，但是延迟消息发现没有订单，把库存返还回去，逻辑不正确。还有就是`幂等操作`，订单超时的情况下，订单入库是入不了的，但是库存返回，如果只有订单号可能会出现误判。所以需要结合时间戳判断，是否是同一个订单，如果是重复订单应该返回库存。

通过库存的补偿，缓存，在订单创建完成肯定是没有问题的。

而数据库修改，则是必须在订单创建的基础上，才能进行数据库修改。所以二者在同一个事务内，通过消息队列通知库存系统来修改数据库。将整个耗时的部分从订单创建中剥离。

```java
 @Override
    public void saveCommodityOrder(List<CommodityOrderNewDTO> orderNewDTOS) {
        long s1 = System.currentTimeMillis();
        List<CommodityReDTO> list = new ArrayList<>();
        List<String> orderNo = new ArrayList<>();
        long current = System.currentTimeMillis();
		// 预维护
        for (int i = 0; i < orderNewDTOS.size(); i++) {
            list.addAll(orderNewDTOS.get(i).getCommodityReDTOList());
            String order = orderNewDTOS.get(i).getOrderId();
            String newOrder = order + current;
            orderNo.add(newOrder);
            CommodityCacheInventoryMO inventoryMO = new CommodityCacheInventoryMO();
            inventoryMO.setOrderNo(newOrder);
            inventoryMO.setReDTOList(orderNewDTOS.get(i).getCommodityReDTOList());
            mqClient.sendDelayTask(MqMsgTypeEnum.ORDER_CACHE_INVENTORY, inventoryMO, 10 * 1000);
            orderNewDTOS.get(i).setTimeStamp(String.valueOf(current));
        }
        
		//扣除库存
        commodityService.subInventory(CommodityDecorateBO.convert(list), orderNo);
        
        long s2 = System.currentTimeMillis();
        List<CommodityOrderNewDTO> dtos = orderNewDTOS;
		//保存订单自动机入口
        doSaveOrder(dtos);
        //成功创建订单后，删除redis中埋得点。
        commodityService.successInitOrder(orderNo);
        long s3 = System.currentTimeMillis();
        //支付订单自动机入口
        payOrders(dtos);
        long s4 = System.currentTimeMillis();
        //        long s3=System.currentTimeMillis();
        log.info("inventory time{},record time{},pay time{}", s2 - s1, s3 - s2, s4 - s3);
    }
```

### **订单未支付超时返回库存**

超时未支付，也是用的延迟队列实现。过程与之前类似，返回缓存与数据库。此时如果未支付，直接返还库存即可，不需要考虑事务性。此时的想法是一定要返还库存。但是如果存在极端情况，例如支付系统宕机了，那就把此条消息保存到redis中，由循环事件检测支付系统，如果一旦存活，将redis的消息进行重试消费。如果redis也宕机了，只能将此消息直接抛出异常，将消息此通道消息阻塞，直到超时（都宕机了玩个屁）。所有的数据库操作都会发到同一个队列，单线程处理。



### **不足**

订单的数据库操作都是单线程处理，肯定会造成瓶颈。或许可以将多个商品返回库存操作拆分成，多个单独的请求，这样可以造成失败请求减少，但对属于统一订单的商品原子性可能造成破坏。这样好处是，对相同的商品分到同一个队列，加速库存入库。消息队列不能长期宕机，一旦消息队列g了，那么会造成消息积压，库存数据库一致性受损。整个过程中数据库可以g，redis可以g，代码也可以g，但是消息队列肯定不能g。

### 思考

1. 真的需要同步数据库嘛？？？

网上一直说，数据库作为持久化是必要的，但现在redis这么稳定，直接放redis里也可以吧，宕机了也有从服务器。

2. 持久化可以尝试用写多读少的组件。

之前看过lsm数据结构，感觉这个应该很适合库存场景，由于数据库写太费时间采用缓存，而lsm可以做到o（1）的增删改过程，非常适合库存系统，可以同步双写缓存和lsm，有读取场景，也只会有一个地方能够读取，慢点就慢点吧。或者说频繁的改动，势必会将库存打入到lsm的memory层，即使宕机的时候也能够很快的访问到。



## 订单系统

订单系统，由自动机支撑起幂等性，与正确性，简单的show下代码，结构还是很清晰的。

由controller触发发送消息到mq。

```java
    @Override
    public void dispatchMq(String orderNo, CommodityOrderActionEnum action, Object param) {
        CommodityOrderFsmMO orderMO = new CommodityOrderFsmMO(action.getAction(), orderNo, param);
        redisCache.incr("action->" + action.getAction(), 1);
        mqClient.sendTaskMsg(MqMsgTypeEnum.COMMODITY_ORDER_MQ, orderMO);
//        mqClient.sendKafkaTaskMsgTopic1(MqMsgTypeEnum.COMMODITY_ORDER_MQ, orderMO);
    }
```

到了消息队列后，整体设计模式是由装饰者设计模式构建出有限状态自动机,此部分结构非常简单。

```java
  new CommodityOrderFsmManageBuilder()
                    .setActionEnum(actionEnum)
                    .setOrderBriefBO(orderBriefBO)
                    .setParam(param)
                    .build()
                    .handle();
```

handle过程分为处理订单和订单后触发逻辑。

handle为核心过程，主要是创建订单与修改订单状态。

action则是行为，主要是订单创建成功后所触发的连锁反应，如通知用户，删除订单缓存等等。


```java
    public void handle() {
        long s1 = System.currentTimeMillis();
        getFsm().handle();
        long s2 = System.currentTimeMillis();
        getAction().onAction(getOrderBriefBO());
        long s3 = System.currentTimeMillis();
        log.info("action {},handle time{},action time{}", getAction(), s2 - s1, s3 - s2);
    }
```

其中各个订单状态的转移，非常重要。所以在自动的handle过程中需要判断状态，也就是checkAction。

```java
    void handle() {
        checkAction();
        checkParam();
        doHandle();
    }
```

例如创建订单的状态，只能是newaction。如果遇到payaction和cancle action是不合理的。

**当订单创建后，变为待支付状态，此时允许的动作只有支付与主动取消和自动取消。**而一切参数ok后，就按照不同的action做出不同的动作，自动机的选取是根据数据库中保存的state字段，例如TO_PAY，读取到自动机后为pay自动机，此时再来核对action。

### 订单过程错误发生。

订单过程，主要为以下3个过程（优惠卷模块还没写）。

1. 库存过程。
2. 订单创建过程。
3. 支付过程。

发生错误的位置有7处，也就是前前后后+3个过程，对此过程，我选择了每处状态标记。

整个流程都在本文的3.5处。

伪代码如下

```
saveOrder(){
 mq.sendDelayMq(order+timestampe,commodity_details)// 发送延迟消息 remark 1
 subInvenotry()//扣减库存 remark 2（具体看上一章）。
//remakr 3 这个过程中由remark 1的消息队列维护。
 saveOrder()//保存订单。remark 4(事务维护数据库，库存还是有remark1的消息队列维护，由于带有时间戳，就算重试，也是可以返回第二次的扣减库存)
 initSuccess()//通知商品服务，订单创建成功remark5(靠的是remark2中redis脚本写入的字段)
 payOrder()//支付订单remark6(订单支付，远程服务，不具有事务性,通过下面的doSafaPay进行)。
 //remark7此处再次发生故障没有意义，因为一旦remark4成功后，之后的消息都无法走到此处。
}
```

`幂等性？`

此消息失败也是有幂等性的，扣除的库存肯定能补偿回来。创建订单成功后，后续的操作到了remark4就不会进行了。

#### remark1 fail

消息队列发送都失败了，直接报错重试完了。

#### remark2 fail

库存不足，直接返回库存不足的消息，如果是其他的异常，需要尝试再次消费消息。

#### remark3 fail

remark1的延迟消息队列起作用，假设重试过程中创建成功了，也不需要担心，订单有一个时间戳字段，如果时间戳不同，直接认为是2次操作，返还库存即可。

#### remark4 fail

这个与上同理。

#### remark5 fail

这个主要是延迟消息回来判断，如果redis中这些字段都没删除，那就直接进行，查库，如果库没有就直接认为没成功返回库存。

#### remark6 fail

这个只能失败一次，所以需要额外维护，也就是上面那个safepay，将消息写入zscore，然后根据时间戳，设置一个最大的超时时间，定时任务去拉去redis中超时的支付，判断是否支付成功，如果遇到了支付服务不可用的情况，直接等下一批次就完事了。直到支付服务可用为止。

#### remark7 fail

这里失败没有任何意义。

#### 总结

前3个没封装好，应该放到库存系统里去做，不应该出现在订单系统代码这里，后面拆分再说。

remark5位置还得讨论，放在此处还是给自动机的action去发一个消息队列。

## 支付系统

loading ... on my way

## 其他组件

### 配置升级兼容过程

rabbitmq并不能做到高可用,而且吞吐有限,所以先兼容Kafka再说.具体创建集群Kafka，和兼容rabbitmq代码文章如下.

[支持高可用高并发的Kafka消息队列](./组件/消息队列.md) 

