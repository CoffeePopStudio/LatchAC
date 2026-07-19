plugins {
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":latchac-core"))
    compileOnly(libs.paper.api)
}

tasks {
    jar {
        archiveBaseName.set("LatchAC")
        // 将 core 模块类并入插件 jar；snakeyaml 不打包（Paper 运行时自带）
        from(project(":latchac-core").sourceSets["main"].output)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
