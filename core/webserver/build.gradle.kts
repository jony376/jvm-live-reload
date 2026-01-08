plugins {
    id("core-java-library")
}

dependencies {
    api(project(":core:build-link"))
    implementation("io.undertow:undertow-core:2.3.20.Final")
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            artifactId = "jvm-live-reload-webserver"
            pom {
                name = "jvm-live-reload-webserver"
                description = "Development-mode proxy webserver for Live Reload experience on JVM"
            }
        }
    }
}
