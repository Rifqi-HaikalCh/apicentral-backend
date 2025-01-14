package com.building.apicentral.service;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

        String title = info.getName();
        swaggerInfo.setTitle(title);
        swaggerInfo.setDescription(title);  // Description same as title

        LocalDate currentDate = LocalDate.now();
        swaggerInfo.setVersion("v." + currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".1");

        // Set default contact
        SwaggerDefinition.Contact swaggerContact = new SwaggerDefinition.Contact();
        String[] nameParts = title.split("\\s+");
        swaggerContact.setName(nameParts.length > 1 ?
                nameParts[0] + " " + nameParts[nameParts.length - 1] : title);
        swaggerContact.setUrl("ntt");
        swaggerContact.setEmail("myeaddress@company.com");

        // Set default license
        SwaggerDefinition.License swaggerLicense = new SwaggerDefinition.License();
        swaggerLicense.setName("For internal usage only");
        swaggerLicense.setUrl("ntt");

        // Override defaults with Postman info if available
        PostmanCollection.Info.Contact postmanContact = info.getContact();
        if (postmanContact != null) {
            if (postmanContact.getName() != null) swaggerContact.setName(postmanContact.getName());
            if (postmanContact.getEmail() != null) swaggerContact.setEmail(postmanContact.getEmail());
            if (postmanContact.getUrl() != null) swaggerContact.setUrl(postmanContact.getUrl());
        }

        PostmanCollection.Info.License postmanLicense = info.getLicense();
        if (postmanLicense != null) {
            if (postmanLicense.getName() != null) swaggerLicense.setName(postmanLicense.getName());
            if (postmanLicense.getUrl() != null) swaggerLicense.setUrl(postmanLicense.getUrl());
        }

        String description = info.getDescription();
        if (description != null && !description.isEmpty()) {
            Pattern contactPattern = Pattern.compile("Contact:\\s*([^\\n]+)\\s*Email:\\s*([^\\n]+)\\s*URL:\\s*([^\\n]+)");
            Matcher contactMatcher = contactPattern.matcher(description);
            if (contactMatcher.find()) {
                swaggerContact.setName(contactMatcher.group(1).trim());
                swaggerContact.setEmail(contactMatcher.group(2).trim());
                swaggerContact.setUrl(contactMatcher.group(3).trim());
            }

            Pattern licensePattern = Pattern.compile("License:\\s*([^\\n]+)\\s*License URL:\\s*([^\\n]+)");
            Matcher licenseMatcher = licensePattern.matcher(description);
            if (licenseMatcher.find()) {
                swaggerLicense.setName(licenseMatcher.group(1).trim());
                swaggerLicense.setUrl(licenseMatcher.group(2).trim());
            }
        }

        swaggerInfo.setContact(swaggerContact);
        swaggerInfo.setLicense(swaggerLicense);
        swaggerDefinition.setInfo(swaggerInfo);
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
                // Pastikan key dikonversi menjadi String
                String keyString = var.getKey().toString();
                variables.put(keyString, var.getValue());
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

    private String getPath(PostmanCollection.Item.UrlObject urlObject) {
        if (urlObject.getPath() != null && !urlObject.getPath().isEmpty()) {
            return "/" + String.join("/", urlObject.getPath())
                    .replaceAll("[:{}]", "");
        } else if (urlObject.getRaw() != null) {
            String rawUrl = extractStringValue(urlObject.getRaw());
            if (!rawUrl.isEmpty()) {
                try {
                    URI uri = new URI(rawUrl);
                    return uri.getPath();
                } catch (URISyntaxException e) {
                    log.error("Error parsing URL", e);
                }
            }
        }
        return "/";
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


    private void setOperationId(SwaggerDefinition.Operation operation, PostmanCollection.Item item) {
        String operationId = generateOperationId(item);
        operation.setOperationId(operationId);  // Changed to set a single String instead of a List
    }

    private String generateOperationId(PostmanCollection.Item item) {
        String name = extractStringValue(item.getName());
        String method = extractStringValue(item.getRequest().getMethod());
        String path = getPath(item.getRequest().getUrl());

        // Remove special characters and spaces from the name
        name = name.replaceAll("[^a-zA-Z0-9]", "");

        // Capitalize the first letter of the method
        method = method.substring(0, 1).toUpperCase() + method.substring(1).toLowerCase();

        // Remove leading slash and replace remaining slashes with underscores in the path
        path = path.replaceAll("^/|/$", "").replaceAll("/", "_");

        // Combine the parts to create the operationId
        String operationId = name + method + "Using" + method.toUpperCase() + "_" + path;

        // Ensure the operationId starts with a lowercase letter (OpenAPI convention)
        return operationId.substring(0, 1).toLowerCase() + operationId.substring(1);
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
                String rawString = body.getRaw().toString();
                JsonNode jsonNode = objectMapper.readTree(rawString);
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

        setContentTypes(operation, item);

        // Add this line to set the operationId
        setOperationId(operation, item);

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
            bodyParameter.setDescription("Request body");
            SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
            schema.setType("object");
            Map<String, SwaggerDefinition.SwaggerProperty> properties = createPropertiesFromBody(body);
            schema.setProperties(properties);
            bodyParameter.setSchema(schema);
            parameters.add(bodyParameter);
        }
    }


    private void addHeaderParameters(List<SwaggerDefinition.Parameter> parameters, List<PostmanCollection.Header> headers) {
        if (headers != null) {
            for (PostmanCollection.Header header : headers) {
                SwaggerDefinition.Parameter headerParameter = new SwaggerDefinition.Parameter();
                String headerName = extractStringValue(header.getKey());
                headerParameter.setName(headerName);
                headerParameter.setIn("header");
                headerParameter.setRequired(false);
                headerParameter.setDescription(formatDescription(headerName));
                SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
                schema.setType("string");
                headerParameter.setSchema(schema);
                if (header.getType() != null) {
                    schema.setType(header.getType().toString());
                }
                parameters.add(headerParameter);
            }
        }
    }

    private void addUrlParameters(List<SwaggerDefinition.Parameter> parameters, PostmanCollection.Item.UrlObject url) {
        if (url != null) {
            addQueryParameters(parameters, url.getQuery());
            addPathParameters(parameters, url.getPath());
        }
    }

    private void addQueryParameters(List<SwaggerDefinition.Parameter> parameters, List<PostmanCollection.Item.UrlObject.Query> queries) {
        if (queries != null) {
            for (PostmanCollection.Item.UrlObject.Query query : queries) {
                SwaggerDefinition.Parameter queryParameter = new SwaggerDefinition.Parameter();
                String keyString = extractStringValue(query.getKey());
                queryParameter.setName(keyString);
                queryParameter.setIn("query");
                queryParameter.setRequired(false);
                queryParameter.setDescription(formatDescription(keyString));
                SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
                schema.setType("string");
                if (query.getValue() != null) {
                    schema.setExample(query.getValue());
                }
                queryParameter.setSchema(schema);
                parameters.add(queryParameter);
            }
        }
    }

    private void addPathParameters(List<SwaggerDefinition.Parameter> parameters, List<String> pathSegments) {
        if (pathSegments != null) {
            for (String segment : pathSegments) {
                if (segment.startsWith("{") && segment.endsWith("}")) {
                    SwaggerDefinition.Parameter pathParameter = new SwaggerDefinition.Parameter();
                    String paramName = segment.substring(1, segment.length() - 1);
                    pathParameter.setName(paramName);
                    pathParameter.setIn("path");
                    pathParameter.setRequired(true);
                    pathParameter.setDescription(formatDescription(paramName));
                    SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
                    schema.setType("string");
                    pathParameter.setSchema(schema);
                    parameters.add(pathParameter);
                }
            }
        }
    }

    private String formatDescription(String name) {
        return name.replaceAll("[-_]", " ").trim();
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
                String rawString = body.getRaw().toString();

                String cleanedJson = preprocessJson(rawString);

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
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            SwaggerDefinition.SwaggerProperty property = createPropertyFromJsonNode(value);
            properties.put(key, property);
        }
        return properties;
    }

    private SwaggerDefinition.SwaggerProperty createPropertyFromJsonNode(JsonNode jsonNode) {
        SwaggerDefinition.SwaggerProperty property = new SwaggerDefinition.SwaggerProperty();
        if (jsonNode.isTextual()) {
            property.setType("string");
            property.setExample(jsonNode.asText());
        } else if (jsonNode.isNumber()) {
            property.setType("number");
            if (jsonNode.isInt()) {
                property.setFormat("int32");
            } else if (jsonNode.isLong()) {
                property.setFormat("int64");
            } else if (jsonNode.isFloat() || jsonNode.isDouble()) {
                property.setFormat("float");
            }
            property.setExample(jsonNode.numberValue());
        } else if (jsonNode.isBoolean()) {
            property.setType("boolean");
            property.setExample(jsonNode.booleanValue());
        } else if (jsonNode.isArray()) {
            property.setType("array");
            SwaggerDefinition.Items items = new SwaggerDefinition.Items();
            if (jsonNode.size() > 0) {
                JsonNode firstItem = jsonNode.get(0);
                if (firstItem.isObject()) {
                    items.setType("object");
                    SwaggerDefinition.SwaggerProperty itemProperty = createPropertyFromJsonNode(firstItem);
                    items.setRef("#/definitions/" + generateDefinitionName(firstItem));
                    // Add the item definition to a global map of definitions (you'll need to implement this)
                    addDefinition(generateDefinitionName(firstItem), itemProperty);
                } else {
                    items.setType(getJsonNodeType(firstItem));
                }
            }
            property.setItems(items);
        } else if (jsonNode.isObject()) {
            property.setType("object");
            property.setProperties(createPropertiesFromJsonNode(jsonNode));
        } else {
            property.setType("string");
        }

        // Set description (you might want to generate a meaningful description based on the property name or content)
        property.setDescription("Description for " + property.getType() + " property");

        return property;
    }

    private String getJsonNodeType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isObject()) return "object";
        if (node.isArray()) return "array";
        return "string"; // default to string for other types
    }

    private String generateDefinitionName(JsonNode node) {
        // Implement a method to generate a unique name for the definition
        // This could be based on the content of the node or use a counter
        return "Definition" + Math.abs(node.hashCode());
    }

    private void addDefinition(String name, SwaggerDefinition.SwaggerProperty property) {
        // Implement a method to add the definition to a global map of definitions
        // This map should be accessible when building the final Swagger document
    }

    private Map<String, SwaggerDefinition.Response> createResponses(PostmanCollection.Item item) {
        Map<String, SwaggerDefinition.Response> responses = new HashMap<>();

        // Adding default responses
        responses.put("200", createResponse("Successful response", "", Collections.emptyList()));
        responses.put("400", createResponse("Bad request", "", Collections.emptyList()));
        responses.put("401", createResponse("Unauthorized", "", Collections.emptyList()));
        responses.put("403", createResponse("Forbidden", "", Collections.emptyList()));
        responses.put("404", createResponse("Not found", "", Collections.emptyList()));
        responses.put("500", createResponse("Internal server error", "", Collections.emptyList()));

        if (item.getResponse() != null) {
            for (PostmanCollection.Item.Response response : item.getResponse()) {
                String statusCode = response.getCode() != null ? String.valueOf(response.getCode()) : "200";
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


    private SwaggerDefinition.Response createResponse(String description, String body, List<PostmanCollection.Item.Response.Header> headers) {
        SwaggerDefinition.Response response = new SwaggerDefinition.Response();
        response.setDescription(description);
        if (body != null && !body.isEmpty()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(body);
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

    private void addHeaders(SwaggerDefinition.Response swaggerResponse, List<PostmanCollection.Item.Response.Header> headers) {
        if (headers != null && !headers.isEmpty()) {
            Map<String, SwaggerDefinition.Header> headersMap = new HashMap<>();
            for (PostmanCollection.Item.Response.Header header : headers) {
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
    private List<Map<String, List<String>>> createSecurity(PostmanCollection.Item.Request.Auth auth) {
        List<Map<String, List<String>>> security = new ArrayList<>();
        if (auth != null && auth.getType() != null) {
            Map<String, List<String>> securityRequirement = new HashMap<>();

            if ("bearer".equalsIgnoreCase(auth.getType())) {
                Object bearerToken = auth.getAuthDetails().get("bearer");
                if (bearerToken instanceof List) {
                    List<?> bearerList = (List<?>) bearerToken;
                    if (!bearerList.isEmpty() && bearerList.get(0) instanceof Map) {
                        Map<?, ?> bearerMap = (Map<?, ?>) bearerList.get(0);
                        Object token = bearerMap.get("value");
                        if (token != null) {
                            securityRequirement.put("bearerAuth", Collections.singletonList(token.toString()));
                        }
                    }
                }
            } else {
                securityRequirement.put(auth.getType(), Collections.emptyList());
            }

            if (!securityRequirement.isEmpty()) {
                security.add(securityRequirement);
            }
        }
        return security;
    }

    private void setContentTypes(SwaggerDefinition.Operation operation, PostmanCollection.Item item) {
        List<String> consumes = new ArrayList<>();
        List<String> produces = new ArrayList<>();

        PostmanCollection.Item.Request request = item.getRequest();

        // Set consumes content-type
        if (request.getHeader() != null) {
            for (PostmanCollection.Header header : request.getHeader()) {
                String key = header.getKey() != null ? header.getKey().toString() : "";
                String value = header.getValue() != null ? header.getValue().toString() : "";
                if ("Content-Type".equalsIgnoreCase(key)) {
                    consumes.add(value);
                    break; // Assuming there's only one Content-Type header
                }
            }
        }

        // If no Content-Type header found, add a default
        if (consumes.isEmpty()) {
            consumes.add("application/json");
        }

        // Set produces content-type
        if (item.getResponse() != null && !item.getResponse().isEmpty()) {
            for (PostmanCollection.Item.Response response : item.getResponse()) {
                if (response.getHeader() != null) {
                    for (PostmanCollection.Item.Response.Header header : response.getHeader()) {
                        String key = header.getKey() != null ? header.getKey().toString() : "";
                        String value = header.getValue() != null ? header.getValue().toString() : "";
                        if ("Content-Type".equalsIgnoreCase(key)) {
                            produces.add(value);
                            break; // Assuming there's only one Content-Type header per response
                        }
                    }
                }
            }
        }

        // If no Content-Type found in responses, add a default
        if (produces.isEmpty()) {
            produces.add("application/json");
        }

        operation.setConsumes(consumes);
        operation.setProduces(produces);
    }

    private List<SwaggerDefinition.Parameter> createParameters(PostmanCollection.Item.Request request) {
        List<SwaggerDefinition.Parameter> parameters = new ArrayList<>();

        if (request != null) {
            // Handle URL parameters
            if (request.getUrl() != null && request.getUrl().getQuery() != null) {
                for (PostmanCollection.Item.UrlObject.Query query : request.getUrl().getQuery()) {
                    SwaggerDefinition.Parameter param = new SwaggerDefinition.Parameter();
                    param.setName(extractStringValue(query.getKey()));
                    param.setDescription(extractStringValue(query.getKey()));
                    param.setIn("query");
                    param.setType("string");
                    param.setRequired(true); // Assuming all query params are required
                    parameters.add(param);
                }
            }

            // Handle headers
            if (request.getHeader() != null) {
                for (PostmanCollection.Header header : request.getHeader()) {
                    SwaggerDefinition.Parameter param = new SwaggerDefinition.Parameter();
                    param.setName(extractStringValue(header.getKey()));
                    param.setDescription(extractStringValue(header.getKey()));
                    param.setIn("header");
                    param.setType("string");
                    param.setRequired(false); // Assuming headers are optional by default
                    parameters.add(param);
                }
            }

            // Handle body parameter
            if (request.getBody() != null && "raw".equals(request.getBody().getMode())) {
                SwaggerDefinition.Parameter param = new SwaggerDefinition.Parameter();
                param.setName("body");
                param.setDescription("Request body");
                param.setIn("body");
                param.setRequired(true);

                SwaggerDefinition.Schema schema = new SwaggerDefinition.Schema();
                schema.setType("string");
                param.setSchema(schema);

                parameters.add(param);
            }

            // Handle parameters defined in the request
            if (request.getParameters() != null) {
                for (PostmanCollection.Item.Request.Parameter param : request.getParameters()) {
                    SwaggerDefinition.Parameter swaggerParam = new SwaggerDefinition.Parameter();
                    swaggerParam.setName(extractStringValue(param.getName()));
                    swaggerParam.setDescription(extractStringValue(param.getDescription()));
                    swaggerParam.setIn("path"); // Assuming these are path parameters
                    swaggerParam.setType(param.getType());
                    swaggerParam.setRequired(param.getRequired());
                    parameters.add(swaggerParam);
                }
            }
        }

        return parameters;
    }
}
