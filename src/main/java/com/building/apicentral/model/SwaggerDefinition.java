package com.building.apicentral.model;

import lombok.Data;
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
    private Paths paths = new Paths();
    private SecurityDefinitions securityDefinitions = new SecurityDefinitions();
    private Definitions definitions = new Definitions();

    @Data
    public static class Info {
        private String title = "";
        private String description = "";
        private String version = "";
    }

    @Data
    public static class Tag {
        private String name = "";
        private String description = "";
    }

    @Data
    public static class Paths {
        private Map<String, PathItem> paths = new HashMap<>();
    }

    @Data
    public static class PathItem {
        private Operation get = new Operation();
        private Operation post = new Operation();
        private Operation put = new Operation();
        private Operation delete = new Operation();
        private Operation patch = new Operation();
        private Operation options = new Operation();
    }

    @Data
    public static class Operation {
        private List<String> tags = new ArrayList<>();
        private String summary = "";
        private String description = "";
        private String operationId = "";
        private List<String> consumes = new ArrayList<>();
        private List<String> produces = new ArrayList<>();
        private List<Parameter> parameters = new ArrayList<>();
        private Responses responses = new Responses();
        private List<SecurityRequirement> security = new ArrayList<>();
    }

    @Data
    public static class Parameter {
        private String name = "";
        private String in = "";
        private String description = "";
        private Boolean required = false;
        private String type = "";
        private String format = "";
        private Schema schema = new Schema(); // Adjust to match the Swagger definition
    }

    @Data
    public static class Schema {
        private String type = "";
        private Map<String, Property> properties = new HashMap<>();
        private List<Schema> items = new ArrayList<>(); // For arrays
    }

    @Data
    public static class Responses {
        private Map<String, Response> responseMap = new HashMap<>();

        public void setResponses(Map<String, Response> responses) {
            this.responseMap = responses;
        }

        public void addResponse(String name, Response response) {
            this.responseMap.put(name, response);
        }
    }

    @Data
    public static class Response {
        private String description = "";
    }

    @Data
    public static class SecurityDefinitions {
        private Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
    }

    @Data
    public static class SecurityScheme {
        private String type = "";
        private String name = "";
        private String in = "";
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
    public static class Definition {
        private String type = "";
        private Map<String, Property> properties = new HashMap<>();
    }

    @Data
    public static class Property {
        private String type = "";
        private String format = "";
        private String description = "";
    }
}
