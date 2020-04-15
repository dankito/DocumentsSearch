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
    sourceCompatibility = JavaVersion.VERSION_1_7
}


/*      versions        */
val kotlinVersion = "1.3.71"
val kotlinCoroutinesVersion = "1.3.5"

val javaUtilsVersion = "1.0.16-SNAPSHOT"

val rxJavaVersion = "2.2.19"

val jsoupVersion = "1.13.1"

val junitVersion = "5.5.2"
val assertJVersion = "3.12.2"
val mockitoVersion = "2.22.0"


dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    api("net.dankito.utils:java-utils:$javaUtilsVersion")

    api("net.dankito.utils:file-system-watcher-common:1.0.0-SNAPSHOT")

    api("net.dankito.mail:mail-fetcher:1.0.0-SNAPSHOT")

    implementation("com.optimaize.languagedetector:language-detector:0.6")

    api("org.jsoup:jsoup:$jsoupVersion")

    api("io.reactivex.rxjava2:rxjava:$rxJavaVersion")


    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")

    testImplementation("org.slf4j:slf4j-simple:1.+")
}
