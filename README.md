This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM), Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

### Server First Startup

The server now manages API users itself and stores them in `server/data/auth-users.json`.
Passwords are stored hashed, never in plain text.

On the very first startup, if the auth-user file is empty, the server requires a bootstrap admin password.
Without it, startup fails intentionally.

Minimum first-start environment:

- `AUTH_BOOTSTRAP_ADMIN_PASSWORD`

Optional first-start environment:

- `AUTH_BOOTSTRAP_ADMIN_USERNAME`
- `AUTH_USERS_FILE`
- `PERFORMANCE_EVALUATIONS_FILE`
- `JWT_SECRET`
- `JWT_ISSUER`
- `JWT_AUDIENCE`
- `JWT_REALM`
- `JWT_TTL_SECONDS`
- `CORS_ALLOWED_ORIGINS`

Example first startup on macOS/Linux:

```shell
AUTH_BOOTSTRAP_ADMIN_PASSWORD='AdminPass123!' \
JWT_SECRET='change-this-secret-for-real-use' \
./gradlew :server:run
```

Example first startup on Windows PowerShell:

```powershell
$env:AUTH_BOOTSTRAP_ADMIN_PASSWORD='AdminPass123!'
$env:JWT_SECRET='change-this-secret-for-real-use'
.\gradlew.bat :server:run
```

After the first successful startup:

- the bootstrap admin user is written to `server/data/auth-users.json`
- the stored password is hashed
- you can authenticate through `POST /api/auth/token`
- you can manage auth users through the admin-only `/api/auth/users` routes

For local setup, see [.env.example](./.env.example).

API contract and deployment notes:

- OpenAPI contract: [docs/openapi.yaml](./docs/openapi.yaml)
- Public deployment checklist: [docs/public-deployment-checklist.md](./docs/public-deployment-checklist.md)

### Production checklist (server)

Before exposing the server to the public internet, make sure that:

- `JWT_SECRET` is set to a strong, randomly generated value (e.g. `openssl rand -hex 64`).
  In non-development mode the server refuses to start without it.
- `AUTH_BOOTSTRAP_ADMIN_PASSWORD` is a strong password and is rotated/disabled after the
  first successful start (delete the env var; the bootstrap is only used when the auth
  user file is empty).
- `CORS_ALLOWED_ORIGINS` only lists the public hosts of your frontend(s).
- `EXTERNAL_API_KEY` is the real Sportradar key. The server refuses to start with a
  placeholder value (e.g. `YOUR_API_KEY_HERE`) when `EXTERNAL_API_ENABLED=true`.
- The `server/data/*.json` files are stored on persistent, backed-up storage with
  appropriate file-system permissions (the JWT-protected stores contain hashed
  passwords and audit-relevant data).
- The server is fronted by HTTPS-terminating infrastructure (e.g. a reverse proxy);
  the embedded Netty server serves plain HTTP only.


### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:

- for the Wasm target (faster, modern browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
      ```
- for the JS target (slower, supports older browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :composeApp:jsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
      ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
