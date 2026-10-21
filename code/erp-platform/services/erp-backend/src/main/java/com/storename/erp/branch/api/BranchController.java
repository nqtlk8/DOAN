package com.storename.erp.branch.api;

import com.storename.erp.branch.application.BranchService;
import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API chi nhánh (chỉ HQ), chỉ đọc. Không có API tạo, sửa hay xóa chi nhánh;
 * gọi POST hoặc DELETE trả HTTP 405.
 */
@RestController
@RequestMapping("/api/v1/branches")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchDTO>>> getAllBranches() {
        return ResponseEntity.ok(ApiResponse.success(branchService.getAllBranches(), "Branches retrieved successfully"));
    }
}
