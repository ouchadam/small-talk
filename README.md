# SmallTalk [![Assemble](https://github.com/ouchadam/small-talk/actions/workflows/assemble.yml/badge.svg)](https://github.com/ouchadam/small-talk/actions/workflows/assemble.yml) [![codecov](https://codecov.io/gh/ouchadam/small-talk/branch/main/graph/badge.svg?token=ETFSLZ9FCI)](https://codecov.io/gh/ouchadam/small-talk) [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![](https://img.shields.io/github/v/release/ouchadam/small-talk?include_prereleases) ![](https://img.shields.io/badge/%5Bmatrix%5D%20-%23small--talk%3Aiswell.cool-blueviolet)

`SmallTalk` is a minimal, modern, friends and family focused Android messenger. Heavily inspired by Whatsapp and Signal, powered by Matrix.


![header](https://github.com/ouchadam/small-talk/blob/main/.github/readme/header.png?raw=true)
[<img align="right" height="70" src="https://github.com/ouchadam/small-talk/blob/main/.github/readme/google-play-badge.png?raw=tru"></a>](https://play.google.com/store/apps/details?id=app.dapk.st)

<br>
<br>
<br>


### Project mantra
- Tiny app size - currently 1.80mb~ when provided via app bundle.
- Focused on reliability and stability.
- Bare-bones feature set.

---

### Feature list

- Login with Matrix ID/Password
- Combined Room and DM interface
- End to end encryption
- Message bubbles, supporting text, replies and edits
- Push notifications (DMs always notify, Rooms notify once)
- Importing of E2E room keys from Element clients
- [UnifiedPush](https://unifiedpush.org/)
- FOSS variant
- Minimal HTML formatting
- Invitations
- Image attachments

### Planned

- Device verification (technically supported but has no UI)
- Room history
- Cross signing
- Google drive backups
- Changing user name/avatar
- Room settings and information
- Exporting E2E room keys
- Local search
- Registration

--- 

#### Technical details

- Built on Jetpack compose and kotlin multiplatform libraries ktor and sqldelight (although the project is not currently setup to be multiplatform until needed).
- Greenfield matrix SDK implementation, focus on separation, testability and parallelisation.
- Heavily optimised build script, clean _cacheless_ builds are sub 10 seconds with a warmed up gradle daemon.
- Avoids code generation where possible in favour of build speed, this mainly means manual DI.
- A pure kotlin test harness to allow for critical flow assertions [Smoke Tests](https://github.com/ouchadam/small-talk/blob/main/test-harness/src/test/kotlin/SmokeTest.kt), currently Linux x86-64 only.

---


### Building


##### Debug `.apk`

```bash
./gradlew assembleDebug
```

##### Release (signed with debug key) `.apk`

```bash
./gradlew assembleRelease
```

##### Unsigned release `.apk`

```bash
./gradlew assembleRelease -Punsigned
```

##### Unsigned release (FOSS) `.apk`

```bash
./gradlew assembleRelease -Punsigned -Pfoss
```

---

### Data and Privacy

- Messages once decrypted are stored as plain text within `SmallTalk's` database. _Always encrypted messages_ comes at the cost of performance and limits features like local search. If maximum security is your number priority, `SmallTalk` is not the best option. This database is not easily accessed without rooting or external hardware. 

- (Not yet implemented and may be configurable) Images once decrypted are stored in their plain form within the devices media directories, organised by room metadata. 

- Push notifications contain no sensitive data by using the [event_id_only](https://github.com/ouchadam/small-talk/blob/main/matrix/services/push/src/main/kotlin/app/dapk/st/matrix/push/internal/RegisterPushUseCase.kt#L31) configuration. Push notifications are used as a _push to sync_ mechanism, where the on device sync fetches the actual contents. 

- Passwords are **NEVER** stored within `SmallTalk`. 

- `SmallTalk` does not explicitly talk to servers other than your home-server or track what you do.  __*__
  - __*__ There is no `SmallTalk` server capturing data from the application however the Google variant likely includes transitive telemetrics through the use of `Firebase` and `Google Play Services` integrations. 

- `SmallTalk` is completely free and will never feature adverts or paid app features. 

---

`SmallTalk` comes in two flavours, `Google` and `FOSS`

##### Google
- Available through the [Google Play Store](https://play.google.com/store/apps/details?id=app.dapk.st) and [Github Releases](https://github.com/ouchadam/small-talk/releases).
- Automatic crash and non fatal error tracking via [Firebase Crashlytics](https://firebase.google.com/products/crashlytics).
- Push notifications provided through [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging).

##### FOSS
- Available through the [IzzySoft's F-Droid Repository](https://android.izzysoft.de/repo) and [Github Releases](https://github.com/ouchadam/small-talk/releases).
- No Google or Firebase services (and their transitive telemetrics).
- No crash tracking.
- No push notifications by default,  a separate [UnifiedPush](https://unifiedpush.org/) [distributor](https://unifiedpush.org/users/distributors/) is required.

---

#### Join the conversation @ https://matrix.to/#/#small-talk:iswell.cool
