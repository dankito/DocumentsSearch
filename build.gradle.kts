
group = "net.dankito.documents"
version = "1.0.0-SNAPSHOT"


buildscript {

    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:3.5.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.70")
    }
}


allprojects {

    tasks.register<DependencyReportTask>("allDeps") { } // to have a simple way to print all dependencies to console
}


subprojects {

    repositories {
        mavenLocal() // TODO: remove again
        mavenCentral()
        jcenter()
    }
}
