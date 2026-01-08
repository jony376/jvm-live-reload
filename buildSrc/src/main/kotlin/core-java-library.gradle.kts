plugins {
    `java-library`
    `maven-publish`
}

group = "me.seroperson"
version = readVersion()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                url = "https://github.com/seroperson/jvm-live-reload"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "seroperson"
                        name = "Daniil Sivak"
                        email = "seroperson@gmail.com"
                        url = "https://seroperson.me/"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/seroperson/jvm-live-reload.git"
                    developerConnection = "scm:git:ssh://git@github.com/seroperson/jvm-live-reload.git"
                    url = "https://github.com/seroperson/jvm-live-reload"
                }
            }
        }
    }
}

fun readVersion() =
    listOf(
        "$projectDir/version.txt",
        "$projectDir/../version.txt",
        "$projectDir/../../version.txt",
    ).map { file(it) }
        .firstOrNull { it.exists() }
        ?.readText()
        ?.trim() ?: "0.0.1-SNAPSHOT"
