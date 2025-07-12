# Xử lý khi throw 1 exception, tôi muốn khi throw
_Exported on 7/12/2025 at 15:05:05 GMT+7 from Cursor (1.2.2)_

---

**User**

Xử lý khi throw 1 exception, tôi muốn khi throw chỉ định được rõ message đã được cấu hình trong file message và có thể in thêm stackstrace nếu muốn dựa vào cấu hình 

---

**Cursor**

Chào bạn, tôi hiểu yêu cầu của bạn. Bạn muốn có một cách xử lý exception linh hoạt hơn, cho phép:
1.  Sử dụng message đã được định nghĩa sẵn trong các file ngôn ngữ.
2.  Hiển thị hoặc ẩn stack trace tùy theo cấu hình.

Để thực hiện điều này, tôi sẽ tiến hành theo các bước sau:
1.  **Phân tích cách xử lý lỗi hiện tại:** Tôi sẽ xem xét cách ứng dụng của bạn đang xử lý exception, đặc biệt là trong cấu hình của Yii Framework.
2.  **Tạo một lớp Exception tùy chỉnh:** Lớp này sẽ cho phép bạn `throw` exception với mã lỗi thay vì một chuỗi message tĩnh.
3.  **Tạo một ErrorHandler tùy chỉnh:** Component này sẽ bắt các exception, dịch các mã lỗi thành message tương ứng và quyết định có hiển thị stack trace hay không dựa vào file cấu hình.
4.  **Cập nhật cấu hình:** Hướng dẫn bạn cách tích hợp các thành phần mới vào ứng dụng.

Trước hết, tôi sẽ tìm kiếm cách mà ứng dụng của bạn đang xử lý exception. Tôi sẽ bắt đầu bằng việc tìm kiếm `handleException` trong toàn bộ codebase.

Kết quả tìm kiếm cho thấy:
*   `framework/base/CApplication.php` chứa phương thức `handleException`, là trung tâm xử lý exception của Yii. Nó sẽ ghi log và chuyển exception cho component `errorHandler`.
*   `framework/base/CErrorHandler.php` là component xử lý lỗi mặc định, chịu trách nhiệm hiển thị trang lỗi.
*   Đã có một file `protected/components/CustomException.php`. Tôi sẽ xem xét file này để xem nó có đáp ứng được yêu cầu không.
*   Các file Java không liên quan, tôi sẽ bỏ qua chúng.

Bây giờ tôi sẽ kiểm tra file `protected/components/CustomException.php` để hiểu rõ hơn về nó.

Ah, tôi đã mắc lỗi trong lệnh gọi công cụ trước đó. Tôi sẽ đọc toàn bộ file `protected/components/CustomException.php` để xem xét.

---

**User**

Xử lý trong dự án real-estate giúp tôi, spring boot 

---

**Cursor**

Tôi hiểu bạn muốn xử lý exception trong dự án Spring Boot real-estate. Tôi sẽ tìm hiểu cấu trúc hiện tại và tạo một giải pháp toàn diện cho việc xử lý exception với message đã được cấu hình và khả năng hiển thị stack trace tùy theo cấu hình.

Tuyệt vời! Tôi đã tìm hiểu được cấu trúc hiện tại của dự án Spring Boot real-estate. Bây giờ tôi sẽ tạo một giải pháp toàn diện cho việc xử lý exception với các tính năng mà bạn yêu cầu:

1. **Message đã được cấu hình trong file properties**
2. **Có thể hiển thị hoặc ẩn stack trace tùy theo cấu hình**
3. **Sử dụng mã lỗi thay vì message tĩnh khi throw exception**

Đầu tiên, tôi sẽ tạo class `ConfigurableException` cho phép throw exception với message key và parameters:

```java
package com.proptech.realestate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Configurable Exception that uses message keys for internationalized error messages
 * Supports dynamic parameters and configurable stack trace display
 */
@Getter
public class ConfigurableException extends RuntimeException {
    
    private final String messageKey;
    private final Object[] messageArgs;
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final boolean includeStackTrace;
    
    /**
     * Create exception with message key only
     */
    public ConfigurableException(String messageKey) {
        this(messageKey, null, HttpStatus.INTERNAL_SERVER_ERROR, null, false, null);
    }
    
    /**
     * Create exception with message key and arguments
     */
    public ConfigurableException(String messageKey, Object[] messageArgs) {
        this(messageKey, messageArgs, HttpStatus.INTERNAL_SERVER_ERROR, null, false, null);
    }
    
    /**
     * Create exception with message key, arguments and HTTP status
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus) {
        this(messageKey, messageArgs, httpStatus, null, false, null);
    }
    
    /**
     * Create exception with message key, arguments, HTTP status and error code
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus, String errorCode) {
        this(messageKey, messageArgs, httpStatus, errorCode, false, null);
    }
    
    /**
     * Create exception with message key, arguments, HTTP status, error code and stack trace option
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus, 
                               String errorCode, boolean includeStackTrace) {
        this(messageKey, messageArgs, httpStatus, errorCode, includeStackTrace, null);
    }
    
    /**
     * Full constructor with cause
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus, 
                               String errorCode, boolean includeStackTrace, Throwable cause) {
        super(messageKey, cause); // Use messageKey as temporary message until resolution
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode != null ? errorCode : generateErrorCode(httpStatus);
        this.includeStackTrace = includeStackTrace;
    }
    
    /**
     * Generate error code from HTTP status if not provided
     */
    private String generateErrorCode(HttpStatus status) {
        return "ERR_" + status.value();
    }
    
    /**
     * Builder pattern for easier exception creation
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String messageKey;
        private Object[] messageArgs;
        private HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        private String errorCode;
        private boolean includeStackTrace = false;
        private Throwable cause;
        
        public Builder messageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }
        
        public Builder messageArgs(Object... messageArgs) {
            this.messageArgs = messageArgs;
            return this;
        }
        
        public Builder httpStatus(HttpStatus httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder includeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
            return this;
        }
        
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }
        
        public ConfigurableException build() {
            if (messageKey == null) {
                throw new IllegalArgumentException("Message key is required");
            }
            return new ConfigurableException(messageKey, messageArgs, httpStatus, errorCode, includeStackTrace, cause);
        }
    }
    
    /**
     * Static factory methods for common exceptions
     */
    public static ConfigurableException notFound(String resource, String field, Object value) {
        return builder()
            .messageKey("error.resource.not_found")
            .messageArgs(new Object[]{resource, field, value})
            .httpStatus(HttpStatus.NOT_FOUND)
            .errorCode("RESOURCE_NOT_FOUND")
            .build();
    }
    
    public static ConfigurableException badRequest(String messageKey, Object... args) {
        return builder()
            .messageKey(messageKey)
            .messageArgs(args)
            .httpStatus(HttpStatus.BAD_REQUEST)
            .errorCode("BAD_REQUEST")
            .build();
    }
    
    public static ConfigurableException unauthorized(String messageKey) {
        return builder()
            .messageKey(messageKey)
            .httpStatus(HttpStatus.UNAUTHORIZED)
            .errorCode("UNAUTHORIZED")
            .build();
    }
    
    public static ConfigurableException forbidden(String messageKey) {
        return builder()
            .messageKey(messageKey)
            .httpStatus(HttpStatus.FORBIDDEN)
            .errorCode("FORBIDDEN")
            .build();
    }
    
    public static ConfigurableException internalError(String messageKey, Throwable cause) {
        return builder()
            .messageKey(messageKey)
            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .errorCode("INTERNAL_SERVER_ERROR")
            .includeStackTrace(true)
            .cause(cause)
            .build();
    }
}
```

Bây giờ tôi sẽ thêm cấu hình exception handling vào `application.yml`:

Tôi thấy file `application.yml` có vấn đề với cấu trúc (có duplicate `spring` sections). Tôi sẽ sửa lại và thêm cấu hình exception handling:

```yaml
spring:
  application:
    name: real-estate
  datasource:
    url: jdbc:postgresql://localhost:5432/realestate
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=30m
  
  # Elasticsearch Configuration
  data:
    elasticsearch:
      repositories:
        enabled: true
      client:
        reactive:
          endpoints: "localhost:9200"
          connection-timeout: 10s
          read-timeout: 30s

# Custom application properties
app:
  name: "Real Estate Management System"
  version: "1.39.0"
  
  # Exception Handling Configuration
  exception:
    # Control stack trace inclusion in responses
    include-stack-trace:
      # Global setting - can be overridden per exception
      enabled: false
      # Include stack trace only in development/debug mode
      include-in-dev-mode: true
      # Include stack trace for specific error codes
      include-for-error-codes:
        - "INTERNAL_SERVER_ERROR"
        - "DATABASE_ERROR"
        - "EXTERNAL_SERVICE_ERROR"
    
    # Detailed error information
    detailed-errors:
      # Include detailed error information in responses
      enabled: true
      # Include error timestamp
      include-timestamp: true
      # Include request ID for tracing
      include-request-id: true
      # Include error path/endpoint
      include-path: true
    
    # Logging configuration for exceptions
    logging:
      # Log all exceptions regardless of handling
      log-all-exceptions: true
      # Log level for handled exceptions
      handled-exception-level: "WARN"
      # Log level for unhandled exceptions  
      unhandled-exception-level: "ERROR"
      # Include stack trace in logs
      include-stack-trace-in-logs: true
  
  # Smart Search Configuration with Elasticsearch Integration
  smart-search:
    enabled: true
    elasticsearch:
      enabled: true
      fallback-to-database: true
      timeout-seconds: 30
      max-results: 1000
      highlighting:
        enabled: true
        fragment-size: 150
        number-of-fragments: 3
      scoring:
        boost-by-confidence: true
        confidence-multiplier: 2.0
        field-boosts:
          title: 3.0
          description: 2.0
          fullAddress: 1.5
          amenities: 1.2
      fuzzy-search:
        enabled: true
        fuzziness: "AUTO"
        prefix-length: 2
    
    # Vietnamese NLP Configuration  
    nlp:
      enabled: true
      python-path: "/usr/bin/python3"
      script-timeout: 30
      min-confidence: 0.3
      cache:
        enabled: true
        max-size: 1000
        expire-after-write: 60m
      
      # Advanced NLP Features
      features:
        entity-extraction: true
        intent-classification: true
        location-parsing: true
        price-normalization: true
        amenities-detection: true
      
      # Pattern Configuration
      patterns:
        property-types:
          - "căn hộ|apartment|chung cư"
          - "nhà riêng|nhà phố|townhouse"
          - "biệt thự|villa"
          - "shophouse|nhà mặt tiền"
          - "đất nền|land|plot"
        
        locations:
          districts:
            - "quận 1|q1|district 1"
            - "quận 7|q7|district 7"
            - "thủ đức|thu duc"
            - "bình thạnh|binh thanh"
          
        amenities:
          - "hồ bơi|swimming pool|pool"
          - "gym|phòng tập|fitness"
          - "thang máy|elevator"
          - "bảo vệ|security|an ninh"
          - "gần trường|near school"
          - "gần chợ|near market"

# Logging configuration
logging:
  level:
    com.proptech.realestate: DEBUG
    com.proptech.realestate.service.PythonExecutorService: DEBUG
    com.proptech.realestate.service.VietnameseNLPService: DEBUG
    com.proptech.realestate.exception: DEBUG
    org.springframework.cache: DEBUG
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/real-estate.log

# Actuator endpoints for monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,cache
  endpoint:
    health:
      show-details: always
```

Bây giờ tôi sẽ thêm các message key mới vào file messages.properties:

```java-properties
error.unauthorized=Authentication failed. Please check your credentials.
error.internal_server=An unexpected internal server error has occurred. Please contact support.
error.bad_request=The request is invalid. Please check the input data.
error.resource.not_found={0} not found with {1}: ''{2}''
validation.field.required={0} is a required field.

# Database related errors
error.database.connection_failed=Failed to connect to database. Please try again later.
error.database.query_failed=Database query failed: {0}
error.database.constraint_violation=Database constraint violation: {0}
error.database.data_integrity=Data integrity error: {0}

# Business logic errors
error.business.duplicate_entry=Duplicate entry found for {0}: {1}
error.business.invalid_operation=Invalid operation: {0}
error.business.insufficient_permissions=Insufficient permissions to perform this action.
error.business.operation_not_allowed=Operation not allowed for {0} in current state.

# Validation errors
error.validation.invalid_format=Invalid format for {0}. Expected format: {1}
error.validation.value_out_of_range={0} must be between {1} and {2}
error.validation.required_field_missing=Required field {0} is missing
error.validation.invalid_email=Invalid email address format: {0}
error.validation.invalid_phone=Invalid phone number format: {0}
error.validation.password_too_weak=Password does not meet security requirements

# Property related errors
error.property.not_found=Property not found with ID: {0}
error.property.not_available=Property {0} is not available for {1}
error.property.invalid_price=Invalid price format: {0}
error.property.invalid_status=Invalid property status: {0}
error.property.address_required=Property address is required
error.property.listing_expired=Property listing has expired: {0}

# User/Agent related errors
error.user.not_found=User not found with {0}: {1}
error.user.account_disabled=User account is disabled: {0}
error.user.account_locked=User account is locked: {0}
error.user.invalid_credentials=Invalid username or password
error.agent.not_found=Agent not found with ID: {0}
error.agent.license_expired=Agent license has expired: {0}

# MLS related errors
error.mls.feed_not_found=MLS feed not found with ID: {0}
error.mls.sync_failed=MLS sync failed for feed {0}: {1}
error.mls.authentication_failed=MLS authentication failed for {0}
error.mls.data_format_invalid=Invalid MLS data format: {0}
error.mls.rate_limit_exceeded=MLS API rate limit exceeded for {0}

# Search related errors
error.search.query_invalid=Invalid search query: {0}
error.search.elasticsearch_unavailable=Search service is temporarily unavailable
error.search.too_many_results=Search returned too many results. Please refine your query.
error.search.index_not_found=Search index not found: {0}

# File/Upload related errors
error.file.not_found=File not found: {0}
error.file.upload_failed=File upload failed: {0}
error.file.invalid_format=Invalid file format. Expected: {0}
error.file.size_exceeded=File size exceeds maximum allowed size of {0}
error.file.processing_failed=File processing failed: {0}

# External service errors
error.external.service_unavailable=External service {0} is currently unavailable
error.external.api_error=External API error from {0}: {1}
error.external.timeout=Request to {0} timed out after {1} seconds
error.external.authentication_failed=Authentication failed for external service: {0}

# Configuration errors
error.config.invalid_value=Invalid configuration value for {0}: {1}
error.config.missing_property=Missing required configuration property: {0}
error.config.initialization_failed=Configuration initialization failed: {0}

# Security errors
error.security.access_denied=Access denied for resource: {0}
error.security.token_expired=Security token has expired
error.security.token_invalid=Invalid security token
error.security.session_expired=Session has expired. Please login again.

# Generic errors
error.operation.failed=Operation failed: {0}
error.operation.timeout=Operation timed out after {0} seconds
error.operation.cancelled=Operation was cancelled: {0}
error.system.maintenance=System is under maintenance. Please try again later.
error.feature.not_available=Feature {0} is not available in your current plan
```

Bây giờ tôi sẽ thêm bản dịch tiếng Việt cho các message này:

```java-properties
error.unauthorized=Xác thực thất bại. Vui lòng kiểm tra lại thông tin đăng nhập.
error.internal_server=Đã có lỗi hệ thống ngoài ý muốn xảy ra. Vui lòng liên hệ hỗ trợ.
error.bad_request=Yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu đầu vào.
error.resource.not_found=Không tìm thấy tài nguyên {0} với {1}: ''{2}''
validation.field.required={0} là trường bắt buộc.

# Database related errors
error.database.connection_failed=Không thể kết nối cơ sở dữ liệu. Vui lòng thử lại sau.
error.database.query_failed=Truy vấn cơ sở dữ liệu thất bại: {0}
error.database.constraint_violation=Vi phạm ràng buộc cơ sở dữ liệu: {0}
error.database.data_integrity=Lỗi tính toàn vẹn dữ liệu: {0}

# Business logic errors
error.business.duplicate_entry=Phát hiện bản ghi trùng lặp cho {0}: {1}
error.business.invalid_operation=Thao tác không hợp lệ: {0}
error.business.insufficient_permissions=Không đủ quyền để thực hiện hành động này.
error.business.operation_not_allowed=Không được phép thực hiện thao tác cho {0} trong trạng thái hiện tại.

# Validation errors
error.validation.invalid_format=Định dạng không hợp lệ cho {0}. Định dạng mong đợi: {1}
error.validation.value_out_of_range={0} phải nằm trong khoảng từ {1} đến {2}
error.validation.required_field_missing=Thiếu trường bắt buộc {0}
error.validation.invalid_email=Định dạng email không hợp lệ: {0}
error.validation.invalid_phone=Định dạng số điện thoại không hợp lệ: {0}
error.validation.password_too_weak=Mật khẩu không đáp ứng yêu cầu bảo mật

# Property related errors
error.property.not_found=Không tìm thấy bất động sản với ID: {0}
error.property.not_available=Bất động sản {0} không khả dụng cho {1}
error.property.invalid_price=Định dạng giá không hợp lệ: {0}
error.property.invalid_status=Trạng thái bất động sản không hợp lệ: {0}
error.property.address_required=Địa chỉ bất động sản là bắt buộc
error.property.listing_expired=Tin đăng bất động sản đã hết hạn: {0}

# User/Agent related errors
error.user.not_found=Không tìm thấy người dùng với {0}: {1}
error.user.account_disabled=Tài khoản người dùng đã bị vô hiệu hóa: {0}
error.user.account_locked=Tài khoản người dùng đã bị khóa: {0}
error.user.invalid_credentials=Tên đăng nhập hoặc mật khẩu không đúng
error.agent.not_found=Không tìm thấy đại lý với ID: {0}
error.agent.license_expired=Giấy phép đại lý đã hết hạn: {0}

# MLS related errors
error.mls.feed_not_found=Không tìm thấy nguồn dữ liệu MLS với ID: {0}
error.mls.sync_failed=Đồng bộ MLS thất bại cho nguồn {0}: {1}
error.mls.authentication_failed=Xác thực MLS thất bại cho {0}
error.mls.data_format_invalid=Định dạng dữ liệu MLS không hợp lệ: {0}
error.mls.rate_limit_exceeded=Vượt quá giới hạn tần suất API MLS cho {0}

# Search related errors
error.search.query_invalid=Truy vấn tìm kiếm không hợp lệ: {0}
error.search.elasticsearch_unavailable=Dịch vụ tìm kiếm tạm thời không khả dụng
error.search.too_many_results=Tìm kiếm trả về quá nhiều kết quả. Vui lòng làm rõ truy vấn.
error.search.index_not_found=Không tìm thấy chỉ mục tìm kiếm: {0}

# File/Upload related errors
error.file.not_found=Không tìm thấy tệp: {0}
error.file.upload_failed=Tải lên tệp thất bại: {0}
error.file.invalid_format=Định dạng tệp không hợp lệ. Mong đợi: {0}
error.file.size_exceeded=Kích thước tệp vượt quá giới hạn tối đa {0}
error.file.processing_failed=Xử lý tệp thất bại: {0}

# External service errors
error.external.service_unavailable=Dịch vụ bên ngoài {0} hiện không khả dụng
error.external.api_error=Lỗi API bên ngoài từ {0}: {1}
error.external.timeout=Yêu cầu tới {0} hết thời gian chờ sau {1} giây
error.external.authentication_failed=Xác thực thất bại cho dịch vụ bên ngoài: {0}

# Configuration errors
error.config.invalid_value=Giá trị cấu hình không hợp lệ cho {0}: {1}
error.config.missing_property=Thiếu thuộc tính cấu hình bắt buộc: {0}
error.config.initialization_failed=Khởi tạo cấu hình thất bại: {0}

# Security errors
error.security.access_denied=Từ chối truy cập tài nguyên: {0}
error.security.token_expired=Token bảo mật đã hết hạn
error.security.token_invalid=Token bảo mật không hợp lệ
error.security.session_expired=Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.

# Generic errors
error.operation.failed=Thao tác thất bại: {0}
error.operation.timeout=Thao tác hết thời gian chờ sau {0} giây
error.operation.cancelled=Thao tác đã bị hủy: {0}
error.system.maintenance=Hệ thống đang bảo trì. Vui lòng thử lại sau.
error.feature.not_available=Tính năng {0} không khả dụng trong gói hiện tại của bạn
```

Bây giờ tôi sẽ cải tiến ErrorResponse DTO để hỗ trợ stack trace có điều kiện:

```java
package com.proptech.realestate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Unique error code for this type of error
     */
    private String errorCode;
    
    /**
     * Human-readable error message (localized)
     */
    private String message;
    
    /**
     * Additional error details (can be string, object, or list)
     */
    private Object details;
    
    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Request ID for tracing purposes
     */
    private String requestId;
    
    /**
     * Request path where the error occurred
     */
    private String path;
    
    /**
     * HTTP status code
     */
    private Integer status;
    
    /**
     * Stack trace information (only included if configured)
     */
    private StackTraceInfo stackTrace;
    
    /**
     * Additional metadata about the error context
     */
    private Map<String, Object> metadata;
    
    /**
     * Validation errors (for form validation)
     */
    private List<ValidationError> validationErrors;
    
    /**
     * Nested error response for chained exceptions
     */
    private ErrorResponse cause;
    
    /**
     * Stack trace information structure
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StackTraceInfo {
        
        /**
         * Exception class name
         */
        private String exceptionClass;
        
        /**
         * File where exception occurred
         */
        private String fileName;
        
        /**
         * Line number where exception occurred
         */
        private Integer lineNumber;
        
        /**
         * Method name where exception occurred
         */
        private String methodName;
        
        /**
         * Full stack trace as string array
         */
        private List<String> stackTraceElements;
        
        /**
         * Simplified stack trace (only application code)
         */
        private List<String> applicationStackTrace;
    }
    
    /**
     * Validation error structure
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        
        /**
         * Field name that failed validation
         */
        private String field;
        
        /**
         * Rejected value
         */
        private Object rejectedValue;
        
        /**
         * Validation error message
         */
        private String message;
        
        /**
         * Validation error code
         */
        private String code;
    }
    
    /**
     * Static factory methods for common error responses
     */
    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .build();
    }
    
    public static ErrorResponse of(String errorCode, String message, Object details) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .build();
    }
    
    public static ErrorResponse withStackTrace(String errorCode, String message, Throwable throwable) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .stackTrace(buildStackTraceInfo(throwable))
                .build();
    }
    
    public static ErrorResponse withValidationErrors(String errorCode, String message, 
                                                   List<ValidationError> validationErrors) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .validationErrors(validationErrors)
                .build();
    }
    
    /**
     * Build stack trace information from throwable
     */
    private static StackTraceInfo buildStackTraceInfo(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StackTraceElement[] elements = throwable.getStackTrace();
        if (elements.length == 0) {
            return null;
        }
        
        // Get the first stack trace element (where exception occurred)
        StackTraceElement firstElement = elements[0];
        
        // Build full stack trace
        List<String> fullStackTrace = java.util.Arrays.stream(elements)
                .map(StackTraceElement::toString)
                .collect(java.util.stream.Collectors.toList());
        
        // Build application-only stack trace (filter framework code)
        List<String> appStackTrace = java.util.Arrays.stream(elements)
                .filter(element -> element.getClassName().startsWith("com.proptech.realestate"))
                .map(StackTraceElement::toString)
                .collect(java.util.stream.Collectors.toList());
        
        return StackTraceInfo.builder()
                .exceptionClass(throwable.getClass().getSimpleName())
                .fileName(firstElement.getFileName())
                .lineNumber(firstElement.getLineNumber())
                .methodName(firstElement.getMethodName())
                .stackTraceElements(fullStackTrace)
                .applicationStackTrace(appStackTrace)
                .build();
    }
    
    /**
     * Add metadata to the error response
     */
    public ErrorResponse addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Set request context information
     */
    public ErrorResponse withRequestContext(String requestId, String path, Integer status) {
        this.requestId = requestId;
        this.path = path;
        this.status = status;
        return this;
    }
}
```

Bây giờ tôi sẽ cải tiến GlobalExceptionHandler để xử lý ConfigurableException và hỗ trợ cấu hình stack trace:

```java
package com.proptech.realestate.exception;

import com.proptech.realestate.dto.ApiResponse;
import com.proptech.realestate.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced Global Exception Handler with configurable stack traces,
 * internationalized messages, and comprehensive error logging
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // Configuration values for exception handling
    @Value("${app.exception.include-stack-trace.enabled:false}")
    private boolean globalStackTraceEnabled;

    @Value("${app.exception.include-stack-trace.include-in-dev-mode:true}")
    private boolean includeStackTraceInDev;

    @Value("${app.exception.detailed-errors.enabled:true}")
    private boolean detailedErrorsEnabled;

    @Value("${app.exception.detailed-errors.include-timestamp:true}")
    private boolean includeTimestamp;

    @Value("${app.exception.detailed-errors.include-request-id:true}")
    private boolean includeRequestId;

    @Value("${app.exception.detailed-errors.include-path:true}")
    private boolean includePath;

    @Value("${app.exception.logging.log-all-exceptions:true}")
    private boolean logAllExceptions;

    @Value("${app.exception.logging.include-stack-trace-in-logs:true}")
    private boolean includeStackTraceInLogs;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Handle ConfigurableException - the main custom exception type
     */
    @ExceptionHandler(ConfigurableException.class)
    public ResponseEntity<ApiResponse<Object>> handleConfigurableException(
            ConfigurableException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Configurable exception occurred");
        
        // Resolve the localized message
        String localizedMessage = resolveMessage(ex.getMessageKey(), ex.getMessageArgs(), request.getLocale());
        
        // Determine if stack trace should be included
        boolean includeStackTrace = shouldIncludeStackTrace(ex);
        
        // Build error response
        ErrorResponse errorResponse = buildErrorResponse(
            ex.getErrorCode(), 
            localizedMessage, 
            ex, 
            request, 
            requestId, 
            includeStackTrace
        );
        
        // Create API response
        ApiResponse<Object> apiResponse = ApiResponse.error(localizedMessage, ex.getErrorCode(), errorResponse);
        
        return new ResponseEntity<>(apiResponse, ex.getHttpStatus());
    }

    /**
     * Handle ResourceNotFoundException (legacy support)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Resource not found");
        
        String message = resolveMessage("error.resource.not_found", 
            new Object[]{ex.getResourceName(), ex.getFieldName(), ex.getFieldValue()}, 
            request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "RESOURCE_NOT_FOUND", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "RESOURCE_NOT_FOUND", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle InvalidRequestException (legacy support)
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRequestException(
            InvalidRequestException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Invalid request");
        
        String message = resolveMessage("error.bad_request", null, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "INVALID_REQUEST", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "INVALID_REQUEST", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle validation errors (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Validation error");
        
        String message = resolveMessage("error.validation.failed", null, request.getLocale());
        
        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::buildValidationError)
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
            "VALIDATION_ERROR", message, validationErrors)
            .withRequestContext(requestId, getRequestPath(request), HttpStatus.BAD_REQUEST.value());
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "VALIDATION_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle bind exceptions
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(
            BindException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Bind exception");
        
        String message = resolveMessage("error.validation.failed", null, request.getLocale());
        
        List<ErrorResponse.ValidationError> validationErrors = ex.getFieldErrors()
            .stream()
            .map(this::buildValidationError)
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
            "BIND_ERROR", message, validationErrors)
            .withRequestContext(requestId, getRequestPath(request), HttpStatus.BAD_REQUEST.value());
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "BIND_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Constraint violation");
        
        String message = resolveMessage("error.validation.constraint_violation", 
            new Object[]{ex.getMessage()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "CONSTRAINT_VIOLATION", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "CONSTRAINT_VIOLATION", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle database access exceptions
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Database access error");
        
        String message = resolveMessage("error.database.query_failed", 
            new Object[]{ex.getMostSpecificCause().getMessage()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "DATABASE_ERROR", message, ex, request, requestId, 
            shouldIncludeStackTraceForErrorCode("DATABASE_ERROR"));
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "DATABASE_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle data integrity violation exceptions
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Data integrity violation");
        
        String message = resolveMessage("error.database.data_integrity", 
            new Object[]{ex.getMostSpecificCause().getMessage()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "DATA_INTEGRITY_ERROR", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "DATA_INTEGRITY_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Access denied");
        
        String message = resolveMessage("error.security.access_denied", 
            new Object[]{getRequestPath(request)}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "ACCESS_DENIED", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "ACCESS_DENIED", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Method argument type mismatch");
        
        String message = resolveMessage("error.validation.invalid_format", 
            new Object[]{ex.getName(), ex.getRequiredType().getSimpleName()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "INVALID_ARGUMENT_TYPE", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "INVALID_ARGUMENT_TYPE", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Unhandled exception occurred");
        
        String message = resolveMessage("error.internal_server", null, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "INTERNAL_SERVER_ERROR", message, ex, request, requestId, 
            shouldIncludeStackTraceForErrorCode("INTERNAL_SERVER_ERROR"));
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "INTERNAL_SERVER_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Build comprehensive error response
     */
    private ErrorResponse buildErrorResponse(String errorCode, String message, Throwable throwable, 
                                           WebRequest request, String requestId, boolean includeStackTrace) {
        
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message);
        
        // Add request context if enabled
        if (detailedErrorsEnabled) {
            if (includeRequestId) {
                builder.requestId(requestId);
            }
            if (includePath) {
                builder.path(getRequestPath(request));
            }
            if (includeTimestamp) {
                // Timestamp is included by default via @Builder.Default
            }
        }
        
        // Add stack trace if configured
        if (includeStackTrace && throwable != null) {
            builder.stackTrace(buildStackTraceInfo(throwable));
        }
        
        // Add cause if present
        if (throwable.getCause() != null) {
            builder.cause(buildErrorResponse(
                "CAUSED_BY", 
                throwable.getCause().getMessage(), 
                throwable.getCause(), 
                request, 
                requestId, 
                false // Don't include stack trace for causes
            ));
        }
        
        return builder.build();
    }

    /**
     * Build stack trace information
     */
    private ErrorResponse.StackTraceInfo buildStackTraceInfo(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StackTraceElement[] elements = throwable.getStackTrace();
        if (elements.length == 0) {
            return null;
        }
        
        StackTraceElement firstElement = elements[0];
        
        List<String> fullStackTrace = java.util.Arrays.stream(elements)
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
        
        List<String> appStackTrace = java.util.Arrays.stream(elements)
                .filter(element -> element.getClassName().startsWith("com.proptech.realestate"))
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
        
        return ErrorResponse.StackTraceInfo.builder()
                .exceptionClass(throwable.getClass().getSimpleName())
                .fileName(firstElement.getFileName())
                .lineNumber(firstElement.getLineNumber())
                .methodName(firstElement.getMethodName())
                .stackTraceElements(fullStackTrace)
                .applicationStackTrace(appStackTrace)
                .build();
    }

    /**
     * Build validation error from field error
     */
    private ErrorResponse.ValidationError buildValidationError(FieldError fieldError) {
        return ErrorResponse.ValidationError.builder()
            .field(fieldError.getField())
            .rejectedValue(fieldError.getRejectedValue())
            .message(fieldError.getDefaultMessage())
            .code(fieldError.getCode())
            .build();
    }

    /**
     * Resolve message from message source with fallback
     */
    private String resolveMessage(String messageKey, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(messageKey, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message key not found: {}", messageKey);
            return messageKey; // Fallback to key itself
        }
    }

    /**
     * Determine if stack trace should be included for ConfigurableException
     */
    private boolean shouldIncludeStackTrace(ConfigurableException ex) {
        // Exception-specific setting takes precedence
        if (ex.isIncludeStackTrace()) {
            return true;
        }
        
        // Check if it's configured for specific error codes
        if (shouldIncludeStackTraceForErrorCode(ex.getErrorCode())) {
            return true;
        }
        
        // Check development mode setting
        if (includeStackTraceInDev && isDevelopmentMode()) {
            return true;
        }
        
        // Global setting
        return globalStackTraceEnabled;
    }

    /**
     * Check if stack trace should be included for specific error code
     */
    private boolean shouldIncludeStackTraceForErrorCode(String errorCode) {
        // This could be made configurable via properties
        return errorCode != null && (
            errorCode.equals("INTERNAL_SERVER_ERROR") ||
            errorCode.equals("DATABASE_ERROR") ||
            errorCode.equals("EXTERNAL_SERVICE_ERROR")
        );
    }

    /**
     * Check if application is running in development mode
     */
    private boolean isDevelopmentMode() {
        return "dev".equals(activeProfile) || "development".equals(activeProfile) || "local".equals(activeProfile);
    }

    /**
     * Generate unique request ID for tracing
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get request path from WebRequest
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
            return httpRequest.getRequestURI();
        }
        return request.getDescription(false);
    }

    /**
     * Log exception with appropriate level and detail
     */
    private void logException(Throwable throwable, String requestId, String context) {
        if (!logAllExceptions) {
            return;
        }
        
        String logMessage = "{} [RequestID: {}]: {}";
        
        if (throwable instanceof ConfigurableException) {
            // Handled exceptions - use WARN level
            if (includeStackTraceInLogs) {
                log.warn(logMessage, context, requestId, throwable.getMessage(), throwable);
            } else {
                log.warn(logMessage, context, requestId, throwable.getMessage());
            }
        } else {
            // Unhandled exceptions - use ERROR level
            if (includeStackTraceInLogs) {
                log.error(logMessage, context, requestId, throwable.getMessage(), throwable);
            } else {
                log.error(logMessage, context, requestId, throwable.getMessage());
            }
        }
    }
}
```

Cuối cùng, tôi sẽ tạo một service demo để minh họa cách sử dụng hệ thống exception mới:

```java
package com.proptech.realestate.service;

import com.proptech.realestate.exception.ConfigurableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Demo service showcasing how to use ConfigurableException
 * with message keys, parameters, and configurable stack traces
 */
@Service
@Slf4j
public class DemoExceptionService {

    private final Random random = new Random();

    /**
     * Demo: Simple exception with message key only
     */
    public void throwSimpleException() {
        log.info("Demonstrating simple exception with message key");
        throw new ConfigurableException("error.unauthorized");
    }

    /**
     * Demo: Exception with message key and parameters
     */
    public void throwExceptionWithParameters(String propertyId) {
        log.info("Demonstrating exception with parameters for property: {}", propertyId);
        throw new ConfigurableException(
            "error.property.not_found", 
            new Object[]{propertyId}
        );
    }

    /**
     * Demo: Exception with custom HTTP status
     */
    public void throwExceptionWithCustomStatus(String email) {
        log.info("Demonstrating exception with custom HTTP status for email: {}", email);
        throw new ConfigurableException(
            "error.validation.invalid_email",
            new Object[]{email},
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }

    /**
     * Demo: Exception with builder pattern
     */
    public void throwExceptionWithBuilder(String resourceName, String fieldName, Object fieldValue) {
        log.info("Demonstrating exception with builder pattern");
        throw ConfigurableException.builder()
            .messageKey("error.resource.not_found")
            .messageArgs(new Object[]{resourceName, fieldName, fieldValue})
            .httpStatus(HttpStatus.NOT_FOUND)
            .errorCode("CUSTOM_NOT_FOUND")
            .includeStackTrace(true) // Force include stack trace for this exception
            .build();
    }

    /**
     * Demo: Static factory method - Not Found
     */
    public void throwNotFoundUsingFactory(String propertyId) {
        log.info("Demonstrating static factory method for not found");
        throw ConfigurableException.notFound("Property", "ID", propertyId);
    }

    /**
     * Demo: Static factory method - Bad Request
     */
    public void throwBadRequestUsingFactory(String invalidPrice) {
        log.info("Demonstrating static factory method for bad request");
        throw ConfigurableException.badRequest(
            "error.property.invalid_price", 
            invalidPrice
        );
    }

    /**
     * Demo: Static factory method - Unauthorized
     */
    public void throwUnauthorizedUsingFactory() {
        log.info("Demonstrating static factory method for unauthorized");
        throw ConfigurableException.unauthorized("error.user.invalid_credentials");
    }

    /**
     * Demo: Static factory method - Internal Error with cause
     */
    public void throwInternalErrorWithCause() {
        log.info("Demonstrating internal error with cause");
        try {
            // Simulate some operation that fails
            simulateFailingOperation();
        } catch (Exception cause) {
            throw ConfigurableException.internalError("error.database.connection_failed", cause);
        }
    }

    /**
     * Demo: Business logic validation with detailed error
     */
    public void validatePropertyPrice(BigDecimal price) {
        log.info("Validating property price: {}", price);
        
        if (price == null) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "price");
        }
        
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw ConfigurableException.builder()
                .messageKey("error.validation.value_out_of_range")
                .messageArgs(new Object[]{"price", "0", "1000000000"})
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("INVALID_PRICE_RANGE")
                .build();
        }
        
        if (price.compareTo(new BigDecimal("1000000000")) > 0) {
            throw ConfigurableException.builder()
                .messageKey("error.validation.value_out_of_range")
                .messageArgs(new Object[]{"price", "0", "1000000000"})
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("PRICE_TOO_HIGH")
                .build();
        }
        
        log.info("Property price validation passed");
    }

    /**
     * Demo: MLS operation with different error scenarios
     */
    public void performMLSOperation(String feedId) {
        log.info("Performing MLS operation for feed: {}", feedId);
        
        if (feedId == null || feedId.trim().isEmpty()) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "feedId");
        }
        
        // Simulate different error scenarios based on feedId
        switch (feedId.toLowerCase()) {
            case "not-found":
                throw ConfigurableException.notFound("MLS Feed", "ID", feedId);
                
            case "auth-failed":
                throw ConfigurableException.builder()
                    .messageKey("error.mls.authentication_failed")
                    .messageArgs(new Object[]{feedId})
                    .httpStatus(HttpStatus.UNAUTHORIZED)
                    .errorCode("MLS_AUTH_FAILED")
                    .build();
                    
            case "rate-limit":
                throw ConfigurableException.builder()
                    .messageKey("error.mls.rate_limit_exceeded")
                    .messageArgs(new Object[]{feedId})
                    .httpStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .errorCode("MLS_RATE_LIMIT")
                    .build();
                    
            case "data-error":
                throw ConfigurableException.builder()
                    .messageKey("error.mls.data_format_invalid")
                    .messageArgs(new Object[]{"Invalid JSON structure"})
                    .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                    .errorCode("MLS_DATA_INVALID")
                    .includeStackTrace(true) // Include stack trace for debugging
                    .build();
                    
            case "system-error":
                // Simulate a system error with cause
                try {
                    simulateFailingOperation();
                } catch (Exception cause) {
                    throw ConfigurableException.builder()
                        .messageKey("error.mls.sync_failed")
                        .messageArgs(new Object[]{feedId, cause.getMessage()})
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .errorCode("MLS_SYNC_FAILED")
                        .includeStackTrace(true)
                        .cause(cause)
                        .build();
                }
                break;
                
            default:
                log.info("MLS operation completed successfully for feed: {}", feedId);
        }
    }

    /**
     * Demo: Search operation with Elasticsearch errors
     */
    public void performSearch(String query) {
        log.info("Performing search with query: {}", query);
        
        if (query == null || query.trim().isEmpty()) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "query");
        }
        
        if (query.length() < 3) {
            throw ConfigurableException.builder()
                .messageKey("error.search.query_invalid")
                .messageArgs(new Object[]{"Query must be at least 3 characters long"})
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("QUERY_TOO_SHORT")
                .build();
        }
        
        // Simulate Elasticsearch unavailable
        if (query.contains("elasticsearch-down")) {
            throw ConfigurableException.builder()
                .messageKey("error.search.elasticsearch_unavailable")
                .httpStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .errorCode("SEARCH_SERVICE_UNAVAILABLE")
                .includeStackTrace(true)
                .build();
        }
        
        // Simulate too many results
        if (query.contains("*") || query.length() < 4) {
            throw ConfigurableException.builder()
                .messageKey("error.search.too_many_results")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("TOO_MANY_RESULTS")
                .build();
        }
        
        log.info("Search completed successfully");
    }

    /**
     * Demo: File upload with various validation scenarios
     */
    public void validateFileUpload(String fileName, long fileSize, String contentType) {
        log.info("Validating file upload: {} (size: {}, type: {})", fileName, fileSize, contentType);
        
        if (fileName == null || fileName.trim().isEmpty()) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "fileName");
        }
        
        // Check file extension
        if (!fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|pdf|doc|docx)$")) {
            throw ConfigurableException.builder()
                .messageKey("error.file.invalid_format")
                .messageArgs(new Object[]{"jpg, jpeg, png, pdf, doc, docx"})
                .httpStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .errorCode("INVALID_FILE_FORMAT")
                .build();
        }
        
        // Check file size (10MB limit)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (fileSize > maxSize) {
            throw ConfigurableException.builder()
                .messageKey("error.file.size_exceeded")
                .messageArgs(new Object[]{"10MB"})
                .httpStatus(HttpStatus.PAYLOAD_TOO_LARGE)
                .errorCode("FILE_TOO_LARGE")
                .build();
        }
        
        log.info("File validation passed");
    }

    /**
     * Demo: Random exception scenario for testing
     */
    public void throwRandomException() {
        log.info("Throwing random exception for testing");
        
        int scenario = random.nextInt(5);
        
        switch (scenario) {
            case 0:
                throwSimpleException();
                break;
            case 1:
                throwExceptionWithParameters("PROP-" + random.nextInt(1000));
                break;
            case 2:
                throwNotFoundUsingFactory("USER-" + random.nextInt(1000));
                break;
            case 3:
                performMLSOperation("rate-limit");
                break;
            case 4:
                throwInternalErrorWithCause();
                break;
            default:
                log.info("No exception thrown in random scenario");
        }
    }

    /**
     * Simulate a failing operation for demonstration
     */
    private void simulateFailingOperation() {
        throw new RuntimeException("Simulated database connection failure");
    }
}
```

Bây giờ tôi sẽ tạo một controller demo để test hệ thống exception mới:

```java
package com.proptech.realestate.controller;

import com.proptech.realestate.service.DemoExceptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Demo controller for testing the new ConfigurableException system
 * Provides various endpoints to trigger different exception scenarios
 */
@RestController
@RequestMapping("/api/demo/exceptions")
@RequiredArgsConstructor
@Slf4j
public class DemoExceptionController {

    private final DemoExceptionService demoService;

    /**
     * Test simple exception with message key only
     * GET /api/demo/exceptions/simple
     */
    @GetMapping("/simple")
    public String testSimpleException() {
        demoService.throwSimpleException();
        return "This should not be reached";
    }

    /**
     * Test exception with parameters
     * GET /api/demo/exceptions/with-params?propertyId=PROP123
     */
    @GetMapping("/with-params")
    public String testExceptionWithParameters(@RequestParam(defaultValue = "PROP123") String propertyId) {
        demoService.throwExceptionWithParameters(propertyId);
        return "This should not be reached";
    }

    /**
     * Test exception with custom HTTP status
     * GET /api/demo/exceptions/custom-status?email=invalid-email
     */
    @GetMapping("/custom-status")
    public String testExceptionWithCustomStatus(@RequestParam(defaultValue = "invalid-email") String email) {
        demoService.throwExceptionWithCustomStatus(email);
        return "This should not be reached";
    }

    /**
     * Test exception with builder pattern
     * GET /api/demo/exceptions/builder?resource=User&field=email&value=test@example.com
     */
    @GetMapping("/builder")
    public String testExceptionWithBuilder(
            @RequestParam(defaultValue = "User") String resource,
            @RequestParam(defaultValue = "email") String field,
            @RequestParam(defaultValue = "test@example.com") String value) {
        demoService.throwExceptionWithBuilder(resource, field, value);
        return "This should not be reached";
    }

    /**
     * Test static factory method - Not Found
     * GET /api/demo/exceptions/not-found?propertyId=PROP456
     */
    @GetMapping("/not-found")
    public String testNotFoundFactory(@RequestParam(defaultValue = "PROP456") String propertyId) {
        demoService.throwNotFoundUsingFactory(propertyId);
        return "This should not be reached";
    }

    /**
     * Test static factory method - Bad Request
     * GET /api/demo/exceptions/bad-request?price=invalid-price
     */
    @GetMapping("/bad-request")
    public String testBadRequestFactory(@RequestParam(defaultValue = "invalid-price") String price) {
        demoService.throwBadRequestUsingFactory(price);
        return "This should not be reached";
    }

    /**
     * Test static factory method - Unauthorized
     * GET /api/demo/exceptions/unauthorized
     */
    @GetMapping("/unauthorized")
    public String testUnauthorizedFactory() {
        demoService.throwUnauthorizedUsingFactory();
        return "This should not be reached";
    }

    /**
     * Test internal error with cause
     * GET /api/demo/exceptions/internal-error
     */
    @GetMapping("/internal-error")
    public String testInternalErrorWithCause() {
        demoService.throwInternalErrorWithCause();
        return "This should not be reached";
    }

    /**
     * Test property price validation
     * GET /api/demo/exceptions/validate-price?price=1500000
     */
    @GetMapping("/validate-price")
    public String testPropertyPriceValidation(@RequestParam(required = false) BigDecimal price) {
        demoService.validatePropertyPrice(price);
        return "Price validation passed: " + price;
    }

    /**
     * Test MLS operation with different scenarios
     * GET /api/demo/exceptions/mls?feedId=not-found
     * 
     * Available scenarios:
     * - not-found: Resource not found
     * - auth-failed: Authentication failed
     * - rate-limit: Rate limit exceeded
     * - data-error: Data format invalid
     * - system-error: System error with cause
     * - success: Normal operation (no exception)
     */
    @GetMapping("/mls")
    public String testMLSOperation(@RequestParam(defaultValue = "success") String feedId) {
        demoService.performMLSOperation(feedId);
        return "MLS operation completed successfully for feed: " + feedId;
    }

    /**
     * Test search operation
     * GET /api/demo/exceptions/search?query=property
     * 
     * Special queries that trigger errors:
     * - "" (empty): Required field missing
     * - "ab" (too short): Query too short
     * - "elasticsearch-down": Service unavailable
     * - "*" or short queries: Too many results
     */
    @GetMapping("/search")
    public String testSearchOperation(@RequestParam(defaultValue = "property") String query) {
        demoService.performSearch(query);
        return "Search completed successfully for query: " + query;
    }

    /**
     * Test file upload validation
     * POST /api/demo/exceptions/upload
     */
    @PostMapping("/upload")
    public String testFileUploadValidation(
            @RequestParam(defaultValue = "document.pdf") String fileName,
            @RequestParam(defaultValue = "1048576") long fileSize,
            @RequestParam(defaultValue = "application/pdf") String contentType) {
        
        demoService.validateFileUpload(fileName, fileSize, contentType);
        return String.format("File validation passed: %s (size: %d, type: %s)", 
            fileName, fileSize, contentType);
    }

    /**
     * Test random exception scenario
     * GET /api/demo/exceptions/random
     */
    @GetMapping("/random")
    public String testRandomException() {
        demoService.throwRandomException();
        return "Random exception test completed (no exception thrown)";
    }

    /**
     * Test all exception scenarios sequentially
     * GET /api/demo/exceptions/test-all
     */
    @GetMapping("/test-all")
    public String testAllScenarios() {
        log.info("Testing all exception scenarios...");
        
        try {
            demoService.throwSimpleException();
        } catch (Exception e) {
            log.info("Caught simple exception: {}", e.getMessage());
        }
        
        try {
            demoService.throwExceptionWithParameters("TEST-PROP");
        } catch (Exception e) {
            log.info("Caught exception with parameters: {}", e.getMessage());
        }
        
        try {
            demoService.throwNotFoundUsingFactory("TEST-USER");
        } catch (Exception e) {
            log.info("Caught not found exception: {}", e.getMessage());
        }
        
        try {
            demoService.performMLSOperation("rate-limit");
        } catch (Exception e) {
            log.info("Caught MLS exception: {}", e.getMessage());
        }
        
        try {
            demoService.validatePropertyPrice(BigDecimal.valueOf(-1000));
        } catch (Exception e) {
            log.info("Caught price validation exception: {}", e.getMessage());
        }
        
        return "All exception scenarios tested successfully. Check logs for details.";
    }

    /**
     * Generate documentation for available test endpoints
     * GET /api/demo/exceptions/help
     */
    @GetMapping("/help")
    public String getHelp() {
        return """
            ConfigurableException Demo Endpoints:
            
            1. GET /api/demo/exceptions/simple
               - Tests simple exception with message key only
            
            2. GET /api/demo/exceptions/with-params?propertyId=PROP123
               - Tests exception with parameters
            
            3. GET /api/demo/exceptions/custom-status?email=invalid-email
               - Tests exception with custom HTTP status
            
            4. GET /api/demo/exceptions/builder?resource=User&field=email&value=test@example.com
               - Tests exception with builder pattern
            
            5. GET /api/demo/exceptions/not-found?propertyId=PROP456
               - Tests static factory method - Not Found
            
            6. GET /api/demo/exceptions/bad-request?price=invalid-price
               - Tests static factory method - Bad Request
            
            7. GET /api/demo/exceptions/unauthorized
               - Tests static factory method - Unauthorized
            
            8. GET /api/demo/exceptions/internal-error
               - Tests internal error with cause
            
            9. GET /api/demo/exceptions/validate-price?price=1500000
               - Tests property price validation (try negative or null values)
            
            10. GET /api/demo/exceptions/mls?feedId=not-found
                - Tests MLS operations (feedId: not-found, auth-failed, rate-limit, data-error, system-error)
            
            11. GET /api/demo/exceptions/search?query=property
                - Tests search operations (try: empty, "ab", "elasticsearch-down", "*")
            
            12. POST /api/demo/exceptions/upload?fileName=document.pdf&fileSize=1048576&contentType=application/pdf
                - Tests file upload validation
            
            13. GET /api/demo/exceptions/random
                - Tests random exception scenario
            
            14. GET /api/demo/exceptions/test-all
                - Tests all scenarios sequentially (safe - catches exceptions)
            
            Configuration:
            - Stack traces can be controlled via application.yml
            - Messages are internationalized (supports Vietnamese via Accept-Language header)
            - Request IDs are generated for tracing
            - Detailed error information is configurable
            """;
    }
}
```

Cuối cùng, tôi sẽ thêm một số message key còn thiếu vào file properties:

```java-properties
error.unauthorized=Authentication failed. Please check your credentials.
error.internal_server=An unexpected internal server error has occurred. Please contact support.
error.bad_request=The request is invalid. Please check the input data.
error.resource.not_found={0} not found with {1}: ''{2}''
validation.field.required={0} is a required field.

# Validation errors
error.validation.failed=Validation failed. Please check your input data.
error.validation.constraint_violation=Constraint violation: {0}
error.validation.invalid_format=Invalid format for {0}. Expected format: {1}
error.validation.value_out_of_range={0} must be between {1} and {2}
error.validation.required_field_missing=Required field {0} is missing
error.validation.invalid_email=Invalid email address format: {0}
error.validation.invalid_phone=Invalid phone number format: {0}
error.validation.password_too_weak=Password does not meet security requirements

# Database related errors
error.database.connection_failed=Failed to connect to database. Please try again later.
error.database.query_failed=Database query failed: {0}
error.database.constraint_violation=Database constraint violation: {0}
error.database.data_integrity=Data integrity error: {0}

# Business logic errors
error.business.duplicate_entry=Duplicate entry found for {0}: {1}
error.business.invalid_operation=Invalid operation: {0}
error.business.insufficient_permissions=Insufficient permissions to perform this action.
error.business.operation_not_allowed=Operation not allowed for {0} in current state.

# Property related errors
error.property.not_found=Property not found with ID: {0}
error.property.not_available=Property {0} is not available for {1}
error.property.invalid_price=Invalid price format: {0}
error.property.invalid_status=Invalid property status: {0}
error.property.address_required=Property address is required
error.property.listing_expired=Property listing has expired: {0}

# User/Agent related errors
error.user.not_found=User not found with {0}: {1}
error.user.account_disabled=User account is disabled: {0}
error.user.account_locked=User account is locked: {0}
error.user.invalid_credentials=Invalid username or password
error.agent.not_found=Agent not found with ID: {0}
error.agent.license_expired=Agent license has expired: {0}

# MLS related errors
error.mls.feed_not_found=MLS feed not found with ID: {0}
error.mls.sync_failed=MLS sync failed for feed {0}: {1}
error.mls.authentication_failed=MLS authentication failed for {0}
error.mls.data_format_invalid=Invalid MLS data format: {0}
error.mls.rate_limit_exceeded=MLS API rate limit exceeded for {0}

# Search related errors
error.search.query_invalid=Invalid search query: {0}
error.search.elasticsearch_unavailable=Search service is temporarily unavailable
error.search.too_many_results=Search returned too many results. Please refine your query.
error.search.index_not_found=Search index not found: {0}

# File/Upload related errors
error.file.not_found=File not found: {0}
error.file.upload_failed=File upload failed: {0}
error.file.invalid_format=Invalid file format. Expected: {0}
error.file.size_exceeded=File size exceeds maximum allowed size of {0}
error.file.processing_failed=File processing failed: {0}

# External service errors
error.external.service_unavailable=External service {0} is currently unavailable
error.external.api_error=External API error from {0}: {1}
error.external.timeout=Request to {0} timed out after {1} seconds
error.external.authentication_failed=Authentication failed for external service: {0}

# Configuration errors
error.config.invalid_value=Invalid configuration value for {0}: {1}
error.config.missing_property=Missing required configuration property: {0}
error.config.initialization_failed=Configuration initialization failed: {0}

# Security errors
error.security.access_denied=Access denied for resource: {0}
error.security.token_expired=Security token has expired
error.security.token_invalid=Invalid security token
error.security.session_expired=Session has expired. Please login again.

# Generic errors
error.operation.failed=Operation failed: {0}
error.operation.timeout=Operation timed out after {0} seconds
error.operation.cancelled=Operation was cancelled: {0}
error.system.maintenance=System is under maintenance. Please try again later.
error.feature.not_available=Feature {0} is not available in your current plan
```

Bây giờ tôi cũng cần cập nhật bản dịch tiếng Việt:

```java-properties
error.unauthorized=Xác thực thất bại. Vui lòng kiểm tra lại thông tin đăng nhập.
error.internal_server=Đã có lỗi hệ thống ngoài ý muốn xảy ra. Vui lòng liên hệ hỗ trợ.
error.bad_request=Yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu đầu vào.
error.resource.not_found=Không tìm thấy tài nguyên {0} với {1}: ''{2}''
validation.field.required={0} là trường bắt buộc.

# Validation errors
error.validation.failed=Xác thực thất bại. Vui lòng kiểm tra lại dữ liệu đầu vào.
error.validation.constraint_violation=Vi phạm ràng buộc: {0}
error.validation.invalid_format=Định dạng không hợp lệ cho {0}. Định dạng mong đợi: {1}
error.validation.value_out_of_range={0} phải nằm trong khoảng từ {1} đến {2}
error.validation.required_field_missing=Thiếu trường bắt buộc {0}
error.validation.invalid_email=Định dạng email không hợp lệ: {0}
error.validation.invalid_phone=Định dạng số điện thoại không hợp lệ: {0}
error.validation.password_too_weak=Mật khẩu không đáp ứng yêu cầu bảo mật

# Database related errors
error.database.connection_failed=Không thể kết nối cơ sở dữ liệu. Vui lòng thử lại sau.
error.database.query_failed=Truy vấn cơ sở dữ liệu thất bại: {0}
error.database.constraint_violation=Vi phạm ràng buộc cơ sở dữ liệu: {0}
error.database.data_integrity=Lỗi tính toàn vẹn dữ liệu: {0}

# Business logic errors
error.business.duplicate_entry=Phát hiện bản ghi trùng lặp cho {0}: {1}
error.business.invalid_operation=Thao tác không hợp lệ: {0}
error.business.insufficient_permissions=Không đủ quyền để thực hiện hành động này.
error.business.operation_not_allowed=Không được phép thực hiện thao tác cho {0} trong trạng thái hiện tại.

# Property related errors
error.property.not_found=Không tìm thấy bất động sản với ID: {0}
error.property.not_available=Bất động sản {0} không khả dụng cho {1}
error.property.invalid_price=Định dạng giá không hợp lệ: {0}
error.property.invalid_status=Trạng thái bất động sản không hợp lệ: {0}
error.property.address_required=Địa chỉ bất động sản là bắt buộc
error.property.listing_expired=Tin đăng bất động sản đã hết hạn: {0}

# User/Agent related errors
error.user.not_found=Không tìm thấy người dùng với {0}: {1}
error.user.account_disabled=Tài khoản người dùng đã bị vô hiệu hóa: {0}
error.user.account_locked=Tài khoản người dùng đã bị khóa: {0}
error.user.invalid_credentials=Tên đăng nhập hoặc mật khẩu không đúng
error.agent.not_found=Không tìm thấy đại lý với ID: {0}
error.agent.license_expired=Giấy phép đại lý đã hết hạn: {0}

# MLS related errors
error.mls.feed_not_found=Không tìm thấy nguồn dữ liệu MLS với ID: {0}
error.mls.sync_failed=Đồng bộ MLS thất bại cho nguồn {0}: {1}
error.mls.authentication_failed=Xác thực MLS thất bại cho {0}
error.mls.data_format_invalid=Định dạng dữ liệu MLS không hợp lệ: {0}
error.mls.rate_limit_exceeded=Vượt quá giới hạn tần suất API MLS cho {0}

# Search related errors
error.search.query_invalid=Truy vấn tìm kiếm không hợp lệ: {0}
error.search.elasticsearch_unavailable=Dịch vụ tìm kiếm tạm thời không khả dụng
error.search.too_many_results=Tìm kiếm trả về quá nhiều kết quả. Vui lòng làm rõ truy vấn.
error.search.index_not_found=Không tìm thấy chỉ mục tìm kiếm: {0}

# File/Upload related errors
error.file.not_found=Không tìm thấy tệp: {0}
error.file.upload_failed=Tải lên tệp thất bại: {0}
error.file.invalid_format=Định dạng tệp không hợp lệ. Mong đợi: {0}
error.file.size_exceeded=Kích thước tệp vượt quá giới hạn tối đa {0}
error.file.processing_failed=Xử lý tệp thất bại: {0}

# External service errors
error.external.service_unavailable=Dịch vụ bên ngoài {0} hiện không khả dụng
error.external.api_error=Lỗi API bên ngoài từ {0}: {1}
error.external.timeout=Yêu cầu tới {0} hết thời gian chờ sau {1} giây
error.external.authentication_failed=Xác thực thất bại cho dịch vụ bên ngoài: {0}

# Configuration errors
error.config.invalid_value=Giá trị cấu hình không hợp lệ cho {0}: {1}
error.config.missing_property=Thiếu thuộc tính cấu hình bắt buộc: {0}
error.config.initialization_failed=Khởi tạo cấu hình thất bại: {0}

# Security errors
error.security.access_denied=Từ chối truy cập tài nguyên: {0}
error.security.token_expired=Token bảo mật đã hết hạn
error.security.token_invalid=Token bảo mật không hợp lệ
error.security.session_expired=Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.

# Generic errors
error.operation.failed=Thao tác thất bại: {0}
error.operation.timeout=Thao tác hết thời gian chờ sau {0} giây
error.operation.cancelled=Thao tác đã bị hủy: {0}
error.system.maintenance=Hệ thống đang bảo trì. Vui lòng thử lại sau.
error.feature.not_available=Tính năng {0} không khả dụng trong gói hiện tại của bạn
```

Tuyệt vời! Tôi đã hoàn thành việc triển khai hệ thống xử lý exception nâng cao cho dự án Spring Boot real-estate của bạn. Dưới đây là tóm tắt những gì đã được tạo ra:

## 🎯 **Hệ thống Exception Đã Triển khai**

### **1. ConfigurableException Class**
- **File**: `real-estate/src/main/java/com/proptech/realestate/exception/ConfigurableException.java`
- **Tính năng**:
  - Sử dụng message key thay vì message tĩnh
  - Hỗ trợ

---

**User**

Tôi đang dựng 1 hệ thóng mls tại folder real-estate. Tôi có thể học hỏi và thêm tính năng gì cho dự án nữa. Dựa vào dự án opensource có sẵn php này 

---

**Cursor**

Chào bạn, đây là một câu hỏi rất hay và là một cách tuyệt vời để phát triển dự án của bạn. Việc tham khảo một dự án mã nguồn mở đã hoàn thiện (dù là trên nền tảng PHP) sẽ cung cấp cho bạn rất nhiều ý tưởng về các tính năng cần có cho một hệ thống MLS (Multiple Listing Service) hoàn chỉnh.

Dựa vào cấu trúc của dự án OpenRealEstate PHP, tôi sẽ phân tích các tính năng chính và đề xuất cách bạn có thể triển khai chúng một cách hiện đại và hiệu quả trong dự án Spring Boot của mình.

Đầu tiên, để có cái nhìn tổng quan về các module chức năng của dự án PHP, tôi sẽ liệt kê các module, components và controllers chính của nó.

Tôi sẽ bắt đầu bằng việc khám phá thư mục `protected/modules`.

Rất tiếc, có vẻ như có sự cố khi liệt kê thư mục. Tuy nhiên, dựa vào cấu trúc file ban đầu và kinh nghiệm với các hệ thống tương tự, tôi có thể suy luận và đưa ra một kế hoạch chi tiết cho bạn.

Dự án OpenRealEstate PHP là một hệ thống rất đầy đủ tính năng. Chúng ta có thể học hỏi rất nhiều từ nó để nâng cấp hệ thống MLS Spring Boot của bạn. Dưới đây là phân tích chi tiết các nhóm tính năng và cách triển khai chúng.

### **Phân Tích & Kế Hoạch Nâng Cấp Tính Năng**

Đây là những nhóm tính năng chính mà dự án PHP có và bạn nên xem xét thêm vào dự án Spring Boot của mình:

#### **1. Nhóm Tính năng Quản lý Người dùng & Phân quyền (Users & Access Control)**

*   **Hiện trạng ở dự án PHP:** Thường có module `users` để quản lý tài khoản (đăng ký, đăng nhập, quên mật khẩu, hồ sơ người dùng), và phân quyền giữa các loại tài khoản (người dùng thường, đại lý, quản trị viên).
*   **Tại sao cần thiết:** Đây là tính năng cốt lõi của mọi hệ thống MLS, cho phép phân biệt giữa người bán, người mua, và người quản trị.
*   **Đề xuất cho dự án Spring Boot:**
    *   **Công nghệ:** Sử dụng **Spring Security** một cách toàn diện.
    *   **Triển khai:**
        1.  **Xác thực (Authentication):** Cấu hình `SecurityFilterChain` để quản lý các trang công khai (`/`, `/login`, `/register`) và các trang cần đăng nhập.
        2.  **Phân quyền (Authorization):** Sử dụng `Role-based` hoặc `Permission-based` authorization. Định nghĩa các `Enum` cho `Role` (ví dụ: `ROLE_USER`, `ROLE_AGENT`, `ROLE_ADMIN`).
        3.  **Bảo mật mật khẩu:** Sử dụng `BCryptPasswordEncoder` để mã hóa mật khẩu người dùng.
        4.  **Hồ sơ người dùng:** Tạo `UserProfile` entity liên kết với `User` entity để lưu các thông tin chi tiết (ảnh đại diện, mô tả, thông tin liên hệ).
        5.  **API Endpoints:** Xây dựng các REST endpoints trong một `UserController` để quản lý thông tin người dùng.

#### **2. Nhóm Tính năng Trả phí & Dịch vụ (Paid Services & Payments)**

*   **Hiện trạng ở dự án PHP:** Có module `paidservices` cho phép quản trị viên tạo các gói dịch vụ (ví dụ: tin đăng nổi bật, đẩy tin lên đầu) và người dùng có thể mua chúng. Tích hợp với các cổng thanh toán.
*   **Tại sao cần thiết:** Đây là cách để thương mại hóa hệ thống MLS của bạn.
*   **Đề xuất cho dự án Spring Boot:**
    *   **Công nghệ:** Tích hợp với các cổng thanh toán hiện đại như **Stripe**, **PayPal**, hoặc các cổng nội địa (MoMo, VNPAY) qua API.
    *   **Triển khai:**
        1.  **Entities:** Tạo các entity `ServicePlan` (gói dịch vụ), `UserSubscription` (lưu lại gói người dùng đã mua), `PaymentTransaction` (lịch sử giao dịch).
        2.  **Service Logic:** Tạo `PaymentService` để xử lý logic thanh toán, giao tiếp với API của cổng thanh toán.
        3.  **Lịch trình (Scheduler):** Sử dụng `@Scheduled` của Spring để kiểm tra và xử lý các gói dịch vụ hết hạn.
        4.  **API Endpoints:** Tạo `PaymentController` để người dùng có thể lấy danh sách gói, tạo yêu cầu thanh toán, và xử lý kết quả trả về từ cổng thanh toán.

#### **3. Nhóm Tính năng Tương tác & Thông báo (Messaging & Notifications)**

*   **Hiện trạng ở dự án PHP:** Có các module `messages`, `comments`, `reviews`, `notifier` để người dùng nhắn tin cho nhau, để lại bình luận/đánh giá trên các tin đăng, và nhận thông báo hệ thống.
*   **Tại sao cần thiết:** Tăng cường sự tương tác và giữ chân người dùng.
*   **Đề xuất cho dự án Spring Boot:**
    *   **Công nghệ:** **Spring WebSocket** (với STOMP) cho real-time chat, **JavaMailSender** cho email, và tích hợp **Firebase Cloud Messaging (FCM)** cho push notification trên di động.
    *   **Triển khai:**
        1.  **Real-time Chat:** Cấu hình `WebSocketMessageBrokerConfigurer` để tạo các endpoint cho việc chat real-time giữa người mua và người bán.
        2.  **Reviews & Comments:** Tạo `ReviewController` với các endpoint RESTful `GET`, `POST`, `PUT`, `DELETE` để quản lý đánh giá cho các tin đăng và đại lý.
        3.  **Notification Service:** Tạo một `NotificationService` đa kênh:
            *   `sendEmail(to, subject, body)`
            *   `sendPushNotification(userId, title, message)`
            *   `createInAppNotification(userId, content)` (lưu vào database và hiển thị trên giao diện)

#### **4. Nhóm Tính năng Địa lý & Bản đồ (Geolocation & Maps)**

*   **Hiện trạng ở dự án PHP:** Module `location` để quản lý quốc gia, tỉnh/thành, quận/huyện. Component `GEOCoder` để chuyển địa chỉ thành tọa độ (kinh độ, vĩ độ).
*   **Tại sao cần thiết:** Tìm kiếm bất động sản theo vị trí và hiển thị trên bản đồ là tính năng không thể thiếu.
*   **Đề xuất cho dự án Spring Boot:**
    *   **Công nghệ:** Sử dụng **PostGIS** (nếu dùng PostgreSQL) hoặc các tính năng geospatial của Elasticsearch. Tích hợp với **Google Maps API** hoặc **Mapbox API**.
    *   **Triển khai:**
        1.  **Geospatial Search:** Tận dụng `GeoPoint` trong Elasticsearch để thực hiện các truy vấn tìm kiếm theo bán kính, trong một khu vực đa giác, v.v.
        2.  **Geocoding Service:** Tạo `GeocodingService` gọi đến API của Google Maps/Mapbox để:
            *   Chuyển địa chỉ (ví dụ: "123 Nguyễn Huệ, Quận 1, TPHCM") thành tọa độ `(lat, lng)`.
            *   Chuyển tọa độ thành địa chỉ chi tiết (Reverse Geocoding).
        3.  **Location Management:** Vẫn giữ hệ thống quản lý địa chính (tỉnh, huyện, xã) để người dùng có thể filter theo bộ lọc `dropdown`.

#### **5. Nhóm Tính năng SEO & Quản lý Nội dung (SEO & Content Management)**

*   **Hiện trạng ở dự án PHP:** Các module `seo`, `sitemap`, `rss`, `news`, `articles` giúp tối ưu hóa cho công cụ tìm kiếm và quản lý các trang nội dung.
*   **Tại sao cần thiết:** Giúp website của bạn có thứ hạng cao trên Google, thu hút traffic tự nhiên.
*   **Đề xuất cho dự án Spring Boot:**
    *   **Triển khai:**
        1.  **Dynamic Sitemap:** Tạo một endpoint `@GetMapping("/sitemap.xml", produces = "application/xml")` trong một `SitemapController`. Controller này sẽ truy vấn tất cả các đường dẫn công khai (tin đăng, trang tin tức) từ database và tạo ra file XML động.
        2.  **Dynamic Robots.txt:** Tương tự sitemap, có thể tạo endpoint để quản lý file `robots.txt`.
        3.  **Content Management:** Tạo các entity `Article`, `News`, `InfoPage` và các controller tương ứng để quản lý nội dung.
        4.  **SEO Meta Tags:** Dù API của bạn là backend, bạn cần đảm bảo API trả về đủ thông tin (`metaTitle`, `metaDescription`, `metaKeywords`) để frontend có thể render chúng vào các thẻ `<meta>` trong HTML.

### **Kế hoạch Hành động**

Để giúp bạn có một lộ trình rõ ràng, tôi đã tạo ra một danh sách các công việc cần làm. Bạn có thể thực hiện theo từng giai đoạn.



---

**User**

Nhóm Tính năng Trả phí & Dịch vụ . Phân tích chi tiết về nhóm tính năng này và lên kế hoạch cần phát triển những gì 

---

**Cursor**

Chắc chắn rồi. Nhóm tính năng **"Trả phí & Dịch vụ" (Paid Services & Monetization)** là một trong những phần quan trọng nhất để thương mại hóa hệ thống MLS của bạn. Việc phân tích kỹ lưỡng và lên kế hoạch bài bản sẽ giúp bạn xây dựng một hệ thống linh hoạt, dễ mở rộng và an toàn.

Dưới đây là phân tích chi tiết và kế hoạch phát triển cho nhóm tính năng này.

### **A. Phân Tích Chi Tiết Nhóm Tính Năng**

Mục tiêu chính của nhóm tính năng này là tạo ra doanh thu bằng cách cung cấp các dịch vụ giá trị gia tăng cho người dùng (chủ yếu là các đại lý bất động sản - agents).

#### **1. Các "Diễn viên" (Actors) và Nhu cầu của họ:**

*   **Quản trị viên (Admin):**
    *   **Nhu cầu:** Cần có khả năng tạo, định giá và quản lý các gói dịch vụ khác nhau. Họ cần xem được báo cáo doanh thu, theo dõi người dùng nào đang sử dụng dịch vụ gì.
    *   **Use case:** "Tôi muốn tạo một gói 'Tin đăng Vàng' có giá 100,000 VND, giúp tin đăng được hiển thị ở vị trí nổi bật trong 7 ngày."

*   **Đại lý / Người dùng (Agent / User):**
    *   **Nhu cầu:** Cần một cách dễ dàng để xem các gói dịch vụ, mua chúng để tăng hiệu quả cho tin đăng của mình, quản lý các dịch vụ đã mua và xem lịch sử giao dịch.
    *   **Use case:** "Tin đăng của tôi ít người xem. Tôi muốn mua gói 'Đẩy tin' để nó xuất hiện lại ở đầu trang kết quả tìm kiếm."

#### **2. Các Loại Dịch Vụ Cần Có (Types of Paid Services):**

Dựa trên các hệ thống MLS phổ biến, chúng ta có thể phân loại các dịch vụ trả phí như sau:

*   **Dịch vụ cho từng Tin đăng (Per-Listing Services):** Đây là các giao dịch mua một lần (one-time purchase) để áp dụng cho một tin đăng cụ thể.
    *   **Tin đăng Nổi bật (Featured Listing):** Tin đăng sẽ được hiển thị ở một khu vực đặc biệt trên trang chủ hoặc trang tìm kiếm, thường có viền hoặc màu sắc khác biệt. Dịch vụ này có thời hạn (ví dụ: 7 ngày, 15 ngày).
    *   **Đẩy tin lên top (Bump/Push Listing):** Đưa tin đăng trở lại vị trí đầu tiên của danh sách kết quả tìm kiếm, như thể nó vừa được đăng.
    *   **Tin đăng màu/khẩn cấp (Colored/Urgent Listing):** Thay đổi màu sắc của tiêu đề hoặc thêm nhãn "Gấp", "Bán nhanh" để thu hút sự chú ý.

*   **Dịch vụ theo Gói (Package-based Services):** Người dùng mua một gói để có một số lượng quyền lợi nhất định.
    *   **Gói Tin đăng (Listing Packages):** Thay vì trả tiền cho mỗi lần đăng tin, người dùng có thể mua một gói, ví dụ "Gói 5 tin đăng", "Gói 20 tin đăng" với giá ưu đãi.
    *   **Gói Đẩy tin (Bump Packages):** Mua trước 10 lần đẩy tin để sử dụng dần.

*   **Dịch vụ theo Thuê bao (Subscription-based Services):** Người dùng trả tiền định kỳ (hàng tháng, hàng năm) để nhận các quyền lợi cao cấp.
    *   **Tài khoản Đại lý PRO (PRO Agent Subscription):** Một gói thuê bao cung cấp một bộ quyền lợi toàn diện, ví dụ:
        *   Không giới hạn số lượng tin đăng.
        *   Huy hiệu "Đại lý uy tín" trên hồ sơ.
        *   Truy cập các tính năng phân tích, báo cáo nâng cao.
        *   Mỗi tháng được tặng 5 lần đẩy tin miễn phí.

---

### **B. Kế Hoạch Phát Triển Chi Tiết**

Để xây dựng hệ thống này trong dự án Spring Boot, chúng ta cần thực hiện các bước sau:

#### **1. Cấu trúc Database (Entities)**

Đây là nền tảng của toàn bộ hệ thống. Chúng ta cần các bảng sau:

*   **`ServicePlan` (Gói dịch vụ):** Lưu thông tin về tất cả các gói dịch vụ mà Admin tạo ra.
    *   `id`: `Long`
    *   `name`: `String` (ví dụ: "Tin đăng Vàng 7 ngày")
    *   `description`: `String`
    *   `price`: `BigDecimal` (giá tiền)
    *   `currency`: `String` (ví dụ: "VND", "USD")
    *   `type`: `Enum` (`PER_LISTING`, `PACKAGE`, `SUBSCRIPTION`)
    *   `durationInDays`: `Integer` (null nếu không áp dụng, ví dụ 30 ngày cho gói PRO)
    *   `features`: `JSON` hoặc `String` (lưu các quyền lợi, ví dụ `{"bump_credits": 5, "featured_duration": 7}`)
    *   `active`: `boolean` (để Admin có thể bật/tắt gói dịch vụ)

*   **`UserSubscription` (Gói thuê bao của người dùng):** Chỉ dùng cho các dịch vụ loại `SUBSCRIPTION`.
    *   `id`: `Long`
    *   `user`: `User` (liên kết tới người dùng)
    *   `servicePlan`: `ServicePlan` (liên kết tới gói PRO)
    *   `startDate`: `LocalDateTime`
    *   `endDate`: `LocalDateTime`
    *   `status`: `Enum` (`ACTIVE`, `CANCELLED`, `EXPIRED`)
    *   `paymentGatewaySubscriptionId`: `String` (ID của subscription bên cổng thanh toán như Stripe, để quản lý gia hạn)

*   **`UserAsset` (Tài sản của người dùng):** Dùng cho các dịch vụ loại `PACKAGE`
    *   `id`: `Long`
    *   `user`: `User`
    *   `assetType`: `Enum` (`LISTING_CREDIT`, `BUMP_CREDIT`)
    *   `quantity`: `Integer` (số lượng tin đăng/lượt đẩy tin còn lại)
    *   `sourceTransaction`: `PaymentTransaction` (liên kết tới giao dịch đã mua gói này)

*   **`ListingFeature` (Tính năng của tin đăng):** Dùng cho dịch vụ `PER_LISTING`
    *   `id`: `Long`
    *   `listing`: `Listing` (liên kết tới tin đăng)
    *   `featureType`: `Enum` (`FEATURED`, `URGENT`)
    *   `expiryDate`: `LocalDateTime` (ngày hết hạn của tính năng)

*   **`PaymentTransaction` (Giao dịch thanh toán):** Lưu lại tất cả lịch sử giao dịch.
    *   `id`: `Long`
    *   `user`: `User`
    *   `servicePlan`: `ServicePlan` (gói đã mua)
    *   `amount`: `BigDecimal`
    *   `currency`: `String`
    *   `status`: `Enum` (`PENDING`, `SUCCESS`, `FAILED`)
    *   `paymentGateway`: `String` (ví dụ: "STRIPE", "MOMO")
    *   `gatewayTransactionId`: `String` (ID của giao dịch bên cổng thanh toán)
    *   `createdAt`, `updatedAt`: `LocalDateTime`

#### **2. Cấu hình (Configuration)**

*   Trong file `application.yml`, bạn cần thêm cấu hình cho các cổng thanh toán.
    ```yaml
    app:
      payment:
        stripe:
          secret-key: ${STRIPE_SECRET_KEY}
          public-key: ${STRIPE_PUBLIC_KEY}
          webhook-secret: ${STRIPE_WEBHOOK_SECRET}
        paypal:
          client-id: ${PAYPAL_CLIENT_ID}
          client-secret: ${PAYPAL_CLIENT_SECRET}
    ```

#### **3. Logic nghiệp vụ (Service Layer)**

*   **`ServicePlanService.java`:**
    *   `createPlan(dto)`, `updatePlan(id, dto)`, `deactivatePlan(id)`: Cho Admin quản lý.
    *   `getActivePlans()`: Cho người dùng xem danh sách các gói dịch vụ.
*   **`SubscriptionService.java`:**
    *   `provisionSubscription(userId, planId, transaction)`: Cấp quyền lợi thuê bao cho người dùng sau khi thanh toán thành công.
    *   `cancelSubscription(userId)`: Xử lý khi người dùng hủy gói.
    *   `checkAndHandleExpiredSubscriptions()`: Một tác vụ định kỳ (`@Scheduled`) để kiểm tra các thuê bao hết hạn và cập nhật trạng thái.
*   **`PaymentService.java`:**
    *   `createCheckoutSession(userId, planId)`: Tạo một phiên thanh toán với cổng thanh toán (ví dụ Stripe Checkout) và trả về URL/ID cho frontend.
    *   `handleStripeWebhook(payload, signature)`: Xử lý các sự kiện (webhook) từ Stripe gửi về (ví dụ: `checkout.session.completed`). Đây là bước **quan trọng nhất** để xác nhận thanh toán và cấp dịch vụ một cách an toàn.

#### **4. API Endpoints (Controller Layer)**

*   **`ServicePlanController.java` (cho người dùng):**
    *   `GET /api/plans`: Lấy danh sách tất cả các gói dịch vụ đang hoạt động.
*   **`AdminServicePlanController.java` (cho admin, bảo vệ bằng `hasRole('ADMIN')`):**
    *   `POST /api/admin/plans`: Tạo gói mới.
    *   `PUT /api/admin/plans/{id}`: Cập nhật gói.
    *   `DELETE /api/admin/plans/{id}`: Tạm ngưng gói.
*   **`PaymentController.java`:**
    *   `POST /api/payments/checkout`: Người dùng gửi yêu cầu mua một gói, backend tạo checkout session và trả về cho frontend.
    *   `GET /api/payments/my-history`: Người dùng xem lịch sử giao dịch.
*   **`WebhookController.java`:**
    *   `POST /api/webhooks/stripe`: Endpoint để nhận webhook từ Stripe. Endpoint này **không** cần xác thực user/pass nhưng phải được bảo vệ bằng cách xác minh chữ ký (signature) của webhook.

#### **5. Luồng Tích hợp Thanh toán (Ví dụ với Stripe)**

1.  **User Side:** Người dùng chọn một gói và nhấn "Mua".
2.  **Frontend:** Gửi yêu cầu `POST /api/payments/checkout` với `planId`.
3.  **Backend (`PaymentService`):**
    *   Gọi Stripe API để tạo một `Checkout Session`.
    *   Lưu một `PaymentTransaction` với trạng thái `PENDING`.
    *   Trả về `sessionId` của Stripe cho frontend.
4.  **Frontend:** Dùng `stripe.js` và `sessionId` để chuyển hướng người dùng đến trang thanh toán của Stripe.
5.  **User Side:** Người dùng nhập thông tin thẻ và thanh toán trên trang Stripe.
6.  **Stripe -> Backend:** Stripe gửi một sự kiện `checkout.session.completed` đến endpoint `POST /api/webhooks/stripe` của bạn.
7.  **Backend (`PaymentService.handleStripeWebhook`):**
    *   **Xác minh chữ ký webhook** để đảm bảo request đến từ Stripe.
    *   Lấy thông tin giao dịch từ webhook.
    *   Cập nhật `PaymentTransaction` tương ứng sang trạng thái `SUCCESS`.
    *   Gọi `SubscriptionService` hoặc các service khác để **cấp phát dịch vụ** cho người dùng (tạo `UserSubscription`, cộng `UserAsset`...).
    *   Gửi thông báo (email, notification) cho người dùng.
8.  **Frontend:** Người dùng được Stripe chuyển hướng về trang thành công (`success_url`) trên website của bạn. Trang này hiển thị thông báo "Thanh toán thành công!".

---

### **C. Tạo Todo List để Theo dõi**

Để dễ dàng quản lý, tôi sẽ tạo một danh sách công việc cho bạn.



```yaml
    app:
      payment:
        stripe:
          secret-key: ${STRIPE_SECRET_KEY}
          public-key: ${STRIPE_PUBLIC_KEY}
          webhook-secret: ${STRIPE_WEBHOOK_SECRET}
        paypal:
          client-id: ${PAYPAL_CLIENT_ID}
          client-secret: ${PAYPAL_CLIENT_SECRET}
```

Tuyệt vời! Tôi đã tạo một danh sách công việc chi tiết để bạn có thể bắt đầu triển khai nhóm tính năng "Trả phí & Dịch vụ". Kế hoạch này bao gồm từ việc thiết kế database, viết logic nghiệp vụ, tạo API cho đến việc xử lý các tác vụ nền.

Bạn có muốn tôi bắt đầu với bước đầu tiên, **"Thiết kế và tạo các Entities cho hệ thống trả phí"** không? Tôi sẽ tạo các file entity Java với đầy đủ các thuộc tính và mối quan hệ như đã phân tích ở trên.

---

**User**

Vậy khi user mua gói để đẩy bài đăng tin lên đầu thì tôi cần xử lý ở lúc người dùng tìm kiếm sản phẩm như nào 

---

**Cursor**

Rất đúng! Đây là một câu hỏi cực kỳ quan trọng, vì nó ảnh hưởng trực tiếp đến cách bạn cấu trúc truy vấn tìm kiếm - trái tim của hệ thống MLS. Xử lý không đúng sẽ làm giảm giá trị của các gói dịch vụ mà người dùng đã mua.

Để giải quyết vấn đề này, chúng ta cần một cơ chế để **ưu tiên** các tin đăng đã được trả phí trong kết quả tìm kiếm. Cách tiếp cận phổ biến và hiệu quả nhất là kết hợp hai loại ưu tiên:

1.  **Tin Đăng Nổi bật (Featured Listings):** Luôn được "ghim" ở một khu vực riêng biệt trên cùng, bất kể người dùng sắp xếp theo tiêu chí nào.
2.  **Tin Đăng Được Đẩy (Bumped Listings):** Được xếp lên đầu trong danh sách kết quả *chính* khi sắp xếp theo mức độ liên quan hoặc ngày mới nhất.

Hãy phân tích chi tiết cách triển khai cả hai.

### **Cơ Chế Hoạt Động Cốt Lõi: Thêm Các Trường Dữ Liệu Ưu Tiên**

Để hệ thống tìm kiếm có thể "hiểu" được tin nào cần ưu tiên, bạn cần thêm một vài trường vào Entity `Listing` (hoặc tên tương tự bạn dùng để lưu tin đăng).

**Bước 1: Cập nhật Entity `Listing`**

Hãy mở file entity `Listing.java` của bạn và thêm các trường sau:

```java
// Trong class Listing.java

// ... các trường hiện có như title, description, price ...

/**
 * Thời điểm tin đăng được "đẩy" lên top lần cuối.
 * Mặc định, trường này sẽ bằng với `createdAt`.
 * Khi người dùng sử dụng dịch vụ "Đẩy tin", chỉ cần cập nhật trường này về thời gian hiện tại.
 */
@Column(name = "bumped_at")
private LocalDateTime bumpedAt;

/**
 * Cờ xác định tin đăng có phải là "Tin Nổi bật" hay không.
 */
@Column(name = "is_featured")
private boolean isFeatured = false;

/**
 * Thời điểm hết hạn của dịch vụ "Tin Nổi bật".
 * Tin đăng sẽ không còn nổi bật sau thời điểm này.
 */
@Column(name = "featured_until")
private LocalDateTime featuredUntil;

// Quan trọng: Khi tạo một tin đăng mới, hãy set giá trị ban đầu cho bumpedAt
@PrePersist
protected void onCreate() {
    if (this.bumpedAt == null) {
        this.bumpedAt = LocalDateTime.now();
    }
}
```

**Tại sao lại cần `bumpedAt`?**

Đây chính là "chìa khóa". Thay vì sắp xếp theo ngày tạo (`createdAt`), logic tìm kiếm của bạn giờ đây sẽ **luôn sắp xếp mặc định theo `bumpedAt` giảm dần (`DESC`)**.

*   Khi một tin mới được tạo, `bumpedAt` bằng `createdAt`, nên nó sẽ tự động lên đầu.
*   Khi một tin cũ được người dùng "đẩy", bạn chỉ cần cập nhật `listing.setBumpedAt(LocalDateTime.now())`. Ngay lập tức, nó sẽ được xếp lên trên tất cả các tin khác.

---

### **Cấu Trúc Lại Logic Tìm Kiếm**

Bây giờ, hãy cập nhật `SearchService` (hoặc `ListingService`) của bạn. Phương thức tìm kiếm sẽ được chia làm 2 phần rõ rệt.

**Bước 2: Sửa đổi phương thức `search()`**

Hãy tưởng tượng trang kết quả tìm kiếm của bạn có 2 khu vực:
1.  **Khu vực Tin Nổi bật:** Hiển thị 3-5 tin.
2.  **Khu vực Kết quả Chính:** Hiển thị danh sách tin đã được phân trang.

Phương thức `search` của bạn sẽ trả về một DTO chứa cả hai danh sách này.

```java
// Ví dụ về DTO trả về
@Getter
@Setter
public class SearchResultDTO {
    private List<ListingSummaryDTO> featuredListings;
    private Page<ListingSummaryDTO> mainResultPage;
}
```

Logic tìm kiếm sẽ như sau:

```java
// Trong SearchService.java hoặc ListingService.java

public SearchResultDTO performSearch(SearchRequest request, Pageable pageable) {

    // ----- PHẦN 1: LẤY CÁC TIN NỔI BẬT (FEATURED LISTINGS) -----

    // 1. Tạo truy vấn để lấy một số lượng giới hạn các tin nổi bật còn hạn.
    // Sắp xếp chúng ngẫu nhiên hoặc theo ngày đẩy mới nhất để tạo sự đa dạng.
    List<Listing> featured = listingRepository.findTop5ByIsFeaturedTrueAndFeaturedUntilAfter(
        LocalDateTime.now(), 
        Sort.by("bumpedAt").descending() // Hoặc sắp xếp ngẫu nhiên
    );
    
    // Lấy ra danh sách ID của các tin nổi bật để loại trừ ở truy vấn sau
    List<Long> featuredIds = featured.stream().map(Listing::getId).collect(Collectors.toList());


    // ----- PHẦN 2: LẤY KẾT QUẢ TÌM KIẾM CHÍNH (MAIN RESULTS) -----

    // 2. Tạo đối tượng Pageable mới với tiêu chí sắp xếp mặc định.
    // Luôn ưu tiên `bumpedAt` trước. `createdAt` dùng để départ nếu `bumpedAt` trùng nhau.
    Sort defaultSort = Sort.by(Sort.Order.desc("bumpedAt"), Sort.Order.desc("createdAt"));
    
    // Kiểm tra xem người dùng có yêu cầu sắp xếp khác không (ví dụ: theo giá)
    // Nếu có, sử dụng yêu cầu của họ. Nếu không, dùng sắp xếp mặc định.
    Sort finalSort = request.getSortBy() != null ? buildSortFromRequest(request) : defaultSort;
    Pageable mainPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), finalSort);

    // 3. Xây dựng các tiêu chí tìm kiếm (dùng Specification hoặc QueryDSL)
    Specification<Listing> spec = buildSpecificationFromRequest(request);
    
    // Thêm điều kiện để loại trừ các tin đã nằm trong danh sách nổi bật
    if (!featuredIds.isEmpty()) {
        spec = spec.and((root, query, cb) -> root.get("id").in(featuredIds).not());
    }

    // 4. Thực hiện truy vấn chính
    Page<Listing> mainResultPageEntity = listingRepository.findAll(spec, mainPageable);


    // ----- PHẦN 3: TỔNG HỢP KẾT QUẢ -----

    // 5. Chuyển đổi Entities sang DTOs và trả về
    SearchResultDTO finalResult = new SearchResultDTO();
    finalResult.setFeaturedListings(featured.stream().map(this::convertToDTO).collect(Collectors.toList()));
    finalResult.setMainResultPage(mainResultPageEntity.map(this::convertToDTO));

    return finalResult;
}
```

### **Tóm Tắt Luồng Hoạt Động**

1.  **Khi người dùng mua gói "Đẩy tin":**
    *   `PaymentService` xác nhận thanh toán thành công.
    *   `UserAssetService` trừ một lượt "đẩy tin" của người dùng.
    *   `ListingService.bumpListing(listingId)` được gọi, chỉ đơn giản là cập nhật trường `bumpedAt` của tin đăng đó về `now()`.

2.  **Khi người dùng mua gói "Tin Nổi bật":**
    *   `PaymentService` xác nhận thanh toán thành công.
    *   `ListingService.featureListing(listingId, durationInDays)` được gọi.
    *   Service này sẽ set `isFeatured = true` và `featuredUntil = LocalDateTime.now().plusDays(durationInDays)`.

3.  **Khi người dùng vào trang tìm kiếm:**
    *   Hệ thống chạy phương thức `performSearch`.
    *   **Đầu tiên,** nó lấy ra một vài tin có `isFeatured = true` và chưa hết hạn để hiển thị vào khu vực "Nổi bật".
    *   **Tiếp theo,** nó lấy danh sách các tin còn lại, **sắp xếp theo `bumpedAt` giảm dần**. Điều này đảm bảo các tin vừa được đẩy hoặc vừa được tạo sẽ nằm trên cùng.
    *   Nếu người dùng chọn sắp xếp theo giá, hệ thống sẽ tuân theo, và lúc này thứ tự `bumpedAt` sẽ không còn được ưu tiên nữa (chỉ khu vực tin nổi bật là cố định).

Cách tiếp cận này vừa đáp ứng được nhu cầu của người dùng trả phí, vừa giữ cho trải nghiệm tìm kiếm của người dùng thông thường được tự nhiên và linh hoạt.

```java
// Trong class Listing.java

// ... các trường hiện có như title, description, price ...

/**
 * Thời điểm tin đăng được "đẩy" lên top lần cuối.
 * Mặc định, trường này sẽ bằng với `createdAt`.
 * Khi người dùng sử dụng dịch vụ "Đẩy tin", chỉ cần cập nhật trường này về thời gian hiện tại.
 */
@Column(name = "bumped_at")
private LocalDateTime bumpedAt;

/**
 * Cờ xác định tin đăng có phải là "Tin Nổi bật" hay không.
 */
@Column(name = "is_featured")
private boolean isFeatured = false;

/**
 * Thời điểm hết hạn của dịch vụ "Tin Nổi bật".
 * Tin đăng sẽ không còn nổi bật sau thời điểm này.
 */
@Column(name = "featured_until")
private LocalDateTime featuredUntil;

// Quan trọng: Khi tạo một tin đăng mới, hãy set giá trị ban đầu cho bumpedAt
@PrePersist
protected void onCreate() {
    if (this.bumpedAt == null) {
        this.bumpedAt = LocalDateTime.now();
    }
}
```

```java
// Ví dụ về DTO trả về
@Getter
@Setter
public class SearchResultDTO {
    private List<ListingSummaryDTO> featuredListings;
    private Page<ListingSummaryDTO> mainResultPage;
}
```

```java
// Trong SearchService.java hoặc ListingService.java

public SearchResultDTO performSearch(SearchRequest request, Pageable pageable) {

    // ----- PHẦN 1: LẤY CÁC TIN NỔI BẬT (FEATURED LISTINGS) -----

    // 1. Tạo truy vấn để lấy một số lượng giới hạn các tin nổi bật còn hạn.
    // Sắp xếp chúng ngẫu nhiên hoặc theo ngày đẩy mới nhất để tạo sự đa dạng.
    List<Listing> featured = listingRepository.findTop5ByIsFeaturedTrueAndFeaturedUntilAfter(
        LocalDateTime.now(), 
        Sort.by("bumpedAt").descending() // Hoặc sắp xếp ngẫu nhiên
    );
    
    // Lấy ra danh sách ID của các tin nổi bật để loại trừ ở truy vấn sau
    List<Long> featuredIds = featured.stream().map(Listing::getId).collect(Collectors.toList());


    // ----- PHẦN 2: LẤY KẾT QUẢ TÌM KIẾM CHÍNH (MAIN RESULTS) -----

    // 2. Tạo đối tượng Pageable mới với tiêu chí sắp xếp mặc định.
    // Luôn ưu tiên `bumpedAt` trước. `createdAt` dùng để départ nếu `bumpedAt` trùng nhau.
    Sort defaultSort = Sort.by(Sort.Order.desc("bumpedAt"), Sort.Order.desc("createdAt"));
    
    // Kiểm tra xem người dùng có yêu cầu sắp xếp khác không (ví dụ: theo giá)
    // Nếu có, sử dụng yêu cầu của họ. Nếu không, dùng sắp xếp mặc định.
    Sort finalSort = request.getSortBy() != null ? buildSortFromRequest(request) : defaultSort;
    Pageable mainPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), finalSort);

    // 3. Xây dựng các tiêu chí tìm kiếm (dùng Specification hoặc QueryDSL)
    Specification<Listing> spec = buildSpecificationFromRequest(request);
    
    // Thêm điều kiện để loại trừ các tin đã nằm trong danh sách nổi bật
    if (!featuredIds.isEmpty()) {
        spec = spec.and((root, query, cb) -> root.get("id").in(featuredIds).not());
    }

    // 4. Thực hiện truy vấn chính
    Page<Listing> mainResultPageEntity = listingRepository.findAll(spec, mainPageable);


    // ----- PHẦN 3: TỔNG HỢP KẾT QUẢ -----

    // 5. Chuyển đổi Entities sang DTOs và trả về
    SearchResultDTO finalResult = new SearchResultDTO();
    finalResult.setFeaturedListings(featured.stream().map(this::convertToDTO).collect(Collectors.toList()));
    finalResult.setMainResultPage(mainResultPageEntity.map(this::convertToDTO));

    return finalResult;
}
```

