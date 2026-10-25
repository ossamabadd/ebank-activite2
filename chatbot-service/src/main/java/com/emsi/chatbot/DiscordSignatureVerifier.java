package com.emsi.chatbot;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.HexFormat;

public final class DiscordSignatureVerifier {
    private final PublicKey publicKey;
    public DiscordSignatureVerifier(String rawPublicKey) {
        try {
            byte[] encoded = HexFormat.of().parseHex("302a300506032b6570032100" + rawPublicKey);
            publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid DISCORD_PUBLIC_KEY", ex);
        }
    }

    public boolean verify(String timestamp, String signature, byte[] body) {
        try {
            long epoch = Long.parseLong(timestamp);
            if (epoch < Instant.now().getEpochSecond() - 300 || epoch > Instant.now().getEpochSecond() + 300) return false;
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(timestamp.getBytes(StandardCharsets.UTF_8));
            verifier.update(body);
            return verifier.verify(HexFormat.of().parseHex(signature));
        } catch (Exception ex) { return false; }
    }
}
