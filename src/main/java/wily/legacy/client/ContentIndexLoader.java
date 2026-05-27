package wily.legacy.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import wily.legacy.Legacy4J;
import wily.legacy.client.ContentManager.Category;
import wily.legacy.client.ContentManager.Pack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class ContentIndexLoader {
    private static final URI STARTERPACKS_PREVIEW = URI.create("https://raw.githubusercontent.com/creeper2eletricboogaloo/L4JDownloadableContent/main/STARTERPACKS/preview_images/starterpacks_bundle_preview.png");

    private ContentIndexLoader() {
    }

    static CompletableFuture<List<Pack>> fetchIndex(Category category) {
        return ContentManager.STARTERPACKS_CATEGORY_ID.equals(category.id()) ? fetchStarterpacksIndex() : fetchRemoteIndex(category);
    }

    private static CompletableFuture<List<Pack>> fetchRemoteIndex(Category category) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream stream = ContentManager.openRemoteStream(new URL(category.indexUrl()), 5000, 10000);
                 InputStreamReader reader = new InputStreamReader(stream)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                List<Pack> packs = Pack.LIST_CODEC.parse(JsonOps.INSTANCE, json.get("packs"))
                        .resultOrPartial(Legacy4J.LOGGER::warn)
                        .orElseGet(ArrayList::new);
                loadAutoApplyTags(category, json);
                return packs;
            } catch (Exception e) {
                Legacy4J.LOGGER.warn("Failed to fetch content index from {}: {}", category.indexUrl(), e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    private static void loadAutoApplyTags(Category category, JsonObject json) {
        String prefix = category.id() + ":";
        ContentManager.AUTO_APPLY_RESOURCE_PACKS.removeIf(key -> key.startsWith(prefix));
        if (!json.has("packs") || !json.get("packs").isJsonArray()) return;
        for (JsonElement element : json.getAsJsonArray("packs")) {
            if (!element.isJsonObject()) continue;
            JsonObject packJson = element.getAsJsonObject();
            if (packJson.has("id") && hasAutoApplyResourcePackTag(packJson)) {
                ContentManager.AUTO_APPLY_RESOURCE_PACKS.add(prefix + packJson.get("id").getAsString());
            }
        }
    }

    private static boolean hasAutoApplyResourcePackTag(JsonObject packJson) {
        if (!packJson.has("tags") || !packJson.get("tags").isJsonArray()) return false;
        for (JsonElement tag : packJson.getAsJsonArray("tags")) {
            if (tag.isJsonPrimitive() && ContentManager.AUTO_APPLY_RESOURCE_PACK_TAG.equals(tag.getAsString())) return true;
        }
        return false;
    }

    private static CompletableFuture<List<Pack>> fetchStarterpacksIndex() {
        return fetchStarterpacksBundlePacks().thenApply(bundlePacks -> List.of(new Pack(
            ContentManager.STARTERPACKS_PACK_ID,
            "Download All!",
            "Download every mash-up pack, skin pack, texture pack, and more!",
            Optional.empty(),
            Optional.of(STARTERPACKS_PREVIEW),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            bundlePacks,
            Optional.empty(),
            List.of(),
            List.of()
        )));
    }

    private static List<Category> getStarterpacksCategories() {
        return ContentManager.CATEGORIES.stream()
            .filter(c -> !ContentManager.STARTERPACKS_CATEGORY_ID.equals(c.id()) && !"bundle_packs".equals(c.id()) && !"legacy4j".equals(c.id()))
            .toList();
    }

    private static CompletableFuture<List<Pack.BundlePack>> fetchStarterpacksBundlePacks() {
        List<Category> categories = getStarterpacksCategories();
        List<CompletableFuture<List<Pack>>> futures = categories.stream().map(ContentIndexLoader::fetchIndex).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
            List<Pack.BundlePack> bundlePacks = new ArrayList<>();
            for (int i = 0; i < categories.size(); i++) {
                Category category = categories.get(i);
                for (Pack pack : futures.get(i).getNow(List.of())) {
                    bundlePacks.add(new Pack.BundlePack(
                        category.id(),
                        pack.id(),
                        pack.name(),
                        pack.description(),
                        pack.downloadURI(),
                        pack.imageUrl(),
                        pack.checkSum(),
                        pack.worldTemplateDownloadURI(),
                        pack.worldTemplateCheckSum(),
                        pack.worldTemplateFolderName(),
                        pack.downloadVariants(),
                        pack.worldTemplateVariants()
                    ));
                }
            }
            return bundlePacks;
        });
    }
}
