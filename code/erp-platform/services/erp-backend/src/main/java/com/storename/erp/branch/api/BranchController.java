package com.storename.erp.branch.api;

import com.storename.erp.branch.application.BranchService;
import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/branches")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<BranchDTO>> createBranch(@RequestBody BranchDTO branchDTO) {
        log.info("Creating new branch: {}", branchDTO.getCode());
        BranchDTO created = branchService.createBranch(branchDTO);
        return ResponseEntity.ok(ApiResponse.success(created, "Branch created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchDTO>>> getAllBranches() {
        log.info("Fetching all branches");
        List<BranchDTO> branches = branchService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success(branches, "Branches retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchDTO>> getBranchById(@PathVariable Long id) {
        log.info("Fetching branch with id: {}", id);
        BranchDTO branch = branchService.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.success(branch, "Branch retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<BranchDTO>> updateBranch(@PathVariable Long id, @RequestBody BranchDTO branchDTO) {
        log.info("Updating branch with id: {}", id);
        BranchDTO updated = branchService.updateBranch(id, branchDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Branch updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        log.info("Deleting branch with id: {}", id);
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Branch deleted successfully"));
    }
}
