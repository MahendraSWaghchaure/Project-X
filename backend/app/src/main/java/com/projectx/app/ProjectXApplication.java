package com.projectx.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This class lives in com.projectx.app, a SIBLING of every module's base package
 * (com.projectx.identity, com.projectx.school, ...) - not a parent of them. Spring Boot's default
 * component/entity/repository scanning only looks at the main class's package and below, so
 * without the explicit basePackages="com.projectx" below, every other module's beans, @Entity
 * classes, and Spring Data repositories would silently go unregistered.
 */
@SpringBootApplication(scanBasePackages = "com.projectx")
@EntityScan("com.projectx")
@EnableJpaRepositories("com.projectx")
@EnableJpaAuditing
@EnableScheduling
public class ProjectXApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectXApplication.class, args);
    }
}
