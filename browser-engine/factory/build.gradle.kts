plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.browserengine.factory"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":browser-engine:core"))
    implementation(project(":browser-engine:webview"))
    implementation(libs.androidx.core.ktx)
}
