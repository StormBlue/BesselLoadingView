plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.bluestrom.gao.customview"
    compileSdk = 37
    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    resourcePrefix = "bessel_"
    publishing { singleVariant("release") { withSourcesJar() } }
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.github.stormblue"
                artifactId = "bessel-loading-view"
                version = "2.0.0"
                pom {
                    name = "BesselLoadingView"
                    description = "A lightweight, lifecycle-aware Bezier ring for Android"
                    url = "https://github.com/StormBlue/BesselLoadingView"
                    scm { url = "https://github.com/StormBlue/BesselLoadingView" }
                    licenses { license { name = "The Apache License, Version 2.0"; url = "https://www.apache.org/licenses/LICENSE-2.0.txt" } }
                    developers { developer { id = "StormBlue"; name = "StormBlue" } }
                }
            }
        }
    }
}
