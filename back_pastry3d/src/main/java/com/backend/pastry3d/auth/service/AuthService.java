package com.backend.pastry3d.auth.service;

import com.backend.pastry3d.auth.dto.AuthResponse;
import com.backend.pastry3d.auth.dto.LoginRequest;
import com.backend.pastry3d.auth.dto.MeResponse;
import com.backend.pastry3d.auth.dto.RegisterRequest;
import com.backend.pastry3d.auth.entity.EmailVerificationToken;
import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.auth.jwt.JwtUtil;
import com.backend.pastry3d.auth.repository.EmailVerificationTokenRepository;
import com.backend.pastry3d.auth.repository.UserRepository;
import com.backend.pastry3d.profile.entity.Profile;
import com.backend.pastry3d.profile.service.ProfileService;
import com.backend.pastry3d.shared.constants.AppConstants;
import com.backend.pastry3d.shared.exception.BadRequestException;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import com.backend.pastry3d.shared.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final ProfileService profileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;
    private final boolean mailEnabled;
    private final int verificationExpirationMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            ProfileService profileService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            MailService mailService,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.register-code-expiration-minutes:30}") int verificationExpirationMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.profileService = profileService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailService = mailService;
        this.mailEnabled = mailEnabled;
        this.verificationExpirationMinutes = verificationExpirationMinutes;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .roles(user.getRole())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String displayName = request.getDisplayName().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Ya existe una cuenta registrada con este email");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(AppConstants.ROLE_USER);

        /*
         * Para demo independiente:
         * si APP_MAIL_ENABLED=false, el usuario queda habilitado de una vez.
         * si APP_MAIL_ENABLED=true, queda pendiente hasta confirmar por correo.
         */
        user.setEnabled(!mailEnabled);

        User savedUser = userRepository.save(user);
        profileService.createDefaultProfile(savedUser, displayName);

        if (mailEnabled) {
            String rawToken = createVerificationToken(savedUser);
            mailService.sendVerificationEmail(savedUser.getEmail(), rawToken);

            AuthResponse response = buildAuthResponse(savedUser, null);
            response.setEnabled(false);
            return response;
        }

        String jwt = jwtUtil.generateToken(savedUser.getEmail());
        return buildAuthResponse(savedUser, jwt);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, AuthenticationManager authenticationManager) {
        String email = normalizeEmail(request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (DisabledException exception) {
            throw new UnauthorizedException("La cuenta aún no está habilitada");
        } catch (BadCredentialsException exception) {
            throw new UnauthorizedException("Email o contraseña incorrectos");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Email o contraseña incorrectos"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("La cuenta aún no está habilitada");
        }

        String jwt = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(user, jwt);
    }

    @Transactional
    public void confirmEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Token inválido");
        }

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Token de confirmación no encontrado"));

        if (token.isUsed()) {
            throw new BadRequestException("El token ya fue usado");
        }

        if (token.isExpired()) {
            throw new BadRequestException("El token ya venció");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setEnabled(true);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public MeResponse me(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        MeResponse response = new MeResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setEnabled(user.isEnabled());

        try {
            Profile profile = profileService.getByUserId(user.getId());
            response.setDisplayName(profile.getDisplayName());
        } catch (RuntimeException exception) {
            response.setDisplayName(null);
        }

        return response;
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setEnabled(user.isEnabled());
        return response;
    }

    private String createVerificationToken(User user) {
        String rawToken = generateRawToken();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(verificationExpirationMinutes));

        tokenRepository.save(token);
        return rawToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }

            return builder.toString();
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo procesar el token");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("El email es obligatorio");
        }

        return email.trim().toLowerCase();
    }
}