package me.zed_0xff.zb_gradle_plugin

import org.gradle.api.provider.Property

/**
 * Extension for configuring ZBS signing (ZombieBuddy mod signature).
 * 
 * Usage in build.gradle:
 * <pre>
 * zbSigning {
 *     steamId = '76561198012345678'
 *     ed25519KeyFile = file('~/.signing/ed25519-private.der')
 *     jarTask = 'jar'  // default: 'shadowJar' if exists, else 'jar'
 * }
 * </pre>
 */
abstract class ZBSigningExtension {
    
    /** Steam ID (17-digit number) for ZBS signing. */
    abstract Property<String> getSteamId()
    
    /** Path to Ed25519 PKCS#8 DER private key file. */
    abstract Property<File> getEd25519KeyFile()
    
    /** Name of the JAR task to sign. Default: 'shadowJar' if exists, else 'jar'. */
    abstract Property<String> getJarTask()
    
    /** Whether ZBS signing is enabled. Default: true. */
    abstract Property<Boolean> getEnabled()
    
    ZBSigningExtension() {
        enabled.convention(true)
    }
}
