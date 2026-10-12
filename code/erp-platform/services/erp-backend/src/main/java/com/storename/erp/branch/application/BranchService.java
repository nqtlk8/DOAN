package com.storename.erp.branch.application;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.security.JwtAuthDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Quản lý chi nhánh (chỉ HQ). Chi nhánh KHÔNG bị xóa: không có thao tác xóa trong hệ thống
 * (dữ liệu giao dịch, tồn kho, công nợ và replication đều tham chiếu id chi nhánh).
 */
@Service
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    private String getRequiredBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails) {
            JwtAuthDetails details = (JwtAuthDetails) auth.getDetails();
            String branchId = details.getBranchId();
            return ("HQ".equals(branchId) || branchId == null || branchId.trim().isEmpty()) ? null : branchId;
        }
        return null;
    }

    @Transactional
    public BranchDTO createBranch(BranchDTO dto) {
        enforceGlobalAdmin();
        String code = requireText(dto.getCode(), "code");
        String name = requireText(dto.getName(), "name");
        if (branchRepository.existsByCode(code)) {
            throw new BusinessConflictException("BRANCH_CODE_EXISTS", "Mã chi nhánh '" + code + "' đã tồn tại");
        }
        Branch branch = Branch.builder()
                .code(code)
                .name(name)
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .openingHours(dto.getOpeningHours())
                .internalUrl(normalizePortalUrl(dto.getInternalUrl()))
                .isActive(dto.getIsActive() == null || dto.getIsActive())
                .build();
        return mapToDTO(branchRepository.save(branch));
    }

    public List<BranchDTO> getAllBranches() {
        enforceGlobalAdmin();
        return branchRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public BranchDTO getBranchById(Long id) {
        String currentBranchIdStr = getRequiredBranchId();
        if (currentBranchIdStr != null && !currentBranchIdStr.equals(id.toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this branch");
        }
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        return mapToDTO(branch);
    }

    /**
     * Cập nhật từng phần: trường không gửi (null) giữ nguyên giá trị cũ. Riêng internalUrl gửi chuỗi
     * rỗng nghĩa là xóa URL cổng. Không gửi isActive thì trạng thái chi nhánh không đổi.
     */
    @Transactional
    public BranchDTO updateBranch(Long id, BranchDTO dto) {
        enforceGlobalAdmin();
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        if (dto.getCode() != null) {
            String code = requireText(dto.getCode(), "code");
            if (!code.equals(branch.getCode()) && branchRepository.existsByCodeAndIdNot(code, id)) {
                throw new BusinessConflictException("BRANCH_CODE_EXISTS", "Mã chi nhánh '" + code + "' đã tồn tại");
            }
            branch.setCode(code);
        }
        if (dto.getName() != null) branch.setName(requireText(dto.getName(), "name"));
        if (dto.getAddress() != null) branch.setAddress(dto.getAddress());
        if (dto.getPhone() != null) branch.setPhone(dto.getPhone());
        if (dto.getOpeningHours() != null) branch.setOpeningHours(dto.getOpeningHours());
        if (dto.getInternalUrl() != null) branch.setInternalUrl(normalizePortalUrl(dto.getInternalUrl()));
        if (dto.getIsActive() != null) branch.setActive(dto.getIsActive());

        return mapToDTO(branchRepository.save(branch));
    }

    private void enforceGlobalAdmin() {
        String currentBranchIdStr = getRequiredBranchId();
        if (currentBranchIdStr != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Global admin access required");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Branch " + field + " is required");
        }
        return value.trim();
    }

    private BranchDTO mapToDTO(Branch branch) {
        return BranchDTO.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .openingHours(branch.getOpeningHours())
                .internalUrl(branch.getInternalUrl())
                .isActive(branch.isActive())
                .build();
    }

    /**
     * Cot branch.internal_url luu dia chi CONG WEB cua chi nhanh (vd. http://localhost:81),
     * duoc tra ve khi dang nhap (branchUrl) de frontend buoc STAFF lam viec dung cong chi nhanh.
     * Chi chap nhan URL tuyet doi http/https; gia tri rong duoc luu la null.
     */
    private static String normalizePortalUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = new java.net.URI(url.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("Branch portal URL must be an absolute http(s) URL: " + url);
            }
            return url.trim();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid branch portal URL: " + url);
        }
    }
}
