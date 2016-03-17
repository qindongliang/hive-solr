package com.easy.hive.inputsplit;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by qindongliang on 2016/3/14.
 */
public class SolrHiveInputSplit extends FileSplit {

    //使用委托对象，便于读取getPath
    private InputSplit delegate;
    private Path path;

    SolrHiveInputSplit() {
        this(new SolrInputSplit());
    }

    SolrHiveInputSplit(final InputSplit delegate) {
        this(delegate, null);
    }

    public SolrHiveInputSplit(final InputSplit delegate, final Path path) {
        super(path, 0, 0, (String[]) null);
        this.delegate = delegate;
        this.path = path;
    }

    public InputSplit getDelegate() {
        return delegate;
    }


    @Override
    public long getLength() {
        return 1L;
    }

    @Override
    public void write(final DataOutput out) throws IOException {
        Text.writeString(out, path.toString());
        delegate.write(out);
    }


    @Override
    public void readFields(final DataInput in) throws IOException {
        path = new Path(Text.readString(in));
        delegate.readFields(in);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public Path getPath() {
        return path;
    }


}
