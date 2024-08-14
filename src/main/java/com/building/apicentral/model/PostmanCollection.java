package com.building.apicentral.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostmanCollection {
    private Info info = new Info();
    private List<Item> item = new ArrayList<>();
    private List<Event> event = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private String _postman_id = "";   // Matches the field in the JSON
        private String name = "";         // Title from the field "name"
        private String description = "";
        private String schema = "";       // URL from "schema"
        private String version = "";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String name = "";
        private String description = "";
        private List<Item> item = new ArrayList<>();
        private Request request = new Request();
        private List<Response> response = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        private String method = "";
        private List<Header> header = new ArrayList<>();
        private Body body = new Body();
        private UrlObject url = new UrlObject();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UrlObject {
        private String raw = "";
        private String protocol = "";
        private List<String> host = new ArrayList<>();
        private List<String> path = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String key = "";
        private String value = "";
        private String type = ""; // Adjusted to include type if needed
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private String mode = "";
        private String raw = ""; // Use raw string for body content
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private String name = "";
        private String description = "";
        private String body = "";
        private List<Header> header = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String listen = "";
        private String script = "";
    }
}
