pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // إجبار المشروع على استخدام المستودعات المعرفة هنا حصراً
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // مستودع مهم جداً لمكتبات الـ Custom UI والـ Audio
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "Ringtone Player"
include(":app")