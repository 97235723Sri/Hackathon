package com.example.controlpanel.dto;

import com.example.controlpanel.entity.ControlButton;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class ButtonDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateButtonRequest {
        @NotBlank(message = "Label is required")
        private String label;

        private String icon;

        @NotNull(message = "Action type is required")
        private ControlButton.ActionType actionType;

        @NotBlank(message = "Target endpoint is required")
        private String targetEndpoint;

        @NotNull(message = "HTTP method is required")
        private ControlButton.HttpMethod httpMethod;

        private Map<String, String> headers;

        private Map<String, Object> payloadParameters;

        @NotNull(message = "Expected output format is required")
        private ControlButton.OutputFormat expectedOutputFormat;

        private Boolean validationEnabled;

        private String validationSchema;

        private Boolean previewEnabled;

        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateButtonRequest {
        private String label;
        private String icon;
        private ControlButton.ActionType actionType;
        private String targetEndpoint;
        private ControlButton.HttpMethod httpMethod;
        private Map<String, String> headers;
        private Map<String, Object> payloadParameters;
        private ControlButton.OutputFormat expectedOutputFormat;
        private Boolean validationEnabled;
        private String validationSchema;
        private Boolean previewEnabled;
        private String category;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ButtonResponse {
        private Long id;
        private String label;
        private String icon;
        private ControlButton.ActionType actionType;
        private String targetEndpoint;
        private ControlButton.HttpMethod httpMethod;
        private Map<String, String> headers;
        private Map<String, Object> payloadParameters;
        private ControlButton.OutputFormat expectedOutputFormat;
        private Boolean validationEnabled;
        private String validationSchema;
        private Boolean previewEnabled;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastExecutedAt;
        private String category;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecuteButtonRequest {
        private Map<String, Object> inputParameters;
        private Boolean isPreview;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecuteButtonResponse {
        private Boolean success;
        private String message;
        private Object data;
        private Integer statusCode;
        private Long executionTimeMs;
    }
}
