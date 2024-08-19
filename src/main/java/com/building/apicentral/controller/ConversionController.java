package com.building.apicentral.controller;

import com.building.apicentral.model.PostmanCollection;
import com.building.apicentral.model.SwaggerDefinition;
import com.building.apicentral.service.PostmanToSwaggerService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/convert")
public class ConversionController {

    private final PostmanToSwaggerService postmanToSwaggerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConversionController(PostmanToSwaggerService postmanToSwaggerService, ObjectMapper objectMapper) {
        this.postmanToSwaggerService = postmanToSwaggerService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/postman-to-swagger",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> convertPostmanToSwagger(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) String jsonBody) {
        try {
            PostmanCollection postmanCollection;
            if (file != null && !file.isEmpty()) {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                postmanCollection = parsePostmanCollection(content);
            } else if (jsonBody != null && !jsonBody.isEmpty()) {
                postmanCollection = parsePostmanCollection(jsonBody);
            } else {
                return ResponseEntity.badRequest().body("No valid input provided");
            }
            SwaggerDefinition swaggerDefinition = postmanToSwaggerService.convertPostmanToSwagger(postmanCollection);
            String swaggerJson = objectMapper.writeValueAsString(swaggerDefinition);
            return ResponseEntity.ok(swaggerJson);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing JSON: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error reading input: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    private PostmanCollection parsePostmanCollection(String json) throws JsonProcessingException {
        try {
            log.debug("Received JSON: " + json);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(json, PostmanCollection.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON: " + e.getMessage(), e);
            throw new JsonProcessingException("Error parsing Postman Collection: " + e.getMessage()) {};
        }
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body("Unsupported Media Type: " + ex.getMessage());
    }
}