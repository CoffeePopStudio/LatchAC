plugins {
    id("java-library")
    alias(libs.plugins.run.paper)
}

dependencies {
    api(project(":latchac-core"))
    compileOnly(rootProject.libs.paper.api)
    compileOnly(rootProject.libs.packetevents.spigot)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        from(project(":latchac-core").sourceSets.main.get().output)
    }

    runServer {
        minecraftVersion(rootProject.libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }
}
