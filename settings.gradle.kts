rootProject.name = "PVNClient"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

include(":app")
include(":core:common")
include(":vpnclient")
include(":auth:ui")
include(":auth:data")
include(":auth:domain")
