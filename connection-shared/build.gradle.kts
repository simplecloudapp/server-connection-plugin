import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.named

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    api("org.spongepowered:configurate-yaml:4.0.0")
    api("org.spongepowered:configurate-extra-kotlin:4.1.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }
    api("commons-io:commons-io:2.15.1")
    api(rootProject.libs.simplecloud.plugin.api)
}

tasks.named("shadowJar", ShadowJar::class) {
    val externalRelocatePath = "app.simplecloud.external"
    relocate("app.simplecloud.plugin.api", "${externalRelocatePath}.plugin.api") {
        include("app.simplecloud.plugin.api.**")
    }
}