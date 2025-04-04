import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.sonatype.central.portal.publisher)
    `maven-publish`
}

val baseVersion = "0.0.1"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotversion = "${baseVersion}-dev.$commitHash"

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
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "net.thebugmc.gradle.sonatype-central-portal-publisher")
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        compileOnly(rootProject.libs.kotlin.jvm)
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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

    signing {
        if (commitHash != null) {
            return@signing
        }

        sign(publishing.publications)
        useGpgCmd()
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")

        val externalRelocatePath = "app.simplecloud.external"
        relocate("kotlinx", "${externalRelocatePath}.kotlinx")
        relocate("io", "${externalRelocatePath}.io")
        relocate("org", "${externalRelocatePath}.org")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
