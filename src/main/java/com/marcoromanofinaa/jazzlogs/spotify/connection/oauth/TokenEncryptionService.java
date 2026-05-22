package com.marcoromanofinaa.jazzlogs.spotify.connection.oauth;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TokenEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenEncryptionService(SpotifyProperties spotifyProperties) {
        var secret = spotifyProperties.security().tokenEncryptionSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Spotify token encryption secret is not configured");
        }
        this.secretKey = new SecretKeySpec(sha256(secret), "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Cannot encrypt blank Spotify token");
        }

        try {
            var iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder()
                    .encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                            .put(iv)
                            .put(encrypted)
                            .array());
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to encrypt Spotify token", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IllegalArgumentException("Cannot decrypt blank Spotify token");
        }

        try {
            var payload = Base64.getDecoder().decode(ciphertext);
            var buffer = ByteBuffer.wrap(payload);

            var iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            var encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to decrypt Spotify token", exception);
        }
    }

    private byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize Spotify token encryption", exception);
        }
    }
}
