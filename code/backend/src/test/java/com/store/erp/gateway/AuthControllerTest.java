package com.store.erp.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.erp.dto.LoginRequest;
import com.store.erp.entity.User;
import com.store.erp.repos.UserRepository;
import com.store.erp.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    public void setup() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash("hashed_admin");
        admin.setRole("ROLE_ADMIN");

        User sales = new User();
        sales.setId(2L);
        sales.setUsername("sales");
        sales.setPasswordHash("hashed_sales");
        sales.setRole("ROLE_SALES");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("sales")).thenReturn(Optional.of(sales));
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        when(passwordEncoder.matches("admin", "hashed_admin")).thenReturn(true);
        when(passwordEncoder.matches("sales", "hashed_sales")).thenReturn(true);
        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invocation -> {
            String rawPassword = invocation.getArgument(0);
            String encodedPassword = invocation.getArgument(1);
            return ("hashed_" + rawPassword).equals(encodedPassword);
        });
    }

    // TC_USER_BE_01
    @Test
    public void testLoginSuccess_Admin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ADMIN"));
    }

    // TC_USER_BE_02
    @Test
    public void testLoginSuccess_Sales() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("sales");
        request.setPassword("sales");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.role").value("ROLE_SALES"));
    }

    // TC_USER_BE_03
    @Test
    public void testLoginFailure_InvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong_password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    // TC_USER_BE_04
    @Test
    public void testLoginFailure_MissingFields() throws Exception {
        LoginRequest request = new LoginRequest();
        // Empty fields
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                // Note: since validation is not implemented in the controller currently,
                // this might return 401 or 400 depending on implementation.
                // QA expects 400 according to contract, so we assert isBadRequest().
                // If it fails, it means the developer needs to fix the validation logic.
                .andExpect(status().isBadRequest());
    }

    // TC_USER_RC_01: Data Integrity & Race Condition test for Token Generation
    @Test
    public void testConcurrentLoginRequests() throws InterruptedException {
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.success").value(true));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(threadCount, successCount.get(), "Not all login requests succeeded concurrently");
        assertEquals(0, errorCount.get(), "There were errors during concurrent login requests");
    }
}
