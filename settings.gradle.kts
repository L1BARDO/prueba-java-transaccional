plugins {
    // Descarga automáticamente el JDK 21 si no está instalado localmente (toolchains)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "switch-transaccional"

include("domain", "application", "infrastructure")

project(":domain").name = "switch-domain"
project(":application").name = "switch-application"
project(":infrastructure").name = "switch-infrastructure"
