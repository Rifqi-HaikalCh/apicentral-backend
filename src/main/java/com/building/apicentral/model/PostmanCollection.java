package com.building.apicentral.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostmanCollection {
    private Info info = new Info();
    private List<Item> item = new ArrayList<>();
    private List<Event> event = new ArrayList<>();
    private List<Variable> variable = new ArrayList<>();
    private String desc = "";
    private Auth auth;
    private List<Tag> tags = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private String _postman_id = "";
        private String name = "";
        private String description = "";
        private String schema = "";
        private String version = "";
        private String _exporter_id = "";
        @JsonIgnoreProperties(ignoreUnknown = true)
        private Contact contact = new Contact();
        @JsonIgnoreProperties(ignoreUnknown = true)
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
        private Object name = "";
        private Object description = "";
        private List<Item> item = new ArrayList<>();
        private Request request = new Request();
        private List<Response> response = new ArrayList<>();
        private List<String> tags = new ArrayList<>();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Request {
            private Object method = "";
            private List<Header> header = new ArrayList<>();
            private Body body = new Body();
            private UrlObject url = new UrlObject();
            private Auth auth = new Auth();
            private Object description = "";
            private String originalRequest;
            private List<Parameter> parameters = new ArrayList<>();

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Auth {
                private String type = "";
                @JsonAnySetter
                private Map<String, Object> authDetails = new HashMap<>();
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Parameter {
                private String name;
                private String description;
                private String type;
                private String format;
                private Boolean required;
                private Boolean allowMultiple;
                private Items items;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Items {
                    private String type;
                    private String format;
                }
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UrlObject {
            private Object raw = "";
            private String protocol = "";
            private List<String> host = new ArrayList<>();
            private List<String> path = new ArrayList<>();
            private List<Query> query = new ArrayList<>();
            private String port;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Query {
                private Object key = "";
                private String value = "";
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Response {
            private Object name = "";
            private Integer code;
            private Object description = "";
            private Object body = "";
            private List<Header> header = new ArrayList<>();
            private String status;
            private String _postman_previewlanguage;
            private Object originalRequest;
            private List<Cookie> cookie = new ArrayList<>();

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Header {
                private Object key = "";
                private Object value = "";
                private Object type = "";
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Cookie {
                private Object key = "";
                private String value = "";
                private Object domain = "";
                private Object path = "";
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Script {
            private String type = "";
            private List<String> exec = new ArrayList<>();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private Object key = "";
        private String value = "";
        private String type = "";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Object mode = "";
        private Object raw = "";
        private Object language = "";
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
        private Object key = "";
        private String value = "";
        private String type = "";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Auth {
        private String type;
        private List<OAuth2> oauth2;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OAuth2 {
            private Object key;
            private String value;
            private String type;
        }
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        private String name;
        private String description;
    }
}
