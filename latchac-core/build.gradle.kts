// LatchAC 核心模块：平台无关，禁止出现任何 Bukkit/Paper 依赖
dependencies {
    implementation(libs.snakeyaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
