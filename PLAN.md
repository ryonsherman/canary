# Canary — Proof-of-Life Android App

## Concept

An Android app that uses an RFID sticker as a physical token. Each day you tap your phone to the sticker, and the app signs and pushes a "proof-of-life" canary to a public GitHub repo. A GitHub Action then stamps it with OpenTimestamps (anchored to Bitcoin) and creates a signed git tag. This forms a verifiable, non-repudiable chain.

## How the Hash Chain Works

Each canary file contains the SHA256 hash of the previous day's canary, forming an unbreakable chain:

```
canary-2026-06-07.txt:
  "I am alive as of 2026-06-07Z. Previous hash: GENESIS"
  SHA256 → a1b2c3d4...

canary-2026-06-08.txt:
  "I am alive as of 2026-06-08Z. Previous hash: a1b2c3d4..."
```

To forge a canary for day N, an attacker would need:
1. The content of day N-1 (to get the correct previous hash)
2. Your GPG private key (to create a valid signature)
3. To make the OTS proof predate the current Bitcoin block (impossible)

## Verification Layers Per Canary

| Layer | Mechanism | Prevents |
|-------|-----------|----------|
| Hash chain | `sha256(day-N)` embedded in day N+1 | Tampering with history |
| GPG file sig | `.asc` clearsigned file | Forgery without private key |
| GPG tag sig | `git tag -s` | History rewrite / force push |
| Bitcoin anchor | OpenTimestamps `.ots` file | Backdating / retroactive insertion |

---

## Design Decisions

| Decision | Choice |
|----------|--------|
| RFID interaction | NFC tag stores SHA256 of a secret in Android Keystore |
| Signing key | Ed25519 in Android Keystore (hardware-backed) |
| GitHub auth | Fine-grained PAT, scoped `contents:write` to one repo |
| Push mechanism | App commits raw canary.txt, GH Action stamps + tags |
| QR backup | Auto-generated passphrase, encrypted bundle in QR |
| OTS verification | In-app — show Bitcoin confirmation status |
| Local DB | Not needed — repo fetch is < 2 seconds |
| App name | Canary |

---

## Architecture

```
┌─────────────────────┐     NFC tap     ┌──────────────────────┐
│   Phone (Canary      │◄──────────────►│  RFID sticker on     │
│   Android App)       │                │  desk / wallet       │
│                      │                └──────────────────────┘
│  1. Read tag UID     │
│  2. Verify secret    │
│  3. Generate canary  │
│  4. Sign (Ed25519)   │
│  5. Push to GitHub   │
└──────────┬───────────┘
           │ git push canary-YYYY-MM-DD.txt
           ▼
┌──────────────────────────────┐
│  GitHub Action                │
│  Runs on push to main         │
│                               │
│  1. GPG --clearsign file      │
│  2. ots stamp file            │
│  3. git tag -s                │
│  4. Push .asc + .ots + tag    │
└──────────────────────────────┘
```

---

## Android App: File Structure

```
canary-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/canary/
│   │   │   ├── CanaryApp.kt                    # Application class
│   │   │   ├── MainActivity.kt                 # Single activity
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt           # Main screen: tap sticker prompt
│   │   │   │   │   ├── SettingsScreen.kt       # Pair tag, generate QR, recovery
│   │   │   │   │   ├── ChainScreen.kt          # Chain health view
│   │   │   │   │   └── SetupScreen.kt          # First-run onboarding
│   │   │   │   └── components/
│   │   │   │       ├── CanaryCard.kt           # Per-canary status card
│   │   │   │       ├── ChainTimeline.kt        # Timeline of recent canaries
│   │   │   │       └── StatusIndicator.kt      # Green/red status dot
│   │   │   ├── viewmodel/
│   │   │   │   ├── HomeViewModel.kt
│   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   └── ChainViewModel.kt
│   │   │   ├── model/
│   │   │   │   ├── Canary.kt                   # Canary data class
│   │   │   │   ├── ChainState.kt               # Chain integrity state
│   │   │   │   └── TagState.kt                 # RFID tag pairing state
│   │   │   ├── service/
│   │   │   │   ├── NfcService.kt               # Read/write RFID tags
│   │   │   │   ├── CryptoService.kt            # Ed25519 signing via Keystore
│   │   │   │   ├── GithubService.kt            # GitHub API (Retrofit)
│   │   │   │   ├── ChainService.kt             # Chain validation logic
│   │   │   │   ├── OtsService.kt               # OTS verification from phone
│   │   │   │   └── QrService.kt                # QR generation / scanning
│   │   │   └── data/
│   │   │       ├── PreferencesManager.kt       # EncryptedSharedPreferences
│   │   │       └── GithubApi.kt                # Retrofit interface
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   ├── values/colors.xml
│   │   │   └── drawable/                       # Icons, NFC animation
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts                            # Root build file
├── settings.gradle.kts
└── gradle.properties
```

---

## Android App: Screens / UI

### Setup Screen (first run, or no tag paired)
- "Welcome to Canary" heading
- "Your public key is:" with copy button
- "Pair an RFID sticker" — tap to start pairing mode
- "Generate QR backup" — after pairing, shows QR + printed passphrase
- Progress: tag detected → writing → done

### Home Screen (daily use)
- Large NFC icon in center (pulsing animation)
- "Tap your sticker" prompt
- Status line: "Last canary: 7 min ago"
- Chain counter: "Chain: 127 days"
- Next due: "Next: ~2026-06-08 12:00Z"
- Quick actions bar: [Settings] [Chain View]

### During NFC tap
- Phone vibrates + shows green ripple animation
- "Canary signed and pushed ✓"
- Or: red shake + "Tag not recognized" if wrong sticker

### Chain Screen
- Scrollable timeline of recent canary entries
- Each card shows: date, counter, hash preview, OTS status (confirmed/pending)
- Top summary: "Chain intact: 127/127" or "BREAK at day 120"
- "Verify full chain" button (fetches entire repo history)

### Settings Screen
- **Paired Tag**: UID, secret hash (truncated), "Re-pair new sticker" button
- **QR Backup**: "Generate backup QR" / "Recover from QR"
- **GitHub**: PAT status (configured / missing), "Update token"
- **Key**: Ed25519 public key displayed + exported
- **Notifications**: Toggle daily reminder

---

## Android App: Key Classes / Responsibilities

### NfcService
```kotlin
class NfcService(private val context: Context) {
    fun getTagHash(intent: Intent): ByteArray?   // read SHA256 from tag
    fun writeTagSecret(tag: Tag, secret: ByteArray) // write hash during pairing
    fun isNfcEnabled(): Boolean
}
```

### CryptoService
```kotlin
class CryptoService(private val context: Context) {
    fun generateKey(): KeyPair                      // called once at setup
    fun sign(data: ByteArray): ByteArray            // Ed25519 sign
    fun getPublicKey(): PublicKey
    fun generateTagSecret(): ByteArray              // 32 random bytes
    fun storeTagSecret(secret: ByteArray)
    fun verifyTagSecret(tagHash: ByteArray): Boolean // compare with stored
}
```

### GithubService
```kotlin
class GithubService(private val token: String) {
    suspend fun pushCanary(
        content: String,
        date: String,
        signature: ByteArray,
        publicKey: String
    ): Result<Unit>
    // Creates: canary-YYYY-MM-DD.txt
    // Then GH Action picks up: signs, stamps, tags
}
```

### OtsService
```kotlin
class OtsService {
    suspend fun verifyOtsProof(
        fileHash: ByteArray,
        otsProof: ByteArray
    ): OtsResult           // "confirmed", "pending", "invalid"
}
```

### ChainService
```kotlin
class ChainService(private val githubService: GithubService) {
    suspend fun fetchRecentCanaries(count: Int): List<Canary>
    suspend fun verifyChain(depth: Int): ChainState
}
```

### QrService
```kotlin
class QrService(private val cryptoService: CryptoService) {
    fun generateBackupQr(passphrase: String): Bitmap
    fun decryptFromQr(qrContent: String, passphrase: String): ByteArray
}
```

---

## GitHub Action: `.github/workflows/canary.yaml`

```yaml
name: Seal Canary
on:
  push:
    paths: ['canary/canary-*.txt']

jobs:
  seal:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Find new canary file
        id: find
        run: |
          NEW_FILE=$(git diff --name-only HEAD~1 HEAD | grep 'canary-.*\.txt$')
          echo "file=$NEW_FILE" >> $GITHUB_OUTPUT
          echo "base=${NEW_FILE%.txt}" >> $GITHUB_OUTPUT

      - name: GPG clearsign
        run: |
          gpg --batch --import <<< "${{ secrets.GPG_PRIVATE_KEY }}"
          gpg --clearsign --armor \
            --output ${{ steps.find.outputs.base }}.txt.asc \
            ${{ steps.find.outputs.file }}

      - name: OpenTimestamps stamp
        run: |
          pip install opentimestamps-client
          ots stamp ${{ steps.find.outputs.file }}

      - name: Wait for OTS calendar round
        run: ots upgrade ${{ steps.find.outputs.file }}.ots

      - name: Create signed git tag
        run: |
          DATE=$(basename ${{ steps.find.outputs.file }} .txt | sed 's/canary-//')
          git tag -s "canary-$DATE" \
            -m "Canary $DATE"
          git tag -v "canary-$DATE"

      - name: Push artifacts + tags
        run: |
          git add ${{ steps.find.outputs.base }}.*
          git commit -m "seal: ${{ steps.find.outputs.file }}"
          git push --atomic origin main --tags
```

---

## Verification Script: `verify-chain.sh`

```bash
#!/usr/bin/env bash
# Verify the entire canary chain from genesis to HEAD

set -euo pipefail

REPO="https://github.com/YOUR_USER/canary.git"
WORKDIR=$(mktemp -d)
trap 'rm -rf $WORKDIR' EXIT

echo "Cloning..."
git clone --depth 1000 "$REPO" "$WORKDIR" 2>/dev/null || {
    git clone "$REPO" "$WORKDIR"
}
cd "$WORKDIR"

PREV_HASH=""
COUNT=0
GAP=0
BREAK=0

for f in $(ls canary/canary-*.txt | sort); do
    COUNT=$((COUNT + 1))
    FILE_HASH=$(sha256sum "$f" | cut -d' ' -f1)
    
    # Extract claimed previous hash from the file
    CLAIMED_PREV=$(grep "Previous Hash" "$f" | sed 's/.*SHA256): //')
    
    echo "[$COUNT] $(basename $f)"

    # Verify GPG signature
    if [ -f "${f}.asc" ]; then
        if ! gpg --verify "${f}.asc" "$f" 2>/dev/null; then
            echo "  ✗ GPG signature INVALID"
            BREAK=1
        else
            echo "  ✓ GPG signature valid"
        fi
    else
        echo "  ✗ No GPG signature file"
        BREAK=1
    fi

    # Verify hash chain link
    if [ "$COUNT" -gt 1 ]; then
        if [ "$CLAIMED_PREV" = "$PREV_HASH" ]; then
            echo "  ✓ Hash chain link intact"
        else
            echo "  ✗ Hash chain BREAK"
            echo "    Expected: $PREV_HASH"
            echo "    Got:      $CLAIMED_PREV"
            BREAK=1
            GAP=$COUNT
        fi
    fi

    # Verify git tag exists
    TAG="canary-$(basename $f .txt | sed 's/canary-//')"
    if git tag -v "$TAG" 2>/dev/null; then
        echo "  ✓ Signed git tag"
    else
        echo "  ✗ Missing or invalid git tag"
        BREAK=1
    fi

    PREV_HASH=$FILE_HASH
    echo ""
done

if [ "$BREAK" -eq 1 ]; then
    echo "❌ CHAIN BROKEN at canary #$GAP"
    exit 1
else
    echo "✅ CHAIN INTACT ($COUNT canaries verified)"
fi
```

---

## Implementation Order

Build in this sequence, testing each before moving on:

1. **GitHub repo + GH Action** (no app yet) — manually push a canary.txt, verify GH Action signs + stamps + tags correctly
2. **Verification scripts** — `verify-chain.sh` works on the test repo
3. **Android app skeleton** — build.gradle, manifest, theme, navigation
4. **CryptoService** — Ed25519 key generation + signing
5. **NfcService** — read/write RFID tag, pairing flow
6. **GithubService** — push canary with PAT
7. **Setup Screen** — first-run onboarding, tag pairing, QR backup
8. **Home Screen** — tap sticker → sign → push flow
9. **Chain Screen** — fetch + verify chain from phone
10. **Settings Screen** — re-pair, QR recovery, PAT management
11. **Polish** — animations, error states, notifications

---

## Threat Model

| Threat | Mitigation |
|--------|------------|
| Phone stolen + unlocked | Sticker is a separate physical object — need both |
| Phone stolen + locked | App requires biometric unlock to open |
| Sticker stolen | Useless without the secret in phone Keystore |
| Both stolen | QR backup in safe allows recovery + re-pair |
| GitHub PAT leaked | Scoped to `contents:write` on one repo — can only push commits |
| GPG key compromised (GH Action) | Revoke GH Action GPG key, deploy new one |
| Ed25519 key extracted from phone | Android Keystore with hardware backing (TEE/StrongBox) |
| Attacker forges canary on GitHub | Can't pass OTS verification — Bitcoin block is earlier than claimed date |
| You die / detained | Chain stops — watchers detect gap |
| You forget to tap | Chain stops — intentional design (proves nothing happened) |

---

## Open Items / Future Enhancements

- [ ] Multi-device: pair the same sticker to multiple phones?
- [ ] Dead-man switch: if chain breaks for N days, auto-release a pre-signed message or file
- [ ] Push notifications: daily reminder "Tap your sticker"
- [ ] Widget: home screen widget showing chain status
- [ ] Wear OS: tap sticker via smartwatch?
- [ ] Remote verification: third parties can subscribe to chain health via Atom feed or webhook
