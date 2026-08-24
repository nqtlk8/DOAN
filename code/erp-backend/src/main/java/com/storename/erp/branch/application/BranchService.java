package com.storename.erp.branch.application;

import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.common.security.JwtAuthDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
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
        Branch branch = Branch.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .openingHours(dto.getOpeningHours())
                .isActive(dto.isActive())
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

    @Transactional
    public BranchDTO updateBranch(Long id, BranchDTO dto) {
        enforceGlobalAdmin();
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        
        branch.setCode(dto.getCode());
        branch.setName(dto.getName());
        branch.setAddress(dto.getAddress());
        branch.setPhone(dto.getPhone());
        branch.setOpeningHours(dto.getOpeningHours());
        branch.setActive(dto.isActive());
        
        return mapToDTO(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(Long id) {
        enforceGlobalAdmin();
        if (!branchRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        }
        branchRepository.deleteById(id);
    }

    private void enforceGlobalAdmin() {
        String currentBranchIdStr = getRequiredBranchId();
        if (currentBranchIdStr != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Global admin access required");
        }
    }

    private BranchDTO mapToDTO(Branch branch) {
        return BranchDTO.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .openingHours(branch.getOpeningHours())
                .isActive(branch.isActive())
                .build();
    }
}
