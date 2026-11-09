plugins {
    id("com.android.application")
}

android {
    namespace = "com.exam.smkn1maltng"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.exam.smkn1maltng"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("EXAM_STORE_FILE") as String? ?: "exam-release.keystore")
            storePassword = project.findProperty("EXAM_STORE_PASSWORD") as String? ?: ""
            keyAlias = project.findProperty("EXAM_KEY_ALIAS") as String? ?: "examkey"
            keyPassword = project.findProperty("EXAM_KEY_PASSWORD") as String? ?: ""
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("com.github.GrenderG:Toasty:1.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
}
