package com.supermarket.auth.controller;

import com.supermarket.auth.dto.LoginRequest;
import com.supermarket.auth.dto.JwtResponse;
import com.supermarket.auth.dto.SignupRequest;
import com.supermarket.auth.entity.User;
import com.supermarket.auth.repository.UserRepository;
import com.supermarket.auth.security.JwtTokenProvider;
import com.supermarket.auth.security.UserDetailsImpl;
import com.supermarket.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        JwtResponse jwtResponse = new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                "",
                userDetails.getRole(),
                userDetails.getTenantId()
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", jwtResponse, null));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, String>>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Username is already taken", null, "USERNAME_EXISTS"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Email is already in use", null, "EMAIL_EXISTS"));
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setEmail(signUpRequest.getEmail());
        user.setTenantId(signUpRequest.getTenantId());
        user.setRole(signUpRequest.getRole() != null ? signUpRequest.getRole() : "USER");
        user.setEnabled(true);

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");

        return ResponseEntity.ok(new ApiResponse<>(true, "User registered successfully", response, null));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, String>>> validateToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            if (jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String tenantId = jwtTokenProvider.getTenantIdFromToken(jwt);
                
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                data.put("tenantId", tenantId);
                
                return ResponseEntity.ok(new ApiResponse<>(true, "Token is valid", data, null));
            }
        }
        
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Invalid token", null, "INVALID_TOKEN"));
    }
}
