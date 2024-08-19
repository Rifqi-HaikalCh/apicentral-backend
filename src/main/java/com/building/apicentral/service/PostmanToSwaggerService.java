package com.building.apicentral.service;

import com.building.apicentral.model.PostmanCollection;
import com.building.apicentral.model.SwaggerDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Service
public class PostmanToSwaggerService {

    private static final Logger log = LoggerFactory.getLogger(PostmanToSwaggerService.class);

    private final ObjectMapper objectMapper;

    public PostmanToSwaggerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Change the method to accept PostmanCollection
    public SwaggerDefinition convertPostmanToSwagger(PostmanCollection postmanCollection) {
        try {
            SwaggerDefinition swaggerDefinition = new SwaggerDefinition();

            setInfo(swaggerDefinition, postmanCollection.getInfo());
            setHostAndBasePath(swaggerDefinition, postmanCollection);
            setTags(swaggerDefinition, postmanCollection.getItem());
            setPaths(swaggerDefinition, postmanCollection.getItem());
            setSecurityDefinitions(swaggerDefinition, postmanCollection);
            setDefinitions(swaggerDefinition);
            setSchemes(swaggerDefinition);

            return swaggerDefinition;

        } catch (Exception e) {
            throw new RuntimeException("Error converting Postman to Swagger", e);
        }
    }

    private SwaggerDefinition.Operation createOperation(PostmanCollection.Item item) {
        SwaggerDefinition.Operation operation = new SwaggerDefinition.Operation();

        if (item == null || item.getRequest() == null) {
            return operation; // Return empty operation if item or request is null
        }

        operation.setTags(Collections.singletonList(item.getName()));
        operation.setSummary(item.getName());
        operation.setOperationId("operation_" + item.getName().replaceAll("\\s+", "_").toLowerCase());
        List<SwaggerDefinition.Parameter> parameters = createParameters(item.getRequest());
        operation.setParameters(parameters);
        Map<String, SwaggerDefinition.Response> responses = createResponses();
        operation.setResponses(responses);
        operation.setConsumes(Collections.singletonList("application/json"));
        operation.setProduces(Collections.singletonList("application/json"));

        return operation;
    }

    private void setPaths(SwaggerDefinition swaggerDefinition, List<PostmanCollection.Item> items) {
        if (items == null) {
            return; // Exit if items is null
        }

        Map<String, SwaggerDefinition.PathItem> pathsMap = new HashMap<>();

        for (PostmanCollection.Item item : items) {
            if (item == null || item.getRequest() == null) {
                continue; // Skip null items or requests
            }

            String path = getPath(item.getRequest().getUrl());
            if (path == null) {
                continue; // Skip if path is null
            }

            SwaggerDefinition.PathItem pathItem = pathsMap.computeIfAbsent(path, k -> new SwaggerDefinition.PathItem());

            SwaggerDefinition.Operation operation = createOperation(item);
            String method = item.getRequest().getMethod().toLowerCase();

            switch (method) {
                case "get":
                    pathItem.setGet(operation);
                    break;
                case "post":
                    pathItem.setPost(operation);
                    break;
                case "put":
                    pathItem.setPut(operation);
                    break;
                case "delete":
                    pathItem.setDelete(operation);
                    break;
                case "patch":
                    pathItem.setPatch(operation);
                    break;
                case "options":
                    pathItem.setOptions(operation);
                    break;
                default:
                    // Optionally log unsupported methods
                    break;
            }
        }

        swaggerDefinition.setPaths(pathsMap);
    }


    private void setTags(SwaggerDefinition swaggerDefinition, List<PostmanCollection.Item> items) {
        List<String> tags = new ArrayList<>();

        for (PostmanCollection.Item item : items) {
            if (item.getName() != null && !tags.contains(item.getName())) {
                tags.add(item.getName());
            }
        }

        swaggerDefinition.setTags(tags.stream()
                .map(tag -> new SwaggerDefinition.Tag(tag)) // Hapus null, hanya gunakan nama tag
                .collect(Collectors.toList()));
    }

    private String getPath(PostmanCollection.UrlObject urlObject) {
        if (urlObject.getPath() != null) {
            return "/" + urlObject.getPath().stream()
                    .collect(Collectors.joining("/"))
                    .replaceAll("[:{}]", "");
        } else if (urlObject.getRaw() != null) {
            try {
                URI uri = new URI(urlObject.getRaw());
                return uri.getPath();
            } catch (URISyntaxException e) {
                log.error("Error parsing URL", e);
            }
        }
        return "/";
    }

    private void setInfo(SwaggerDefinition swaggerDefinition, PostmanCollection.Info info) {
        SwaggerDefinition.Info swaggerInfo = new SwaggerDefinition.Info();
        swaggerInfo.setTitle(info.getName());
        swaggerInfo.setDescription(info.getDescription());
        swaggerInfo.setVersion(info.getVersion());
        swaggerInfo.setTermsOfService("ntt");

        SwaggerDefinition.Contact contact = new SwaggerDefinition.Contact();
        contact.setName(info.getContact().getName());
        contact.setEmail(info.getContact().getEmail());
        contact.setUrl(info.getContact().getUrl());
        swaggerInfo.setContact(contact);

        SwaggerDefinition.License license = new SwaggerDefinition.License();
        license.setName("For internal usage only");
        license.setUrl("ntt");
        swaggerInfo.setLicense(license);

        swaggerDefinition.setInfo(swaggerInfo);
    }

    private String generateRealTimeVersion() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        return "v" + sdf.format(new Date());
    }

    private void setHostAndBasePath(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        String url = postmanCollection.getItem().get(0).getRequest().getUrl().getRaw();
        try {
            URI uri = new URI(url);
            swaggerDefinition.setHost(uri.getHost());
            swaggerDefinition.setBasePath("/");
        } catch (URISyntaxException e) {
            log.error("Error parsing URL", e);
        }
    }

    private String getHostFromVariables(JsonNode variablesNode) {
        for (JsonNode variableNode : variablesNode) {
            if ("baseUrl".equals(variableNode.path("key").asText())) {
                return extractHostFromUrl(variableNode.path("value").asText());
            }
        }
        return null;
    }

    private String extractHostFromUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            log.error("Error extracting host from URL", e);
            return null;
        }
    }

    private void setTags(SwaggerDefinition swaggerDefinition, JsonNode itemsNode) {
        List<SwaggerDefinition.Tag> tags = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            String name = itemNode.path("name").asText("");
            String description = "Operations related to " + name;
            SwaggerDefinition.Tag tag = new SwaggerDefinition.Tag(name); // Use the constructor with the name
            tag.setDescription(description); // Set the description separately
            tags.add(tag);
        }
        swaggerDefinition.setTags(tags);
    }

    private Map<String, SwaggerDefinition.Response> createResponses() {
        Map<String, SwaggerDefinition.Response> responses = new HashMap<>();

        // Helper method to create a response with a schema
        BiFunction<String, String, SwaggerDefinition.Response> createResponseWithSchema = (code, description) -> {
            SwaggerDefinition.Response response = new SwaggerDefinition.Response();
            response.setDescription(description);
            SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
            schema.setType("object");
            response.setSchema(schema);
            return response;
        };

        responses.put("200", createResponseWithSchema.apply("200", "OK"));
        responses.put("201", createResponseWithSchema.apply("201", "Created"));
        responses.put("401", createResponseWithSchema.apply("401", "Unauthorized"));
        responses.put("403", createResponseWithSchema.apply("403", "Forbidden"));
        responses.put("404", createResponseWithSchema.apply("404", "Not Found"));

        return responses;
    }

    private List<SwaggerDefinition.Parameter> createParameters(PostmanCollection.Request request) {
        List<SwaggerDefinition.Parameter> parameters = new ArrayList<>();

        // Add path parameters
        for (String pathSegment : request.getUrl().getPath()) {
            if (pathSegment.startsWith(":")) {
                SwaggerDefinition.Parameter parameter = new SwaggerDefinition.Parameter();
                parameter.setName(pathSegment.substring(1));
                parameter.setIn("path");
                parameter.setRequired(true);
                parameter.setType("string");
                parameters.add(parameter);
            }
        }

        // Add query parameters
        for (PostmanCollection.UrlObject.Query query : request.getUrl().getQuery()) {
            SwaggerDefinition.Parameter parameter = new SwaggerDefinition.Parameter();
            parameter.setName(query.getKey());
            parameter.setIn("query");
            parameter.setRequired(false);
            parameter.setType("string");
            parameter.setDescription(query.getValue());
            parameters.add(parameter);
        }

        // Add body parameter if present
        if (request.getBody() != null && "raw".equals(request.getBody().getMode())) {
            SwaggerDefinition.Parameter bodyParam = new SwaggerDefinition.Parameter();
            bodyParam.setName("body");
            bodyParam.setIn("body");
            bodyParam.setRequired(true);

            SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
            schema.setType("object");
            bodyParam.setSchema(schema);
            parameters.add(bodyParam);
        }

        // Add authorization header if present
        if (request.getAuth() != null && "bearer".equals(request.getAuth().getType())) {
            SwaggerDefinition.Parameter authParam = new SwaggerDefinition.Parameter();
            authParam.setName("Authorization");
            authParam.setIn("header");
            authParam.setRequired(true);
            authParam.setType("string");
            parameters.add(authParam);
        }

        return parameters;
    }


    private void setSchemes(SwaggerDefinition swaggerDefinition) {
        swaggerDefinition.setSchemes(Arrays.asList("http", "https"));
    }

    private void setSecurityDefinitions(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        Map<String, SwaggerDefinition.SecurityScheme> securityDefinitions = new HashMap<>();

        SwaggerDefinition.SecurityScheme jwtScheme = new SwaggerDefinition.SecurityScheme();
        jwtScheme.setType("apiKey");
        jwtScheme.setName("Authorization");
        jwtScheme.setIn("header");
        securityDefinitions.put("JWT", jwtScheme);

        swaggerDefinition.setSecurityDefinitions(securityDefinitions);
    }

    private void setDefinitions(SwaggerDefinition swaggerDefinition) {
        Map<String, SwaggerDefinition.Definition> definitionsMap = new HashMap<>();

        // Add Resource definition
        SwaggerDefinition.Definition resourceDefinition = createResourceDefinition();
        definitionsMap.put("Resource", resourceDefinition);

        // Add ResponseEntity definition
        SwaggerDefinition.Definition responseEntityDefinition = createResponseEntityDefinition();
        definitionsMap.put("ResponseEntity", responseEntityDefinition);

        // Add Timestamp definition
        SwaggerDefinition.Definition timestampDefinition = createTimestampDefinition();
        definitionsMap.put("Timestamp", timestampDefinition);

        // Add UploadFileResponse definition
        SwaggerDefinition.Definition uploadFileResponseDefinition = createUploadFileResponseDefinition();
        definitionsMap.put("UploadFileResponse", uploadFileResponseDefinition);

        swaggerDefinition.setDefinitions(definitionsMap);
    }

    private SwaggerDefinition.Definition createResourceDefinition() {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        properties.put("description", createProperty("string", null));
        properties.put("file", createProperty("file", null));
        properties.put("filename", createProperty("string", null));
        properties.put("inputStream", createRefProperty("#/definitions/InputStream"));
        properties.put("open", createProperty("boolean", null));
        properties.put("readable", createProperty("boolean", null));
        properties.put("uri", createProperty("string", "uri"));
        properties.put("url", createProperty("string", "url"));

        definition.setProperties(properties);
        return definition;
    }

    private SwaggerDefinition.Definition createResponseEntityDefinition() {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        properties.put("body", createProperty("object", null));
        properties.put("statusCode", createEnumProperty(Arrays.asList("USE_PROXY", "VARIANT_ALSO_NEGOTIATES")));
        properties.put("statusCodeValue", createProperty("integer", "int32"));

        definition.setProperties(properties);
        return definition;
    }

    private SwaggerDefinition.Definition createTimestampDefinition() {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        properties.put("date", createProperty("integer", "int32"));
        properties.put("time", createProperty("integer", "int64"));
        properties.put("year", createProperty("integer", "int32"));

        definition.setProperties(properties);
        return definition;
    }

    private SwaggerDefinition.Definition createUploadFileResponseDefinition() {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        properties.put("fileDownloadUri", createProperty("string", null));
        properties.put("size", createProperty("number", "double"));
        properties.put("success", createProperty("boolean", null));

        definition.setProperties(properties);
        return definition;
    }

    private SwaggerDefinition.SwaggerProperty createProperty(String type, String format) {
        SwaggerDefinition.SwaggerProperty property = new SwaggerDefinition.SwaggerProperty();
        property.setType(type);
        if (format != null) {
            property.setFormat(format);
        }
        return property;
    }

    private SwaggerDefinition.SwaggerProperty createRefProperty(String ref) {
        SwaggerDefinition.SwaggerProperty property = new SwaggerDefinition.SwaggerProperty();
        property.setRef(ref);
        return property;
    }

    private SwaggerDefinition.SwaggerProperty createEnumProperty(List<String> enumValues) {
        SwaggerDefinition.SwaggerProperty property = new SwaggerDefinition.SwaggerProperty();
        property.setType("string");
        property.setEnumValues(enumValues);
        return property;
    }
}