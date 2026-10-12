package com.storename.erp.common.exception;

/**
 * Vi phạm quy tắc nghiệp vụ do trạng thái dữ liệu hiện tại (vd không đủ tồn kho) -> HTTP 409.
 * {@code code} được trả về trong {@code ApiResponse.errors} để frontend nhận diện.
 */
public class BusinessConflictException extends RuntimeException {

    private final String code;

    public BusinessConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
