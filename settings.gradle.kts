plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    "connection-shared",
     "connection-velocity", 
     "connection-bungeecord", 
     "connection-waterdog"
     )

rootProject.name = "server-connection-plugin"