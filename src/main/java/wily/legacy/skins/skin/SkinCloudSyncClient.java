package wily.legacy.skins.skin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import wily.legacy.Legacy4JClient;
import wily.legacy.skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.skins.util.DebugLog;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

final class SkinCloudSyncClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final AtomicBoolean SNAPSHOT_IN_FLIGHT = new AtomicBoolean(false);
    private static volatile long lastSnapshotRequestAt;

    private SkinCloudSyncClient() {
    }

    static boolean isActive(Minecraft client) {
        return client != null
                && client.player != null
                && client.getConnection() != null
                && !client.hasSingleplayerServer()
                && !Legacy4JClient.hasModOnServer()
                && !ConsoleSkinsClientSettings.getCloudRelayUrl().isBlank()
                && SkinFairness.resolveServerKey(client) != null;
    }

    static void submitSelection(Minecraft client, String skinId) {
        String relayUrl = ConsoleSkinsClientSettings.getCloudRelayUrl();
        String serverKey = SkinFairness.resolveServerKey(client);
        UUID playerId = client == null || client.player == null ? null : client.player.getUUID();
        if (relayUrl.isBlank() || serverKey == null || playerId == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("serverKey", serverKey);
        body.addProperty("playerUuid", playerId.toString());
        body.addProperty("skinId", SkinIdUtil.normalize(skinId));
        sendAsync(buildPost(relayUrl + "/v1/session/upsert", GSON.toJson(body)), false);
    }

    static void requestSnapshot(Minecraft client, boolean force) {
        String relayUrl = ConsoleSkinsClientSettings.getCloudRelayUrl();
        String serverKey = SkinFairness.resolveServerKey(client);
        if (relayUrl.isBlank() || serverKey == null) return;
        long now = System.currentTimeMillis();
        if (!force && (SNAPSHOT_IN_FLIGHT.get() || now - lastSnapshotRequestAt < 5000L)) return;
        if (!SNAPSHOT_IN_FLIGHT.compareAndSet(false, true)) return;
        lastSnapshotRequestAt = now;

        String query = relayUrl + "/v1/session?serverKey=" + URLEncoder.encode(serverKey, StandardCharsets.UTF_8);
        sendAsync(HttpRequest.newBuilder(URI.create(query)).timeout(Duration.ofSeconds(8)).GET().build(), true);
    }

    private static HttpRequest buildPost(String url, String body) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    private static void sendAsync(HttpRequest request, boolean snapshotRequest) {
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((response, error) -> {
            if (snapshotRequest) SNAPSHOT_IN_FLIGHT.set(false);
            if (error != null) {
                DebugLog.debug("Cloud skin sync request failed {}", error.toString());
                return;
            }
            if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
                DebugLog.debug("Cloud skin sync returned status {}", response == null ? "null" : response.statusCode());
                return;
            }
            if (!snapshotRequest) return;
            Map<UUID, String> skins = parseSnapshot(response.body());
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) mc.execute(() -> SkinSyncClient.onCloudSnapshot(skins));
        });
    }

    private static Map<UUID, String> parseSnapshot(String body) {
        LinkedHashMap<UUID, String> skins = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return skins;
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            if (json == null) return skins;
            JsonArray players = json.getAsJsonArray("players");
            if (players == null) return skins;
            for (JsonElement element : players) {
                if (!(element instanceof JsonObject player)) continue;
                String uuid = player.has("playerUuid") ? player.get("playerUuid").getAsString() : "";
                String skinId = player.has("skinId") ? player.get("skinId").getAsString() : "";
                if (uuid.isBlank()) continue;
                try {
                    skins.put(UUID.fromString(uuid), SkinIdUtil.normalize(skinId));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (RuntimeException ex) {
            DebugLog.debug("Cloud skin snapshot parse failed {}", ex.toString());
        }
        return skins;
    }
}
