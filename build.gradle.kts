plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("java-library")
    id("maven-publish")
    id("signing")
}

group = "com.aiagentkit"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    // Spring Boot
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-webflux")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // JSON processing
    api("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

    // HTTP Client for AI providers
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.okhttp3:logging-interceptor:4.12.0")
    api("com.squareup.okhttp3:okhttp-sse:4.12.0")

    // Vector store integrations
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.postgresql:postgresql:42.7.5")

    // Embedding support
    api("io.github.strikerrocker:simple-embedding:0.1.0")

    // WebSocket for streaming
    api("org.springframework.boot:spring-boot-starter-websocket")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Spring Boot AI Agent Toolkit"
                description = "A production-ready Spring Boot Starter for integrating AI agents, RAG pipelines, and MCP tools into Java applications."
                url = "https://github.com/liufeng/spring-boot-ai-agent-toolkit"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "liufeng"
                        name = "F L"
                        email = "liufeng@gmail.com"
                    }
                }
            }
        }
    }
}
