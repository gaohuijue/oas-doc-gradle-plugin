plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.13.0"
    kotlin("jvm") version "1.3.70"
    `maven-publish`
}

group = "com.shinow"
version = "1.0.2"

repositories {
    mavenCentral()
    maven {
        name = "Spring Repositories"
        url = uri("https://repo.spring.io/libs-release/")
    }
    gradlePluginPortal()
}

publishing {
    repositories {
        maven {
            val OSSRH_USER: String by project
            val OSSRH_PASS: String by project
            credentials {
                username = OSSRH_USER
                password = OSSRH_PASS
            }
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(group = "khttp", name = "khttp", version = "1.0.0")
    implementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")
    implementation(files("$projectDir/libs/gradle-processes-0.5.0.jar"))

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13")
    testImplementation("com.beust:klaxon:5.2")
}

gradlePlugin {
    plugins {
        create("oas-doc-gradle-plugin") {
            id = "com.shinow.oas-doc-gradle-plugin"
            displayName = "A Gradle plugin for the OAS document."
            description =
                "This plugin uses swagger-ui to generate an OpenAPI description at spring application runtime."
            implementationClass = "com.shinow.oasdoc.gradle.plugin.OpenApiGradlePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/gaohuijue/oas-doc-gradle-plugin"
    vcsUrl = "https://github.com/gaohuijue/oas-doc-gradle-plugin.git"
    tags = listOf("gradle-plugin", "openapi", "swagger")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
