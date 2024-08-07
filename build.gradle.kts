// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//    }
//    dependencies {
//        classpath ("com.android.tools.build:gradle:7.0.0")
//        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
//        // Add the duplicate-finder plugin classpath
//        classpath ("gradle.plugin.com.github.franciscolopezsancho:gradle-duplicate-finder-plugin:1.0.3")
//    }
//}
//
//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//    }
//}
//
//tasks.register("clean", Delete::class) {
//    delete(rootProject.buildDir)
//}
