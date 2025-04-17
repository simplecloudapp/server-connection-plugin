plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":connection-shared"))
    api("net.kyori:adventure-text-minimessage:4.16.0")
    api("net.kyori:adventure-platform-bungeecord:4.3.2")
    compileOnly("net.md-5:bungeecord-api:1.20-R0.2")
}

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("4Vw4Mgla")
    versionNumber.set(rootProject.version.toString())
    versionType.set("beta")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.2",
        "1.21.3",
        "1.21.4",
        "1.21.5",
    )
    loaders.add("bungeecord")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}