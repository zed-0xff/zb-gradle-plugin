# zb-gradle-plugin

Gradle plugin for ZBS signing (ZombieBuddy mod signature).

## Features

- Ed25519 signatures for Project Zomboid mod verification
- Auto-detects `shadowJar` or falls back to `jar` task

## Installation

### From Gradle Plugin Portal

```groovy
plugins {
    id 'me.zed_0xff.zb-gradle-plugin' version '1.0.0'
}
```

### Local Development (composite build)

In your mod's `settings.gradle`:
```groovy
includeBuild '/path/to/zb-gradle-plugin'
```

Then in `build.gradle`:
```groovy
plugins {
    id 'me.zed_0xff.zb-gradle-plugin'
}
```

## Usage

### Generate Ed25519 Key

Use homebrew openssl on macOS (system openssl doesn't support Ed25519):
```bash
openssl genpkey -algorithm ed25519 -outform DER -out ~/.signing/ed25519-private.der
```

After build, the plugin prints the string to add to your Steam profile summary:

```
> Task :signJarZBS
Add to Steam profile: JavaModZBS:cafebabe0123456789abcdef0123456789abcdef0123456789abcdef01234567

BUILD SUCCESSFUL in 1s
8 actionable tasks: 5 executed, 3 up-to-date
```

### Configure

In `build.gradle`:
```groovy
zbSigning {
    steamId = '76561198012345678'
    ed25519KeyFile = file("${System.getProperty('user.home')}/.signing/ed25519-private.der")
}
```

Or in `~/.gradle/gradle.properties`:
```properties
zbsSteamID64=76561198012345678
zbsPrivateKeyFile=/Users/you/.signing/ed25519-private.der
```

### Options

| Property | Description | Default |
|----------|-------------|---------|
| `steamId` | Steam ID (17 digits) | - |
| `ed25519KeyFile` | Path to Ed25519 private key | - |
| `jarTask` | Task to sign | `shadowJar` or `jar` |
| `enabled` | Enable ZBS signing | `true` |

## Tasks

- `signJarZBS` - Creates `.zbs` signature file (runs after `jar`)

## License

MIT
