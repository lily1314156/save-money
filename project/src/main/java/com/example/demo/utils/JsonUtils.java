package com.example.demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Component
public class JsonUtils {

    private final ObjectMapper objectMapper;

    public String serialize(Object obj) {
        return this.serialize(obj, String.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T serialize(Object source, Class<T> valueType) {
        try {
            if (valueType == String.class) {
                return (T) objectMapper.writeValueAsString(source);
            } else if (valueType == byte[].class) {
                return (T) objectMapper.writeValueAsBytes(source);
            } else {
                String message = String.format("Unsupported serialize type: %s", source.getClass());
                log.error(message);
                throw new IllegalArgumentException(message);
            }
        } catch (JsonProcessingException e) {
            String message = String.format("Failed to convert object to JSON string: %s", e.getMessage());
            log.error(message);
            throw new UncheckedIOException(message, e);
        }
    }

    public <T> T deserialize(Object source, Class<T> valueType) {
        try {
            if (source instanceof String string) {
                return objectMapper.readValue(string, valueType);
            } else if (source instanceof byte[] byteArray) {
                return objectMapper.readValue(byteArray, valueType);
            } else if (source instanceof InputStream inputStream) {
                return objectMapper.readValue(inputStream, valueType);
            } else {
                String message = String.format("Unsupported JSON source type: %s", source.getClass());
                log.error(message);
                throw new IllegalArgumentException(message);
            }
        } catch (IOException e) {
            String message = String.format("Failed to deserialize JSON: %s", e.getMessage());
            log.error(message);
            throw new UncheckedIOException(message, e);
        }
    }

    public <T> List<T> toList(Object obj, Class<T> valueType) {
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, valueType);
        return objectMapper.convertValue(obj, listType);
    }

    public Map<String, Object> toMap(Object source) {
        try {
            if (source instanceof String string) {
                return objectMapper.readValue(string, new TypeReference<Map<String, Object>>() {});
            } else if (source instanceof byte[] byteArray) {
                return objectMapper.readValue(byteArray, new TypeReference<Map<String, Object>>() {});
            } else if (source instanceof InputStream inputStream) {
                return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
            } else {
                return objectMapper.convertValue(source, new TypeReference<Map<String, Object>>() {});
            }
        } catch (IOException e) {
            String message = String.format("Failed to convert source to Map: %s", e.getMessage());
            log.error(message);
            throw new UncheckedIOException(message, e);
        }
    }

    public JsonNode toJsonNode(Object source) {
        try {
            if (source instanceof String string) {
                return objectMapper.readTree(string);
            } else if (source instanceof byte[] byteArray) {
                return objectMapper.readTree(byteArray);
            } else if (source instanceof InputStream inputStream) {
                return objectMapper.readTree(inputStream);
            } else {
                return objectMapper.valueToTree(source);
            }
        } catch (IOException e) {
            String message = String.format("Failed to parse to JsonNode: %s", e.getMessage());
            log.error(message);
            throw new UncheckedIOException(message, e);
        }
    }

    public String getFormattedJson(Object source) {
        try {
            JsonNode node;
            if (source instanceof String string) {
                node = objectMapper.readTree(string);
            } else if (source instanceof byte[] byteArray) {
                node = objectMapper.readTree(byteArray);
            } else if (source instanceof InputStream inputStream) {
                node = objectMapper.readTree(inputStream);
            } else {
                node = objectMapper.valueToTree(source);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (IOException e) {
            String message = String.format("Failed to get formatted JSON: %s", e.getMessage());
            log.error(message);
            throw new UncheckedIOException(message, e);
        }
    }

    public String minifyJson(Object source) {
        try {
            JsonNode node;
            if (source instanceof String string) {
                node = objectMapper.readTree(string);
            } else if (source instanceof byte[] byteArray) {
                node = objectMapper.readTree(byteArray);
            } else if (source instanceof InputStream inputStream) {
                node = objectMapper.readTree(inputStream);
            } else {
                node = objectMapper.valueToTree(source);
            }
            return objectMapper.writeValueAsString(node);
        } catch (IOException e) {
            String message = String.format("Failed to minify JSON: %s", e.getMessage());
            log.error(message);
            throw new UncheckedIOException(message, e);
        }
    }
}