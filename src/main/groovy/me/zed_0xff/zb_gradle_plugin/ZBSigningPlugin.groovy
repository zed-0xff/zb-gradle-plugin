package me.zed_0xff.zb_gradle_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.util.PrivateKeyFactory

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Gradle plugin for ZBS signing (ZombieBuddy mod signature).
 * 
 * Creates Ed25519 signatures for mod verification.
 * 
 * Usage:
 * <pre>
 * plugins {
 *     id 'io.github.zed-0xff.zb-gradle-plugin' version '1.0.0'
 * }
 * 
 * zbSigning {
 *     steamId = '76561198012345678'
 * }
 * </pre>
 * 
 * Generate Ed25519 key (use homebrew openssl on macOS):
 *   openssl genpkey -algorithm ed25519 -outform DER -out ~/.signing/ed25519-private.der
 */
class ZBSigningPlugin implements Plugin<Project> {
    
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('zbSigning', ZBSigningExtension)
        
        project.afterEvaluate {
            if (!extension.enabled.getOrElse(true)) {
                return
            }
            
            def jarTaskName = resolveJarTaskName(project, extension)
            def jarTask = project.tasks.findByName(jarTaskName)
            
            if (!jarTask) {
                project.logger.warn("zb-signing: JAR task '${jarTaskName}' not found")
                return
            }
            
            configureZbsSigning(project, extension, jarTask)
        }
    }
    
    private void configureZbsSigning(Project project, ZBSigningExtension ext, jarTask) {
        // Capture all values at configuration time
        def steamId = ext.steamId.orNull ?: project.findProperty('zbsSteamID64')
        def keyFileProp = ext.ed25519KeyFile.orNull
        def keyPath = keyFileProp?.absolutePath ?: project.findProperty('zbsPrivateKeyFile')
        
        if (!steamId && !keyPath) {
            project.logger.info("zb-signing: No steamId/ed25519KeyFile configured, ZBS signing disabled")
            return
        }
        
        // Resolve key file at configuration time
        def keyFile = keyPath ? project.file(keyPath) : null
        
        project.logger.info("zb-signing: Configuring ZBS signing for '${jarTask.name}'")
        
        // Capture values for use in doLast (configuration cache compatible)
        final String finalSteamId = steamId?.trim()
        final File finalKeyFile = keyFile
        final String finalKeyPath = keyPath
        
        def signTask = project.tasks.register('signJarZBS') {
            dependsOn jarTask
            
            inputs.file(jarTask.archiveFile)
            outputs.file(project.layout.buildDirectory.file(
                jarTask.archiveFile.get().asFile.name + '.zbs'
            ))
            
            doLast {
                // Use captured values instead of accessing project
                if (!finalSteamId) {
                    throw new org.gradle.api.GradleException(
                        'signJarZBS: set zbsSteamID64 (-PzbsSteamID64=) or zbSigning.steamId'
                    )
                }
                if (!(finalSteamId ==~ /^\d{17}$/)) {
                    throw new org.gradle.api.GradleException(
                        "signJarZBS: invalid SteamID64: ${finalSteamId}"
                    )
                }
                
                if (!finalKeyPath?.trim() || !finalKeyFile?.isFile()) {
                    throw new org.gradle.api.GradleException(
                        "signJarZBS: missing Ed25519 PKCS#8 DER private key (${finalKeyPath}).\n" +
                        'Generate with: openssl genpkey -algorithm ed25519 -outform DER -out ~/.signing/ed25519-private.der\n' +
                        '(on macOS use openssl from homebrew for Ed25519 support)\n' +
                        'Set via zbSigning.ed25519KeyFile or -PzbsPrivateKeyFile='
                    )
                }
                
                def jarFile = jarTask.archiveFile.get().asFile
                def jarShaHex = sha256Hex(jarFile)
                
                def canonical = "ZBS:${finalSteamId}:${jarShaHex}"
                def messageBytes = canonical.getBytes(StandardCharsets.UTF_8)
                
                def pkcs8 = finalKeyFile.bytes
                try {
                    def pkInfo = PrivateKeyInfo.getInstance(pkcs8)
                    def asym = PrivateKeyFactory.createKey(pkInfo)
                    def priv = (Ed25519PrivateKeyParameters) asym
                    def pubHex = hexBytes(priv.generatePublicKey().getEncoded())
                    
                    def signer = new Ed25519Signer()
                    signer.init(true, priv)
                    signer.update(messageBytes, 0, messageBytes.length)
                    def sigBytes = signer.generateSignature()
                    def sigHex = hexBytes(sigBytes)
                    
                    def outFile = new File(jarFile.parentFile, jarFile.name + '.zbs')
                    outFile.setText(
                        "ZBS\nSteamID64:${finalSteamId}\nSignature:${sigHex}\n",
                        StandardCharsets.UTF_8.name()
                    )
                    logger.lifecycle("Add to Steam profile: JavaModZBS:${pubHex}")
                } finally {
                    Arrays.fill(pkcs8, (byte) 0)
                }
            }
        }
        
        project.tasks.named('jar').configure {
            finalizedBy signTask
        }
    }
    
    private static String sha256Hex(File file) {
        def digest = MessageDigest.getInstance('SHA-256')
        file.withInputStream { is ->
            byte[] buf = new byte[8192]
            int n
            while ((n = is.read(buf)) != -1) {
                digest.update(buf, 0, n)
            }
        }
        hexBytes(digest.digest())
    }
    
    private static String hexBytes(byte[] b) {
        def sb = new StringBuilder(b.length * 2)
        for (byte x : b) {
            sb.append(String.format('%02x', x & 0xff))
        }
        sb.toString()
    }
    
    private String resolveJarTaskName(Project project, ZBSigningExtension ext) {
        if (ext.jarTask.isPresent()) {
            return ext.jarTask.get()
        }
        return project.tasks.findByName('shadowJar') ? 'shadowJar' : 'jar'
    }
}
