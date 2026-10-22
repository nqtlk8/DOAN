package com.storename.erp.common.aop;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Khóa chống gửi trùng của request đang xử lý, do {@link IdempotencyAspect} gắn vào request.
 *
 * <p>Giá trị là khóa đã gắn phạm vi người dùng ({@code <người dùng>:<Idempotency-Key>}), giống khóa lưu
 * trong {@code idempotency_record}. Service ghi giá trị này vào cột {@code request_key} của chứng từ
 * (trong cùng transaction nghiệp vụ) để một lần lập phiếu không thể tạo hai chứng từ, kể cả khi bản ghi
 * idempotency bị mất hoặc đã được dọn.</p>
 */
public final class IdempotencyContext {

    static final String REQUEST_KEY_ATTRIBUTE = IdempotencyContext.class.getName() + ".requestKey";

    private IdempotencyContext() {
    }

    /** Khóa của request hiện tại; {@code null} nếu request không gửi Idempotency-Key hoặc không có request. */
    public static String currentRequestKey() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(REQUEST_KEY_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return value instanceof String s ? s : null;
    }

    static void bind(RequestAttributes attributes, String scopedKey) {
        attributes.setAttribute(REQUEST_KEY_ATTRIBUTE, scopedKey, RequestAttributes.SCOPE_REQUEST);
    }
}
