plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val sampleAppId = "ca-app-pub-3940256099942544~3347511713"
val sampleBannerId = "ca-app-pub-3940256099942544/9214589741"
val productionAppId = providers.gradleProperty("admobAppId").orNull
val productionBannerId = providers.gradleProperty("admobBannerId").orNull
val productionAdsConfigured = !productionAppId.isNullOrBlank() && !productionBannerId.isNullOrBlank()

android {
    namespace = "net.utgardnet.downsizing"
    compileSdk = 36
    defaultConfig {
        applicationId = "net.utgardnet.downsizing"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["admobAppId"] = productionAppId ?: sampleAppId
        buildConfigField("boolean", "PRODUCTION_ADS_CONFIGURED", productionAdsConfigured.toString())
        buildConfigField("String", "BANNER_AD_UNIT_ID",
            "\"" + (productionBannerId ?: sampleBannerId) + "\"")
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
