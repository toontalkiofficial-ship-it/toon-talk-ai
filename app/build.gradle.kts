plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.toontalkai.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.toontalkai.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["adMobAppId"] = (project.findProperty("ADMOB_APP_ID") as String?) ?: "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "SUPABASE_URL", "\"${(project.findProperty("SUPABASE_URL") as String?) ?: ""}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${(project.findProperty("SUPABASE_PUBLISHABLE_KEY") as String?) ?: ""}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${(project.findProperty("ADMOB_BANNER_ID") as String?) ?: "ca-app-pub-3940256099942544/6300978111"}\"")
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}
