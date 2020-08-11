import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.3.1.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.spring") version "1.3.72"
    id("com.google.cloud.tools.jib") version "2.4.0"
}

group = "com.example"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11
val buildNumber by extra("0")

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

extra["springCloudVersion"] = "Hoxton.SR6"
extra["springBootAdminVersion"] = "2.2.4"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("de.codecentric:spring-boot-admin-starter-client")
    implementation("org.springframework.cloud:spring-cloud-config-client")
    implementation("com.example:cqrs_command:1.0.0")
    implementation("org.postgresql:postgresql:42.2.10")
    implementation("io.eventuate.tram.sagas:eventuate-tram-sagas-spring-participant:0.13.0.RELEASE")
    implementation("io.eventuate.tram.core:eventuate-tram-spring-optimistic-locking:0.24.0.RELEASE")
    implementation("io.eventuate.tram.core:eventuate-tram-spring-consumer-kafka:0.24.0.RELEASE")
    implementation("io.eventuate.tram.core:eventuate-tram-spring-consumer-jdbc:0.24.0.RELEASE")
    implementation("io.eventuate.tram.core:eventuate-tram-spring-producer-jdbc:0.24.0.RELEASE")
    implementation("io.springfox:springfox-swagger2:2.9.2")
    implementation("io.springfox:springfox-swagger-ui:2.9.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

jib {
    to {
        image = "elvaliev/store"
        tags = setOf("$version", "$version.${extra["buildNumber"]}")
        auth {
            username = System.getenv("DOCKERHUB_USERNAME")
            password = System.getenv("DOCKERHUB_PASSWORD")
        }
    }
    container {
        labels = mapOf(
            "maintainer" to "Elina Valieva <veaufa@mail.ru>",
            "org.opencontainers.image.title" to "store",
            "org.opencontainers.image.description" to "Spring microservices",
            "org.opencontainers.image.version" to "$version",
            "org.opencontainers.image.authors" to "Elina Valieva <veaufa@mail.ru>>",
            "org.opencontainers.image.url" to "https://github.com/ElinaValieva/spring-microservices"
        )
        jvmFlags = listOf(
            "-server",
            "-Djava.awt.headless=true",
            "-XX:InitialRAMFraction=2",
            "-XX:MinRAMFraction=2",
            "-XX:MaxRAMFraction=2",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=100",
            "-XX:+UseStringDeduplication"
        )
        workingDirectory = "/store"
    }
}

