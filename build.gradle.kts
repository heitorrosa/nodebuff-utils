plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
}

version = "1.0.0"
group = "dev.heitorrosa"

base {
    archivesName.set("nodebuffutils")
}

repositories {
    // terraformersmc.com hangs on HEAD/POM probes; resolve modmenu by artifact only
    maven("https://maven.terraformersmc.com/releases") {
        metadataSources {
            artifact()
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:26.3")
    implementation("net.fabricmc:fabric-loader:0.19.3")
    compileOnly("com.terraformersmc:modmenu:21.0.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
