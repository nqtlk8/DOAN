package com.storename.erp.common.security;

/**
 * Kiem tra phien cua access token con hop le theo trang thai user trong DB.
 * <p>Interface nam o common de filter khong phu thuoc truc tiep vao module identity;
 * trien khai la {@code identity.application.UserSessionValidator}.
 */
public interface TokenSessionValidator {

    /**
     * @param subject      claim sub cua token (user_account.public_id)
     * @param tokenVersion claim tv cua token
     * @return true neu user ton tai, dang hoat dong va token_version khop
     */
    boolean isSessionValid(String subject, int tokenVersion);

    /** Xoa cache cua mot user (goi sau khi doi token_version hoac khoa tai khoan). */
    void evict(String subject);
}
