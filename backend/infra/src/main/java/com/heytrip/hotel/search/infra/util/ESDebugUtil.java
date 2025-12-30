package com.heytrip.hotel.search.infra.util;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.stream.JsonGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.StringWriter;

@Slf4j
public class ESDebugUtil {

    private static final JsonpMapper mapper = new JacksonJsonpMapper();

    /**
     * 将SearchRequest转换为Curl命令格式，方便调试
     *
     * @param request SearchRequest对象
     * @param esHost  Elasticsearch主机地址，如 http://localhost:9200
     * @return Curl命令字符串
     */
    public static String toCurlCommand(SearchRequest request, String esHost) {
        try {
            StringWriter writer = new StringWriter();
            JsonGenerator generator = mapper.jsonProvider().createGenerator(writer);
            
            request.serialize(generator, mapper);
            generator.close();
            
            String jsonBody = writer.toString();
            
            // 获取索引名称
            String indices = String.join(",", request.index());
            
            // 构建Curl命令
            StringBuilder curl = new StringBuilder();
            curl.append("curl -X POST '").append(esHost).append("/").append(indices).append("/_search' \\\n");
            curl.append("  -H 'Content-Type: application/json' \\\n");
            curl.append("  -d '").append(jsonBody).append("'");
            
            return curl.toString();
        } catch (Exception e) {
            log.error("生成Curl命令失败", e);
            return "Failed to generate curl command: " + e.getMessage();
        }
    }

    /**
     * 仅输出SearchRequest的JSON格式（不包含Curl命令）
     *
     * @param request SearchRequest对象
     * @return JSON字符串
     */
    public static String toJson(SearchRequest request) {
        try {
            StringWriter writer = new StringWriter();
            JsonGenerator generator = mapper.jsonProvider().createGenerator(writer);
            
            request.serialize(generator, mapper);
            generator.close();
            
            return writer.toString();
        } catch (Exception e) {
            log.error("序列化SearchRequest失败", e);
            return "Failed to serialize: " + e.getMessage();
        }
    }
}
