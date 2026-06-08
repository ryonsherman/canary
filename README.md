# Canary

A proof-of-life Android app that uses an RFID sticker as a physical token.  
Tap daily to prove you're alive — the canary is signed, pushed to GitHub, and anchored to Bitcoin.

## How It Works

```
┌──────────────────────┐     NFC tap     ┌─────────────────────┐
│   Phone (Canary      │◄──────────────►│  RFID sticker on     │
│   Android App)       │                │  desk / wallet       │
│                      │                └─────────────────────┘
│  1. Read tag secret  │
│  2. Sign canary      │
│  3. Push to GitHub   │
└──────────┬───────────┘
           │ git push canary-YYYY-MM-DD.txt
           ▼
┌──────────────────────────────┐
│  GitHub Action (on push)      │
│  1. GPG clearsign file        │
│  2. ots stamp (Bitcoin anchor)│
│  3. Create signed git tag     │
│  4. Push .asc + .ots + tag    │
└──────────────────────────────┘
```

Each canary contains the SHA256 hash of the previous day's canary, forming a tamper-evident chain.  
To forge a single entry an attacker needs: the previous day's hash, your GPG key, and a Bitcoin block from before the claimed date.

### Verification Layers

| Layer | Mechanism | Prevents |
|---|---|---|
| Hash chain | `sha256(day-N)` in day N+1 | History tampering |
| Ed25519 sig | Phone-side signature | Forgery without the sticker |
| GPG sig | Clearsigned `.asc` file | Forgery without GPG key |
| Git tag | `git tag -s` | Force-push / history rewrite |
| OTS | `.ots` file on Bitcoin | Backdating / insertion |

## Requirements

- Android phone with NFC (Android 9+)
- One or more writable NTAG RFID stickers (e.g. NTAG213/215/216)
- GitHub account

## Setup

### 1. GitHub

1. Fork or push this repo to your GitHub account
2. Create a **fine-grained PAT** (Settings → Developer settings → Fine-grained tokens) with `contents:write` scope on that repo
3. Add two **repository secrets** for the GPG signing:
   - `GPG_PRIVATE_KEY` — your GPG private key (export: `gpg --export-secret-key --armor <key-id>`)
   - `GPG_PASSPHRASE` — the passphrase for that key

### 2. Build the App

```bash
git clone git@github.com:youruser/canary.git
cd canary
./gradlew assembleDebug
```

Or install directly to a connected phone:

```bash
./gradlew installDebug
```

### 3. First-Run Setup

1. Open the app — you'll see the **Setup** screen
2. Enter your GitHub PAT, repo owner, and repo name
3. Tap **"Pair an RFID sticker"** — hold the sticker to the back of your phone
4. The app writes a secret to the tag and generates a **QR backup** (save this in a safe place — it's the only way to recover if your phone is lost)

### 4. Daily Use

1. Open the app
2. Tap the phone to the sticker
3. The app signs a canary with the hardware-backed Ed25519 key and pushes it to GitHub
4. The GitHub Action GPG-signs it, stamps it with OpenTimestamps, and creates a signed git tag

If you forget, the app sends a nag notification every hour until you tap.

## Verification

### From the App

The **Chain** screen shows recent canaries and checks hash chain integrity.

### From a Watcher's Machine

```bash
./verify-chain.sh https://github.com/youruser/canary
```

This clones the repo and checks:
- GPG signature on each canary
- Hash chain continuity
- Signed git tag existence
- OpenTimestamps proof existence

## Architecture

```
app/
├── service/
│   ├── CryptoService.kt       # Ed25519 via Android Keystore
│   ├── NfcService.kt          # Read/write RFID tags
│   ├── GithubService.kt       # Push to GitHub
│   ├── ChainService.kt        # Chain validation
│   ├── OtsService.kt          # OTS proof verification
│   ├── QrService.kt           # QR backup generation/scanning
│   ├── ReminderReceiver.kt    # Hourly nag BroadcastReceiver
│   └── ReminderScheduler.kt   # AlarmManager scheduling
├── data/
│   ├── PreferencesManager.kt  # EncryptedSharedPreferences
│   └── GithubApi.kt           # OkHttp GitHub API client
├── model/
│   ├── Canary.kt
│   ├── ChainState.kt
│   └── TagState.kt
├── ui/screens/
│   ├── SetupScreen.kt         # Onboarding + tag pairing + QR
│   ├── HomeScreen.kt          # Daily tap → sign → push flow
│   ├── ChainScreen.kt         # Chain health timeline
│   └── SettingsScreen.kt      # Key info, re-pair, recovery
└── viewmodel/
    ├── HomeViewModel.kt
    ├── SettingsViewModel.kt
    └── ChainViewModel.kt
```

## Threat Model

| Threat | Mitigation |
|---|---|
| Phone stolen + unlocked | Sticker is a separate physical object — need both |
| Sticker stolen | Useless without the secret in Android Keystore |
| Both stolen | QR backup in safe allows recovery + re-pair |
| PAT leaked | Scoped to `contents:write` on a single repo |
| GPG key compromised (Action) | Revoke and deploy a new one |
| Ed25519 key extracted | Android Keystore TEE/StrongBox hardware backing |
| Forged canary on GitHub | OTS verification fails — Bitcoin block predates claim |
| You die / detained | Chain stops — watchers detect the gap |

## License

MIT
