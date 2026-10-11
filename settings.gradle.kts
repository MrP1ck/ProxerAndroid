pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google {
            mavenContent { releasesOnly() }
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroupByRegex("com\\.google\\.ads.*")
                includeGroupByRegex("com\\.google\\.testing.*")
                includeGroupByRegex("com\\.google\\.firebase.*")
                includeGroupByRegex("com\\.google\\.gms.*")
            }
        }

        mavenCentral {
            mavenContent { releasesOnly() }
            content {
                excludeGroupByRegex("androidx.*")
                excludeGroupByRegex("com\\.android.*")
            }
        }

        maven {
            url = uri("https://jitpack.io")
            mavenContent { releasesOnly() }
            content { includeGroupByRegex("com\\.github.*") }
        }
    }
}

rootProject.name = "ProxerAndroid"

// The API library is built from the sibling checkout of the fork (or the directory given by -PproxerLib.path), since
// it contains endpoints the published artifact is missing. Pass -PproxerLib.remote=true to resolve the artifact
// declared in the version catalog instead.
val localProxerLib = file(providers.gradleProperty("proxerLib.path").orElse("../ProxerLibJava").get())

if (localProxerLib.isDirectory && providers.gradleProperty("proxerLib.remote").orNull != "true") {
    includeBuild(localProxerLib) {
        dependencySubstitution {
            substitute(module("com.github.proxer:ProxerLibJava")).using(project(":library"))
        }
    }
}
