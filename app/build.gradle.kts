plugins { id("com.android.application") }
android {
    namespace = "com.crystal.volumebooster"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.crystal.volumebooster"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "1.5"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
