plugins {
    id("java-library")
}

dependencies {
    api(rootProject.libs.adventure.api)
    compileOnly(rootProject.libs.paper.api)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}
