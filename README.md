# SmallTalk [![codecov](https://codecov.io/gh/ouchadam/small-talk/branch/main/graph/badge.svg?token=ETFSLZ9FCI)](https://codecov.io/gh/ouchadam/small-talk)

`SmallTalk` is a minimal, modern, friends and family focused Android messenger. Heavily inspired by Whatsapp and Signal, powered by Matrix.

---

Project mantra
- Tiny app size - currently 1.72mb~ when provided via app bundle.
- Focused on reliability and stability.
- Bare-bones feature set.

##### _*Google play only with automatic crash reporting enabled_

---

#### Feature list

- Login with username/password (home servers must serve `${domain}.well-known/matrix/client`)
- Combined Room and DM interface
- End to end encryption
- Message bubbles, supporting text, replies and edits
- Push notifications (DMs always notify, Rooms notify once)
- Importing of E2E room keys from Element clients

#### Planned

- Device verification (technically supported but has no UI)
- Invitations (technically supported but has no UI)
- Room history
- Message media
- Cross signing
- Google drive backups
- Markdown subset (bold, italic, blocks)
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
