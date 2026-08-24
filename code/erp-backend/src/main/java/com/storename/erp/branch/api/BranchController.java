package com.storename.erp.branch.api;

import com.storename.erp.branch.application.BranchService;
import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
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
    public ResponseEntity<ApiResponse<BranchDTO>> updateBranch(@PathVariable Long id, @RequestBody BranchDTO branchDTO) {
        log.info("Updating branch with id: {}", id);
        BranchDTO updated = branchService.updateBranch(id, branchDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Branch updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        log.info("Deleting branch with id: {}", id);
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Branch deleted successfully"));
    }
}
