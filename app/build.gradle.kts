plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply true}

val apiHostProd = "https://winpay-prod.windel.com.br"
val portProd = "3333"
val apiHostHml = "https://winpay-develop.windel.com.br"
val portHml = "3002"
val apiHostDev = "http://192.168.1.82"
val portDev = "3334"
val apiKey = "zOds60ZPbh4iHzMImrXafcDMvBi9RCMiJtOjTXiFbwtTFAoUBbEDrNCiKIbiqLUKlemc7Sa4OEMGvcfDu1BzGlqme4yfDR9yVbH1jfUqnysSabetplGY5DLAODtbHTmF"
val socketName = "payment-vero"

android {
    namespace = "br.com.windel.pos"
    compileSdk = 33

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "br.com.windel.pos"
        minSdk = 22
        this.targetSdk = 22
        versionCode = 9
        versionName = "1.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "WINDEL_POS_HOST", "\"$apiHostProd:$portProd\"")
            buildConfigField("String", "WINDEL_POS_API_KEY", "\"$apiKey\"")
            buildConfigField("String", "WINDEL_POS_AUTH_USER", "d2luZGVsdXNlcg==")
            buildConfigField("String", "WINDEL_POS_AUTH_PASS", "dzFuZDNsQEAyMzIw")
        }
        debug {
            buildConfigField("String", "WINDEL_POS_HOST", "\"$apiHostHml\"")
            buildConfigField("String", "WINDEL_POS_API_KEY", "\"$apiKey\"")
            buildConfigField("String", "WINDEL_POS_AUTH_USER", "\"d2luZGVsdXNlcg==\"")
            buildConfigField("String", "WINDEL_POS_AUTH_PASS", "\"dzFuZDNsQEAyMzIw\"")
        }
    }
    compileOptions {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.airbnb.android:lottie:6.1.0")
     implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("com.sunmi:printerlibrary:1.0.22")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")
    implementation("io.socket:socket.io-client:2.0.0") {
        exclude(group = "org.json", module = "json")
    }
    implementation("com.rabbitmq:amqp-client:5.20.0")
    val roomVersion = "2.4.1"
    ksp("androidx.room:room-compiler:2.5.0")
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-rxjava2:$roomVersion")
    implementation("androidx.room:room-rxjava3:$roomVersion")
    implementation("androidx.room:room-guava:$roomVersion")
    implementation("androidx.room:room-paging:2.4.1")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

}
