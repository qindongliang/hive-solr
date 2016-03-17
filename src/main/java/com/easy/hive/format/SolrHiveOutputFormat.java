package com.easy.hive.format;

import com.easy.hive.reader.SolrHiveWriter;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator;
import org.apache.hadoop.hive.ql.io.HiveOutputFormat;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.util.Progressable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by qindongliang on 2016/3/15.
 */
public class SolrHiveOutputFormat implements HiveOutputFormat<NullWritable,MapWritable> {


    final static Logger log= LoggerFactory.getLogger(SolrHiveOutputFormat.class);


    @Override
    public FileSinkOperator.RecordWriter getHiveRecordWriter(JobConf jobConf, Path path, Class aClass, boolean b, Properties properties, Progressable progressable) throws IOException {
        //hive调用这个writer
        return new SolrHiveWriter(jobConf);
    }


    @Override
    public RecordWriter<NullWritable, MapWritable> getRecordWriter(FileSystem fileSystem, JobConf jobConf, String s, Progressable progressable) throws IOException {
        throw new IOException("Hive should call 'getHiveRecordWriter' instead of 'getRecordWriter'");
    }

    @Override
    public void checkOutputSpecs(FileSystem fileSystem, JobConf jobConf) throws IOException {

    }
}
