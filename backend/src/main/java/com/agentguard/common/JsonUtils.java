package com.agentguard.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root.isArray()) {
                List<String> values = new ArrayList<>();
                for (JsonNode node : root) {
                    values.add(node.asText());
                }
                return values;
            }
            if (root.isObject()) {
                JsonNode suggestionsNode = root.get("suggestions");
                if (suggestionsNode != null && suggestionsNode.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode node : suggestionsNode) {
                        values.add(node.asText());
                    }
                    return values;
                }
                JsonNode riskItemsNode = root.get("riskItems");
                if (riskItemsNode != null && riskItemsNode.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode node : riskItemsNode) {
                        values.add(node.asText());
                    }
                    return values;
                }
                return Collections.emptyList();
            }
            return Collections.singletonList(root.asText());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to parse JSON string list", exception);
        }
    }

    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize object as JSON", exception);
        }
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (!root.isObject()) {
                return Collections.emptyMap();
            }
            return OBJECT_MAPPER.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to parse JSON map", exception);
        }
    }

    public static <T> T parseObject(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to parse JSON object", exception);
        }
    }
}
