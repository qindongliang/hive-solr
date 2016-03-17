# hive-solr
使用Hive读写solr


###（一）Hive+Solr简介
Hive作为Hadoop生态系统里面离线的数据仓库，可以非常方便的使用SQL的方式来离线分析海量的历史数据，并根据分析的结果，来干一些其他的事情，如报表统计查询等。
<br/>Solr作为高性能的搜索服务器，能够提供快速，强大的全文检索功能。

###（二）为什么需要hive集成solr？
 有时候，我们需要将hive的分析完的结果，存储到solr里面进行全文检索服务，比如以前我们有个业务，对我们电商网站的搜索日志使用hive分析完后
存储到solr里面做报表查询，因为里面涉及到搜索关键词，这个字段是需要能分词查询和不分词查询的，通过分词查询可以查看改词的相关的产品在某一段时间内的一个走势图。
有时候，我们又需要将solr里面的数据加载到hive里面，使用sql完成一些join分析功能， 两者之间优劣互补，以更好的适应我们的业务需求。当然网上已经有一些hive集成solr的开源项目，但由于
版本比较旧，所以无法在新的版本里面运行，经过散仙改造修补后的可以运行在最新的版本。

###（三）如何才能使hive集成solr？
所谓的集成，其实就是重写hadoop的MR编程接口的一些组件而已。我们都知道MR的编程接口非常灵活，而且高度抽象，MR不仅仅可以从HDFS上加载
数据源，也可以从任何非HDFS的系统中加载数据，当然前提是我们需要自定义：
<br/>InputFormat<br/>
OutputFormat<br/>
RecordReader<br/>
RecordWriter<br/>
InputSplit<br/>
组件，虽然稍微麻烦了点，但从任何地方加载数据这件事确实可以做到，包括mysql，sqlserver，oracle，mongodb，
solr，es，redis等等。<br/>

上面说的是定制Hadoop的MR编程接口，在Hive里面除了上面的一些组件外，还需要额外定义SerDe组件和组装StorageHandler，在hive里面
SerDe指的是 Serializer and Deserializer，也就是我们所说的序列化和反序列化，hive需要使用serde和fileinput来读写hive 表里面的一行行数据。<br/>
读的流程：<br/>
HDFS files / every source ->  InputFileFormat --> <key, value> --> Deserializer --> Row object<br/>
写的流程：<br/>
Row object --> Serializer --> <key, value> --> OutputFileFormat --> HDFS files / every source<br/>

###（四）hive集成solr后能干什么？
（1）读取solr数据，以hive的支持的SQL语法，能进行各种聚合，统计，分析，join等<br/>
（2）生成solr索引，一句SQL，就能通过MR的方式给大规模数据构建索引<br/>

###（五）如何安装部署以及使用？
非常简单，使用git clone [hive-solr这个项目](https://github.com/qindongliang/hive-solr)后，修改少许pom文件后，执行<br/>
mvn clean package <br/>
命令构建生成jar包，并将此jar包拷贝至hive的lib目录即可<br/>

例子如下：<br/>
（1）hive读取solr数据<br/>

建表：
````sql
--存在表就删除
drop table  if exists solr;

--创建一个外部表
create external table solr (
  --定义字段，这里面的字段需要与solr的字段一致
  rowkey string,
  sname string

)
--定义存储的storehandler
stored by "com.easy.hive.store.SolrStorageHandler"
--配置solr属性
tblproperties('solr.url' = 'http://192.168.1.28:8983/solr/a',
                    'solr.query' = '*:*',
                    'solr.cursor.batch.size'='10000',
                    'solr.primary_key'='rowkey'
                       );

````
执行bin/hive 命令，进行hive的命令行终端：<br/>

````sql
--查询所有数据
select * from solr limit 5;
--查询指定字段
select rowkey from solr;
--以mr的方式聚合统计solr数据
select sname ,count(*) as c from solr group by sname  order by c desc

````

（2）使用hive给solr构建索引的例子

首先构建数据源表:
````sql
--如果存在就删除
drop table if exists index_source;

--构建一个数据表
CREATE TABLE index_source(id string, yname string,sname string) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE;

--向数据源里面导入本地数据
load  data local inpath '/ROOT/server/hive/test_solr' into table index_source;
````

其次，构建solr的关联表：<br/>
````sql
--删除已经存在的表
drop table  if exists index_solr;

--创建关联solr表
create external table index_solr (
  id string,
  yname string,
  sname string
) 
--定义存储引擎
 stored by "com.easy.hive.store.SolrStorageHandler"
--设置solr服务属性
tblproperties('solr.url' = 'http://192.168.1.28:8983/solr/b',
                    'solr.query' = '*:*',
                    'solr.cursor.batch.size'='10000',
                    'solr.primary_key'='id'
                       );
````
最后，执行下面的sql命令，即可给数据源中的数据，构建solr索引：<br/>
````sql
--注册hive-solr的jar包，否则MR方式运行的时候，将不能正常启动
add jar /ROOT/server/hive/lib/hive-solr.jar;
--执行插入命令
INSERT OVERWRITE TABLE index_solr SELECT * FROM  index_source ; 
--执行成功之后，即可在solr的终端界面查看，也可以再hive里面执行下面的solr查询
select * from index_solr limit 10 ;
````
###（六）他们还能其他的框架集成么？
当然，作为开源独立的框架，我们可以进行各种组合， hive也可以和elasticsearch进行集成，也可以跟mongodb集成，
solr也可以跟spark集成，也可以跟pig集成,但都需要我们自定义相关的组件才行,思路大致与这个项目的思路一致。


###参考资料：




