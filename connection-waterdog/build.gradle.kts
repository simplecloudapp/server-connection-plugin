dependencies {
    implementation(project(":connection-shared"))
    implementation(libs.bundles.adventure)
    compileOnly(libs.simplecloud.api)
    compileOnly(libs.waterdog.api)
}