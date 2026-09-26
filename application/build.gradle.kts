// Casos de uso y puertos (entrada/salida). Sin dependencias de Spring.
plugins {
    id("switch.java-conventions")
}

dependencies {
    api(project(":switch-domain"))
    implementation("org.apache.logging.log4j:log4j-api")

    testImplementation("org.mockito:mockito-junit-jupiter")
}
