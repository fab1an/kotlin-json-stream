import java.net.URI

plugins {
    id("org.jetbrains.kotlin.multiplatform").version("2.1.0")
    id("publishing-conventions")
    id("org.jetbrains.dokka").version("2.0.0")
}

group = "com.fab1an"
version = "1.2.3-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    mingwX64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
    jvm {
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js(IR) {
        nodejs()
        binaries.library()
    }

    compilerOptions {
        jvmToolchain(21)
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api("com.squareup.okio:okio:3.9.1")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                implementation("com.code-intelligence:jazzer-junit:0.22.1")
            }
        }
    }
}

dependencies {
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:2.0.0")
}

dokka {
    dokkaSourceSets {
        commonMain {
            externalDocumentationLinks {
                register("okio") {
                    url.set(URI("https://square.github.io/okio/3.x/okio/"))
                    packageListUrl.set(URI("https://square.github.io/okio/3.x/okio/okio/package-list"))
                }
            }
        }
    }
}

tasks.dokkaGeneratePublicationHtml {
    dokka.pluginsConfiguration.versioning {
        olderVersionsDir = projectDir.resolve("documentation")
    }
}

tasks.register<DefaultTask>("buildDocumentation") {
    dependsOn("dokkaGenerate")
    doLast {
        if (!project.version.toString().endsWith("-SNAPSHOT")) {
            delete("documentation/${project.version}")
            copy {
                from(project.layout.buildDirectory.dir("dokka/html"))
                into("documentation/${project.version}")
            }
            delete("documentation/${project.version}/older")
        }
    }
}
