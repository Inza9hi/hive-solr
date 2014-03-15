package com.chimpler.hive.solr;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter;
import org.apache.hadoop.hive.serde2.io.ByteWritable;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.hive.serde2.io.ShortWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;

public class SolrWriter implements RecordWriter {

    private Logger log = Logger.getLogger(SolrWriter.class);
    private SolrTable table;
    private Map<String, String> typeMapping;

    public SolrWriter(String url, int numOutputBufferRows, String typeMapping) {
        this.typeMapping = new HashMap<String, String>();
        if (typeMapping != null) {
            String[] types = typeMapping.split(",");
            for (int i = 0; i < types.length; i++) {
                String[] map = types[i].split(":");
                this.typeMapping.put(map[0], map[1].toLowerCase());
            }
        }
        this.table = new SolrTable(url);
        if (numOutputBufferRows > 0) {
            table.setNumInputBufferRows(numOutputBufferRows);
        }
        try {
            this.table.drop();
        } catch (IOException e) {
            log.error("Error occured while removing data from Solr. It will not stop other steps.", e);
        }

    }

    @Override
    public void close(boolean abort) throws IOException {
        if (!abort) {
            table.commit();
        } else {
            table.rollback();
        }
    }

    @Override
    public void write(Writable w) throws IOException {
        MapWritable map = (MapWritable) w;
        SolrInputDocument doc = new SolrInputDocument();
        for (final Map.Entry<Writable, Writable> entry : map.entrySet()) {
            if ("array".equals(typeMapping.get(entry.getKey().toString()))) {
                String key = entry.getKey().toString();
                List<String> values = Arrays.asList(entry.getValue().toString().split(","));
                doc.setField(key, values);
            } else {
                String key = entry.getKey().toString();
                doc.setField(key, entry.getValue().toString());
            }
        }
        table.save(doc);
    }

    private Object getObjectFromWritable(Writable w) {
        if (w instanceof IntWritable) {
            // int
            return ((IntWritable) w).get();
        } else if (w instanceof ShortWritable) {
            // short
            return ((ShortWritable) w).get();
        } else if (w instanceof ByteWritable) {
            // byte
            return ((ByteWritable) w).get();
        } else if (w instanceof BooleanWritable) {
            // boolean
            return ((BooleanWritable) w).get();
        } else if (w instanceof LongWritable) {
            // long
            return ((LongWritable) w).get();
        } else if (w instanceof FloatWritable) {
            // float
            return ((FloatWritable) w).get();
        } else if (w instanceof DoubleWritable) {
            // double
            return ((DoubleWritable) w).get();
        } else if (w instanceof NullWritable) {
            // null
            return null;
        } else {
            // treat as string
            return w.toString();
        }
    }
}
