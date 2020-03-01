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
val javaFxUtilsVersion = "1.0.4"

val logbackVersion = "1.2.3"


dependencies {
    implementation(project(":DocumentsSearchCommon"))
    implementation(project(":ElasticsearchDocumentsSearcher"))

    implementation("net.dankito.utils:java-fx-utils:$javaFxUtilsVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}
