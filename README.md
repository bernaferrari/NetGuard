# QuietGuard

<p align="center">
  <img src="assets/more-light.png" alt="QuietGuard overview" width="980">
</p>

<p align="center">
  <strong>A private, no-root firewall with clear per-app network controls.</strong><br>
  Built with Kotlin Multiplatform, Compose, and Material 3.
</p>

<p align="center">
  <a href="https://quietguard.vercel.app/"><strong>Try the Web Demo</strong></a>
</p>

---

QuietGuard lets you decide which Android apps can use Wi-Fi or mobile data. It runs entirely on your device through Android's VPN API: no root access, remote proxy, account, tracking, or analytics required.

It is a modern Kotlin and Compose evolution of [NetGuard](https://github.com/M66B/NetGuard), with a redesigned interface and clearer workflows for rules, logs, and settings.

## Highlights

- **On-device filtering:** Uses Android `VpnService`; your traffic is never sent through a QuietGuard server.
- **Per-app control:** Allow or block Wi-Fi and mobile data independently for each app.
- **Readable activity logs:** Quickly understand what connected, what was blocked, and why.
- **Modern adaptive UI:** A Compose and Material 3 interface designed for phones, tablets, and foldables.
- **Local-first and open source:** No account, advertisements, tracking, or analytics.
- **Web demo:** Explore the interface in a browser without installing the Android app.

## Screenshots

![Comparison](assets/comparison.png)

## What QuietGuard Changes

If you're coming from the original NetGuard, here are the major differences you'll notice:

- A complete Kotlin and Jetpack Compose rewrite.
- A focused Material 3 interface with clearer controls and feedback.
- Adaptive two-pane layouts for tablets and foldables.
- Redesigned traffic logs that distinguish allowed and blocked connections at a glance.
- Better-organized settings with less legacy clutter.
- A smaller, more intentional feature set centered on the core firewall experience.

## How It Works

QuietGuard operates as a local-only application. It uses Android's built-in `VpnService` to route your device's traffic through a local sinkhole.

Traffic stays on your device. QuietGuard evaluates outbound connection attempts and allows or drops them according to your per-app rules. The local VPN is the mechanism Android provides for filtering traffic without root; it is not a remote VPN service and does not change your public location.

## Adaptive Layouts

QuietGuard dynamically scales to take advantage of larger screens, offering a dedicated two-pane layout for comfortable viewing on tablets and foldables.

<p align="center">
  <img src="assets/foldable.png" alt="Foldable experience" width="980">
</p>

## Getting Started

### Prerequisites

- **JDK 17** or higher.
- **Android Studio** (current stable) with the Android SDK (API 26+ target).

### Build & Run

#### Android
```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

You can also build a release variant configured for production checks and shrinker behavior by running `./gradlew :app:assembleRelease`.

#### Web (local dev server)

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Production wasm bundle:

```bash
./gradlew :webApp:wasmJsBrowserDistribution
```

#### Web deploy (Vercel)

Simulate the CI build locally:

```bash
./scripts/simulate-web-ci.sh
```

Deploy manually (after `vercel login` or with a token):

```bash
./scripts/verify-web-deploy.sh --ci
VERCEL_TOKEN=... VERCEL_ORG_ID=... VERCEL_PROJECT_ID=... ./scripts/deploy-web-vercel.sh
```

Pushes to `main` run `.github/workflows/deploy-web-vercel.yml`, which builds the wasm bundle and deploys to [quietguard.vercel.app](https://quietguard.vercel.app/). Add these repository secrets:

| Secret | Where to find it |
|--------|------------------|
| `VERCEL_TOKEN` | [vercel.com/account/tokens](https://vercel.com/account/tokens) |
| `VERCEL_ORG_ID` | Vercel project → Settings → General |
| `VERCEL_PROJECT_ID` | Vercel project → Settings → General |

## Behavior Notes

- This app is local-first and does not proxy traffic through a third-party backend.
- Some device manufacturers apply stricter VPN policies; behavior can vary.
- Certain background/network capabilities depend on notification, battery optimization, and device permissions.

## Credits and License

- **Original Project:** [NetGuard](https://github.com/M66B/NetGuard) by Marcel Bokhorst.
- **License:** GNU GPLv3. See [LICENSE](LICENSE) for details.

<p align="center">
  <img src="assets/more-dark.png" alt="QuietGuard Dark Mode variant" width="980">
</p>
