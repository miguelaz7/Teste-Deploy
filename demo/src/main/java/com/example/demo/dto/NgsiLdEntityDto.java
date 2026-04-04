package com.example.demo.dto;

import java.util.Map;

public class NgsiLdEntityDto {

    private String id;
    private String type;
    private String etlMappingVersion;
    private Map<String, Object> properties;

    public NgsiLdEntityDto(String id, String type, String etlMappingVersion, Map<String, Object> properties) {
        this.id = id;
        this.type = type;
        this.etlMappingVersion = etlMappingVersion;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getEtlMappingVersion() {
        return etlMappingVersion;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
