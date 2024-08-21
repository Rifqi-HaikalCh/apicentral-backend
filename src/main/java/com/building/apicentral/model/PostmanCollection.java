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
    private List<Variable> variable = new ArrayList<>();
    private String desc = "";

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private String _postman_id = "";
        private String name = "";
        private String description = "";
        private String schema = "";
        private String version = "";
        private Contact contact = new Contact();
        private License license = new License();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Contact {
            private String name = "";
            private String email = "";
            private String url = "";
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class License {
            private String name = "";
            private String url = "";
        }
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
        private Auth auth = new Auth();
        private String description = "";

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Auth {
            private String type = "";
            private List<Bearer> bearer = new ArrayList<>();

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Bearer {
                private String key = "";
                private String value = "";
                private String type = "";
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UrlObject {
        private String raw = "";
        private String protocol = "";
        private List<String> host = new ArrayList<>();
        private List<String> path = new ArrayList<>();
        private List<Query> query = new ArrayList<>();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Query {
            private String key = "";
            private String value = "";
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String key = "";
        private String value = "";
        private String type = "";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private String mode = "";
        private String raw = "";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private String name = "";
        private Integer code;
        private String description = "";
        private String body = "";
        private List<Header> header = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String listen = "";
        private Script script = new Script();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Script {
            private String type = "";
            private List<String> exec = new ArrayList<>();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Variable {
        private String key = "";
        private String value = "";
        private String type = "";
    }
}
