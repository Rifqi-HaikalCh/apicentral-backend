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
        setSecurityDefinitions(swaggerDefinition, postmanCollection);

        // Set definitions (schemas)
        setDefinitions(swaggerDefinition);

        return swaggerDefinition;
    }

    private void setInfo(SwaggerDefinition swaggerDefinition, PostmanCollection.Info postmanInfo) {
        SwaggerDefinition.Info swaggerInfo = new SwaggerDefinition.Info();

        // Mengambil title dari "name" PostmanCollection.Info dan memasukannya ke dalam SwaggerDefinition.Info.title
        swaggerInfo.setTitle(postmanInfo.getName());

        // Mengambil deskripsi dari PostmanCollection.Info dan memasukannya ke dalam SwaggerDefinition.Info.description
        swaggerInfo.setDescription(postmanInfo.getDescription());

        // Mengambil versi dari PostmanCollection.Info dan memasukannya ke dalam SwaggerDefinition.Info.version
        swaggerInfo.setVersion(postmanInfo.getVersion());

        // Setup contact informasi
        SwaggerDefinition.Contact contact = new SwaggerDefinition.Contact();
        if (postmanInfo.getEmail() != null && !postmanInfo.getEmail().isEmpty()) {
            contact.setEmail(postmanInfo.getEmail());
        } else {
            contact.setEmail("support@example.com"); // Email default jika tidak ada
        }
        contact.setName("API Support"); // Nama kontak default
        swaggerInfo.setContact(contact);

        // Setup license informasi
        SwaggerDefinition.License license = new SwaggerDefinition.License();
        if (postmanInfo.getSchema() != null && !postmanInfo.getSchema().isEmpty()) {
            license.setUrl(postmanInfo.getSchema());
        } else {
            license.setUrl("http://example.com"); // URL license default jika tidak ada
        }
        license.setName("API License"); // Nama license default
        swaggerInfo.setLicense(license);

        // Set SwaggerDefinition.Info ke SwaggerDefinition
        swaggerDefinition.setInfo(swaggerInfo);
    }


    private void setHostAndBasePath(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        if (postmanCollection.getItem() != null && !postmanCollection.getItem().isEmpty()) {
            PostmanCollection.Item firstItem = postmanCollection.getItem().get(0);
            if (firstItem.getRequest() != null && firstItem.getRequest().getUrl() != null) {
                PostmanCollection.UrlObject url = firstItem.getRequest().getUrl();
                if (url.getHost() != null && !url.getHost().isEmpty()) {
                    swaggerDefinition.setHost(String.join(".", url.getHost()));
                }
                if (url.getPath() != null && !url.getPath().isEmpty()) {
                    swaggerDefinition.setBasePath("/" + String.join("/", url.getPath()));
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

        List<SwaggerDefinition.Tag> tags = new ArrayList<>();
        for (String tag : uniqueTags) {
            SwaggerDefinition.Tag swaggerTag = new SwaggerDefinition.Tag();
            swaggerTag.setName(tag);
            swaggerTag.setDescription("Operations related to " + tag);
            tags.add(swaggerTag);
        }
        swaggerDefinition.setTags(tags);
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
                }
            }
        }

        paths.setPaths(pathsMap);
        swaggerDefinition.setPaths(paths);
    }

    private String getPath(PostmanCollection.UrlObject url) {
        if (url != null && url.getPath() != null) {
            return "/" + String.join("/", url.getPath());
        }
        return "/";
    }

    private SwaggerDefinition.Operation createOperation(PostmanCollection.Item item) {
        SwaggerDefinition.Operation operation = new SwaggerDefinition.Operation();
        operation.setTags(Collections.singletonList(item.getName()));
        operation.setSummary(item.getName());
        operation.setDescription(item.getDescription());
        operation.setOperationId(item.getName().replaceAll("\\s+", "") + item.getRequest().getMethod());

        List<SwaggerDefinition.Parameter> parameters = new ArrayList<>();
        if (item.getRequest().getUrl() != null) {
            if (item.getRequest().getUrl().getPath() != null) {
                for (String pathParam : item.getRequest().getUrl().getPath()) {
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
            if (item.getRequest().getUrl().getRaw() != null) {
                for (String pathSegment : item.getRequest().getUrl().getRaw().split("/")) {
                    if (pathSegment.startsWith(":")) {
                        SwaggerDefinition.Parameter parameter = new SwaggerDefinition.Parameter();
                        parameter.setName(pathSegment.substring(1));
                        parameter.setIn("path");
                        parameter.setRequired(true);
                        parameter.setType("string");
                        parameters.add(parameter);
                    }
                }
            }
        }
        operation.setParameters(parameters);

        SwaggerDefinition.Responses responses = new SwaggerDefinition.Responses();
        SwaggerDefinition.Response response = new SwaggerDefinition.Response();
        response.setDescription("Successful operation");
        responses.addResponse("200", response);
        operation.setResponses(responses);

        return operation;
    }

    private void setSecurityDefinitions(SwaggerDefinition swaggerDefinition, PostmanCollection postmanCollection) {
        SwaggerDefinition.SecurityDefinitions securityDefinitions = new SwaggerDefinition.SecurityDefinitions();
        Map<String, SwaggerDefinition.SecurityScheme> securitySchemesMap = new HashMap<>();

        PostmanCollection.Auth auth = postmanCollection.getAuth();
        if (auth != null && auth.getType() != null) {
            SwaggerDefinition.SecurityScheme securityScheme = new SwaggerDefinition.SecurityScheme();
            securityScheme.setType(auth.getType());

            if ("bearer".equalsIgnoreCase(auth.getType()) && auth.getApikey() != null) {
                List<PostmanCollection.ApiKeyElement> bearerTokens = auth.getApikey().getKey();
                if (!bearerTokens.isEmpty()) {
                    securityScheme.setName(bearerTokens.get(0).getKey());
                    securityScheme.setIn("header");
                    securitySchemesMap.put("Bearer Token", securityScheme);
                }
            }
        }

        securityDefinitions.setSecurityDefinitions(securitySchemesMap);
        swaggerDefinition.setSecurityDefinitions(securityDefinitions);
    }

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
