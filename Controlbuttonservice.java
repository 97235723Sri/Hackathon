package com.example.controlpanel.service;

import com.example.controlpanel.dto.ButtonDTO;
import com.example.controlpanel.entity.AuditLog;
import com.example.controlpanel.entity.ControlButton;
import com.example.controlpanel.repository.AuditLogRepository;
import com.example.controlpanel.repository.ControlButtonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ControlButtonService {

    private final ControlButtonRepository buttonRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Transactional
    public ButtonDTO.ButtonResponse createButton(ButtonDTO.CreateButtonRequest request) {
        String currentUser = getCurrentUser();
        
        ControlButton button = new ControlButton();
        button.setLabel(request.getLabel());
        button.setIcon(request.getIcon());
        button.setActionType(request.getActionType());
        button.setTargetEndpoint(request.getTargetEndpoint());
        button.setHttpMethod(request.getHttpMethod());
        button.setHeaders(convertMapToJson(request.getHeaders()));
        button.setPayloadParameters(convertMapToJson(request.getPayloadParameters()));
        button.setExpectedOutputFormat(request.getExpectedOutputFormat());
        button.setValidationEnabled(request.getValidationEnabled() != null ? request.getValidationEnabled() : true);
        button.setValidationSchema(request.getValidationSchema());
        button.setPreviewEnabled(request.getPreviewEnabled() != null ? request.getPreviewEnabled() : true);
        button.setCreatedBy(currentUser);
        button.setCategory(request.getCategory());
        button.setActive(true);

        ControlButton savedButton = buttonRepository.save(button);
        return convertToResponse(savedButton);
    }

    @Transactional
    public ButtonDTO.ButtonResponse updateButton(Long id, ButtonDTO.UpdateButtonRequest request) {
        ControlButton button = buttonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Button not found with id: " + id));

        if (request.getLabel() != null) button.setLabel(request.getLabel());
        if (request.getIcon() != null) button.setIcon(request.getIcon());
        if (request.getActionType() != null) button.setActionType(request.getActionType());
        if (request.getTargetEndpoint() != null) button.setTargetEndpoint(request.getTargetEndpoint());
        if (request.getHttpMethod() != null) button.setHttpMethod(request.getHttpMethod());
        if (request.getHeaders() != null) button.setHeaders(convertMapToJson(request.getHeaders()));
        if (request.getPayloadParameters() != null) button.setPayloadParameters(convertMapToJson(request.getPayloadParameters()));
        if (request.getExpectedOutputFormat() != null) button.setExpectedOutputFormat(request.getExpectedOutputFormat());
        if (request.getValidationEnabled() != null) button.setValidationEnabled(request.getValidationEnabled());
        if (request.getValidationSchema() != null) button.setValidationSchema(request.getValidationSchema());
        if (request.getPreviewEnabled() != null) button.setPreviewEnabled(request.getPreviewEnabled());
        if (request.getCategory() != null) button.setCategory(request.getCategory());
        if (request.getActive() != null) button.setActive(request.getActive());

        ControlButton updatedButton = buttonRepository.save(button);
        return convertToResponse(updatedButton);
    }

    @Transactional(readOnly = true)
    public List<ButtonDTO.ButtonResponse> getAllButtons() {
        return buttonRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ButtonDTO.ButtonResponse getButtonById(Long id) {
        ControlButton button = buttonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Button not found with id: " + id));
        return convertToResponse(button);
    }

    @Transactional
    public void deleteButton(Long id) {
        if (!buttonRepository.existsById(id)) {
            throw new RuntimeException("Button not found with id: " + id);
        }
        buttonRepository.deleteById(id);
    }

    @Transactional
    public ButtonDTO.ExecuteButtonResponse executeButton(Long id, ButtonDTO.ExecuteButtonRequest request) {
        long startTime = System.currentTimeMillis();
        String currentUser = getCurrentUser();

        ControlButton button = buttonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Button not found with id: " + id));

        if (!button.getActive()) {
            throw new RuntimeException("Button is inactive");
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setButtonId(id);
        auditLog.setExecutedBy(currentUser);
        auditLog.setAction(button.getHttpMethod().toString() + " " + button.getTargetEndpoint());

        try {
            // Prepare request
            Map<String, Object> inputParams = request.getInputParameters() != null ? 
                    request.getInputParameters() : new HashMap<>();
            
            String requestBody = convertMapToJson(inputParams);
            auditLog.setRequestPayload(requestBody);

            // Execute API call
            Response response = executeApiCall(button, inputParams);
            long executionTime = System.currentTimeMillis() - startTime;

            String responseBody = response.body() != null ? response.body().string() : "";
            
            ButtonDTO.ExecuteButtonResponse executeResponse = new ButtonDTO.ExecuteButtonResponse();
            executeResponse.setSuccess(response.isSuccessful());
            executeResponse.setStatusCode(response.code());
            executeResponse.setExecutionTimeMs(executionTime);
            executeResponse.setMessage(response.isSuccessful() ? "Execution successful" : "Execution failed");

            // Parse response based on format
            if (button.getExpectedOutputFormat() == ControlButton.OutputFormat.JSON && !responseBody.isEmpty()) {
                try {
                    executeResponse.setData(objectMapper.readValue(responseBody, Object.class));
                } catch (Exception e) {
                    executeResponse.setData(responseBody);
                }
            } else {
                executeResponse.setData(responseBody);
            }

            // Update audit log
            auditLog.setResponseData(responseBody);
            auditLog.setStatus(response.isSuccessful() ? AuditLog.ExecutionStatus.SUCCESS : AuditLog.ExecutionStatus.FAILURE);
            auditLog.setExecutionTimeMs(executionTime);

            if (!response.isSuccessful()) {
                auditLog.setErrorMessage("HTTP " + response.code() + ": " + response.message());
            }

            // Update button last executed time
            if (!Boolean.TRUE.equals(request.getIsPreview())) {
                button.setLastExecutedAt(LocalDateTime.now());
                buttonRepository.save(button);
            }

            return executeResponse;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Error executing button {}: {}", id, e.getMessage(), e);

            auditLog.setStatus(AuditLog.ExecutionStatus.FAILURE);
            auditLog.setErrorMessage(e.getMessage());
            auditLog.setExecutionTimeMs(executionTime);

            ButtonDTO.ExecuteButtonResponse errorResponse = new ButtonDTO.ExecuteButtonResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Execution failed: " + e.getMessage());
            errorResponse.setExecutionTimeMs(executionTime);

            return errorResponse;

        } finally {
            auditLogRepository.save(auditLog);
        }
    }

    private Response executeApiCall(ControlButton button, Map<String, Object> inputParams) throws IOException {
        Request.Builder requestBuilder = new Request.Builder();
        
        // Add headers
        if (button.getHeaders() != null && !button.getHeaders().isEmpty()) {
            Map<String, String> headers = convertJsonToMap(button.getHeaders());
            headers.forEach(requestBuilder::addHeader);
        }

        String url = button.getTargetEndpoint();
        RequestBody body = null;

        // Prepare request body for POST/PUT
        if (button.getHttpMethod() == ControlButton.HttpMethod.POST || 
            button.getHttpMethod() == ControlButton.HttpMethod.PUT) {
            
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String jsonBody = convertMapToJson(inputParams);
            body = RequestBody.create(jsonBody, mediaType);
        }

        // Build request based on HTTP method
        switch (button.getHttpMethod()) {
            case GET:
                requestBuilder.url(url).get();
                break;
            case POST:
                requestBuilder.url(url).post(body);
                break;
            case PUT:
                requestBuilder.url(url).put(body);
                break;
            case DELETE:
                requestBuilder.url(url).delete();
                break;
        }

        Request request = requestBuilder.build();
        return httpClient.newCall(request).execute();
    }

    private String convertMapToJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error converting map to JSON", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> convertJsonToMap(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to map", e);
            return new HashMap<>();
        }
    }

    private ButtonDTO.ButtonResponse convertToResponse(ControlButton button) {
        ButtonDTO.ButtonResponse response = new ButtonDTO.ButtonResponse();
        response.setId(button.getId());
        response.setLabel(button.getLabel());
        response.setIcon(button.getIcon());
        response.setActionType(button.getActionType());
        response.setTargetEndpoint(button.getTargetEndpoint());
        response.setHttpMethod(button.getHttpMethod());
        response.setHeaders(convertJsonToMap(button.getHeaders()));
        response.setPayloadParameters(convertJsonToObjectMap(button.getPayloadParameters()));
        response.setExpectedOutputFormat(button.getExpectedOutputFormat());
        response.setValidationEnabled(button.getValidationEnabled());
        response.setValidationSchema(button.getValidationSchema());
        response.setPreviewEnabled(button.getPreviewEnabled());
        response.setCreatedBy(button.getCreatedBy());
        response.setCreatedAt(button.getCreatedAt());
        response.setUpdatedAt(button.getUpdatedAt());
        response.setLastExecutedAt(button.getLastExecutedAt());
        response.setCategory(button.getCategory());
        response.setActive(button.getActive());
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertJsonToObjectMap(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to object map", e);
            return new HashMap<>();
        }
    }

    private String getCurrentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
