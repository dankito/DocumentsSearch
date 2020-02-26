import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm")
}



val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.6"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.6"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_6
}


/*      versions        */
val kotlinVersion = "1.3.61"

val javaUtilsVersion = "1.0.9"

val junitVersion = "5.5.2"
val assertJVersion = "3.12.2"
val mockitoVersion = "2.22.0"


dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    api("net.dankito.utils:java-utils:$javaUtilsVersion")


    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testCompile("org.assertj:assertj-core:$assertJVersion")
    testCompile("org.mockito:mockito-core:$mockitoVersion")
}
