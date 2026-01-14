plugins {
    id("java")
}

group = "com.github.kusoroadeolu"
version = "0.0.7"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor(rootProject)
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    implementation("com.google.code.gson:gson:2.13.2")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}

tasks.test {
    useJUnitPlatform()
}