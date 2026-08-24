package com.store.erp.core.service;

import com.store.erp.core.entity.SysSequence;
import com.store.erp.core.repos.SysSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final SysSequenceRepository sysSequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateCode(String prefix, int length) {
        SysSequence seq = sysSequenceRepository.findById(prefix)
                .orElseGet(() -> new SysSequence(prefix, 0L));

        long nextValue = seq.getCurrentValue() + 1;
        seq.setCurrentValue(nextValue);
        sysSequenceRepository.save(seq);

        String format = "%0" + length + "d";
        return prefix + String.format(format, nextValue);
    }
}

