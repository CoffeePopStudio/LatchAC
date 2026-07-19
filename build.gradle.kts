plugins {
    id("java-library")
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
    }
}
