plugins {
    `java-gradle-plugin`
    `maven-publish`
    groovy
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "io.github.zed-0xff"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    website = "https://github.com/zed-0xff/zb-gradle-plugin"
    vcsUrl = "https://github.com/zed-0xff/zb-gradle-plugin"
    
    plugins {
        create("signing") {
            id = "io.github.zed-0xff.zb-gradle-plugin"
            displayName = "ZB Gradle Plugin"
            description = "JAR signing support for Project Zomboid Java mods"
            tags = listOf("zomboid", "signing", "modding", "jarsigner", "keychain")
            implementationClass = "me.zed_0xff.zb_gradle_plugin.ZBSigningPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "localRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
