package com.iimp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimp.dto.AuthDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IimpApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void contextLoads() {}

    @Test
    void loginAsAdmin_shouldReturn200WithToken() throws Exception {
        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setEmail("alice@iimp.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginAsEmployee_shouldReturn200() throws Exception {
        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setEmail("dave@iimp.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void protectedEndpoint_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidLogin_shouldReturn401() throws Exception {
        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setEmail("alice@iimp.com");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
