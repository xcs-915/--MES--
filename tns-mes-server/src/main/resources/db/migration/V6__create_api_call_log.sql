CREATE TABLE int_api_call_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    system_code NVARCHAR(40),
    endpoint NVARCHAR(500),
    http_method NVARCHAR(10),
    request_params NVARCHAR(MAX),
    request_body NVARCHAR(MAX),
    response_status INT,
    response_body NVARCHAR(MAX),
    duration_ms BIGINT,
    success BIT NOT NULL,
    error_message NVARCHAR(1000),
    created_at DATETIME2
);

CREATE INDEX ix_int_api_call_log_endpoint ON int_api_call_log(endpoint);
CREATE INDEX ix_int_api_call_log_system_code ON int_api_call_log(system_code);
CREATE INDEX ix_int_api_call_log_created_at ON int_api_call_log(created_at);
