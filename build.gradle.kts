plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

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

dependencies {
    testImplementation(sourceSets["testUtilities"].output)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

tasks.test {
    reports {
        junitXml.required.set(true)
        html.required.set(false)
    }
    useJUnitPlatform()
}