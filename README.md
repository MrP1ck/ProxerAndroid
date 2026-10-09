# ![](art/logo/proxer-logo-title.png) Proxer.Me Android [![Latest Release](https://img.shields.io/github/release/proxer/ProxerAndroid.svg)](https://github.com/proxer/ProxerAndroid/releases/latest) [![Build status](https://github.com/proxer/ProxerAndroid/workflows/CI/badge.svg)](https://github.com/proxer/ProxerAndroid/actions?workflow=CI)

### Unmaintained

This app is not maintained anymore and will not receive any more updates. It might or might not work anymore.

### About this fork

This fork contains fixes on top of the last official release (1.11.5):

- The manga reader shows WebP pages, which are used by many newer webtoon uploads. Previously these pages stayed
  blank forever ([#1](https://github.com/MrP1ck/ProxerAndroid/pull/1)).
- The project builds again after the shutdown of jcenter.

The downloads below are the official releases and do not include these fixes. To use them, build the app yourself.

### What is this?

Proxer.Me Android is the official mobile client for the german Anime & Manga page [Proxer.Me](https://proxer.me).<br>
It features major functionalities including an anime player for various hosters and languages, a mobile-friendly manga reader, offline synchronized chat and much more.

### Downloads

| ![](art/logo/play-logo.png) Google Play Store                           | ![](art/logo/proxer-logo.png) Proxer App Store | ![](art/logo/github-logo.png) Github                                |
|-------------------------------------------------------------------------|------------------------------------------------|---------------------------------------------------------------------|
| [Download](https://play.google.com/store/apps/details?id=me.proxer.app) | [Download](https://proxer.me/apps/info/3)      | [Download](https://github.com/proxer/ProxerAndroid/releases/latest) |

#### Building yourself

After having installed the following tools: 

- [Git](https://git-scm.com/download)
- [JDK 17](https://adoptium.net/temurin/releases/?version=17)
- [Android SDK](https://developer.android.com/studio/#downloads)

You can run these commands:

- `git clone https://github.com/MrP1ck/ProxerAndroid`
- `cd ProxerAndroid`

This app needs an API-key to work. You can request one from the administrators at Proxer.
You then need to create a `secrets.properties` file in the root of the project with the following contents:

```
PROXER_API_KEY = YourApiKey
```

This app offers three variants to build: `debug`, `release` and `logRelease`.<br>
It is strongly recommended to use the `release` variant as it is faster and does not log sensitive data.

Before building, [generate a key](https://developer.android.com/studio/publish/app-signing.html#generate-key)
for signing the app if you have none yet.<br>
Add these fields to your `secrets.properties` file:

```
RELEASE_STORE_FILE = /path/to/the/keystore
RELEASE_STORE_PASSWORD = theKeystorePassword
RELEASE_KEY_ALIAS = theAlias
RELEASE_KEY_PASSWORD = thePasswordForThatAlias
```

You can then build the app by running:

```bash
# Linux
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

You can find the app in the `build/outputs/apk/release/` folder.<br>
A direct install of the app is possible for phones connected to your pc by running:

```bash
# Linux
./gradlew installRelease

# Windows
gradlew.bat installRelease
```

If you want to build the app for testing purposes in the `debug` variant, run:

```bash
# Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

Some dependencies were only published on jcenter, which is shut down. Most of them are replaced with artifacts from
Maven Central (see `gradle/dependencies.gradle`). ExoPlayer 2.11.8 is not available anywhere else, so it is downloaded
from the [Aliyun jcenter mirror](https://maven.aliyun.com/repository/jcenter) (see `gradle/repositories.gradle`).

### Screenshots

| News                         | Anime List                         | Manga Reader                         |
|------------------------------|------------------------------------|--------------------------------------|
| ![](art/screenshot/news.png) | ![](art/screenshot/anime-list.png) | ![](art/screenshot/manga-reader.png) |

| Media Detail                         | Instant Chat                            | Public Chat                         |
|--------------------------------------|-----------------------------------------|-------------------------------------|
| ![](art/screenshot/media-detail.png) | ![](art/screenshot/conference-list.png) | ![](art/screenshot/public-chat.png) |

| Anime Stream List                     | Anime Player                       |
|---------------------------------------|------------------------------------|
| ![](art/screenshot/anime-streams.png) | ![](art/screenshot/anime-play.png) |

| Profile Overview                | Profile Top Ten                    | Profile Media List               |
|---------------------------------|------------------------------------|----------------------------------|
| ![](art/screenshot/profile.png) | ![](art/screenshot/ucp-topten.png) | ![](art/screenshot/ucp-list.png) |

### Contributions and contributors

A guide for contribution can be found [here](.github/CONTRIBUTING.md).

- [@InfiniteSoul](https://github.com/InfiniteSoul) for implementing a persistent Navigation Drawer for tablets and UI improvements.
