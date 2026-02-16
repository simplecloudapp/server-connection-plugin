import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    id("maven-publish")
}

val baseVersion = "0.0.1"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotversion = "${baseVersion}-platform.$commitHash"

allprojects {
    group = "app.simplecloud.plugin"
    version = if (commitHash != null) snapshotversion else baseVersion

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://libraries.minecraft.net")
        maven("https://repo.papermc.io/repository/maven-public")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://repo.waterdog.dev/releases/")
        maven("https://repo.waterdog.dev/snapshots/")
        maven("https://repo.opencollab.dev/maven-releases/")
        maven("https://repo.opencollab.dev/maven-snapshots/")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.jvm)
        implementation(rootProject.libs.kotlin.coroutines.core)
        implementation(rootProject.libs.bundles.logging)
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    publishing {
        repositories {
            maven {
                name = "simplecloud"
                url = uri("https://repo.simplecloud.app/snapshots/")
                credentials {
                    username = System.getenv("SIMPLECLOUD_USERNAME")?: (project.findProperty("simplecloudUsername") as? String)
                    password = System.getenv("SIMPLECLOUD_PASSWORD")?: (project.findProperty("simplecloudPassword") as? String)
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        relocate("org.spongepowered", "app.simplecloud.plugin.relocate.spongepowered")
        relocate("app.simplecloud.plugin.api", "app.simplecloud.plugin.relocate.plugin.api")
        archiveFileName.set("${project.name}.jar")
        archiveClassifier.set("")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
