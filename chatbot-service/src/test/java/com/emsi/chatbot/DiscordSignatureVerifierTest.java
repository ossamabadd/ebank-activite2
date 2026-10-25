package com.emsi.chatbot;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class DiscordSignatureVerifierTest {
    @Test void acceptsOnlyAuthenticRecentBodies() throws Exception {
        var pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] encoded = pair.getPublic().getEncoded();
        var verifier = new DiscordSignatureVerifier(HexFormat.of().formatHex(
                Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length)));
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        byte[] body = "{\"type\":1}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(pair, timestamp, body);
        assertThat(verifier.verify(timestamp, signature, body)).isTrue();
        assertThat(verifier.verify(timestamp, signature, "{}".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(verifier.verify(timestamp, "bad", body)).isFalse();
        String stale = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
        assertThat(verifier.verify(stale, sign(pair, stale, body), body)).isFalse();
    }

    private String sign(KeyPair pair, String timestamp, byte[] body) throws Exception {
        var signature = Signature.getInstance("Ed25519");
        signature.initSign(pair.getPrivate());
        signature.update(timestamp.getBytes(StandardCharsets.UTF_8));
        signature.update(body);
        return HexFormat.of().formatHex(signature.sign());
    }
}
