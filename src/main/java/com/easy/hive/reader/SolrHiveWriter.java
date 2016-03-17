package com.easy.hive.reader;

import com.easy.hive.conf.Conf;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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
    SystemDefaultHttpClient httpClient = new SystemDefaultHttpClient();
    HttpSolrClient sc =null;

    int batchSize;

    //批量插入
    List<SolrInputDocument> datas=new ArrayList<SolrInputDocument>();

    public SolrHiveWriter(JobConf conf){
        sc=new HttpSolrClient(conf.get(Conf.SOLR_URL),httpClient);
        this.batchSize=Integer.parseInt(conf.get(Conf.SOLR_CURSOR_BATCH_SIZE));
    }

    final static Logger log= LoggerFactory.getLogger(SolrHiveWriter.class);

    @Override
    public void write(Writable w) throws IOException {
        MapWritable map = (MapWritable) w;
        SolrInputDocument doc = new SolrInputDocument();
        for (final Map.Entry<Writable, Writable> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            doc.setField(key, entry.getValue().toString());
        }
        datas.add(doc);
        //批量处理，大于等于一定量提交
        if(datas.size()==batchSize){
            try {
                sc.add(datas);
                sc.commit();
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
            sc.add(datas);
            sc.commit();
            sc.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
