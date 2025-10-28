-- Database initialization script for Application Control Panel

-- Create database (run as postgres superuser)
CREATE DATABASE control_panel_db;

-- Connect to the database
\c control_panel_db;

-- Create tables (Spring Boot will auto-create these with JPA, but this is for reference)

-- Control Buttons Table
CREATE TABLE IF NOT EXISTS control_buttons (
    id BIGSERIAL PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    icon VARCHAR(100),
    action_type VARCHAR(50) NOT NULL,
    target_endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    headers TEXT,
    payload_parameters TEXT,
    expected_output_format VARCHAR(50) NOT NULL,
    validation_enabled BOOLEAN DEFAULT TRUE,
    validation_schema TEXT,
    preview_enabled BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_executed_at TIMESTAMP,
    category VARCHAR(100),
    active BOOLEAN DEFAULT TRUE
);

-- Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    button_id BIGINT NOT NULL,
    executed_by VARCHAR(100) NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action VARCHAR(500) NOT NULL,
    request_payload TEXT,
    response_data TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    execution_time_ms BIGINT,
    FOREIGN KEY (button_id) REFERENCES control_buttons(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_control_buttons_created_by ON control_buttons(created_by);
CREATE INDEX idx_control_buttons_active ON control_buttons(active);
CREATE INDEX idx_control_buttons_category ON control_buttons(category);
CREATE INDEX idx_audit_logs_button_id ON audit_logs(button_id);
CREATE INDEX idx_audit_logs_executed_by ON audit_logs(executed_by);
CREATE INDEX idx_audit_logs_executed_at ON audit_logs(executed_at DESC);
CREATE INDEX idx_audit_logs_status ON audit_logs(status);

-- Sample data for testing (optional)
INSERT INTO control_buttons (
    label, icon, action_type, target_endpoint, http_method, 
    headers, payload_parameters, expected_output_format, 
    created_by, category, active
) VALUES 
(
    'Restart Kubernetes Service',
    'restart_alt',
    'REST_API_CALL',
    'https://api.example.com/k8s/service/restart',
    'POST',
    '{"Authorization": "Bearer token", "Content-Type": "application/json"}',
    '{"serviceName": "my-service", "namespace": "default"}',
    'JSON',
    'admin',
    'Kubernetes',
    true
),
(
    'Scale Pod Up',
    'add_circle',
    'REST_API_CALL',
    'https://api.example.com/k8s/pod/scale',
    'POST',
    '{"Authorization": "Bearer token", "Content-Type": "application/json"}',
    '{"podName": "my-pod", "replicas": 5}',
    'JSON',
    'admin',
    'Kubernetes',
    true
),
(
    'Check Service Health',
    'health_and_safety',
    'REST_API_CALL',
    'https://api.example.com/health',
    'GET',
    '{}',
    '{}',
    'JSON',
    'admin',
    'Monitoring',
    true
);

-- Grant permissions (adjust as needed for your setup)
-- GRANT ALL PRIVILEGES ON DATABASE control_panel_db TO your_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO your_user;

-- View to check button execution statistics
CREATE OR REPLACE VIEW button_execution_stats AS
SELECT 
    cb.id,
    cb.label,
    cb.category,
    COUNT(al.id) as total_executions,
    SUM(CASE WHEN al.status = 'SUCCESS' THEN 1 ELSE 0 END) as successful_executions,
    SUM(CASE WHEN al.status = 'FAILURE' THEN 1 ELSE 0 END) as failed_executions,
    AVG(al.execution_time_ms) as avg_execution_time_ms,
    MAX(al.executed_at) as last_executed_at
FROM control_buttons cb
LEFT JOIN audit_logs al ON cb.id = al.button_id
GROUP BY cb.id, cb.label, cb.category;

COMMENT ON TABLE control_buttons IS 'Stores configuration for control panel buttons';
COMMENT ON TABLE audit_logs IS 'Stores audit trail for button executions';
COMMENT ON VIEW button_execution_stats IS 'Provides statistics on button execution history';
