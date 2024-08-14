package com.building.apicentral.service;

import com.building.apicentral.model.PostmanCollection;
import com.building.apicentral.model.SwaggerDefinition;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostmanToSwaggerService {

    public SwaggerDefinition convertPostmanToSwagger(PostmanCollection postmanCollection) {
        SwaggerDefinition swaggerDefinition = new SwaggerDefinition();

        // Set basic info
        swaggerDefinition.setSwagger("2.0");
        setInfo(swaggerDefinition, postmanCollection.getInfo());

        // Set host and basePath
        setHostAndBasePath(swaggerDefinition, postmanCollection);

        // Set tags
        setTags(swaggerDefinition, postmanCollection);

        // Set paths
        setPaths(swaggerDefinition, postmanCollection);

        // Set security definitions
//        setSecurityDefinitions(swaggerDefinition, postmanCollection);

        // Set definitions (schemas)
        setDefinitions(swaggerDefinition);

        return swaggerDefinition;
    }

    private void setInfo(SwaggerDefinition swaggerDefinition, PostmanCollection.Info postmanInfo) {
        SwaggerDefinition.Info swaggerInfo = new SwaggerDefinition.Info();

        if (postmanInfo.getName() != null) swaggerInfo.setTitle(postmanInfo.getName());
        if (postmanInfo.getDescription() != null) swaggerInfo.setDescription(postmanInfo.getDescription());
        if (postmanInfo.getVersion() != null) swaggerInfo.setVersion(postmanInfo.getVersion());

        swaggerDefinition.setInfo(swaggerInfo);
    }

    private void setHostAndBasePath(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        if (!postmanCollection.getItem().isEmpty()) {
            PostmanCollection.Item firstItem = postmanCollection.getItem().get(0);
            if (firstItem.getRequest() != null && firstItem.getRequest().getUrl() != null) {
                PostmanCollection.UrlObject url = firstItem.getRequest().getUrl();
                if (url.getHost() != null && !url.getHost().isEmpty()) {
                    swaggerDefinition.setHost(String.join(".", url.getHost()));
                }
                if (url.getPath() != null && !url.getPath().isEmpty()) {
                    swaggerDefinition.setBasePath("/" + url.getPath().get(0));
                }
            }
        }
    }

    private void setTags(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        Set<String> uniqueTags = new HashSet<>();
        for (PostmanCollection.Item item : postmanCollection.getItem()) {
            if (item.getName() != null && !item.getName().isEmpty()) {
                uniqueTags.add(item.getName());
            }
        }

        if (!uniqueTags.isEmpty()) {
            List<SwaggerDefinition.Tag> tags = new ArrayList<>();
            for (String tag : uniqueTags) {
                SwaggerDefinition.Tag swaggerTag = new SwaggerDefinition.Tag();
                swaggerTag.setName(tag);
                swaggerTag.setDescription("Operations related to " + tag);
                tags.add(swaggerTag);
            }
            swaggerDefinition.setTags(tags);
        }
    }


    private void setPaths(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        SwaggerDefinition.Paths paths = new SwaggerDefinition.Paths();
        Map<String, SwaggerDefinition.PathItem> pathsMap = new HashMap<>();

        for (PostmanCollection.Item item : postmanCollection.getItem()) {
            if (item.getRequest() != null) {
                String path = getPath(item.getRequest().getUrl());
                SwaggerDefinition.PathItem pathItem = pathsMap.computeIfAbsent(path, k -> new SwaggerDefinition.PathItem());
                SwaggerDefinition.Operation operation = createOperation(item);

                switch (item.getRequest().getMethod().toLowerCase()) {
                    case "get": pathItem.setGet(operation); break;
                    case "post": pathItem.setPost(operation); break;
                    case "put": pathItem.setPut(operation); break;
                    case "delete": pathItem.setDelete(operation); break;
                    case "patch": pathItem.setPatch(operation); break;
                    case "options": pathItem.setOptions(operation); break;
                }
            }
        }

        if (!pathsMap.isEmpty()) {
            paths.setPaths(pathsMap);
            swaggerDefinition.setPaths(paths);
        }
    }

    private String getPath(PostmanCollection.UrlObject url) {
        if (url != null && url.getPath() != null && !url.getPath().isEmpty()) {
            return "/" + String.join("/", url.getPath());
        }
        return "/";
    }


    private SwaggerDefinition.Operation createOperation(PostmanCollection.Item item) {
        SwaggerDefinition.Operation operation = new SwaggerDefinition.Operation();
        if (item.getName() != null) {
            operation.setTags(Collections.singletonList(item.getName()));
            operation.setSummary(item.getName());
            operation.setOperationId(item.getName().replaceAll("\\s+", "") + item.getRequest().getMethod());
        }
        if (item.getDescription() != null) operation.setDescription(item.getDescription());

        List<SwaggerDefinition.Parameter> parameters = createParameters(item.getRequest());
        if (!parameters.isEmpty()) operation.setParameters(parameters);

        SwaggerDefinition.Responses responses = new SwaggerDefinition.Responses();
        SwaggerDefinition.Response response = new SwaggerDefinition.Response();
        response.setDescription("Successful operation");
        responses.addResponse("200", response);
        operation.setResponses(responses);

        return operation;
    }

    private List<SwaggerDefinition.Parameter> createParameters(PostmanCollection.Request request) {
        List<SwaggerDefinition.Parameter> parameters = new ArrayList<>();
        if (request.getUrl() != null && request.getUrl().getPath() != null) {
            for (String pathParam : request.getUrl().getPath()) {
                if (pathParam.startsWith(":")) {
                    SwaggerDefinition.Parameter parameter = new SwaggerDefinition.Parameter();
                    parameter.setName(pathParam.substring(1));
                    parameter.setIn("path");
                    parameter.setRequired(true);
                    parameter.setType("string");
                    parameters.add(parameter);
                }
            }
        }
        return parameters;
    }

//    private void setSecurityDefinitions(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
//        SwaggerDefinition.SecurityDefinitions securityDefinitions = new SwaggerDefinition.SecurityDefinitions();
//        Map<String, SwaggerDefinition.SecurityScheme> securitySchemesMap = new HashMap<>();
//
//        PostmanCollection.Auth auth = postmanCollection.getAuth();
//        if (auth != null && auth.getType() != null) {
//            SwaggerDefinition.SecurityScheme securityScheme = new SwaggerDefinition.SecurityScheme();
//            securityScheme.setType(auth.getType());
//
//            if ("bearer".equalsIgnoreCase(auth.getType()) && auth.getApikey() != null) {
//                List<PostmanCollection.ApiKeyElement> bearerTokens = auth.getApikey().getKey();
//                if (!bearerTokens.isEmpty()) {
//                    securityScheme.setName(bearerTokens.get(0).getKey());
//                    securityScheme.setIn("header");
//                    securitySchemesMap.put("Bearer Token", securityScheme);
//                }
//            }
//        }
//
//        securityDefinitions.setSecurityDefinitions(securitySchemesMap);
//        swaggerDefinition.setSecurityDefinitions(securityDefinitions);
//    }

    private void setDefinitions(SwaggerDefinition swaggerDefinition) {
        SwaggerDefinition.Definitions definitions = new SwaggerDefinition.Definitions();
        Map<String, SwaggerDefinition.Definition> definitionsMap = new HashMap<>();

        SwaggerDefinition.Definition definition = new SwaggerDefinition.Definition();
        definition.setType("object");
        Map<String, SwaggerDefinition.Property> properties = new HashMap<>();
        SwaggerDefinition.Property property = new SwaggerDefinition.Property();
        property.setType("string");
        property.setDescription("An example property");
        properties.put("exampleProperty", property);
        definition.setProperties(properties);

        definitionsMap.put("ExampleDefinition", definition);
        definitions.setDefinitions(definitionsMap);

        swaggerDefinition.setDefinitions(definitions);
    }
}
