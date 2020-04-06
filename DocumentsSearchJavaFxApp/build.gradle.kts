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
val javaFxUtilsVersion = "1.0.5-SNAPSHOT"

val rxJavaVersion = "2.2.19"

val logbackVersion = "1.2.3"


dependencies {
    implementation(project(":DocumentsSearchCommon"))
    implementation(project(":LuceneDocumentsIndexerAndSearcher"))
    implementation(project(":FileContentExtractor"))

    implementation("net.dankito.utils:java-fx-utils:$javaFxUtilsVersion") {
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
    }

    implementation("io.reactivex.rxjava2:rxjava:$rxJavaVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}


val mainClassName = "net.dankito.documents.search.ui.DocumentsSearchJavaFXApplication"

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    isZip64 = true // to fix "archive contains more than 65535 entries"

    // thanks so much to Andreas Volkmann and Robert for explaining this issue to me: https://stackoverflow.com/questions/51455197/gradle-fatjar-could-not-find-or-load-main-class
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")

    manifest {
        attributes["Implementation-Title"] = "Documents Search"
        attributes["Implementation-Version"] = "1.0.0-SNAPSHOT" // TODO: use project wide property
        attributes["Main-Class"] = mainClassName
    }

    from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
