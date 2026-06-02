# Compose Voting App (MVP)

This module now contains a first cross-platform Compose Multiplatform voting implementation.

## What is implemented

- Login against backend (`POST /api/auth/token`)
- Load phases (`GET /api/phases`)
- Load matches of a phase (`GET /api/phases/{phaseId}/matches`)
- Submit vote as performance evaluation (`POST /api/performance-evaluations`)
- Referee-team and delegate evaluator modes

## Supported targets

The UI and voting flow are implemented in `commonMain` and run via platform HTTP clients on:

- Android
- iOS
- JVM desktop
- Web (JS/WASM runtime availability depends on local toolchain/browser support)

## Key files

- `src/commonMain/kotlin/de/exhumedo/kmp/handball_support/App.kt`
- `src/commonMain/kotlin/de/exhumedo/kmp/handball_support/client/VoteApiClient.kt`
- `src/commonMain/kotlin/de/exhumedo/kmp/handball_support/vote/VoteAppPresenter.kt`
- `src/commonMain/kotlin/de/exhumedo/kmp/handball_support/vote/VotePayloadFactory.kt`

## Quick start

1. Start backend server on `http://localhost:8080` (or change base URL in app)
2. Run Compose app target
3. Login with an account that can create evaluations (ADMIN or REFEREE)
4. Load phases, select phase and match, submit vote

## Useful commands

```zsh
cd /Users/bhvonderlinde/Documents/workspace/kmp/kmp-handball-support
./gradlew :composeApp:allTests --no-daemon
./gradlew :composeApp:run --no-daemon
```

For Android/iOS/Web, use the corresponding Gradle/Xcode run target from IDE.

