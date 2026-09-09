package com.storename.erp.common.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class HealthController {

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
