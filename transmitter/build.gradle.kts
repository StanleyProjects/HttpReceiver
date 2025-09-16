import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots") // todo
}

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
}

application {
    mainClass.set("sp.service.transmitter.AppKt")
}

tasks.getByName<JavaCompile>("compileJava") {
    targetCompatibility = Version.jvmTarget
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = Version.jvmTarget
}

dependencies {
    implementation(project(":lib"))
    implementation("com.github.kepocnhh:Bytes:0.4.0")
    implementation("com.github.kepocnhh:Secrets:0.1.0")
    implementation("com.github.kepocnhh:TLSMessages:0.0.2-SNAPSHOT")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
