package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class NgsiLdEntityDto {

    private static final List<String> DEFAULT_CONTEXT = List.of(
        "https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld"
    );

    private String id;
    private String type;
    @JsonProperty("@context")
    private List<String> context;
    private String etlMappingVersion;
    private Map<String, Object> properties;

    public NgsiLdEntityDto(String id, String type, String etlMappingVersion, Map<String, Object> properties) {
        this(id, type, DEFAULT_CONTEXT, etlMappingVersion, properties);
    }

    public NgsiLdEntityDto(String id, String type, List<String> context, String etlMappingVersion, Map<String, Object> properties) {
        this.id = id;
        this.type = type;
        this.context = context;
        this.etlMappingVersion = etlMappingVersion;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public List<String> getContext() {
        return context;
    }

    public String getEtlMappingVersion() {
        return etlMappingVersion;
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }
}
