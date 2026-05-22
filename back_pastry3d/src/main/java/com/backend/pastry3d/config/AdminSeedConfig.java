package com.backend.pastry3d.config;

import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.auth.repository.UserRepository;
import com.backend.pastry3d.profile.service.ProfileService;
import com.backend.pastry3d.shared.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedConfig {
    @Bean
    public CommandLineRunner seedAdminUser(
            UserRepository userRepository,
            ProfileService profileService,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.seed-enabled:true}") boolean seedEnabled,
            @Value("${app.admin.email:admin@pastry3d.local}") String adminEmail,
            @Value("${app.admin.password:Admin12345}") String adminPassword
    ) {
        return args -> {
            if (!seedEnabled) return;
            if (userRepository.existsByEmailIgnoreCase(adminEmail)) return;

            User admin = new User();
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(AppConstants.ROLE_ADMIN);
            admin.setEnabled(true);
            User saved = userRepository.save(admin);
            profileService.createDefaultProfile(saved, "Administrador Pastry3D");
        };
    }
}
