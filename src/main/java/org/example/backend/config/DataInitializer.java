package org.example.backend.config;

import org.example.backend.entity.Role;
import org.example.backend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        // Tạo role USER nếu chưa tồn tại
        if (!roleRepository.findByRoleName("USER").isPresent()) {
            Role userRole = new Role();
            userRole.setRoleName("USER");
            userRole.setDescription("Regular user role");
            roleRepository.save(userRole);
        }

        // Tạo role ADMIN nếu chưa tồn tại
        if (!roleRepository.findByRoleName("ADMIN").isPresent()) {
            Role adminRole = new Role();
            adminRole.setRoleName("ADMIN");
            adminRole.setDescription("Administrator role");
            roleRepository.save(adminRole);
        }
    }
} 