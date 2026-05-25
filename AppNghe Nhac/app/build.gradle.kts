plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}
android {
    namespace = "quocanh.ntu.appnghenhac"
    compileSdk = 36
    defaultConfig {
        applicationId = "quocanh.ntu.appnghenhac"
        minSdk = 24
        targetSdk = 36
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
    // ANDROID
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // FIREBASE
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-database")
    // RECYCLERVIEW
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // GLIDE LOAD IMAGE
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor(
        "com.github.bumptech.glide:compiler:4.16.0"
    )
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    // TEST
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}