# Immich Android TV

Immich is a self hosted backup solution for photos and videos. Current features include:

- Upload and view videos and photos
- Auto backup when the app is opened
- Selective album(s) for backup
- Multi-user support
- Album and Shared albums

More info here: https://github.com/immich-app/immich

## Compatibility

This app is compatible with Immich server **v3.0.0 and newer**. Immich 3.0.0 introduced several
breaking API changes (album asset loading via `search/metadata`, search visibility defaults,
error response format); this app has been adapted accordingly. No additional API key permissions
are required for v3 compared to earlier versions — the existing `asset.read`/`album.read`
permissions listed below already cover the endpoints this app uses.

This Android TV app will allow you to view those uploaded photos and videos. Current features
include:

| Features                                                                       | Status       |
|:-------------------------------------------------------------------------------|--------------|
| Sign in by phone (https://github.com/giejay/Immich-Android-TV-Authentication)  | Done         |
| Sign in by entering API key                                                    | Done         |
| Demo environment                                                               | Done         |
| Album fetching + Lazy loading                                                  | Done         |
| Showing the photos inside an album                                             | Done         |
| Showing people, random, recent or seasonal photos                              | Done         |
| Slideshow of the photos and videos with a configured interval                  | Done         |
| Setting the app as the screensaver                                             | Done         |
| Setting the albums to show in the screensaver                                  | Done         |
| Configure the interval of the screensaver                                      | Done         |
| Add generic sorting of albums and photos                                       | Done         |
| Add sorting for specific album (select last item in row and press right again) | Done         |
| Showing the 4K thumbnail instead of the full image to speed up loading         | Done         |
| Showing the EXIF data and improving the slideshow view                         | Done         |
| Configure whether to play sound with videos                                    | Done         |
| Smarter merging of portrait photos (same people, same date, same city)         | Todo         |
| Add transitions to slideshow                                                   | Done         |
| Add places/tags view                                                           | Todo         |
| Add background media playing info to screensaver                               | Todo         |
| Casting capabilities                                                           | Out of scope |
| Searching in and for albums                                                    | Todo         |
| Dependency injection with Hilt/Dagger                                          | Todo         |

## Required API Permissions

When setting up your API key in Immich, make sure to grant the following permissions for the app to function properly:

- `activity.read` - Read activity data
- `album.download` - Download album content
- `album.read` - Read album information
- `archive.read` - Read archived items
- `asset.download` - Download assets for viewing
- `asset.read` - Read asset metadata
- `asset.view` - View assets (photos/videos)
- `face.read` - Read face detection data
- `folder.read` - Read folder information
- `library.read` - Read library information
- `memory.read` - Read memory/moment data
- `partner.read` - Read partner sharing data
- `person.read` - Read person/people data
- `session.read` - Read session information
- `tag.asset` - Read asset tag associations
- `tag.read` - Read tag information
- `timeline.read` - Read timeline data

## Cloudflare Access (service token)

If your Immich server is only reachable through [Cloudflare Access](https://developers.cloudflare.com/cloudflare-one/policies/access/),
the app can authenticate with an Access **service token**:

1. In the Cloudflare Zero Trust dashboard, create a service token (Access → Service Auth) and copy its
   Client ID and Client Secret.
2. Add the token to the Access application policy of your Immich host (policy action `Service Auth`).
3. Enter Client ID and Secret in the app - on the sign-in screen, or later via Settings → Network.

Both fields are optional and only sent when both are filled (as `CF-Access-Client-Id` and
`CF-Access-Client-Secret`). They are added to every request to the Immich server: the API, thumbnails
and images (Glide) and video playback (ExoPlayer). The secret is encrypted at rest with the same
Android Keystore mechanism as the API key.

Limitations, because such URLs are fetched outside of the app's HTTP client (no headers possible):

- opening a video in an external player (`?apiKey=` query parameter instead of a header)
- home screen channel poster art, fetched by the launcher process

With Cloudflare Access enabled those two features need a bypass, otherwise they fail. "Sign in by
phone" talks to a separate authentication service, which needs its own exemption if it is behind
Access too.

## In-app updates

The app can update itself from the [GitHub releases](https://github.com/kya1422/Immich-Android-TV/releases)
of this fork:

- It checks at most once a day on start (can be turned off in Settings → Updates) and whenever
  "Check for updates now" is pressed.
- If the release version is newer than the installed one, it offers to download the APK and hands it
  to the system package installer.

Requirements and caveats:

- The installed APK and the release APK must be signed with the **same key** - that is why the release
  key lives in `keystore/` and is configured as GitHub secrets. Switching keys (or installing the
  upstream APK built with giejay's key) requires one manual uninstall.
- Installations from an APK need "install unknown apps" permission for Immich TV; the app asks for it.
- Releases must have a version in the tag (`v7.1.0`) and an `.apk` asset, both of which
  `.github/workflows/release.yml` produces.

## Screenshots

|                                                                                    |                                                                      |                                                                                    |
|:----------------------------------------------------------------------------------:|:--------------------------------------------------------------------:|:----------------------------------------------------------------------------------:|
|        ![Alt text](/screenshots/homescreen-1.png?raw=true "Album overview")        |      ![Alt text](/screenshots/photos.png?raw=true "All photos")      |      ![Alt text](/screenshots/sorting-options.png?raw=true "Sorting options")      |
|         ![Alt text](/screenshots/home-edit.png?raw=true "Edit homescreen")         | ![Alt text](/screenshots/settings-view.png?raw=true "View settings") | ![Alt text](/screenshots/settings-screensaver.png?raw=true "Screensaver settings") |
| ![Alt text](/screenshots/screensaver-portrait.png?raw=true "Screensaver portrait") |        ![Alt text](/screenshots/people.png?raw=true "People")        |             ![Alt text](/screenshots/seasonl.png?raw=true "Seasonal")              |

## Build steps

1. Clone project with `git clone --recurse git@github.com:giejay/Immich-Android-TV.git`
2. Create an account at firebase and create a google-services.json file, or
   `cp app/google-services.example app/google-services.json`
3. copy app/strings_other.xml.example to app/src/main/res/values/strings_other.xml and modify
   the address and API keys for your demo server.
4. Build apk with `./gradlew assembleRelease`

Release builds are signed with a fixed release key so that in-app updates can be installed over an
existing installation. The key is never committed: put `storeFile`, `storePassword`, `keyAlias` and
`keyPassword` into `keystore/keystore.properties` (git-ignored) or provide the same values as the
`RELEASE_KEYSTORE_PATH` / `RELEASE_KEYSTORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD`
environment variables (that is what CI does). Without them the release APK stays unsigned.

## Support the project

You can support the project in several ways. The first one is by creating nice descriptive bug
reports if you find any: https://github.com/giejay/Immich-Android-TV/issues/new/choose.
<br><br>Even better is creating a PR: https://github.com/giejay/Immich-Android-TV/pulls.
<br><br>
Lastly, if you feel this Android TV app is a useful addition to the already great Immich app, you
might consider buying me a coffee or a beer:

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/giejay)

## FAQ

#### I'n not able to set the app as a screensaver

1. Enable development mode on the device (click the build number or "Android TV OS Build" 7 times in
   the System->About settings).
2. Go to System -> Developer Options and enable USB Debugging.
3. If you don't have ADB installed on your PC, follow these
   instructions: https://www.xda-developers.com/install-adb-windows-macos-linux/
4. After downloading/installing ADB on the PC, connect to the device using it's IP: adb connect
   192.168.xx.xx.
5. If you have an Amazon Fire TV, first execute this command and reboot after: 'adb shell settings put secure amazon_ambient_enabled 0'
6. Execute the following command: 'adb shell settings put secure
   screensaver_components nl.giejay.android.tv.immich/.screensaver.ScreenSaverService'
7. Done!
