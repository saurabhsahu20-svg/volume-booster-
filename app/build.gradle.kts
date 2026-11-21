plugins { id("com.android.application") }
android {
    namespace = "com.crystal.volumebooster"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.crystal.volumebooster"
        minSdk = 28
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
