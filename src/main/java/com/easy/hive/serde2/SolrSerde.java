package com.easy.hive.serde2;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.SerDe;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeStats;
import org.apache.hadoop.hive.serde2.objectinspector.*;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *
 * 定义SerDe，用来实现序列化和反序列时与hive对接
 * Created by qindongliang on 2016/3/11.
 */
public class SolrSerde implements SerDe {

    // params
    private List<String> columnNames = null;
    private List<TypeInfo> columnTypes = null;
    private ObjectInspector objectInspector = null;

    private List<Object> row;

    final static Logger log= LoggerFactory.getLogger(SolrSerde.class);

    @Override
    public void initialize(@Nullable Configuration configuration, Properties tbl) throws SerDeException {

        row=new ArrayList<Object>();

        // Read Column Names
        String columnNameProp = tbl.getProperty(serdeConstants.LIST_COLUMNS);
        if (columnNameProp != null && columnNameProp.length() > 0) {
            columnNames = Arrays.asList(columnNameProp.split(","));
        } else {
            columnNames = new ArrayList<String>();
        }

        // Read Column Types
        String columnTypeProp = tbl.getProperty(serdeConstants.LIST_COLUMN_TYPES);
        // default all string
        if (columnTypeProp == null) {
            String[] types = new String[columnNames.size()];
            Arrays.fill(types, 0, types.length, serdeConstants.STRING_TYPE_NAME);
            columnTypeProp = StringUtils.join(types, ":");
        }
        columnTypes = TypeInfoUtils.getTypeInfosFromTypeString(columnTypeProp);

        // Check column and types equals
        if (columnTypes.size() != columnNames.size()) {
            throw new SerDeException("len(columnNames) != len(columntTypes)");
        }

        // Create ObjectInspectors from the type information for each column
        List<ObjectInspector> columnOIs = new ArrayList<ObjectInspector>();
        ObjectInspector oi;
        for (int c = 0; c < columnNames.size(); c++) {
            oi = TypeInfoUtils
                    .getStandardJavaObjectInspectorFromTypeInfo(columnTypes
                            .get(c));
            columnOIs.add(oi);
        }
        objectInspector = ObjectInspectorFactory.getStandardStructObjectInspector(columnNames, columnOIs);





    }

    private final MapWritable mapWritable = new MapWritable();

    @Override
    public Class<? extends Writable> getSerializedClass() {
        return MapWritable.class;
    }

    @Override
    public Writable serialize(Object o, ObjectInspector objectInspector) throws SerDeException {
        // Fields...
        StructObjectInspector inspector = (StructObjectInspector) objectInspector;
        List<? extends StructField> fields = inspector.getAllStructFieldRefs();

        for (int i = 0; i < fields.size(); i++) {
            StructField f = fields.get(i);
            String docFieldName = columnNames.get(i);
                switch (f.getFieldObjectInspector().getCategory()) {
                    case PRIMITIVE:
                        Object value = ObjectInspectorUtils.copyToStandardJavaObject(inspector.getStructFieldData(o, f),
                                        f.getFieldObjectInspector());
                        mapWritable.put(new Text(docFieldName),new Text(value.toString()));
                        break;

                    case STRUCT:
                    case MAP:
                    case LIST:
                    case UNION:
                        throw new SerDeException(
                                "We don't yet support nested types (found " + f.getFieldObjectInspector()
                                        .getTypeName() + ")");
                }
        }

        return mapWritable;
    }

    @Override
    public SerDeStats getSerDeStats() {
        return null;
    }

    @Override
    public Object deserialize(Writable writable) throws SerDeException {
        final MapWritable input = (MapWritable) writable;
        final Text t = new Text();
        row.clear();

        for(int i=0;i<columnNames.size();i++){
            String k=columnNames.get(i);
            t.set(k);

            final Writable value = input.get(t);
            if (value != null && !NullWritable.get().equals(value)) {


                String colName = null;
                TypeInfo type_info = null;
                Object obj = null;

                    colName = columnNames.get(i);
                    type_info = columnTypes.get(i);
                    obj = null;
                    if (type_info.getCategory() == ObjectInspector.Category.PRIMITIVE) {
                        PrimitiveTypeInfo p_type_info = (PrimitiveTypeInfo) type_info;
                        switch (p_type_info.getPrimitiveCategory()) {
                            case STRING:
                                obj = value.toString();
                                break;
                            case LONG:
                            case INT:
                                try {
                                    obj = Long.parseLong(value.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                        }
                    }
                    row.add(obj);


            }

        }

        return row;

    }

    @Override
    public ObjectInspector getObjectInspector() throws SerDeException {
        return this.objectInspector;
    }





}
