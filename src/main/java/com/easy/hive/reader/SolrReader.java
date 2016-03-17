package com.easy.hive.reader;

import com.easy.hive.conf.Conf;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by qindongliang on 2016/3/14.
 */
public class SolrReader implements RecordReader<LongWritable,MapWritable> {


    //SolrClient
    protected SolrClient sc;
    //查询对象
    protected SolrQuery sq;
    protected String cursorMark = null;//游标
    protected QueryResponse response;//结果集请求
    protected SolrDocumentList docs;//doc结果集

    private int hits;//命中数量

    int pos;    //记录当前的处理的doc数量

    private String[] cols;//读取hive元数据所有的列

    private JobConf conf; //MR任务封装的JobConf对象

    private int solrBatchSize; // solr游标批量读取的大小

    private String solr_pk; //solr的主键

    private String solr_query; //solr的查询字符串

    int current; //循环处理数据时的增量标记



    public SolrReader(SolrClient sc, SolrQuery sq, String cursorMark, JobConf jobConf) {
        this.conf=jobConf;
        this.sc = sc;
        this.sq = sq;
        this.cursorMark = cursorMark;
        cols=Conf.getAllClos(jobConf.get(Conf.COLUMNS));
        solrBatchSize=Integer.parseInt(conf.get(Conf.SOLR_CURSOR_BATCH_SIZE));
        solr_pk=conf.get(Conf.SOLR_PK);
        solr_query=conf.get(Conf.SOLR_QUERY);
        //初始化SolrQuery , 在构造函数执行时，先执行一次查询
        init(sq,cursorMark);
        try {
            response = sc.query(sq);
            docs = response.getResults();
            hits = (int) docs.getNumFound();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /***
     * 初始化SolrQuery
     * @param sq  SolrQuery
     * @param cursorMark 游标
     */
    public void init(SolrQuery sq,String cursorMark){
        sq.setSort(solr_pk, SolrQuery.ORDER.asc);
        sq.set("cursorMark", cursorMark);
        sq.set("q",solr_query);
        sq.setRows(solrBatchSize);
    }

    /**
     * 将solr的document封装进MapWritable里面
     * @param doc 转换的solr document
     * @param value MapWritable
     */
    public void collect(SolrDocument doc,MapWritable value){
        for (String col : cols) {
            Object vObj = doc.get(col);
            Writable v = (vObj == null) ? NullWritable.get() : new Text(vObj.toString());
            value.put(new Text(col), v);
        }
    }


    final static Logger log= LoggerFactory.getLogger(SolrReader.class);
    @Override
    public boolean next(LongWritable key, MapWritable value) throws IOException {

        if (cursorMark != null) {
            //由于在构造函数执行时，先查询了一次请求，所以下面的if会进去，然后处理完这一批数据再进else逻辑
            //查询一次后会缓存doclist，等待遍历完
            if (current < docs.size()) {
                key.set(pos++);
                SolrDocument doc = docs.get(current);
                //转换doc到value
                collect(doc,value);
                current++;
                return true;
            } else {//如果遍历完后，进行下一批游标查询
                key.set(pos++);
                try {
                    //前一次查询的数据已经处理完，重新发起请求，得到数据
                    init(sq,response.getNextCursorMark());
                    response = sc.query(sq);
                    docs = response.getResults();
                    hits = (int) docs.getNumFound();
                    if(docs.size()==0){
                        return false;
                    }
                    //更新游标记录
                    if (cursorMark != null) {
                        String nextCursorMark = response.getNextCursorMark();
                        if (cursorMark.equals(nextCursorMark)) {//没有更多的记录
                            return false;
                        }
                        cursorMark = nextCursorMark;
                    }
                    /*这个地方，为什么一定要先处理一条呢，返回继续循环呢？
                     *因为每次next=ture时候，都会进行一次反序列化操作,并将数据append到hive的行里面
                     *如果你不get(0)，而直接设置current=0，那么将会出现数据重复，next=true，将会
                     *继续把上一次MapWritable的值，再次追加到hive的行里面，从而会最后一条重复的数据
                     *这个地方，也不能return false跳过，因为false意味着整个读取流程结束。
                     */
                    SolrDocument doc = docs.get(0);
                    collect(doc,value);
                    current = 1;
                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
        return false;
    }

    @Override
    public LongWritable createKey() {
        return new LongWritable();
    }

    @Override
    public MapWritable createValue() {
        return new MapWritable();
    }

    @Override
    public long getPos() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
        sc.close();
    }

    @Override
    public float getProgress() throws IOException {
        return 1.0F;
    }
}
