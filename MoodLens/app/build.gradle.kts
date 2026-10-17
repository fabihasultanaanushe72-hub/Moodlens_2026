plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.moodlens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.moodlens"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    androidResources { noCompress += "tflite" }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // Face location/cropping before FER inference.
    implementation("com.google.mlkit:face-detection:16.1.7")

    // LiteRT 1.x keeps the org.tensorflow.lite.Interpreter API, so it fits
    // this existing project without the larger LiteRT 2.x CompiledModel rewrite.
    implementation("com.google.ai.edge.litert:litert:1.4.2")

    // Corrects camera/gallery image orientation before face detection.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("androidx.room:room-runtime:2.7.2")
    annotationProcessor("androidx.room:room-compiler:2.7.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
