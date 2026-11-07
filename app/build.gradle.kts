import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")?.trim().orEmpty()
val facebookAppId = localProperties.getProperty("FACEBOOK_APP_ID")?.trim().orEmpty()
val facebookClientToken = localProperties.getProperty("FACEBOOK_CLIENT_TOKEN")?.trim().orEmpty()

fun extractWebClientIdFromGoogleServices(googleServicesFile: java.io.File): String {
    if (!googleServicesFile.exists()) return ""
    val text = googleServicesFile.readText()
    val oauthSection = Regex(""""oauth_client"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
        .find(text)?.groupValues?.get(1) ?: return ""
    for (chunk in oauthSection.split("}")) {
        if (!Regex(""""client_type"\s*:\s*3""").containsMatchIn(chunk)) continue
        val id = Regex(""""client_id"\s*:\s*"([^"]+)"""")
            .find(chunk)?.groupValues?.get(1)
        if (!id.isNullOrBlank()) return id
    }
    return ""
}

fun isValidGoogleWebClientId(id: String): Boolean =
    id.isNotBlank() &&
        id.endsWith(".apps.googleusercontent.com") &&
        !id.contains("xxxx", ignoreCase = true)

val googleServicesFile = layout.projectDirectory.file("google-services.json").asFile
val webClientFromJson = extractWebClientIdFromGoogleServices(googleServicesFile)
val effectiveGoogleWebClientId = when {
    isValidGoogleWebClientId(googleWebClientId) -> googleWebClientId
    isValidGoogleWebClientId(webClientFromJson) -> webClientFromJson
    else -> ""
}

android {
    namespace = "com.example.darhoney"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.darhoney"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        resValue("string", "google_maps_key", mapsApiKey)

        resValue("string", "google_web_client_id", effectiveGoogleWebClientId)
        resValue("string", "facebook_app_id", facebookAppId)
        resValue(
            "string",
            "fb_login_protocol_scheme",
            if (facebookAppId.isNotBlank()) "fb$facebookAppId" else ""
        )
        resValue("string", "facebook_client_token", facebookClientToken)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.viewpager2)
    implementation(libs.swiperefresh)
    implementation(libs.maps)
    implementation(libs.location)
    implementation(libs.glide)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.auth)
    implementation(libs.facebook.login)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
