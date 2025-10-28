package com.example.controlpanel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "control_buttons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlButton {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    private String icon;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(nullable = false)
    private String targetEndpoint;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HttpMethod httpMethod;

    @Column(columnDefinition = "TEXT")
    private String headers;

    @Column(columnDefinition = "TEXT")
    private String payloadParameters;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutputFormat expectedOutputFormat;

    private Boolean validationEnabled = true;

    @Column(columnDefinition = "TEXT")
    private String validationSchema;

    private Boolean previewEnabled = true;

    @Column(nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastExecutedAt;

    private String category;

    @Column(nullable = false)
    private Boolean active = true;

    public enum ActionType {
        REST_API_CALL
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE
    }

    public enum OutputFormat {
        JSON, PLAIN_TEXT, XML
    }
}
