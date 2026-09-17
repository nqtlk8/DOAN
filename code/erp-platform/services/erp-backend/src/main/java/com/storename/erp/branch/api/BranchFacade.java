package com.storename.erp.branch.api;

import com.storename.erp.branch.infrastructure.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchFacade {
    private final BranchRepository branchRepository;

    public Map<Long, String> getBranchNames(Collection<Long> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) return Map.of();
        return branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(com.storename.erp.branch.domain.Branch::getId, com.storename.erp.branch.domain.Branch::getName));
    }
}
