Qiniu Streaming Design v2
==============

## 数据结构

### repo表

```scala
name:			string, unique	// repo表名，全局唯一
schema:			object			// repo数据点的schema
```

### transform表

```scala
repo:	string
name:	string, unique(repo, name)
to:		string
type:	string
code:	string
```

### export表

```scala
repo:	string
name:	string, unique(repo, name)
to:		string
type:	string
```

## 架构图
![architecture](https://github.com/AndyRao/SprayESSearch/blob/master/image/pipeline_architecture.jpeg)


## 模块设计

### ApiServer

职责：

* 接收客户打点请求，将打点数据转发给Kafka rest proxy进行存储
* 接收客户对数据进行计算请求，将计算代码、sql、dataframe填入事先准备好的spark streaming计算模版中，打包上传到spark集群运行 
* 接收客户的数据导出请求，与export(export的设计方案待定)进行交互

### Kafka
职责：

* 存储用户的打点数据，以及存储对打点数据进行各种transform之后的中间数据，api中指定的每一个repo对应Kafka里面的一个topic




### Kafka Confluent Platform(包括图中蓝色部分的三个组件)
#### Kafka rest proxy

职责：

* 接收api server发送过来的注册数据schema的请求，将schema注册到Kafka schema registry服务中
* 接收api server发送过来的http打点请求，将数据以json格式发送到kafka中，发送的数据格式要通过schema的校验检查


规格：

```
TODO
```

#### Kafka schema registry

职责：

* 接收avro schema的注册请求
* 对注册的avro schema进行缓存
* 接收avro schema的查询请求


规格：

```
TODO
```

#### Kafka-connect-hdfs

职责：

* 负责将Kafka中的数据写入到hdfs中，支持avro和parquet等格式的存储


规格：

```
TODO
```

### Spark cluster(on yarn or standalone)
职责：

* 运行api server提交的spark streaming application，对数据进行transform操作


规格：

```
TODO
```

