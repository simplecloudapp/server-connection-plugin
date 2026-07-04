import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "1.0.2"

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")
        maven("https://repo.papermc.io/repository/maven-public")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://repo.waterdog.dev/releases/")
        maven("https://repo.waterdog.dev/snapshots/")
        maven("https://repo.opencollab.dev/maven-releases/")
        maven("https://repo.opencollab.dev/maven-snapshots/")
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.gradleup.shadow")
    }

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.coroutines.core)
        implementation(rootProject.libs.log4j.api)
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion = KotlinVersion.KOTLIN_2_4
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs = listOf("-Xannotation-default-target=param-property")
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        relocate("org.spongepowered", "app.simplecloud.plugin.connection.shaded.spongepowered")
        relocate("app.simplecloud.plugin.api", "app.simplecloud.plugin.connection.shaded.plugin.api")
        archiveFileName.set("${project.name}.jar")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.processResources {
        expand(
            "version" to project.version
        )
    }

}
