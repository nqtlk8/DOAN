package com.storename.erp.branch.application;

import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.common.security.JwtAuthDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Đọc danh sách chi nhánh (chỉ HQ). Không có API tạo, sửa hay xóa chi nhánh: chi nhánh được
 * khai báo bằng dữ liệu khởi tạo tại HQ cùng cấu hình replication.
 */
@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public List<BranchDTO> getAllBranches() {
        enforceGlobalAdmin();
        return branchRepository.findAll().stream()
                .map(BranchService::mapToDTO)
                .toList();
    }

    /** Chỉ tài khoản không gắn chi nhánh (HQ) được xem danh sách chi nhánh. */
    private static void enforceGlobalAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            String branchId = details.getBranchId();
            boolean global = branchId == null || branchId.isBlank() || "HQ".equals(branchId);
            if (!global) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Global admin access required");
            }
        }
    }

    private static BranchDTO mapToDTO(Branch branch) {
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
}
