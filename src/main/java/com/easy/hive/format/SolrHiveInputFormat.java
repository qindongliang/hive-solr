package com.easy.hive.format;

import com.easy.hive.conf.Conf;
import com.easy.hive.inputsplit.SolrHiveInputSplit;
import com.easy.hive.inputsplit.SolrInputSplit;
import com.easy.hive.reader.SolrReader;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.HiveInputFormat;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapred.*;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 定义solr的inputformat
 * Created by qindongliang on 2016/3/14.
 */
public class SolrHiveInputFormat extends HiveInputFormat<LongWritable,MapWritable> {
    final static Logger log= LoggerFactory.getLogger(SolrHiveInputFormat.class);
    //使用下面这个默认的httpclient，避免与hive或者hadoop的加载的httpclient包版本不一致冲突
    SystemDefaultHttpClient httpClient = new SystemDefaultHttpClient();
    SolrClient sc =null;
    LBHttpSolrClient lbHttpSolrClient=new LBHttpSolrClient(httpClient);
    @Override
    public InputSplit[] getSplits(JobConf conf, int numSplits) throws IOException {
        FileSplit[] wrappers=null;
        //初始化solrclient// 必须在这里初始化，否则，提取到一个公用类里面初始化会报错，因为MR的进程JVM和客户端的类是独立的
        if(conf.get(Conf.IS_SOLRCLOUD).equals("1")) {
            //solrcloud模式
            sc=new CloudSolrClient(conf.get(Conf.SOLR_URL).trim(),lbHttpSolrClient);//设置Cloud的client
            ((CloudSolrClient)sc).setDefaultCollection(conf.get(Conf.COLLECTION_NAME));//设置集合名
//            ((CloudSolrClient)sc).setParallelUpdates(false);
        }else{
            //普通模式
            sc = new HttpSolrClient(conf.get(Conf.SOLR_URL),httpClient);
        }
        try {
            SolrQuery sq = new SolrQuery();
            sq.set("q", conf.get(Conf.SOLR_QUERY));
            long count=0;
            count=sc.query(sq).getResults().getNumFound();
            //目前定义一个inputsplit，全量读取使用游标
            InputSplit[] splitIns =new InputSplit[1];
            SolrInputSplit solrInputSplit=new SolrInputSplit(0,count);
            splitIns[0]=solrInputSplit;
            // wrap InputSplits in FileSplits so that 'getPath'
            // doesn't produce an error (Hive bug)
            wrappers = new FileSplit[splitIns.length];
            Path path = new Path(conf.get(Conf.LOCATION_TABLE));
            for (int i = 0; i < wrappers.length; i++) {
                wrappers[i] = new SolrHiveInputSplit(splitIns[i], path);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return wrappers;
    }


    @Override
    public RecordReader getRecordReader(InputSplit split, JobConf conf, Reporter reporter) throws IOException {
        //初始化solrclient// 必须在这里初始化，否则，提取到一个公用类里面初始化会报错，因为MR的进程JVM和客户端的类是独立的
        if(conf.get(Conf.IS_SOLRCLOUD).equals("1")) {
            //solrcloud模式
            sc=new CloudSolrClient(conf.get(Conf.SOLR_URL).trim(),httpClient);//设置Cloud的client
            ((CloudSolrClient)sc).setDefaultCollection(conf.get(Conf.COLLECTION_NAME));//设置集合名
//            ((CloudSolrClient)sc).setParallelUpdates(false);
        }else{
            //普通模式
            sc = new HttpSolrClient(conf.get(Conf.SOLR_URL),httpClient);
        }
        //第三个参数是游标的初始化查询参数
        return new SolrReader(sc,new SolrQuery(),"*",conf);
    }
}
