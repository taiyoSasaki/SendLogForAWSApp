plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "jp.co.ods.sendlogforawsapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "jp.co.ods.sendlogforawsapp"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //kotlinコルーチン関係
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Amplify plugins
    implementation("com.amplifyframework:core:1.6.5")
    implementation("com.amplifyframework:aws-api:1.6.5")
    implementation("com.amplifyframework:aws-datastore:1.6.5")

    //aws cognito & client
    implementation("com.amplifyframework:aws-auth-cognito:2.14.1")
    implementation ("com.amazonaws:aws-android-sdk-mobile-client:2.63.0")

    //aws storage S3
    implementation("com.amplifyframework:aws-storage-s3:2.14.2")
}