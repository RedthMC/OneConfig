@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.kotlin) apply false
    id(libs.plugins.pgt.api.validator.get().pluginId)
    id(libs.plugins.pgt.root.get().pluginId)
}

preprocess {
    // FOR ALL NEW VERSIONS ENSURE TO UPDATE settings.gradle.kts !

    val forge10809 = createNode("1.8.9-forge", 10809, "srg")
    val fabric10809 = createNode("1.8.9-fabric", 10809, "yarn")
    val forge11202 = createNode("1.12.2-forge", 11202, "srg")
    val fabric11202 = createNode("1.12.2-fabric", 11202, "yarn")
    val forge11605 = createNode("1.16.5-forge", 11605, "srg")
    val fabric11605 = createNode("1.16.5-fabric", 11605, "yarn")
    val forge11701 = createNode("1.17.1-forge", 11701, "srg")
    val fabric11701 = createNode("1.17.1-fabric", 11701, "yarn")
    val forge11801 = createNode("1.18.1-forge", 11801, "srg")
    val fabric11801 = createNode("1.18.1-fabric", 11801, "yarn")
    val forge11904 = createNode("1.19.4-forge", 11904, "srg")
    val fabric11904 = createNode("1.19.4-fabric", 11904, "yarn")
    val fabric12004 = createNode("1.20.4-fabric", 12004, "yarn")
    val forge12004 = createNode("1.20.4-forge", 12004, "srg")

    fabric12004.link(fabric11904)
    fabric11904.link(fabric11801, file("mappings/fabric-1.19.4-1.18.1.txt"))
    fabric11801.link(fabric11701)
    fabric11701.link(fabric11605, file("mappings/fabric-1.17.1-1.16.5.txt"))
    fabric11605.link(forge11605, file("mappings/fabric-forge-1.16.5.txt"))

    fabric11202.link(fabric10809)
    fabric10809.link(forge10809, file("mappings/fabric-forge-1.8.9.txt"))

    forge12004.link(forge11904, file("mappings/forge-1.20.4-1.19.4.txt"))
    forge11904.link(forge11801, file("mappings/forge-1.19.4-1.18.1.txt"))
    forge11801.link(forge11701, file("mappings/forge-1.18.1-1.17.1.txt"))
    forge11701.link(forge11605, file("mappings/forge-1.17.1-1.16.5.txt"))
    forge11605.link(forge11202, file("mappings/forge-1.16.5-1.12.2.txt"))
    forge11202.link(forge10809, file("mappings/forge-1.12.2-1.8.9.txt"))
}

apiValidation {
    nonPublicMarkers.add("org.polyfrost.oneconfig.api.PlatformDeclaration")
    ignoredPackages.add("org.polyfrost.oneconfig.internal")
    ignoredPackages.add("org.polyfrost.oneconfig.api.platform.v1.internal")
    ignoredPackages.add("org.polyfrost.oneconfig.api.hypixel.v0.internal")
    ignoredPackages.add("org.polyfrost.oneconfig.test")
    for (proj in rootProject.project(":modules").subprojects) {
        ignoredPackages.add("org.polyfrost.oneconfig.api.${proj.name}.v1.internal")
    }
}