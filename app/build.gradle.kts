plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.anserview"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.anserview"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Dependencias osmdroid
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    implementation("org.osmdroid:osmdroid-wms:6.1.14")
    //Dependencias de la libreria de google volley
    implementation("com.android.volley:volley:1.2.1")

    //Gestion de Imagenes
    implementation("com.github.bumptech.glide:glide:4.15.1")

    //Animaciones
    implementation("com.airbnb.android:lottie:6.0.0")
}