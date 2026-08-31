# Redis 复习梳理版

## 0. 一句话总览

Redis 是一个以内存为主的高性能 key-value 数据库。它不只是缓存，还提供丰富的数据结构、过期机制、持久化、主从复制、哨兵、集群、Lua、Pipeline、分布式锁等能力。

复习 Redis 时建议按这条主线理解：

```text
为什么快 -> 能存什么 -> 怎么不丢数据 -> 怎么高可用 -> 缓存问题怎么处理 -> 底层结构和实战应用
```

复习优先级标记：

| 标记 | 含义 |
|---|---|
| 【必须掌握】 | 高级 Java 开发面试和项目里高频，必须能讲清原理、方案和坑 |
| 【了解即可】 | 要知道是什么、能解决什么问题，但不用源码级深入 |
| 【降权】 | 不作为主线复习，项目或简历写到再深入 |

---

## 1. Redis 基础【必须掌握】

### 1.1 Redis 是什么【必须掌握】

Redis 是基于 key-value 的 NoSQL 数据库，数据主要存放在内存中，所以读写性能很高。

它的 value 不只是简单字符串，还支持：

| 类型 | 典型用途 |
|---|---|
| String | 缓存、计数器、Session、分布式锁 |
| Hash | 用户对象、商品对象、字段级更新 |
| List | 简单队列、文章列表、时间线 |
| Set | 标签、去重、共同好友 |
| ZSet | 排行榜、延时队列、权重排序 |
| Bitmap | 签到、在线状态、布尔统计 |
| HyperLogLog | UV 去重统计 |
| GEO | 地理位置附近的人、门店距离，普通 Java 后端可降权 |
| Stream | 消息队列、消费组、ACK |

### 1.2 Redis 常见应用场景【必须掌握】

| 场景 | 说明 |
|---|---|
| 缓存 | 缓解数据库压力，提高读取速度 |
| 计数器 | 浏览量、点赞数、库存扣减 |
| 排行榜 | ZSet 按 score 排序 |
| 社交关系 | Set 做关注、粉丝、共同好友 |
| 消息队列 | List 简单队列，Stream 更完整 |
| 分布式锁 | `SET key value NX EX seconds` |
| 限流 | String 计数 + 过期时间，或 Lua 保证原子性 |
| Session/Token | 存登录态、验证码、临时凭证 |

高级 Java 开发要多补一句：Redis 通常不作为权威数据源，权威数据仍然在 MySQL/PostgreSQL 等数据库里。Redis 更多承担缓存、热点数据、临时状态、轻量协调和部分异步流量削峰。

---

## 2. Redis 为什么快【必须掌握】

核心原因：

1. **基于内存操作**：避免磁盘随机 IO。
2. **单线程执行命令**：避免多线程锁竞争和上下文切换。
3. **IO 多路复用**：一个线程可以管理大量连接。
4. **高效数据结构**：SDS、dict、skiplist、quicklist、intset 等。
5. **命令设计简单**：大多数命令时间复杂度低。

### 2.1 Redis 真的是单线程吗【必须掌握】

要分清楚：

```text
命令执行主逻辑：单线程
网络 IO、持久化、异步删除等：可能使用额外线程或子进程
```

Redis 6.0 引入多线程主要是为了处理网络 IO 的读写，命令执行仍然保持单线程模型，所以不用担心命令之间复杂的并发竞争。

高级 Java 面试里要特别强调：Redis 单线程模型的代价是，一旦执行慢命令、大 key 删除、长 Lua 脚本，整个实例的其他请求都会被拖慢。

### 2.2 IO 多路复用怎么理解【了解即可】

普通阻塞 IO 像老师一个个检查学生作业，遇到一个没写完就卡住。

IO 多路复用像老师站在讲台等，谁写完谁举手，老师只处理已经准备好的连接。

Linux 下常见实现：

```text
select -> poll -> epoll
```

Redis 基于这种机制可以用较少线程处理大量客户端连接。

---

## 3. 数据结构复习【必须掌握】

### 3.1 String【必须掌握】

适合：

```text
缓存简单值
计数器
分布式锁
验证码
Session/Token
限流
```

常见命令：

```redis
SET key value
GET key
INCR key
DECR key
SET key value NX EX 30
```

注意点：

String 可以存 JSON，但如果对象字段需要频繁单独修改，Hash 更合适。

高级 Java 里还要注意序列化：不要无脑使用 Java 原生序列化。推荐使用 JSON、String、MsgPack、ProtoBuf 等可控格式，方便排查、兼容和跨语言读取。

### 3.2 Hash【必须掌握】

Hash 是 field-value 结构，适合存对象。

例如：

```text
user:1001
  name -> 张三
  age -> 28
  email -> xxx
```

优点：

```text
字段级读写
避免整个 JSON 反序列化
适合用户、商品、配置等对象缓存
```

String 存 JSON 和 Hash 存对象对比：

| 对比 | String JSON | Hash |
|---|---|---|
| 修改单字段 | 需要读整段 JSON 再写回 | 直接 HSET |
| 并发覆盖风险 | 较高 | 较低 |
| 序列化成本 | 有 | 较少 |
| 使用复杂度 | 简单 | 略高 |

实际取舍：如果对象通常整体读写，String JSON 更简单；如果字段经常单独变更，Hash 更合适。不要为了“看起来专业”强行把所有对象都拆成 Hash。

### 3.3 List【了解即可】

List 是有序列表，可以从左右两端插入或弹出。

典型用法：

```text
栈
队列
文章列表
简单任务队列
```

常见命令：

```redis
LPUSH queue task
RPOP queue
BLPOP queue 10
LRANGE list 0 9
```

List 做消息队列的问题：

```text
没有天然 ACK
消费者挂掉可能丢消息
重试机制要自己做
复杂场景不如 Stream / Kafka / RabbitMQ
```

高级 Java 开发只需要知道 List 能做简单队列，但可靠消息队列不要优先选 List。生产上如果需要 ACK、重试、消费组、消息堆积治理，应考虑 Redis Stream 或专业 MQ。

### 3.4 Set【必须掌握】

Set 是无序不重复集合。

适合：

```text
标签系统
去重
共同好友
共同关注
抽奖
黑白名单
```

常见命令：

```redis
SADD key member
SISMEMBER key member
SINTER key1 key2
SUNION key1 key2
SDIFF key1 key2
```

核心能力：

```text
O(1) 判断元素是否存在
支持交集、并集、差集
```

### 3.5 ZSet【必须掌握】

ZSet 是有序集合，每个 member 绑定一个 score。

适合：

```text
排行榜
热度榜
延时队列
权重排序
按时间范围查询
```

常见命令：

```redis
ZADD rank 100 user1
ZREVRANGE rank 0 9 WITHSCORES
ZRANK rank user1
ZREVRANK rank user1
ZRANGEBYSCORE delay 0 now
```

面试重点：

```text
小 ZSet：压缩结构
大 ZSet：skiplist + hashtable
skiplist 支持范围查询和排名
hashtable 支持按 member 快速查 score
```

注意：ZSet 的 score 是 double，涉及金额、精确排序、复合排序时不要直接用小数。常见做法是把分数放大成整数，或者把时间戳、权重编码成 long 型分数。

### 3.6 Bitmap【了解即可】

Bitmap 本质是 String 的位操作。

适合：

```text
签到
用户在线状态
布尔型统计
活跃用户标记
```

特点：

```text
极省内存
适合大量 0/1 状态
```

例如用户签到：一个用户一天只占 1 bit。

### 3.7 HyperLogLog【了解即可】

HyperLogLog 用于估算去重数量，典型场景是 UV。

特点：

```text
内存极小
有约 0.81% 误差
只能统计基数，不能取出具体元素
```

适合：

```text
网站 UV
页面访问独立用户数
大规模去重计数
```

### 3.8 Stream【必须掌握】

Stream 是 Redis 5.0 后提供的消息流。

比 List 更适合做消息队列，因为它支持：

```text
消息持久化
消费者组
ACK
Pending List
消息重试
```

如果只是简单异步任务，List 可以用；如果要可靠消费，优先考虑 Stream，复杂系统优先 Kafka/RabbitMQ。

高级 Java 需要补充：

```text
Stream 有消费者组 Consumer Group
消费者消费后需要 ACK
未 ACK 的消息会进入 Pending List
消费者挂了以后，可以由其他消费者认领 Pending 消息
```

使用边界：

```text
Redis Stream 适合轻量级消息流、业务内部异步任务、低到中等规模的可靠消费
核心交易链路、跨系统复杂消息治理、长期堆积、严格顺序和复杂重试，优先 Kafka/RabbitMQ/RocketMQ
```

---

## 4. 持久化【必须掌握】

Redis 的数据在内存中，为了重启恢复，需要持久化。

主要方式：

```text
RDB：快照
AOF：追加写命令日志
混合持久化：RDB + AOF
```

### 4.1 RDB【必须掌握】

RDB 是某一时刻的数据快照，文件通常是 `dump.rdb`。

触发方式：

| 方式 | 说明 |
|---|---|
| `save m n` | m 秒内至少 n 次写入，触发快照 |
| `SAVE` | 同步生成 RDB，会阻塞主线程 |
| `BGSAVE` | fork 子进程后台生成 RDB |
| 主从全量复制 | master 生成 RDB 给 replica |
| 正常关闭 | 可能触发快照，取决于配置 |

优点：

```text
文件紧凑
恢复速度快
适合备份
对主线程影响较小，因为通常由子进程完成
```

缺点：

```text
两次快照之间的数据可能丢失
fork 时可能有内存和 CPU 开销
大数据量时生成快照耗时
```

高级 Java 面试要会补充：RDB 通常通过 fork 子进程生成。fork 本身可能造成瞬时阻塞，子进程写 RDB 时还会触发写时复制，如果此时写入很多，内存压力会明显增加。

### 4.2 AOF【必须掌握】

AOF 是把 Redis 写命令追加到日志文件中，重启时重放命令恢复数据。

开启：

```conf
appendonly yes
```

刷盘策略：

| 配置 | 含义 | 数据安全 | 性能 |
|---|---|---|---|
| `appendfsync always` | 每次写都 fsync | 最安全 | 最慢 |
| `appendfsync everysec` | 每秒 fsync | 通常最多丢 1 秒 | 折中，最常用 |
| `appendfsync no` | 交给操作系统决定 | 风险较高 | 最快 |

优点：

```text
数据丢失更少
日志可读性较好
everysec 兼顾性能和可靠性
```

缺点：

```text
文件更大
恢复可能比 RDB 慢
需要 AOF rewrite 压缩
```

注意区分：

```text
write：Redis 把日志写到操作系统缓冲区
fsync：操作系统把缓冲区刷到磁盘
appendfsync 控制的是 fsync 频率
```

### 4.3 AOF Rewrite【必须掌握】

AOF 会不断追加，文件越来越大。Rewrite 会把冗余命令压缩成当前数据状态所需的最少命令。

例如：

```redis
SET count 1
SET count 2
SET count 3
```

重写后只需要：

```redis
SET count 3
```

常见触发：

```conf
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
```

Rewrite 也可能通过子进程完成，同样要关注 fork、磁盘 IO、写时复制带来的性能波动。

### 4.4 RDB 和 AOF 怎么选【必须掌握】

| 需求 | 建议 |
|---|---|
| 只做缓存，丢了可重建 | 可不开持久化，或只开 RDB |
| 希望少丢数据 | AOF everysec |
| 需要快速备份恢复 | RDB |
| 生产环境通用选择 | RDB + AOF everysec |

Redis 同时开启 RDB 和 AOF 时，重启恢复通常优先用 AOF，因为 AOF 更新。

### 4.5 混合持久化【了解即可】

Redis 4.0 引入混合持久化，AOF rewrite 后的新 AOF 文件前半部分是 RDB 格式的全量快照，后半部分是 rewrite 期间产生的 AOF 增量命令。

优点：

```text
恢复速度接近 RDB
数据完整性接近 AOF
```

面试回答一句话即可：混合持久化本质是“RDB 全量 + AOF 增量”的组合。

---

## 5. 高可用【必须掌握】

Redis 高可用分三层：

```text
持久化：解决重启恢复
主从复制 + Sentinel：解决自动故障转移
Redis Cluster：解决水平扩展和集群容灾
```

### 5.1 主从复制【必须掌握】

结构：

```text
Master 负责写
Replica 复制 Master 数据，可承担读
```

作用：

```text
数据副本
读写分离
故障恢复基础
```

同步方式：

| 类型 | 说明 |
|---|---|
| 全量同步 | 首次复制或复制断层较大时，master 生成 RDB 发给 replica |
| 增量同步 | 复制偏移量 + backlog，只同步断开期间缺失的数据 |
| 命令传播 | master 持续把写命令同步给 replica |

问题：

```text
异步复制可能丢数据
主节点故障不能自动切换
主从延迟导致读到旧数据
```

高级 Java 项目里要注意：如果业务强依赖“读己之写”，刚写完 master 后马上读 replica，可能读到旧数据。解决方式包括读 master、延迟读、业务版本校验，或者只在可接受弱一致的场景读从库。

### 5.2 Sentinel 哨兵【必须掌握】

Sentinel 负责：

```text
监控 master/replica
判断 master 是否下线
选举 Sentinel leader
从 replica 中选出新 master
让其他 replica 复制新 master
通知客户端新 master 地址
```

关键概念：

| 概念 | 说明 |
|---|---|
| 主观下线 | 一个 Sentinel 认为 master 挂了 |
| 客观下线 | 多个 Sentinel 达成共识，认为 master 挂了 |
| quorum | 判断客观下线需要的票数 |
| failover | 故障转移 |

Sentinel 适合：

```text
单主多从
数据量不需要分片
希望自动故障转移
```

### 5.3 新主节点怎么选【了解即可】

大致依据：

```text
过滤不健康 replica
优先级 replica-priority 高的优先
复制偏移量越大，数据越新
runid 字典序作为最后兜底
```

不需要死记完整投票细节，但必须知道 Sentinel 会从较健康、数据较新的 replica 里挑新 master。

### 5.4 Redis Cluster【必须掌握】

Redis Cluster 用来做分布式集群。

核心机制：

```text
整个 key 空间分成 16384 个 hash slot
每个 master 负责一部分 slot
每个 master 可以有 replica
节点之间通过 Gossip 通信
客户端根据 MOVED/ASK 重定向访问正确节点
```

适合：

```text
数据量大
写入压力大
单机内存或 CPU 扛不住
需要水平扩展
```

限制：

```text
跨 slot 的多 key 操作受限
事务和 Lua 跨 slot 受限
客户端必须支持 Cluster
运维复杂度更高
```

让多个 key 落同一个 slot：

```text
user:{1001}:profile
user:{1001}:orders
```

Cluster 只计算 `{1001}` 的 hash。

### 5.5 脑裂和数据丢失【必须掌握】

这是高级 Java 开发必须补的点。

Redis 主从复制通常是异步的，所以可能出现：

```text
客户端写 master 成功
master 还没同步给 replica
master 宕机
replica 被提升为新 master
刚才那部分写入丢失
```

脑裂场景：

```text
旧 master 因网络隔离没有真正宕机
Sentinel 在另一侧选出新 master
一段时间内系统出现两个 master
部分客户端继续写旧 master
网络恢复后旧 master 降级为 replica，它上面的新写入可能被覆盖
```

降低风险：

```conf
min-replicas-to-write 1
min-replicas-max-lag 10
```

含义：至少有 1 个 replica 延迟不超过 10 秒，master 才继续接受写入。

代价：

```text
降低数据丢失风险
但在网络抖动或 replica 异常时，master 可能拒绝写入，可用性下降
```

面试结论：Redis 高可用不是强一致方案，重要数据仍要以数据库为准，业务要做幂等、补偿和校验。

---

## 6. 缓存设计常见难点【必须掌握】

### 6.1 缓存穿透【必须掌握】

现象：

```text
查询不存在的数据，每次缓存 miss，每次都打数据库
```

解决：

```text
参数校验
缓存空值，设置短 TTL
布隆过滤器
```

布隆过滤器补充：

```text
布隆过滤器判断“不存在”一定不存在
判断“存在”只是可能存在
它会误判，但不会漏判
删除困难，数据变化大时要考虑重建
```

### 6.2 缓存击穿【必须掌握】

现象：

```text
热点 key 过期，大量请求同时打到数据库
```

解决：

```text
互斥锁，只让一个请求回源
逻辑过期，异步刷新
热点 key 不设置过短 TTL
提前刷新
```

逻辑过期的思路：

```text
缓存 value 里额外放一个逻辑过期时间
请求发现逻辑过期后，先返回旧值
后台异步刷新缓存
```

优点是热点请求不直接打爆数据库，缺点是短时间内可能返回旧数据。

### 6.3 缓存雪崩【必须掌握】

现象：

```text
大量 key 同时过期，或 Redis 整体不可用，数据库瞬间被打爆
```

解决：

```text
TTL 加随机值
热点数据分批过期
多级缓存
限流、降级、熔断
Redis 高可用
核心数据预热
```

### 6.4 缓存和数据库一致性【必须掌握】

常用策略：

```text
先更新数据库，再删除缓存
```

不推荐直接“更新数据库后更新缓存”，因为缓存值可能由多张表或复杂逻辑组成，更新缓存容易出错。

增强方案：

```text
延迟双删
消息队列异步删除
订阅 binlog 删除缓存
缓存 TTL 兜底
业务版本号防旧数据覆盖
```

这部分原文偏薄，高级 Java 必须补完整。

推荐基础策略：

```text
先更新数据库，再删除缓存
```

为什么不是先删缓存再更新数据库：

```text
线程 A 先删缓存
线程 B 读缓存 miss，读到旧 DB，写旧值到缓存
线程 A 再更新 DB
结果缓存里还是旧值
```

为什么不是更新 DB 后直接更新缓存：

```text
缓存值可能由多张表、多段逻辑计算得来
并发更新时可能发生旧请求覆盖新缓存
删除缓存更简单，后续读请求重新加载即可
```

删除缓存失败怎么办：

```text
重试删除
投递 MQ 异步删除
订阅 binlog 删除缓存
设置 TTL 作为最终兜底
```

一致性要求很高时：

```text
不要依赖缓存保证强一致
读数据库
加业务版本号
关键链路加锁或串行化
用事务消息/binlog 做补偿
```

### 6.5 热 key【必须掌握】

现象：

```text
某个 key 被极高频访问，单节点压力很大
```

解决：

```text
本地缓存
读副本分担读
热点 key 拆成多个副本 key
提前识别和监控热点
限流降级
```

进一步补充：

```text
本地缓存适合读多写少的热点 key
副本 key 可以把 product:1 拆成 product:1:copy:1/2/3
热点发现依赖监控、代理层统计或 Redis hotkeys 能力
```

### 6.6 大 key【必须掌握】

大 key 例子：

```text
超大 String
几十万个 field 的 Hash
百万元素 Set/ZSet/List
```

危害：

```text
阻塞主线程
网络传输慢
删除慢
复制和迁移压力大
Cluster slot 倾斜
```

解决：

```text
拆分 key
分页读取，避免 HGETALL/SMEMBERS 大范围命令
UNLINK 替代 DEL
控制集合大小
定期扫描 big key
```

补充判断标准：大 key 没有绝对阈值，通常看 value 大小、集合元素数量、网络传输耗时和命令执行耗时。高级回答不要只说“超过多少 MB”，要强调它对延迟、复制、迁移和删除的影响。

### 6.7 Redis 阻塞【必须掌握】

常见原因：

```text
慢命令
大 key 删除
持久化 fork
AOF fsync 卡顿
CPU 打满
网络问题
Lua 脚本执行过久
```

排查：

```text
SLOWLOG
LATENCY DOCTOR
INFO
MONITOR 慎用
bigkeys 扫描
查看客户端连接和网络
```

危险命令：

```redis
KEYS *
HGETALL big_hash
SMEMBERS big_set
LRANGE big_list 0 -1
DEL big_key
```

替代：

```redis
SCAN
HSCAN
SSCAN
ZSCAN
UNLINK
```

### 6.8 内存淘汰策略【必须掌握】

当 Redis 达到 `maxmemory` 后，根据策略淘汰数据。

常见策略：

| 策略 | 说明 |
|---|---|
| noeviction | 不淘汰，写入报错 |
| allkeys-lru | 所有 key 中淘汰最近最少使用 |
| volatile-lru | 只在设置过期时间的 key 中淘汰 LRU |
| allkeys-random | 所有 key 随机淘汰 |
| volatile-random | 设置过期时间的 key 随机淘汰 |
| volatile-ttl | 优先淘汰 TTL 更短的 key |
| allkeys-lfu | 所有 key 中淘汰低频访问 |
| volatile-lfu | 设置过期时间的 key 中淘汰低频访问 |

缓存场景常用：

```text
allkeys-lru
allkeys-lfu
```

### 6.9 过期删除策略【必须掌握】

这部分原文容易和内存淘汰混在一起，需要单独记。

Redis 过期 key 的删除主要有两种：

```text
惰性删除：访问 key 时发现过期，再删除
定期删除：后台周期性抽样检查并删除过期 key
```

内存淘汰是另一件事：

```text
过期删除：key 到期后怎么清理
内存淘汰：内存达到 maxmemory 后，按策略淘汰哪些 key
```

面试里要把这两者分清。

### 6.10 缓存 Key 设计【必须掌握】

高级 Java 项目里，key 设计非常重要。

建议规范：

```text
业务名:模块名:数据类型:版本:{业务ID}
```

示例：

```text
mall:user:profile:v1:{1001}
mall:order:detail:v2:{20260831001}
mall:product:stock:v1:{skuId}
```

注意点：

```text
key 不要过长，浪费内存和网络
key 要可读，方便排查
不同业务加前缀，避免冲突
缓存结构升级时加版本号
Cluster 中需要多 key 操作时合理使用 hash tag
TTL 要按业务设计，不要所有 key 同一时间过期
```

### 6.11 多级缓存【必须掌握】

当 Redis 热 key 本身也扛不住时，常用：

```text
Caffeine 本地缓存 + Redis 分布式缓存 + DB
```

读取链路：

```text
先读本地缓存
本地 miss 再读 Redis
Redis miss 再读 DB
回填 Redis 和本地缓存
```

难点：

```text
多实例本地缓存不一致
更新后如何通知各节点失效
本地缓存容量和 TTL 控制
热点数据变更频繁时不适合长时间本地缓存
```

常见失效方案：

```text
MQ 广播删除本地缓存
Redis Pub/Sub 做轻量通知
短 TTL 兜底
配置中心或业务事件触发刷新
```

### 6.12 限流【必须掌握】

Redis 常见限流模型：

| 模型 | 说明 |
|---|---|
| 固定窗口 | 简单，容易出现窗口边界流量突刺 |
| 滑动窗口 | 更平滑，可用 ZSet 记录时间戳 |
| 令牌桶 | 允许一定突发流量 |
| 漏桶 | 平滑固定速率处理 |

简单固定窗口：

```text
INCR limit:user:1001
第一次 INCR 时设置 EXPIRE
超过阈值则拒绝
```

问题：`INCR` 和 `EXPIRE` 组合要注意原子性，生产中常用 Lua 封装。

---

## 7. Redis 应用专题【必须掌握】

### 7.1 分布式锁【必须掌握】

正确加锁：

```redis
SET lock:order:1 requestId NX EX 30
```

要求：

```text
NX：不存在才设置
EX：设置过期时间，避免死锁
value：必须是唯一标识，避免误删别人锁
```

释放锁要用 Lua 保证原子性：

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
  return redis.call("del", KEYS[1])
else
  return 0
end
```

常见坑：

```text
setnx 后再 expire 不是原子操作
业务执行超过锁过期时间
释放锁时误删别人锁
Redis 主从切换导致锁状态丢失
```

严格一致性场景要谨慎使用 Redis 锁，可以考虑数据库锁、ZooKeeper、etcd。

### 7.2 Redisson 看门狗【必须掌握】

看门狗解决的问题：

```text
线程拿到锁后，业务时间超过锁过期时间，锁提前释放
```

Redisson 默认：

```text
锁过期时间：30 秒
续期频率：约 10 秒，通常是过期时间的 1/3
```

逻辑：

```text
线程持有锁且业务未完成 -> 定时续期
业务执行完成 -> 主动释放锁 -> 停止续期
线程宕机 -> 无法续期 -> 锁最终过期释放
```

注意：

```text
看门狗是 Redisson 客户端机制，不是 Redis 服务端能力
```

注意：如果调用 `lock.lock(10, TimeUnit.SECONDS)` 显式指定 leaseTime，Redisson 通常不会启用自动续期。只有未指定固定 leaseTime 的加锁方式，才更符合看门狗自动续期场景。

### 7.3 延时队列【了解即可】

常见做法：ZSet。

```text
score = 任务执行时间戳
value = 任务内容
```

消费者定时扫描：

```redis
ZRANGEBYSCORE delay_queue 0 now
```

拿到到期任务后处理并删除。

注意：

```text
并发消费者要避免重复消费
可以用 Lua 保证取出 + 删除原子性
高可靠延时任务建议用专业消息队列
```

### 7.4 Lua 脚本【必须掌握】

Lua 的作用：

```text
多条 Redis 命令原子执行
减少网络 RTT
封装复杂逻辑
```

典型场景：

```text
释放分布式锁
库存扣减
限流
延时队列抢任务
```

注意：

```text
Lua 脚本执行期间会阻塞 Redis
脚本不能写太重
避免长循环和大 key 操作
```

### 7.5 Pipeline【了解即可】

Pipeline 是客户端把多条命令一次性发给 Redis，减少网络往返。

它解决的是：

```text
RTT 开销
```

不保证：

```text
事务
原子性
失败自动回滚
```

对比：

| 能力 | Pipeline | Transaction | Lua |
|---|---|---|---|
| 减少 RTT | 是 | 一定程度 | 是 |
| 原子性 | 否 | 命令顺序执行但不回滚 | 是 |
| 适合复杂逻辑 | 否 | 一般 | 是 |

### 7.6 Pub/Sub【降权】

Pub/Sub 是发布订阅模型，适合轻量通知，不适合可靠消息。

降权原因：

```text
消息不持久化
消费者断开会丢消息
没有 ACK
没有重试
没有消费进度管理
```

高级 Java 回答时可以说：可靠消息优先 Stream 或专业 MQ，Pub/Sub 更适合缓存失效通知、配置变更通知这类可容忍丢失或有兜底机制的场景。

### 7.7 GEO【降权】

GEO 适合：

```text
附近的人
附近门店
距离计算
```

如果简历和项目没有 LBS、地图、门店距离相关业务，知道用途即可，不需要重点背命令。

### 7.8 Java 客户端实践【必须掌握】

这部分原文缺失，但高级 Java 开发很容易被问。

常见客户端：

| 客户端 | 特点 |
|---|---|
| Jedis | 老牌客户端，API 直观，早期常见，连接通常非线程安全，需要连接池 |
| Lettuce | 基于 Netty，线程安全，支持同步/异步/响应式，Spring Boot 默认常用 |
| Redisson | 提供分布式锁、集合、延时队列等高级封装 |

必须注意：

```text
连接池大小不是越大越好，要结合 Redis QPS、命令耗时、业务线程数评估
必须设置连接超时、读写超时
重试要谨慎，避免 Redis 抖动时把流量放大
序列化格式要稳定、可读、可演进
Cluster/Sentinel 模式下客户端要支持拓扑刷新和主从切换
```

常见线上坑：

```text
连接池耗尽导致业务线程阻塞
超时时间过长导致故障扩散
重试风暴打爆 Redis
序列化类变更导致老缓存无法反序列化
key 无规范导致排查困难
```

---

## 8. 底层结构【了解即可】

### 8.1 Redis Object【了解即可】

Redis 内部不是直接用数据结构暴露给用户，而是通过 redisObject 统一表示对象。

一个对象大致包含：

```text
type：对象类型，如 string/list/hash/set/zset
encoding：底层编码
ptr：指向具体底层结构
```

### 8.2 常见底层结构【了解即可】

| 底层结构 | 用途 |
|---|---|
| SDS | Redis 字符串 |
| dict | 字典、Hash、Set、大量 key 空间 |
| linkedlist / quicklist | List 相关结构 |
| skiplist | ZSet 排序和范围查询 |
| intset | 小整数 Set |
| ziplist/listpack | 小对象压缩存储 |

### 8.3 SDS【了解即可】

Redis 没有直接使用 C 字符串，而是 SDS。

优点：

```text
O(1) 获取长度
避免缓冲区溢出
减少内存重分配
二进制安全
```

### 8.4 dict【了解即可】

Redis 字典使用哈希表，冲突通常用链地址法。

重点：

```text
Redis 有渐进式 rehash
扩容不是一次性完成，而是分批迁移
避免长时间阻塞
```

### 8.5 skiplist【了解即可】

跳表是有序数据结构，Redis 用它支持 ZSet 的范围查询和排名。

为什么不用红黑树：

```text
跳表实现更简单
范围查询方便
插入删除只影响局部
平均复杂度 O(logN)
```

跳表节点包含：

```text
score
member
多层 forward 指针
span，用于计算排名
backward 指针
```

高级 Java 面试掌握到这里即可，不需要背 C 源码结构体字段。除非简历写了“深入 Redis 源码”，否则底层结构不是最高优先级。

---

## 9. 高频面试回答模板【必须掌握】

### 9.1 Redis 为什么快

回答：

```text
Redis 快主要因为数据在内存中，命令执行采用单线程模型，避免锁竞争和线程切换；网络层使用 IO 多路复用处理大量连接；同时 Redis 针对不同数据类型设计了高效的底层结构，比如 SDS、dict、skiplist、quicklist。Redis 6 以后虽然引入多线程，但主要用于网络 IO，命令执行主流程仍然是单线程。
```

### 9.2 RDB 和 AOF 区别

回答：

```text
RDB 是快照，保存某一时刻的完整数据，文件小、恢复快，但可能丢失两次快照之间的数据。AOF 是追加写命令日志，数据安全性更好，常用 everysec 策略，通常最多丢 1 秒数据，但文件更大、恢复可能更慢，需要 rewrite。生产中常见组合是 RDB + AOF everysec。
```

### 9.3 Sentinel 和 Cluster 区别

回答：

```text
Sentinel 解决的是主从架构下的自动故障转移，适合单主多从但不需要分片的场景。Cluster 解决的是水平扩展，它把 key 分到 16384 个 hash slot 上，由多个 master 分片负责，每个 master 可以挂 replica。Cluster 既能扩容量也能做故障转移，但对跨 slot 多 key 操作有限制。
```

### 9.4 缓存穿透、击穿、雪崩

回答：

```text
穿透是查不存在的数据，缓存和数据库都没有，可以用参数校验、缓存空值、布隆过滤器。击穿是热点 key 过期后大量请求同时回源，可以用互斥锁、逻辑过期、提前刷新。雪崩是大量 key 同时过期或 Redis 整体不可用，可以给 TTL 加随机值、做多级缓存、限流降级、Redis 高可用。
```

### 9.5 Redis 分布式锁怎么实现

回答：

```text
使用 SET key value NX EX seconds 原子加锁，value 用唯一 requestId 标识持有者，释放时用 Lua 判断 value 是自己的再 DEL，避免误删别人锁。需要注意锁过期时间、业务超时、续期、主从切换导致锁丢失等问题。Java 中通常使用 Redisson，它有看门狗机制自动续期。
```

### 9.6 大 key 和热 key 怎么处理

回答：

```text
大 key 会导致网络传输慢、删除阻塞、迁移复制压力大，可以拆分 key、分页读取、用 UNLINK 删除、限制集合大小。热 key 会让单个节点压力过大，可以用本地缓存、读副本、热点 key 副本、限流降级和热点监控处理。
```

### 9.7 缓存和数据库一致性怎么保证

回答：

```text
缓存和数据库很难做到绝对强一致，常用策略是先更新数据库，再删除缓存。删除失败可以通过重试、MQ、binlog 订阅做补偿，同时设置 TTL 兜底。并发场景下要防止旧值回写缓存，可以用延迟双删、版本号或关键链路加锁。强一致业务不能依赖缓存，要以数据库为准。
```

### 9.8 Redis 脑裂怎么处理

回答：

```text
脑裂通常来自网络分区，旧 master 没死但和 Sentinel/replica 失联，Sentinel 选出新 master 后短时间出现两个 master。旧 master 继续接收写入，网络恢复后它会被降级为 replica，这部分写入可能丢失。可以通过合理部署 Sentinel、设置 quorum、配置 min-replicas-to-write 和 min-replicas-max-lag 降低风险，但会牺牲一定可用性。
```

### 9.9 Redis 限流怎么做

回答：

```text
简单限流可以用 INCR + EXPIRE 做固定窗口，但要注意原子性，生产中通常用 Lua 封装。更平滑的限流可以用 ZSet 做滑动窗口，记录请求时间戳并清理窗口外数据。高并发场景也可以用令牌桶或漏桶思想，具体选择取决于是否允许突发流量。
```

---

## 10. 复习路线

### 第一轮：建立全局框架

重点看【必须掌握】：

```text
Redis 是什么
Redis 为什么快
五大基本数据结构
RDB/AOF
主从/Sentinel/Cluster
```

目标：能画出 Redis 整体知识图。

### 第二轮：背高频问题

重点看【必须掌握】：

```text
缓存穿透/击穿/雪崩
缓存一致性
分布式锁
大 key/热 key
内存淘汰
阻塞排查
缓存 Key 设计
Java 客户端实践
限流
```

目标：每个问题能按“现象 -> 原因 -> 方案 -> 注意点”回答。

### 第三轮：补底层

重点看【了解即可】：

```text
SDS
dict
渐进式 rehash
skiplist
对象编码
IO 多路复用
```

目标：能解释 Redis 为什么这样设计。

### 第四轮：低频内容

这些内容【降权】：

```text
GEO
Pub/Sub
List 做可靠消息队列
全量 Redis 命令背诵
底层编码阈值死记硬背
```

目标：知道用途和局限即可，项目没用到不用优先深入。

---

## 11. 易混点速记

| 问题 | 正确理解 |
|---|---|
| Redis 单线程为什么快 | 单线程指命令执行，网络和后台任务不一定单线程 |
| RDB 会不会丢数据 | 会，可能丢两次快照之间的数据 |
| AOF everysec 会不会丢数据 | 宕机通常最多丢约 1 秒 |
| Pipeline 是不是事务 | 不是，只是减少网络 RTT |
| Lua 是不是不会阻塞 | 会阻塞，所以脚本要短 |
| 删除缓存还是更新缓存 | 常见做法是先更新 DB，再删除缓存 |
| Redis 锁是不是绝对安全 | 不是，强一致场景要谨慎 |
| Cluster 能不能随便多 key 操作 | 不能，跨 slot 受限 |
| List 能不能做消息队列 | 能做简单队列，可靠队列优先 Stream/MQ |
| HyperLogLog 能不能取用户列表 | 不能，只能估算基数 |
| Pub/Sub 能不能做可靠消息 | 不能，断线会丢消息，没有 ACK |
| 过期删除和内存淘汰是不是一回事 | 不是，过期删除处理到期 key，内存淘汰处理 maxmemory |
| Redisson 看门狗是不是 Redis 功能 | 不是，是 Redisson 客户端续期机制 |
| 布隆过滤器判断存在是否一定存在 | 不一定，可能误判 |
| Cluster hash tag 能不能乱用 | 不能，会造成 slot 倾斜 |

---

## 12. 最小背诵版

Redis 复习可以浓缩成下面几句话：

```text
Redis 是内存型 key-value 数据库，支持多种数据结构。
它快是因为内存、单线程命令执行、IO 多路复用和高效数据结构。
String 做缓存计数，Hash 存对象，List 做简单队列，Set 做去重关系，ZSet 做排行榜和延时队列。
持久化有 RDB 快照和 AOF 日志，生产常用 AOF everysec + RDB。
高可用靠主从复制、Sentinel 自动故障转移，水平扩展靠 Cluster 的 16384 个 hash slot，但异步复制和脑裂都可能导致数据丢失。
缓存常见问题是穿透、击穿、雪崩、一致性、大 key、热 key、阻塞、过期删除和内存淘汰。
分布式锁用 SET NX EX + 唯一值 + Lua 释放，Redisson 看门狗可以自动续期。
高级 Java 还要掌握 key 设计、Java 客户端配置、多级缓存、限流和线上排查。
底层了解 SDS、dict、skiplist、渐进式 rehash 和对象编码即可。
```
