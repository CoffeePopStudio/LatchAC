plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    alias(libs.plugins.run.paper)
}

dependencies {
    api(project(":latchac-core"))
    paperweight.paperDevBundle("26.1.2.build.+")
    compileOnly(rootProject.libs.packetevents.spigot)
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

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
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }
}
