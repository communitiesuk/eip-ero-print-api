import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.lang.ProcessBuilder.Redirect

plugins {
    id("org.springframework.boot") version "2.7.10"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.8.10"
    kotlin("kapt") version "1.8.10"
    kotlin("plugin.spring") version "1.8.10"
    kotlin("plugin.jpa") version "1.8.10"
    kotlin("plugin.allopen") version "1.8.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "11.0.0"
    id("org.openapi.generator") version "6.2.1"
    id("org.owasp.dependencycheck") version "8.1.2"
    id("org.jsonschema2dataclass") version "4.5.0"
}

group = "uk.gov.dluhc"
version = "latest"
java.sourceCompatibility = JavaVersion.VERSION_17

ext["snakeyaml.version"] = "1.33"
extra["springCloudVersion"] = "2.4.2"
extra["awsSdkVersion"] = "2.18.9"

allOpen {
    annotations("javax.persistence.Entity", "javax.persistence.MappedSuperclass", "javax.persistence.Embedabble")
}

val codeArtifactToken = System.getenv("CODEARTIFACT_PAT")
    ?: "aws codeartifact get-authorization-token --domain erop-artifacts --domain-owner 063998039290 --query authorizationToken --output text --profile code-artifact".runCommand()

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
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.mapstruct:mapstruct:1.5.3.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.3.Final")

    // internal libs
    implementation("uk.gov.dluhc:logging-lib:0.0.0")

    // api
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.15")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.opencsv:opencsv:5.7.1") {
        exclude("commons-collections", "commons-collections")
        exclude("org.apache.commons", "commons-text")
    }
    implementation("org.springframework.integration:spring-integration-sftp")

    // webclient
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")

    // spring security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // mysql
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("software.aws.rds:aws-mysql-jdbc:1.1.1")
    runtimeOnly("software.amazon.awssdk:rds")

    // AWS messaging
    implementation("io.awspring.cloud:spring-cloud-starter-aws-messaging")

    // AWS v2 dependencies
    implementation("software.amazon.awssdk:s3")

    // mongo core datatypes, so that we can generate a Mongo ObjectId (a 12 byte/24 char hex string ID)
    implementation("org.mongodb:bson:4.7.1")

    // Scheduling
    implementation("net.javacrumbs.shedlock:shedlock-spring:4.42.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:4.42.0")

    // OpenPDF
    implementation("com.github.librepdf:openpdf:1.3.30")

    // Test implementations
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")

    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")

    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.testcontainers:mysql:1.17.6")

    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    testImplementation("net.datafaker:datafaker:1.8.0")

    // Libraries to support creating JWTs in tests
    testImplementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    testImplementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
}

dependencyManagement {
    imports {
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudVersion")}")
        mavenBom("software.amazon.awssdk:bom:${property("awsSdkVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn(tasks.withType<GenerateTask>())
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
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "UPPERCASE",
            "useBeanValidation" to "true",
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

tasks.create("generate-models-from-openapi-document-EROManagementAPIs.yaml", GenerateTask::class) {
    enabled = true
    inputSpec.set("$projectDir/src/main/resources/openapi/external/EROManagementAPIs.yaml")
    packageName.set("uk.gov.dluhc.eromanagementapi")
}

// Codegen the Print Provider schemas from yamlschema into java pojos
jsonSchema2Pojo {
    targetPackage.set("uk.gov.dluhc.printapi.printprovider.models")
    source.setFrom("$projectDir/src/main/resources/yamlschema")
    sourceType.set("yamlschema")
    useTitleAsClassname.set(true)
    includeConstructors.set(true)
    constructorsRequiredPropertiesOnly.set(true)
    includeCopyConstructor.set(true)
    includeAdditionalProperties.set(false)
    serializable.set(true)
    generateBuilders.set(true)
    useInnerClassBuilders.set(true)
    includeJsr303Annotations.set(true)
    includeGeneratedAnnotation.set(false)
    includeToString.set(false)
    dateTimeType.set("java.time.OffsetDateTime")
    dateType.set("java.time.LocalDate")
    formatDateTimes.set(true)
    formatDates.set(true)
}

// Add the generated code to the source sets
sourceSets["main"].java {
    this.srcDir("$projectDir/build/generated")
}

// Linting is dependent on GenerateTask
tasks.withType<KtLintCheckTask> {
    dependsOn(tasks.withType<GenerateTask>())
}
/* Linting is also dependent on Js2pGenerationTask but the dependency cannot be declared in the above manner because
   `Js2pGenerationTask` is an internal class (so we cannot import and reference it), its abstract, and it's implementation
   `Js2pGenerationTask_Decorated` appears to be dynamically generated. Over and above the task class being dynamically
   generated, the task instantiation is handled by the plugin class `Js2pPlugin`.
   We need to hook into the task addition lifecycle and define the dependency at that point.
 */
tasks.whenTaskAdded {
    if (this.javaClass.name == "org.jsonschema2dataclass.js2p.Js2pGenerationTask_Decorated") {
        val jsonschema2dataclassTask = this
        tasks.withType<KtLintCheckTask> {
            dependsOn(jsonschema2dataclassTask)
        }
    }
}

tasks.withType<BootBuildImage> {
    environment = mapOf("BP_HEALTH_CHECKER_ENABLED" to "true")
    buildpacks = listOf(
        "urn:cnb:builder:paketo-buildpacks/java",
        "gcr.io/paketo-buildpacks/health-checker",
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
