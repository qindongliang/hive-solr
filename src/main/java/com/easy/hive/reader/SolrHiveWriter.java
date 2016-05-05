package com.easy.hive.reader;

import com.easy.hive.conf.Conf;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * hive写数据到指定的solr中
 * Created by qindongliang on 2016/3/15.
 */
public class SolrHiveWriter implements FileSinkOperator.RecordWriter {
    final static Logger log= LoggerFactory.getLogger(SolrHiveWriter.class);
    SolrClient sc =null;
    int batchSize;
    SystemDefaultHttpClient httpClient = new SystemDefaultHttpClient();
    //批量插入
    List<SolrInputDocument> datas=new ArrayList<SolrInputDocument>();

//    AtomicInteger count=new AtomicInteger();
    LBHttpSolrClient lbHttpSolrClient=new LBHttpSolrClient(httpClient);

    public SolrHiveWriter(JobConf conf){

        //初始化solrclient// 必须在这里初始化，否则，提取到一个公用类里面初始化会报错，因为MR的进程JVM和客户端的类是独立的
        if(conf.get(Conf.IS_SOLRCLOUD).equals("1")) {
            //solrcloud模式
            sc=new CloudSolrClient(conf.get(Conf.SOLR_URL).trim(),lbHttpSolrClient);//设置Cloud的client
            ((CloudSolrClient)sc).setDefaultCollection(conf.get(Conf.COLLECTION_NAME));//设置集合名
//            ((CloudSolrClient)sc).setParallelUpdates(false);//取消并行更新
        }else{
            //普通模式
            sc = new HttpSolrClient(conf.get(Conf.SOLR_URL),httpClient);
        }
        this.batchSize=Integer.parseInt(conf.get(Conf.SOLR_CURSOR_BATCH_SIZE));
        log.info("批处理提交数量：{}",batchSize);
    }

    @Override
    public void write(Writable w) throws IOException {
        MapWritable map = (MapWritable) w;
        SolrInputDocument doc = new SolrInputDocument();
        for (final Map.Entry<Writable, Writable> entry : map.entrySet()) {
            String key = entry.getKey().toString();//得到key
            String value=entry.getValue().toString().trim();// null值会转成空字符串 得到value
            //只有value有值的数据，我们才推送到solr里面，无值数据，不再发送到solr里面
            if(value.length()>0){
            doc.setField(key,value);
            }
        }

//        count.incrementAndGet();


        datas.add(doc);
        //批量处理，大于等于一定量提交
        if(datas.size()==batchSize){
            try {
                sc.add(datas);
//                sc.commit();//不提交，等待flush
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                //清空集合数据
                datas.clear();
            }
        }

    }

    @Override
    public void close(boolean abort) throws IOException {
        try {
            //关闭资源再次提交索引
//            if(sc==null||datas==null){
//                log.info("sc 在close 是null");
//                System.out.println("sc 在close 是null");
//            }
            //关闭前，再次追加索引
            sc.add(datas);
//            sc.commit();

//            log.info("Map结束，提交完毕，总共计数：{}",count.get());
//            sc.commit();
            sc.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
