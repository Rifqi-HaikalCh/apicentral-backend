package com.building.apicentral.service;

import com.building.apicentral.model.PostmanCollection.Header;
import com.building.apicentral.model.PostmanCollection;
import com.building.apicentral.model.SwaggerDefinition;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PostmanToSwaggerService {

    private static final Logger log = LoggerFactory.getLogger(PostmanToSwaggerService.class);
    private final ObjectMapper objectMapper;

    public PostmanToSwaggerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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

    private void setInfo(SwaggerDefinition swaggerDefinition, PostmanCollection.Info info) {
        if (info == null) {
            return;
        }

        SwaggerDefinition.Info swaggerInfo = new SwaggerDefinition.Info();
        swaggerInfo.setTitle(info.getName());
        swaggerInfo.setDescription(info.getDescription());
        swaggerInfo.setVersion(info.getVersion() != null ? info.getVersion() : "1.0.0");

        PostmanCollection.Info.Contact contact = info.getContact();
        if (contact != null) {
            SwaggerDefinition.Contact swaggerContact = new SwaggerDefinition.Contact();
            swaggerContact.setName(contact.getName());
            swaggerContact.setEmail(contact.getEmail());
            swaggerContact.setUrl(contact.getUrl());
            swaggerInfo.setContact(swaggerContact);
        }

        PostmanCollection.Info.License license = info.getLicense();
        if (license != null) {
            SwaggerDefinition.License swaggerLicense = new SwaggerDefinition.License();
            swaggerLicense.setName(license.getName());
            swaggerLicense.setUrl(license.getUrl());
            swaggerInfo.setLicense(swaggerLicense);
        }

        swaggerDefinition.setInfo(swaggerInfo);
    }

    private void extractContactAndLicenseFromDescription(SwaggerDefinition.Info swaggerInfo, String description) {
        if (description == null || description.isEmpty()) {
            return;
        }

        // Extract contact information from description
        Pattern contactPattern = Pattern.compile("Contact:\\s*([^\\n]+)\\s*Email:\\s*([^\\n]+)\\s*URL:\\s*([^\\n]+)");
        Matcher contactMatcher = contactPattern.matcher(description);
        if (contactMatcher.find()) {
            SwaggerDefinition.Contact contact = new SwaggerDefinition.Contact();
            contact.setName(contactMatcher.group(1).trim());
            contact.setEmail(contactMatcher.group(2).trim());
            contact.setUrl(contactMatcher.group(3).trim());
            swaggerInfo.setContact(contact);
        }

        // Extract license information from description
        Pattern licensePattern = Pattern.compile("License:\\s*([^\\n]+)\\s*License URL:\\s*([^\\n]+)");
        Matcher licenseMatcher = licensePattern.matcher(description);
        if (licenseMatcher.find()) {
            SwaggerDefinition.License license = new SwaggerDefinition.License();
            license.setName(licenseMatcher.group(1).trim());
            license.setUrl(licenseMatcher.group(2).trim());
            swaggerInfo.setLicense(license);
        }
    }


    private void setHostAndBasePath(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        if (postmanCollection.getItem() == null || postmanCollection.getItem().isEmpty()) {
            return;
        }

        String url = findFirstValidUrl(postmanCollection.getItem());
        if (url == null) {
            return;
        }

        try {
            url = resolvePostmanVariables(url, postmanCollection);
            URI uri = new URI(url);
            swaggerDefinition.setHost(uri.getHost());
            String path = uri.getPath();
            int firstSlash = path.indexOf('/', 1);
            if (firstSlash != -1) {
                swaggerDefinition.setBasePath(path.substring(0, firstSlash));
            } else {
                swaggerDefinition.setBasePath(path);
            }
        } catch (URISyntaxException e) {
            log.error("Error parsing URL", e);
        }
    }

    private String findFirstValidUrl(List<PostmanCollection.Item> items) {
        for (PostmanCollection.Item item : items) {
            if (item.getRequest() != null && item.getRequest().getUrl() != null) {
                String raw = extractString(item.getRequest().getUrl().getRaw());
                if (raw != null && !raw.isEmpty()) {
                    return raw;
                }
            }
            if (item.getItem() != null && !item.getItem().isEmpty()) {
                String url = findFirstValidUrl(item.getItem());
                if (url != null) {
                    return url;
                }
            }
        }
        return null;
    }

    private String extractString(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    private String resolvePostmanVariables(String url, PostmanCollection postmanCollection) {
        Map<String, String> variables = extractVariables(postmanCollection);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            url = url.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return url;
    }

    private Map<String, String> extractVariables(PostmanCollection postmanCollection) {
        Map<String, String> variables = new HashMap<>();
        if (postmanCollection.getVariable() != null) {
            for (PostmanCollection.Variable var : postmanCollection.getVariable()) {
                variables.put(var.getKey(), var.getValue());
            }
        }
        return variables;
    }
    private void setTags(SwaggerDefinition swaggerDefinition, List<PostmanCollection.Item> items) {
        if (items == null) {
            return;
        }

        List<String> tagNames = items.stream()
                .map(this::extractStringName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<SwaggerDefinition.Tag> tags = tagNames.stream()
                .map(SwaggerDefinition.Tag::new)
                .collect(Collectors.toList());

        swaggerDefinition.setTags(tags);
    }

    private String extractStringName(PostmanCollection.Item item) {
        Object name = item.getName();
        if (name instanceof String) {
            return (String) name;
        }
        return null;
    }

    private void setPaths(SwaggerDefinition swaggerDefinition, List<PostmanCollection.Item> items) {
        Map<String, SwaggerDefinition.PathItem> pathsMap = new HashMap<>();
        processItems(items, pathsMap, "");
        swaggerDefinition.setPaths(pathsMap);
    }

    private void processItems(List<PostmanCollection.Item> items, Map<String, SwaggerDefinition.PathItem> pathsMap, String parentPath) {
        if (items == null) {
            return;
        }

        for (PostmanCollection.Item item : items) {
            if (item.getItem() != null && !item.getItem().isEmpty()) {
                processItems(item.getItem(), pathsMap, parentPath + "/" + extractStringValue(item.getName()));
            } else if (item.getRequest() != null) {
                String path = getPath(item.getRequest().getUrl());
                if (path == null) {
                    continue;
                }

                String fullPath = parentPath + path;
                SwaggerDefinition.PathItem pathItem = pathsMap.computeIfAbsent(fullPath, k -> new SwaggerDefinition.PathItem());

                SwaggerDefinition.Operation operation = createOperation(item);
                String method = extractStringValue(item.getRequest().getMethod()).toLowerCase();

                setOperationForMethod(pathItem, method, operation);
            }
        }
    }

    private void setOperationForMethod(SwaggerDefinition.PathItem pathItem, String method, SwaggerDefinition.Operation operation) {
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
                log.warn("Unsupported HTTP method: {}", method);
                break;
        }
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

    private void setSecurityDefinitions(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        Map<String, SwaggerDefinition.SecurityScheme> securityDefinitions = new HashMap<>();

        if (postmanCollection.getItem().stream()
                .anyMatch(item -> item.getRequest().getAuth() != null && "bearer".equals(item.getRequest().getAuth().getType()))) {
            SwaggerDefinition.SecurityScheme jwtScheme = new SwaggerDefinition.SecurityScheme();
            jwtScheme.setType("apiKey");
            jwtScheme.setName("Authorization");
            jwtScheme.setIn("header");
            securityDefinitions.put("JWT", jwtScheme);
        }

        swaggerDefinition.setSecurityDefinitions(securityDefinitions);
    }

    private void setDefinitions(SwaggerDefinition swaggerDefinition) {
        Map<String, SwaggerDefinition.Definition> definitionsMap = new HashMap<>();
        extractDefinitionsFromItems(swaggerDefinition.getPaths(), definitionsMap);
        swaggerDefinition.setDefinitions(definitionsMap);
    }

    private void extractDefinitionsFromItems(Map<String, SwaggerDefinition.PathItem> paths, Map<String, SwaggerDefinition.Definition> definitionsMap) {
        for (Map.Entry<String, SwaggerDefinition.PathItem> entry : paths.entrySet()) {
            SwaggerDefinition.PathItem pathItem = entry.getValue();
            extractDefinitionsFromOperation(pathItem.getGet(), definitionsMap);
            extractDefinitionsFromOperation(pathItem.getPost(), definitionsMap);
            extractDefinitionsFromOperation(pathItem.getPut(), definitionsMap);
            extractDefinitionsFromOperation(pathItem.getDelete(), definitionsMap);
        }
    }

    private void extractDefinitionsFromOperation(SwaggerDefinition.Operation operation, Map<String, SwaggerDefinition.Definition> definitionsMap) {
        if (operation == null) return;
        for (SwaggerDefinition.Parameter parameter : operation.getParameters()) {
            if (parameter.getSchema() != null) {
                String modelName = parameter.getName() + "Request";
                SwaggerDefinition.Definition definition = createDefinitionFromSchema(parameter.getSchema());
                definitionsMap.put(modelName, definition);
            }
        }
        for (Map.Entry<String, SwaggerDefinition.Response> responseEntry : operation.getResponses().entrySet()) {
            if (responseEntry.getValue().getSchema() != null) {
                String modelName = responseEntry.getKey() + "Response";
                SwaggerDefinition.Definition definition = createDefinitionFromSchema(responseEntry.getValue().getSchema());
                definitionsMap.put(modelName, definition);
            }
        }
    }

    private SwaggerDefinition.Definition createDefinitionFromSchema(SwaggerDefinition.Schema schema) {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType(schema.getType());
        definition.setProperties(schema.getProperties());
        return definition;
    }

    private SwaggerDefinition.Definition createDefinitionFromBody(PostmanCollection.Body body) {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        try {
            if (body != null && body.getRaw() != null) {
                JsonNode jsonNode = objectMapper.readTree(body.getRaw());
                definition.setProperties(createPropertiesFromJsonNode(jsonNode));
            }
        } catch (IOException e) {
            log.error("Error parsing body", e);
        }
        return definition;
    }

    private SwaggerDefinition.Definition createTimestampDefinition() {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        properties.put("date", createProperty("string", "date-time"));

        definition.setProperties(properties);
        return definition;
    }

    private SwaggerDefinition.Definition createUploadFileResponseDefinition() {
        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        properties.put("fileName", createProperty("string", null));
        properties.put("uploadId", createProperty("string", null));
        properties.put("url", createProperty("string", "uri"));

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

    private SwaggerDefinition.SwaggerProperty createEnumProperty(List<String> enumValues) {
        SwaggerDefinition.SwaggerProperty property = new SwaggerDefinition.SwaggerProperty();
        property.setType("string");
        property.setEnum(enumValues);
        return property;
    }

    private SwaggerDefinition.SwaggerProperty createRefProperty(String ref) {
        SwaggerDefinition.SwaggerProperty property = new SwaggerDefinition.SwaggerProperty();
        property.setRef(ref);
        return property;
    }

    private void setSchemes(SwaggerDefinition swaggerDefinition) {
        List<String> schemes = Collections.singletonList("http");
        swaggerDefinition.setSchemes(schemes);
    }

    private SwaggerDefinition.Operation createOperation(PostmanCollection.Item item) {
        SwaggerDefinition.Operation operation = new SwaggerDefinition.Operation();

        String itemName = extractStringValue(item.getName());
        String itemDescription = extractStringValue(item.getDescription());

        operation.setTags(Collections.singletonList(itemName));
        operation.setSummary(itemName);
        operation.setDescription(itemDescription);

        List<SwaggerDefinition.Parameter> parameters = new ArrayList<>();
        addBodyParameter(parameters, item.getRequest().getBody());
        addHeaderParameters(parameters, item.getRequest().getHeader());
        addUrlParameters(parameters, item.getRequest().getUrl());
        operation.setParameters(parameters);

        operation.setResponses(createResponses(item));

        if (item.getRequest().getAuth() != null) {
            operation.setSecurity(createSecurity(item.getRequest().getAuth()));
        }
        return operation;
    }

    private String extractStringValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : "";
    }


    private void addBodyParameter(List<SwaggerDefinition.Parameter> parameters, PostmanCollection.Body body) {
        if (body != null && body.getRaw() != null) {
            SwaggerDefinition.Parameter bodyParameter = new SwaggerDefinition.Parameter();
            bodyParameter.setName("body");
            bodyParameter.setIn("body");
            bodyParameter.setRequired(true);
            SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
            schema.setType("object");
            schema.setProperties(createPropertiesFromBody(body));
            bodyParameter.setSchema(schema);
            parameters.add(bodyParameter);
        }
    }

    private void addHeaderParameters(List<SwaggerDefinition.Parameter> parameters, List<PostmanCollection.Header> headers) {
        if (headers != null) {
            for (PostmanCollection.Header header : headers) {
                SwaggerDefinition.Parameter headerParameter = new SwaggerDefinition.Parameter();
                headerParameter.setName(extractStringValue(header.getKey()));
                headerParameter.setIn("header");
                headerParameter.setRequired(false);
                SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
                schema.setType("string");
                headerParameter.setSchema(schema);
                parameters.add(headerParameter);
            }
        }
    }

    private void addUrlParameters(List<SwaggerDefinition.Parameter> parameters, PostmanCollection.UrlObject url) {
        if (url != null && url.getQuery() != null) {
            for (PostmanCollection.UrlObject.Query query : url.getQuery()) {
                SwaggerDefinition.Parameter queryParameter = new SwaggerDefinition.Parameter();
                queryParameter.setName(extractStringValue(query.getKey()));
                queryParameter.setIn("query");
                queryParameter.setRequired(false);
                SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
                schema.setType("string");
                queryParameter.setSchema(schema);
                parameters.add(queryParameter);
            }
        }
    }

    public class JsonProcessingException extends RuntimeException {
        public JsonProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private Map<String, SwaggerDefinition.SwaggerProperty> createPropertiesFromBody(PostmanCollection.Body body) {
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        if (body.getRaw() != null) {
            try {
                String cleanedJson = preprocessJson(body.getRaw());

                validateJsonString(cleanedJson);

                JsonNode jsonNode = objectMapper.readTree(cleanedJson);
                properties = createPropertiesFromJsonNode(jsonNode);
            } catch (JsonProcessingException e) {
                log.error("Error processing body raw JSON: {}", e.getMessage());
                throw new RuntimeException("Invalid JSON in request body: " + e.getMessage(), e);
            } catch (IOException e) {
                log.error("Error reading JSON data: {}", e.getMessage());
                throw new RuntimeException("Error processing JSON in request body", e);
            }
        }
        return properties;
    }

    private void validateJsonString(String json) throws JsonProcessingException {
        try {
            objectMapper.readTree(json);
        } catch (JsonParseException | JsonMappingException e) {
            log.error("Invalid JSON: {} | Error: {}", json, e.getMessage());
            throw new JsonProcessingException("Malformed JSON: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error processing JSON: {} | Error: {}", json, e.getMessage());
            throw new JsonProcessingException("Error processing JSON: " + e.getMessage(), e);
        }
    }

    private String preprocessJson(String json) {
        StringBuilder sb = new StringBuilder(json.trim());

        int openBraces = 0;
        int closeBraces = 0;
        for (char c : sb.toString().toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') closeBraces++;
        }

        while (closeBraces < openBraces) {
            sb.append("}");
            closeBraces++;
        }

        String result = sb.toString().replaceAll(",\\s*}", "}");

        if (!json.equals(result)) {
            log.warn("Incomplete JSON detected and fixed: Original: {}, Fixed: {}", json, result);
        }

        return result;
    }

    private boolean isJsonComplete(String json) {
        int openBraces = 0;
        int closeBraces = 0;
        for (char c : json.toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') closeBraces++;
        }
        return openBraces == closeBraces;
    }

    private Map<String, SwaggerDefinition.SwaggerProperty> createPropertiesFromJsonNode(JsonNode jsonNode) {
        Map<String, SwaggerDefinition.SwaggerProperty> properties = new HashMap<>();

        if (jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                SwaggerDefinition.SwaggerProperty property = createPropertyFromJsonNode(value);
                properties.put(key, property);
            });
        } else if (jsonNode.isArray()) {
            if (jsonNode.size() > 0) {
                SwaggerDefinition.SwaggerProperty itemProperty = createPropertyFromJsonNode(jsonNode.get(0));
                properties.put("[*]", itemProperty);
            }
        } else {
            SwaggerDefinition.SwaggerProperty property = createPropertyFromJsonNode(jsonNode);
            properties.put("", property);
        }

        return properties;
    }

    private SwaggerDefinition.SwaggerProperty createPropertyFromJsonNode(JsonNode jsonNode) {
        SwaggerDefinition.SwaggerProperty property = new SwaggerDefinition.SwaggerProperty();
        if (jsonNode.isTextual()) {
            property.setType("string");
        } else if (jsonNode.isNumber()) {
            property.setType("number");
        } else if (jsonNode.isBoolean()) {
            property.setType("boolean");
        } else if (jsonNode.isArray()) {
            property.setType("array");
            if (jsonNode.size() > 0) {
                SwaggerDefinition.Items items = new SwaggerDefinition.Items();
                SwaggerDefinition.SwaggerProperty itemProperty = createPropertyFromJsonNode(jsonNode.get(0));
                items.setType(itemProperty.getType());
                property.setItems(items);
            }
        } else if (jsonNode.isObject()) {
            property.setType("object");
            property.setProperties(createPropertiesFromJsonNode(jsonNode));
        }
        return property;
    }

    private Map<String, SwaggerDefinition.Response> createResponses(PostmanCollection.Item item) {
        Map<String, SwaggerDefinition.Response> responses = new HashMap<>();

        // Memanggil createResponse dengan tiga parameter
        responses.put("200", createResponse("Successful response", "", Collections.emptyList()));
        responses.put("400", createResponse("Bad request", "", Collections.emptyList()));
        responses.put("401", createResponse("Unauthorized", "", Collections.emptyList()));
        responses.put("403", createResponse("Forbidden", "", Collections.emptyList()));
        responses.put("404", createResponse("Not found", "", Collections.emptyList()));
        responses.put("500", createResponse("Internal server error", "", Collections.emptyList()));

        if (item.getResponse() != null) {
            for (PostmanCollection.Response response : item.getResponse()) {
                String statusCode = String.valueOf(extractIntValue(response.getCode()));
                SwaggerDefinition.Response swaggerResponse = createResponse(
                        extractStringValue(response.getName()),
                        extractStringValue(response.getBody()),
                        response.getHeader()
                );
                responses.put(statusCode, swaggerResponse);
            }
        }
        return responses;
    }


    private SwaggerDefinition.Response createResponse(String description, String exampleBody, List<PostmanCollection.Response.Header> headers) {
        SwaggerDefinition.Response response = new SwaggerDefinition.Response();
        response.setDescription(description);
        if (exampleBody != null && !exampleBody.isEmpty()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(exampleBody);
                SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
                schema.setType("object");
                schema.setProperties(createPropertiesFromJsonNode(jsonNode));
                response.setSchema(schema);
            } catch (IOException e) {
                log.error("Error parsing response body", e);
            }
        }
        addHeaders(response, headers);
        return response;
    }

    private void addHeaders(SwaggerDefinition.Response swaggerResponse, List<PostmanCollection.Response.Header> headers) {
        if (headers != null && !headers.isEmpty()) {
            Map<String, SwaggerDefinition.Header> headersMap = new HashMap<>();
            for (PostmanCollection.Response.Header header : headers) {
                SwaggerDefinition.Header swaggerHeader = new SwaggerDefinition.Header();
                swaggerHeader.setDescription(extractStringValue(header.getValue()));
                swaggerHeader.setSchema(extractStringValue(header.getType()));
                headersMap.put(extractStringValue(header.getKey()), swaggerHeader);
            }
            swaggerResponse.setHeaders(headersMap);
        }
    }


    private Integer extractIntValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }
    private List<Map<String, List<String>>> createSecurity(PostmanCollection.Request.Auth auth) {
        List<Map<String, List<String>>> security = new ArrayList<>();
        if (auth != null) {
            Map<String, List<String>> securityRequirement = new HashMap<>();
            securityRequirement.put(auth.getType(), Collections.emptyList());
            security.add(securityRequirement);
        }
        return security;
    }
}
