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
val luceneUtilsVersion = "0.5.0-SNAPSHOT"

val junitVersion = "5.5.2"
val assertJVersion = "3.12.2"
val mockitoVersion = "2.22.0"


dependencies {
    api(project(":DocumentsSearchCommon"))

    implementation("net.dankito.utils:lucene:$luceneUtilsVersion")


    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
}
