import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm")
}



val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}


/*      versions        */
val textExtractorVersion = "1.0.0-SNAPSHOT"

val junitVersion = "5.5.2"
val assertJVersion = "3.12.2"
val mockitoVersion = "2.22.0"


dependencies {
    api(project(":DocumentsSearchCommon"))

    implementation("net.dankito.text.extraction:text-extractor-common:$textExtractorVersion")
    implementation("net.dankito.text.extraction:poppler-text-extractor:$textExtractorVersion")
    implementation("net.dankito.text.extraction:openpdf-text-extractor:$textExtractorVersion")
    implementation("net.dankito.text.extraction:tesseract4-commandline-text-extractor:$textExtractorVersion")
    implementation("net.dankito.text.extraction:tika-text-extractor:$textExtractorVersion")


    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")

    testImplementation("org.slf4j:slf4j-simple:1.+")
}
