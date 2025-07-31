plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("org.gameboy.Main")
}

repositories {
    mavenCentral()
    mavenLocal()
}

sourceSets {
    val main by getting
    val test by getting
    val testUtilities by creating {
        java.srcDir("src/testUtilities/java")
        compileClasspath += main.output
        runtimeClasspath += main.output
    }
}

val testUtilitiesImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    implementation("com.google.inject:guice:7.0.0")
    
    testImplementation(sourceSets["testUtilities"].output)
    testUtilitiesImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("org.mockito:mockito-core:5.16.0")
    testRuntimeOnly("net.bytebuddy:byte-buddy-agent:1.15.11")
}

tasks.test {
    reports {
        junitXml.required.set(true)
        html.required.set(false)
    }
    useJUnitPlatform()

    doFirst {
        val agentJar = configurations.testRuntimeClasspath.get()
            .firstOrNull { it.name.contains("byte-buddy-agent") }
        if (agentJar != null) {
            jvmArgs = (jvmArgs ?: emptyList()) + "-javaagent:${agentJar.absolutePath}"
        }
    }
}
