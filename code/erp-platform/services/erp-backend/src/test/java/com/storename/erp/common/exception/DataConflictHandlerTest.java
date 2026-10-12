package com.storename.erp.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ánh xạ lỗi CSDL và lỗi phương thức HTTP sang mã 4xx thay vì 500. */
class DataConflictHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("TC-E01: vi phạm ràng buộc CSDL -> 409 DATA_INTEGRITY_VIOLATION, không lộ chi tiết SQL")
    void dataIntegrityViolation_returns409() throws Exception {
        mockMvc.perform(get("/test/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("DATA_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Data conflicts with an existing record or a database constraint"));
    }

    @Test
    @DisplayName("TC-E02: xung đột @Version -> 409 CONCURRENT_UPDATE")
    void optimisticLockFailure_returns409() throws Exception {
        mockMvc.perform(get("/test/optimistic"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("CONCURRENT_UPDATE"));
    }

    @Test
    @DisplayName("TC-E03: phương thức HTTP không hỗ trợ -> 405 METHOD_NOT_ALLOWED")
    void unsupportedMethod_returns405() throws Exception {
        mockMvc.perform(delete("/test/integrity"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errors[0]").value("METHOD_NOT_ALLOWED"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/integrity")
        public void integrity() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"product_code_key\"");
        }

        @GetMapping("/test/optimistic")
        public void optimistic() {
            throw new ObjectOptimisticLockingFailureException(Object.class, "id-1");
        }
    }
}
