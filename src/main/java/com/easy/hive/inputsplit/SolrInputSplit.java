package com.easy.hive.inputsplit;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 如果想采用分页的方式读取solr
 * 可以定义多个map读取，不过需要
 * 较大的内存
 * Created by qindongliang on 2016/3/14.
 */
public class SolrInputSplit  extends InputSplit implements Writable, org.apache.hadoop.mapred.InputSplit {


    private long end=0;
    private long start=0;

    public SolrInputSplit() {
    }

    public SolrInputSplit(long end, long start) {
        this.end = end;
        this.start = start;
    }

    @Override
    public long getLength() throws IOException {
        return 1;
    }

    @Override
    public String[] getLocations() throws IOException {
        return new String[0];
    }

    @Override
    public void write(DataOutput out) throws IOException {

        out.writeLong(this.start);
        out.writeLong(this.end);


    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.start=in.readLong();
        this.end=in.readLong();
    }
}
