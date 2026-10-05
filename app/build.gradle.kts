plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.toontalkai.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.toontalkai.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
        val adMobAppId = (project.findProperty("ADMOB_APP_ID") as String?)
            ?: System.getenv("ADMOB_APP_ID")
            ?: "ca-app-pub-3940256099942544~3347511713"
        val adMobBannerId = (project.findProperty("ADMOB_BANNER_ID") as String?)
            ?: System.getenv("ADMOB_BANNER_ID")
            ?: "ca-app-pub-3940256099942544/6300978111"
        manifestPlaceholders["adMobAppId"] = adMobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$adMobBannerId\"")
        buildConfigField("String", "SUPABASE_URL", "\"${(project.findProperty("SUPABASE_URL") as String?) ?: System.getenv("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${(project.findProperty("SUPABASE_PUBLISHABLE_KEY") as String?) ?: System.getenv("SUPABASE_PUBLISHABLE_KEY") ?: ""}\"")
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