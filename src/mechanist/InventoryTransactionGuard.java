package mechanist;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Server-authoritative idempotency and purchase-validation guard. */
final class InventoryTransactionGuard {
    private static final int MAX_TOKENS_PER_PLAYER = 2048;
    private static final long TOKEN_TTL_MILLIS = 15 * 60 * 1000L;

    private final ConcurrentMap<String, PlayerTokenCache> tokenCaches = new ConcurrentHashMap<>();
    private final AdminSecurityLogger logger;

    InventoryTransactionGuard(AdminSecurityLogger logger) { this.logger = Objects.requireNonNull(logger, "logger"); }

    TransactionResult processPurchase(PurchaseRequest request, PlayerInventoryState state, MerchantInventoryView merchant) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(merchant, "merchant");
        if (!request.playerId().equals(state.playerId())) {
            TransactionResult rejected = TransactionResult.rejected(request.idempotencyToken(), "request player mismatch");
            logDesync(request, state, rejected.reason());
            return rejected;
        }
        PlayerTokenCache cache = tokenCaches.computeIfAbsent(request.playerId(), ignored -> new PlayerTokenCache(MAX_TOKENS_PER_PLAYER, TOKEN_TTL_MILLIS));
        return cache.evaluate(request.idempotencyToken(), () -> executePurchase(request, state, merchant));
    }

    private TransactionResult executePurchase(PurchaseRequest request, PlayerInventoryState state, MerchantInventoryView merchant) {
        Integer price = merchant.itemPrices().get(request.itemId());
        if (price == null || price < 0) {
            TransactionResult rejected = TransactionResult.rejected(request.idempotencyToken(), "merchant does not offer item " + request.itemId());
            logDesync(request, state, rejected.reason());
            return rejected;
        }
        int serverCurrency = state.currencyByType().getOrDefault(request.currencyType(), 0);
        if (serverCurrency < price) {
            TransactionResult rejected = TransactionResult.rejected(request.idempotencyToken(), "insufficient server-side currency: required=" + price + " available=" + serverCurrency);
            logDesync(request, state, rejected.reason());
            return rejected;
        }
        double distance = state.position().distanceTo(merchant.position());
        if (!state.zoneId().equals(merchant.zoneId()) || distance > merchant.interactionRadius()) {
            TransactionResult rejected = TransactionResult.rejected(request.idempotencyToken(), "merchant interaction out of range: distance=" + distance + " radius=" + merchant.interactionRadius());
            logDesync(request, state, rejected.reason());
            return rejected;
        }
        Map<String, Integer> updatedCurrency = new LinkedHashMap<>(state.currencyByType());
        updatedCurrency.put(request.currencyType(), serverCurrency - price);
        Map<String, Integer> updatedItems = new LinkedHashMap<>(state.inventoryByItemId());
        updatedItems.merge(request.itemId(), request.quantity(), Integer::sum);
        PlayerInventoryState updated = new PlayerInventoryState(state.playerId(), state.zoneId(), state.position(), Map.copyOf(updatedCurrency), Map.copyOf(updatedItems), Instant.now());
        return TransactionResult.applied(request.idempotencyToken(), "purchase applied", updated);
    }

    void evictExpiredNow() { tokenCaches.values().forEach(PlayerTokenCache::evictExpired); }

    private void logDesync(PurchaseRequest request, PlayerInventoryState state, String reason) {
        try {
            String json = "{\n"
                    + "  \"event\": " + AdminSecurityLogger.quote(ObfuscatedStringTable.text(ObfuscatedStringTable.Key.STATE_DESYNCHRONIZATION_DEFILER_ANOMALIE)) + ",\n"
                    + "  \"playerId\": " + AdminSecurityLogger.quote(request.playerId()) + ",\n"
                    + "  \"itemId\": " + AdminSecurityLogger.quote(request.itemId()) + ",\n"
                    + "  \"token\": " + AdminSecurityLogger.quote(request.idempotencyToken()) + ",\n"
                    + "  \"reason\": " + AdminSecurityLogger.quote(reason) + ",\n"
                    + "  \"serverPosition\": " + state.position().toJson() + ",\n"
                    + "  \"createdAt\": " + AdminSecurityLogger.quote(Instant.now().toString()) + "\n"
                    + "}\n";
            logger.writeJsonEvent("transaction-desync", request.playerId(), json);
        } catch (IOException ex) {
            DebugLog.error("INVENTORY_DESYNC_LOG", "Could not log desync for " + request.playerId(), ex);
        }
    }

    private static final class PlayerTokenCache {
        private final int maxEntries;
        private final long ttlMillis;
        private final LinkedHashMap<String, CachedTransaction> cache = new LinkedHashMap<>(64, 0.75f, true);
        private final ArrayDeque<String> order = new ArrayDeque<>();

        PlayerTokenCache(int maxEntries, long ttlMillis) {
            this.maxEntries = Math.max(16, maxEntries);
            this.ttlMillis = Math.max(1_000L, ttlMillis);
        }

        synchronized TransactionResult evaluate(String token, java.util.function.Supplier<TransactionResult> executor) {
            String safeToken = validateToken(token);
            evictExpired();
            CachedTransaction cached = cache.get(safeToken);
            if (cached != null) return cached.result().asDuplicateReplay();
            TransactionResult result = executor.get();
            cache.put(safeToken, new CachedTransaction(result, System.currentTimeMillis()));
            order.addLast(safeToken);
            while (cache.size() > maxEntries && !order.isEmpty()) cache.remove(order.removeFirst());
            return result;
        }

        synchronized void evictExpired() {
            long now = System.currentTimeMillis();
            while (!order.isEmpty()) {
                String oldest = order.peekFirst();
                CachedTransaction cached = cache.get(oldest);
                if (cached == null) { order.removeFirst(); continue; }
                if (now - cached.createdAtMillis() <= ttlMillis) break;
                order.removeFirst();
                cache.remove(oldest);
            }
        }

        private static String validateToken(String token) {
            if (token == null || token.isBlank()) throw new IllegalArgumentException("idempotency token is required");
            String trimmed = token.trim();
            if (trimmed.length() > 128) throw new IllegalArgumentException("idempotency token exceeds maximum length");
            try { UUID.fromString(trimmed); return trimmed; }
            catch (IllegalArgumentException ignored) {
                if (!trimmed.matches("[A-Za-z0-9._:-]{16,128}")) throw new IllegalArgumentException("idempotency token has illegal format");
                return trimmed;
            }
        }
    }

    private record CachedTransaction(TransactionResult result, long createdAtMillis) { }

    record PurchaseRequest(String playerId, String idempotencyToken, String merchantId, String itemId, int quantity, String currencyType, long clientDeclaredSequenceId) {
        PurchaseRequest {
            playerId = requireText(playerId, "playerId");
            idempotencyToken = requireText(idempotencyToken, "idempotencyToken");
            merchantId = requireText(merchantId, "merchantId");
            itemId = requireText(itemId, "itemId");
            quantity = Math.max(1, Math.min(10_000, quantity));
            currencyType = requireText(currencyType == null ? "credits" : currencyType, "currencyType");
            if (clientDeclaredSequenceId < 0) throw new IllegalArgumentException("clientDeclaredSequenceId must be non-negative");
        }
    }

    record TransactionResult(String idempotencyToken, boolean applied, boolean duplicateReplay, String reason, PlayerInventoryState updatedState) {
        static TransactionResult applied(String token, String reason, PlayerInventoryState updated) { return new TransactionResult(token, true, false, reason, updated); }
        static TransactionResult rejected(String token, String reason) { return new TransactionResult(token, false, false, reason, null); }
        TransactionResult asDuplicateReplay() { return new TransactionResult(idempotencyToken, applied, true, reason, updatedState); }
    }

    record PlayerInventoryState(String playerId, String zoneId, Position3 position, Map<String, Integer> currencyByType, Map<String, Integer> inventoryByItemId, Instant updatedAt) {
        PlayerInventoryState {
            playerId = requireText(playerId, "playerId");
            zoneId = requireText(zoneId == null ? "origin-zone" : zoneId, "zoneId");
            position = position == null ? new Position3(0, 0, 0) : position;
            currencyByType = Map.copyOf(Objects.requireNonNullElse(currencyByType, Map.of()));
            inventoryByItemId = Map.copyOf(Objects.requireNonNullElse(inventoryByItemId, Map.of()));
            updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        }
    }

    record MerchantInventoryView(String merchantId, String zoneId, Position3 position, double interactionRadius, Map<String, Integer> itemPrices) {
        MerchantInventoryView {
            merchantId = requireText(merchantId, "merchantId");
            zoneId = requireText(zoneId == null ? "origin-zone" : zoneId, "zoneId");
            position = position == null ? new Position3(0, 0, 0) : position;
            if (!Double.isFinite(interactionRadius) || interactionRadius <= 0 || interactionRadius > 128.0) throw new IllegalArgumentException("interactionRadius outside safe bounds");
            itemPrices = Map.copyOf(Objects.requireNonNullElse(itemPrices, Map.of()));
        }
    }

    record Position3(double x, double y, double z) {
        Position3 {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("position coordinates must be finite");
        }
        double distanceTo(Position3 other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        String toJson() { return "{\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z + "}"; }
    }

    private static String requireText(String text, String field) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(field + " is required");
        String trimmed = text.trim();
        if (trimmed.length() > 160) throw new IllegalArgumentException(field + " exceeds maximum length");
        return trimmed;
    }
}
