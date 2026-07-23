plugins {
    id("java-library")
}

dependencies {
    api(rootProject.libs.adventure.api)
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}
