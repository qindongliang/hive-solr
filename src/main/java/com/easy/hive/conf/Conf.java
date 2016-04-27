package com.easy.hive.conf;

import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.mapred.JobConf;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.util.Map;
import java.util.Properties;

/**
 * 有关solr常量配置
 * Created by qindongliang on 2016/3/14.
 */
public  class Conf {

    //Solr的url地址
    public static final String SOLR_URL="solr.url";

    //是否为solrcloud模式，0为普通模式，1为solrcloud模式
    public static final String IS_SOLRCLOUD="is.solrcloud";

    //solrcloud模式下，设置集合名
    public static final String COLLECTION_NAME="collection.name";

    //表所在位置
    public static final String LOCATION_TABLE="location";

    //Solr查询条件
    public static final  String SOLR_QUERY="solr.query";

    //游标处理数量
    public static  final  String   SOLR_CURSOR_BATCH_SIZE="solr.cursor.batch.size";

    //列值映射  这个直接用Hive里面定义的常量字段
    public static final String  COLUMNS=serdeConstants.LIST_COLUMNS;

    //列类型  这个直接用Hive里面定义的常量字段
    public static  final String  COLUMNS_TYPE=serdeConstants.LIST_COLUMN_TYPES;

    //字段名分隔符
    private final  static String COLUMNS_DELIMITER=",";

    //字段类型分隔符
    private final static String COLUMNS_TYPE_DELIMITER=":";

    //主键
    public static final String SOLR_PK="solr.primary_key";

    //设置属性

    /***
     * 将hive的附件属性设置到JobConf对象里面
     *
     * @param from tbl属性
     * @param to   jobconf属性
     */
    public static void copyProperties(Properties from, Map<String, String> to) {
        for(Map.Entry<Object,Object> map:from.entrySet()){

            to.put(String.valueOf(map.getKey()),String.valueOf(map.getValue()));
        }
        //修复bug   Character reference "&#
        to.remove("columns.comments");

    }


    public static void copyProperties(Properties from, JobConf to) {
        for(Map.Entry<Object,Object> map:from.entrySet()){

            to.set(String.valueOf(map.getKey()),String.valueOf(map.getValue()));
        }
        //修复bug   Character reference "&#
        to.unset("columns.comments");

    }


    /***
     * 获取列的所有值
     * @param cols 列字符串
     * @return
     */
    public static   String[] getAllClos(String cols){
        return  cols.split(COLUMNS_DELIMITER);

    }


    }








