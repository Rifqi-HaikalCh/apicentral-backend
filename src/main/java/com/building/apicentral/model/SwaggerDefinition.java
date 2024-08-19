package com.building.apicentral.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.io.File;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SwaggerDefinition {
    private String swagger = "2.0";
    private Info info = new Info();
    private String host = "";
    private String basePath = "";
    private List<Tag> tags = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, PathItem> paths = new HashMap<>();

    private Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
    private Map<String, Definition> definitions = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> schemes = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> consumes = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> produces = new ArrayList<>();


    @Data
    public static class Response {
        private String description = "";

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Schema schema;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Info {
        private String description = "";
        private String version = "";
        private String title = "";
        private String termsOfService = "";
        private Contact contact = new Contact();
        private License license = new License();
    }

    @Data
    public static class Contact {
        private String name = "";

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String url = "";

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String email = "";
    }

    @Data
    public static class License {
        private String name = "";
        private String url = "";
    }

    @Data
    public static class Tag {
        private String name = "";
        private String description = "";

        // Constructor that accepts a name
        public Tag(String name) {
            this.name = name;
        }

        // Constructor that accepts both name and description
        public Tag(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    @Data
    public static class Paths {
        private Map<String, PathItem> paths = new HashMap<>();
    }

    @Data
    public static class PathItem {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Operation get;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Operation post;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Operation put;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Operation delete;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Operation patch;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Operation options;
    }

    @Data
    public static class Operation {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> tags = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String summary = "";

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String description = "";

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String operationId = "";

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> consumes = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> produces = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<Parameter> parameters = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Response> responses = new HashMap<>(); // Directly using Map<String, Response>

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<Map<String, List<String>>> security = new ArrayList<>();
    }

    @Data
    public static class Schema {

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String type;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, SwaggerProperty> properties;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> required;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String ref;
    }

    @Data
    public static class Parameter {
        private String name;
        private String in;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String description;

        private Boolean required;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String type;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String format;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Schema schema;
    }

    @Data
    public static class Responses {
        private Map<String, Response> responses = new HashMap<>();

        public void setResponses(Map<String, Response> responses) {
            this.responses = responses;
        }

        public void addResponse(String name, Response response) {
            this.responses.put(name, response);
        }
    }

    @Data
    public static class SecurityDefinitions {
        private Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
    }

    @Data
    public static class SecurityRequirement {
        private Map<String, List<String>> requirements = new HashMap<>();

        public void put(String name, List<String> scopes) {
            requirements.put(name, scopes);
        }
    }

    @Data
    public static class Definitions {
        private Map<String, Definition> definitions = new HashMap<>();
    }

    @Data
    public static class SecurityScheme {
        private String type;
        private String name;
        private String in;
    }

    @Data
    public static class Definition {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String type = "";
        private Map<String, SwaggerProperty> properties = new HashMap<>();
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> required = new ArrayList<>();
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> enumValues;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Items items;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String title; // Added based on the example
    }

    @Data
    public static class SwaggerProperty {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String type;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String format;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String description;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Object example;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Items items;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String ref;  // Renamed from $ref
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> enumValues;
    }

    @Data
    public static class BearerAuth {
        private String token;
    }

    @Data
    public static class RequestBody {

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String description;
        private Map<String, MediaType> content = new HashMap<>();
    }

    @Data
    public static class MediaType {
        private Schema schema;
        private Map<String, Example> examples = new HashMap<>();
    }

    @Data
    public static class Example {
        private String summary;
        private Object value;
    }

    @Data
    public static class Items {
        private String type;
        private String format;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String ref;  // Renamed from $ref
    }
    @Data
    public static class Resource {
        private String description;
        private File file; // Assuming there's a File class
        private String filename;
        private InputStream inputStream; // Assuming there's an InputStream class
        private Boolean open;
        private Boolean readable;
        private String uri;
        private String url;
    }

    @Data
    public static class ResponseEntity {
        private Object body;
        private String statusCode;
        private Integer statusCodeValue;
    }

    @Data
    public static class Timestamp {
        private Integer date;
        private Long time;
        private Integer year;
    }

    @Data
    public static class UploadFileResponse {
        private String fileDownloadUri;
        private Double size;
        private Boolean success;
    }
}
