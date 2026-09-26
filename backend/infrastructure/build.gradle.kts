// Adaptadores (REST, persistencia PostgreSQL), configuración y arranque Spring Boot.
plugins {
    id("switch.java-conventions")
    alias(libs.plugins.spring.boot)
}

configurations.configureEach {
    // El proyecto usa Log4j2: se excluye Logback (starter de logging por defecto)
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
}

dependencies {
    implementation(project(":switch-application"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")

    // Persistencia: Spring Data JPA + pool de conexiones HikariCP (incluido en el starter).
    // El esquema NO lo crea la aplicación: se crea con los scripts de /database.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // Documentación OpenAPI / Swagger UI
    implementation(libs.springdoc.webmvc.ui)

    // Seguridad: Spring Security + JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.bootJar {
    archiveFileName = "switch-transaccional.jar"
}

tasks.jar {
    enabled = false
}
