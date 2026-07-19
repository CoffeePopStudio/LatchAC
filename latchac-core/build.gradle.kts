plugins {
    id("java-library")
}

dependencies {
    compileOnly(rootProject.libs.paper.api)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}
