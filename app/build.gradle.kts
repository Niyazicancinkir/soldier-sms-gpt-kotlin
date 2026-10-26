import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.soldier_sms_gpt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.soldier_sms_gpt"

        minSdk = 24
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(
        "androidx.security:security-crypto:1.1.0-alpha06"
    )

    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    )

    implementation(
        libs.androidx.activity.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    implementation(
        libs.androidx.constraintlayout
    )

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.material
    )

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )
}