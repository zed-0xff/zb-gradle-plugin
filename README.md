# zb-gradle-plugin

Signs your Project Zomboid **Java mod JAR** for [ZombieBuddy](https://github.com/zed-0xff/ZombieBuddy) (ZBS). The build prints a line you paste into your **Steam profile** so players can verify the mod.

---

## 1. Add the plugin

In the **root** `build.gradle` of the project that produces the JAR you publish:

```groovy
plugins {
    id 'io.github.zed-0xff.zb-gradle-plugin' version '1.0.1'
}
```

Use the latest version from the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.zed-0xff.zb-gradle-plugin) if it is newer than `1.0.1`.

---

## 2. Create your Ed25519 private key

The key must be **PKCS#8 DER**. You need **OpenSSL 1.1.1 or newer** with Ed25519 (check with `openssl version`).

**Linux and macOS** — create `~/.signing` and write the key there:

```bash
mkdir -p ~/.signing
openssl genpkey -algorithm ed25519 -outform DER -out ~/.signing/ed25519-private.der
```

On **macOS**, do not rely on `/usr/bin/openssl` for this step; if the command fails, run the same two lines but call Homebrew’s binary instead of `openssl`: Apple Silicon typically `/opt/homebrew/opt/openssl/bin/openssl`, Intel Homebrew typically `/usr/local/opt/openssl/bin/openssl`.

**Windows** — Command Prompt, with `openssl` on your `PATH` (for example from [Git for Windows](https://git-scm.com/download/win) or a standalone OpenSSL install):

```bat
mkdir "%USERPROFILE%\.signing" 2>nul
openssl genpkey -algorithm ed25519 -outform DER -out "%USERPROFILE%\.signing\ed25519-private.der"
```

Use that `.der` path (for example `C:\Users\YourName\.signing\ed25519-private.der`) in `zbsPrivateKeyFile` / `ed25519KeyFile`.

Keep this file secret. Back it up safely; anyone with it can sign as you.

---

## 3. Configure Steam ID and key path

You must set:

- **Steam ID** — your **SteamID64** (exactly **17 digits**, often shown on steamid.io or similar).
- **Private key file** — path to `ed25519-private.der` from step 2.

Pick **one** of these approaches (not required to use both).

### Option A — `gradle.properties` (good for local machines)

In the project’s `gradle.properties` or in `~/.gradle/gradle.properties`:

```properties
zbsSteamID64=76561198012345678
zbsPrivateKeyFile=/absolute/path/to/ed25519-private.der
```

Use a real 17-digit ID and a real absolute path.

### Option B — `build.gradle`

```groovy
zbSigning {
    steamId = '76561198012345678'
    ed25519KeyFile = file("${System.getProperty('user.home')}/.signing/ed25519-private.der")
}
```

If you set **neither** Steam ID nor key path, signing is skipped (no error at configuration time).

---

## 4. Build the JAR and create the `.zbs` file

From the same Gradle project:

```bash
./gradlew signJarZBS
```

Gradle runs the JAR task the plugin targets (see below), then writes a **sidecar file** next to the JAR: `YourMod.jar.zbs`.

**Which JAR is signed**

- If your project has a **`shadowJar`** task, that task’s output is signed by default.
- Otherwise the plain **`jar`** output is signed.

To force a specific task name: `zbSigning { jarTask = 'jar' }`.

To turn signing off entirely: `zbSigning { enabled = false }`.

---

## 5. Add the public key to Steam

After a successful `signJarZBS`, the log contains **one line** like:

```text
Add to Steam profile: JavaModZBS:<64-character-hex>
```

Copy the full value starting with `JavaModZBS:` (including that prefix) into your **Steam profile** summary field (as required by ZombieBuddy). That hex is your **public** key; the private `.der` file never goes in the profile or the mod.

---

## 6. What to ship

Ship **both**:

- The mod `.jar` you built, and  
- The matching `.zbs` file in the **same folder** with the **same basename** (e.g. `MyMod.jar` and `MyMod.jar.zbs`).

---

## License

MIT
