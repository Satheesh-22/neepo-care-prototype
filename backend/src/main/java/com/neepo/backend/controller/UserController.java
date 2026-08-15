package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.api.PageResponse;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CreateUserRequest;
import com.neepo.backend.dto.UserResponse;
import com.neepo.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(userService.createUser(request), TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be zero or greater") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100") int size,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                userService.getUsers(page, size),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(userService.getUserById(userId), TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/by-email")
    public ApiResponse<UserResponse> getUserByEmail(
            @RequestParam @Email(message = "Email must be valid") String email,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(userService.getUserByEmail(email), TraceIdFilter.traceId(httpRequest));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
    }
}
