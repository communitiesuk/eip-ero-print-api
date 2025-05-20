import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.lang.ProcessBuilder.Redirect

plugins {
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("plugin.allopen") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "11.0.0"
    id("org.openapi.generator") version "7.9.0"
    id("org.owasp.dependencycheck") version "12.1.1"
    id("org.jsonschema2dataclass") version "6.0.0"
}

group = "uk.gov.dluhc"
version = "latest"
java.sourceCompatibility = JavaVersion.VERSION_17

extra["awsSdkVersion"] = "2.29.6"
extra["springCloudAwsVersion"] = "3.2.1"

allOpen {
    annotations("jakarta.persistence.Entity", "jakarta.persistence.MappedSuperclass", "jakarta.persistence.Embedabble")
}

val awsProfile = System.getenv("AWS_PROFILE_ARG") ?: "--profile code-artifact"
val codeArtifactToken = "aws codeartifact get-authorization-token --domain erop-artifacts --domain-owner 063998039290 --query authorizationToken --output text $awsProfile".runCommand()

repositories {
    mavenCentral()
    maven {
        url = uri("https://erop-artifacts-063998039290.d.codeartifact.eu-west-2.amazonaws.com/maven/api-repo/")
        credentials {
            username = "aws"
            password = codeArtifactToken
        }
    }
}

apply(plugin = "org.jlleitschuh.gradle.ktlint")
apply(plugin = "org.openapi.generator")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")
apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

dependencies {
    // framework
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.mapstruct:mapstruct:1.6.2")
    kapt("org.mapstruct:mapstruct-processor:1.6.2")

    // internal libs
    implementation("uk.gov.dluhc:logging-library:3.0.4")
    implementation("uk.gov.dluhc:bank-holidays-data-client-library:1.0.1")
    implementation("uk.gov.dluhc:messaging-support-library:2.3.0")
    implementation("uk.gov.dluhc:email-client:1.0.1")

    // api
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-ui:1.8.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.integration:spring-integration-sftp")
    implementation("com.opencsv:opencsv:5.9")

    constraints {
        implementation("org.webjars:swagger-ui:5.20.0") {
            because("Lower versions (imported by org.springdoc:springdoc-openapi-ui:1.8.0) triggers CVE-2024-45801, CVE-2024-47875, CVE-2025-26791")
        }
    }

    // Logging
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.0")

    // spring security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // mysql
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("software.aws.rds:aws-mysql-jdbc:1.1.10")
    runtimeOnly("software.amazon.awssdk:rds")

    // AWS dependencies
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudAwsVersion")}"))
    testImplementation(platform("software.amazon.awssdk:bom:${property("awsSdkVersion")}"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")

    // AWS library
    implementation("software.amazon.awssdk:s3")
    testImplementation("software.amazon.awssdk:auth")
    testImplementation("software.amazon.awssdk:sts")

    // email
    implementation("software.amazon.awssdk:ses")

    // mongo core datatypes, so that we can generate a Mongo ObjectId (a 12 byte/24 char hex string ID)
    implementation("org.mongodb:bson:4.7.1")

    // Scheduling
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")

    // OpenPDF
    implementation("com.github.librepdf:openpdf:2.0.3")

    // caching
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Test implementations
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.testcontainers:junit-jupiter:1.20.3")
    testImplementation("org.testcontainers:testcontainers:1.20.3")
    testImplementation("org.testcontainers:mysql:1.20.3")

    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation("net.datafaker:datafaker:2.4.1")

    // Libraries to support creating JWTs in tests
    testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

tasks.withType<KotlinCompile> {
    dependsOn(tasks.withType<GenerateTask>())

    // Cannot use "withType" notation like above as Task class is internal
    dependsOn("generateJsonSchema2DataClassConfigMain")

    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    dependsOn(tasks.withType<GenerateTask>())
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
}

tasks.withType<GenerateTask> {
    enabled = false
    validateSpec.set(true)
    outputDir.set("$projectDir/build/generated")
    generatorName.set("kotlin-spring")
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "invokers" to "false",
            "models" to "",
        )
    )
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "enumPropertyNaming" to "UPPERCASE",
            "useBeanValidation" to "true",
            "useSpringBoot3" to "true",
        )
    )
}

tasks.create("generate-models-from-openapi-document-PrintAPIs.yaml", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/PrintAPIs.yaml")
    packageName.set("uk.gov.dluhc.printapi")
}

tasks.create("generate-models-from-openapi-document-print-api-sqs-messaging.yaml", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/sqs/print-api-sqs-messaging.yaml")
    packageName.set("uk.gov.dluhc.printapi.messaging")
}

tasks.create("generate-models-from-openapi-document-vca-api-sqs-messaging-erop.yaml", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/sqs/vca-api-sqs-messaging-erop.yaml")
    packageName.set("uk.gov.dluhc.votercardapplicationsapi.messaging")
}

tasks.create("generate-models-from-openapi-document-EROManagementAPIs.yaml", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/external/EROManagementAPIs.yaml")
    packageName.set("uk.gov.dluhc.eromanagementapi")
}

// Codegen the Print Provider schemas from yamlschema into java pojos
jsonSchema2Pojo {
    executions {
        create("main") {
            klass {
                targetPackage.set("uk.gov.dluhc.printapi.printprovider.models")
                nameUseTitle.set(true)
                annotateSerializable.set(true)
                annotateGenerated.set(false)
            }

            io {
                source.setFrom("$projectDir/src/main/resources/yamlschema")
                sourceType.set("yamlschema")
            }

            constructors {
                requiredProperties.set(true)
                copyConstructor.set(true)
            }

            methods {
                additionalProperties.set(false)
                builders.set(true)
                buildersInnerClass.set(true)
                annotateJsr303.set(true)
                toStringMethod.set(false)
                annotateJsr303Jakarta.set(true)
            }

            dateTime {
                dateTimeType.set("java.time.OffsetDateTime")
                dateType.set("java.time.LocalDate")
                dateTimeFormat.set(true)
                dateFormat.set(true)
            }
        }
    }
}

// Add the generated code to the source sets
sourceSets["main"].java {
    this.srcDir("$projectDir/build/generated")
}

// Linting is dependent on GenerateTask
tasks.withType<KtLintCheckTask> {
    dependsOn(tasks.withType<GenerateTask>())

    // Cannot use "withType" notation like above as Task class is internal
    dependsOn("generateJsonSchema2DataClassConfigMain")
}

tasks.withType<BootBuildImage> {
    builder.set("paketobuildpacks/builder-jammy-base")
    environment.set(mapOf("BP_HEALTH_CHECKER_ENABLED" to "true"))
    buildpacks.set(
        listOf(
            "urn:cnb:builder:paketo-buildpacks/java",
            "docker.io/paketobuildpacks/health-checker",
        )
    )
}

// Exclude generated code from linting
ktlint {
    filter {
        exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated/") }
    }
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedTargetPolicy", "IGNORE")
    }
}

fun String.runCommand(): String {
    val parts = this.split("\\s".toRegex())
    val process = ProcessBuilder(*parts.toTypedArray())
        .redirectOutput(Redirect.PIPE)
        .start()
    process.waitFor()
    return process.inputStream.bufferedReader().readText().trim()
}

/* Configuration for the OWASP dependency check */
dependencyCheck {
    autoUpdate = true
    failOnError = true
    failBuildOnCVSS = 0.toFloat()
    analyzers.assemblyEnabled = false
    analyzers.centralEnabled = true
    format = HTML.name
    suppressionFiles = listOf("owasp.suppressions.xml")
}
