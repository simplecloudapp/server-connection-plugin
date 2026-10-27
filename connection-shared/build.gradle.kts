dependencies {
    api(libs.simplecloud.plugin.api)
    implementation(libs.bundles.adventure)
    implementation(libs.bundles.configurate)

    testImplementation(libs.simplecloud.api)
    testImplementation(libs.slf4j.api)
}