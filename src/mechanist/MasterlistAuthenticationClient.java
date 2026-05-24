package mechanist;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** HTTPS/TLS masterlist registration heartbeat with signed server payloads. */
final class MasterlistAuthenticationClient {
    record ServerHeartbeat(String serverId, String endpoint, int playerCount, String modListHash, Instant generatedAt) {
        String canonicalPayload() {
            return serverId + "\n" + endpoint + "\n" + playerCount + "\n" + modListHash + "\n" + generatedAt;
        }

        String toJson(String signatureBase64) {
            return "{"
                    + "\"serverId\":" + AdminSecurityLogger.quote(serverId) + ","
                    + "\"endpoint\":" + AdminSecurityLogger.quote(endpoint) + ","
                    + "\"playerCount\":" + playerCount + ","
                    + "\"modListHash\":" + AdminSecurityLogger.quote(modListHash) + ","
                    + "\"generatedAt\":" + AdminSecurityLogger.quote(generatedAt.toString()) + ","
                    + "\"signature\":" + AdminSecurityLogger.quote(signatureBase64)
                    + "}";
        }
    }

    record ServerPing(String serverId, String endpoint, int playerCount, String modListHash, String signatureBase64) { }

    private final HttpClient httpClient;
    private final URI registrationEndpoint;
    private final PrivateKey privateKey;

    MasterlistAuthenticationClient(URI registrationEndpoint, PrivateKey privateKey) {
        this.registrationEndpoint = Objects.requireNonNull(registrationEndpoint, "registrationEndpoint");
        if (!"https".equalsIgnoreCase(registrationEndpoint.getScheme())) throw new IllegalArgumentException("masterlist registration requires HTTPS");
        this.privateKey = Objects.requireNonNull(privateKey, "privateKey");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    CompletableFuture<HttpResponse<String>> sendHeartbeat(ServerHeartbeat heartbeat) throws IOException {
        String signature = sign(heartbeat.canonicalPayload(), privateKey);
        HttpRequest request = HttpRequest.newBuilder(registrationEndpoint)
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .header("X-Mechanist-Server-Id", heartbeat.serverId())
                .POST(HttpRequest.BodyPublishers.ofString(heartbeat.toJson(signature), StandardCharsets.UTF_8))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static String sign(String payload, PrivateKey privateKey) throws IOException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException ex) {
            throw new IOException("server heartbeat signing failed", ex);
        }
    }

    static boolean verify(String payload, String signatureBase64, java.security.PublicKey publicKey) throws IOException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new IOException("server heartbeat signature verification failed", ex);
        }
    }

    static KeyPair createServerIdentity() throws IOException {
        try {
            java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IOException("server identity generation failed", ex);
        }
    }
}
