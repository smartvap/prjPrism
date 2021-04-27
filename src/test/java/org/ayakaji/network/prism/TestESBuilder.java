package org.ayakaji.network.prism;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Test;

import java.io.IOException;

public class TestESBuilder {

    @Test
    public void testCreateDoc() throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("properties");
            {
                builder.startObject("sys_id");
                {
                    builder.field("type", "text");
                }
                builder.endObject();
                builder.startObject("agent_type");
                {
                    builder.field("type", "text");
                }
                builder.endObject();
                builder.startObject("ip_list");
                {
                    builder.field("type", "text");
                }
                builder.endObject();
                builder.startObject("update_time");
                {
                    builder.field("type", "date");
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();
        System.out.println(builder.toString());
    }
}
