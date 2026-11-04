import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// ---------------------------------------------------------------------------------------------
// Personal / environment-specific settings live OUTSIDE git:
//   local.properties     (api.baseUrl, google.webClientId)  - see README "Configuration"
//   keystore.properties  (release signing key)              - see README "Publishing"
// ---------------------------------------------------------------------------------------------
fun loadProps(fileName: String) = Properties().apply {
    val file = rootProject.file(fileName)
    if (file.exists()) file.inputStream().use { load(it) }
}

val localProps = loadProps("local.properties")
val keystoreProps = loadProps("keystore.properties")

// The hosted API (Azure App Service). CHANGE THIS to your own URL - it must end with "/".
val productionApiUrl = "https://YOUR-APP-NAME.azurewebsites.net/"

// Debug builds may point at a different API (e.g. http://localhost:3000/ via `adb reverse`) by
// setting api.baseUrl in local.properties. Release builds ALWAYS use productionApiUrl.
val debugApiUrl: String = localProps.getProperty("api.baseUrl") ?: productionApiUrl

// WEB OAuth client ID from Google Cloud Console (used for "Sign in with Google").
val webClientId: String = localProps.getProperty("google.webClientId")
    ?: "REPLACE_WITH_YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

android {
    namespace = "com.studyflow.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.studyflow.app"
        minSdk = 24
        targetSdk = 36 // Google Play requires API 36 for new apps/updates since 31 Aug 2026
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "default_web_client_id", webClientId)
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugApiUrl\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"$productionApiUrl\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")

    // Room (offline-first local database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit (REST API client)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // WorkManager (background sync + daily reminder)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Password hashing (bcrypt)
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Firebase Cloud Messaging (push notifications) - needs app/google-services.json
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Sign-In (SSO)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
