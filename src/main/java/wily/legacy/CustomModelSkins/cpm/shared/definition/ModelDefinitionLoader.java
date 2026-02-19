package wily.legacy.CustomModelSkins.cpm.shared.definition;

import com.google.common.cache.*;
import com.google.common.util.concurrent.UncheckedExecutionException;
import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpl.util.LocalizedIOException;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftObjectHolder;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.config.ResourceLoader;
import wily.legacy.CustomModelSkins.cpm.shared.config.ResourceLoader.ResourceEncoding;
import wily.legacy.CustomModelSkins.cpm.shared.definition.Link.ResolvedLink;
import wily.legacy.CustomModelSkins.cpm.shared.io.ChecksumInputStream;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.parts.IModelPart;
import wily.legacy.CustomModelSkins.cpm.shared.parts.ModelPartEnd;
import wily.legacy.CustomModelSkins.cpm.shared.parts.ModelPartType;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureType;
import wily.legacy.CustomModelSkins.cpm.shared.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class ModelDefinitionLoader<GP> {
    public static final String PLAYER_UNIQUE = "player";
    public static final String SKULL_UNIQUE = "skull";
    public static final Executor THREAD_POOL = java.util.concurrent.ForkJoinPool.commonPool();
    private Function<GP, Player<?>> playerFactory;
    private Function<GP, UUID> getUUID;
    private Function<GP, String> getName;
    private final LoadingCache<Key, Player<?>> cache = CacheBuilder.newBuilder().expireAfterAccess(MinecraftObjectHolder.DEBUGGING ? 10000L : 15L, TimeUnit.SECONDS).removalListener(new RemovalListener<Key, Player<?>>() {
        @Override
        public void onRemoval(RemovalNotification<ModelDefinitionLoader<GP>.Key, Player<?>> notification) {
            notification.getValue().cleanup();
        }
    }).build(CacheLoader.from(this::loadPlayer));

    private Player<?> loadPlayer(Key key) {
        Player<?> player = playerFactory.apply(key.profile);
        try {
            player.unique = key.uniqueKey;
            CompletableFuture<Void> texLoad = player.getTextures().load();
            if (key.uniqueKey.startsWith("model:")) {
                String b64 = key.uniqueKey.substring(6);
                Log.debug("Loading key model for " + key.profile);
                player.setModelDefinition(CompletableFuture.supplyAsync(() -> loadModel(b64, player), THREAD_POOL), true);
            } else if (serverModels.containsKey(key)) {
                Log.debug("Loading server model for " + key.profile);
                player.setModelDefinition(CompletableFuture.supplyAsync(() -> loadModel(serverModels.get(key), player), THREAD_POOL), true);
            } else {
                Log.debug("Loading skin model for " + key.profile);
                player.setModelDefinition(texLoad.thenCompose(v -> player.getTextures().getTexture(TextureType.SKIN)).thenApplyAsync(skin -> {
                    if (skin != null && player.getModelDefinition() == null) {
                        return loadModel(skin, player);
                    } else if (!player.getTextures().hasTexture(TextureType.SKIN) && player.isClientPlayer()) {
                        return new ModelDefinition(new LocalizedIOException("Custom skin not found", net.minecraft.network.chat.Component.translatable("error.cpm.no_skin_url")), player);
                    } else {
                        return null;
                    }
                }, THREAD_POOL), false);
            }
        } catch (Exception e) {
            player.setModelDefinition(CompletableFuture.completedFuture(new ModelDefinition(e, player)), false);
        }
        return player;
    }

    private static final Map<String, ResourceLoader> LOADERS = new HashMap<>();
    private final Cache<Link, ResolvedLink> linkCache = CacheBuilder.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).build();
    private final Cache<Link, ResolvedLink> localCache = CacheBuilder.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).build();
    private ConcurrentHashMap<Key, byte[]> serverModels = new ConcurrentHashMap<>();

    static {
        LOADERS.put("local", new ResourceLoader() {
            @Override
            public byte[] loadResource(String path, ResourceEncoding enc, ModelDefinition def) throws IOException {
                throw new LocalizedIOException("Test in-game model", net.minecraft.network.chat.Component.translatable("error.cpm.testModel"));
            }
        });
    }

    private Image template;
    public static final int HEADER = 0x53;

    public ModelDefinitionLoader(Function<GP, Player<?>> playerFactory, Function<GP, UUID> getUUID, Function<GP, String> getName) {
        try (InputStream is = ModelDefinitionLoader.class.getResourceAsStream("/assets/cpm/textures/template/free_space_template.png")) {
            this.template = Image.loadFrom(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template", e);
        }
        this.playerFactory = playerFactory;
        this.getUUID = getUUID;
        this.getName = getName;
    }

    public Player<?> loadPlayer(GP player, String unique) {
        try {
            return cache.get(new Key(player, unique));
        } catch (ExecutionException | UncheckedExecutionException e) {
            Log.debug("Error loading player model data", e);
            return null;
        }
    }

    public ModelDefinition loadModel(String data, Player<?> player) {
        return loadModel(Base64.getDecoder().decode(data), player);
    }

    public ModelDefinition loadModel(byte[] data, Player<?> player) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            return loadModel(in, player);
        } catch (Exception e) {
            return new ModelDefinition(e, player);
        }
    }

    public ModelDefinition loadModel(Image skin, Player<?> player) {
        return new ModelDefinition(new IOException("Skin-embedded CPM decode disabled"), player);
    }

    private ModelDefinition loadModel(InputStream in, Player<?> player) {
        ModelDefinition def = new ModelDefinition(this, player);
        try {
            if (in.read() != HEADER) return null;
            ChecksumInputStream cis = new ChecksumInputStream(in);
            IOHelper din = new IOHelper(cis);
            List<IModelPart> parts = new ArrayList<>();
            while (true) {
                IModelPart part = din.readObjectBlock(ModelPartType.VALUES, (t, d) -> t.getFactory().create(d, def));
                if (part == null) continue;
                if (part instanceof ModelPartEnd) {
                    cis.checkSum();
                    break;
                }
                parts.add(part);
            }
            def.setParts(parts);
            def.validate();
            Log.debug(def);
        } catch (Throwable e) {
            def.setError(e);
        }
        return def;
    }

    private ResolvedLink load0(Link link, ResourceEncoding enc, ModelDefinition def) throws SafetyException {
        try {
            ResourceLoader rl = LOADERS.get(link.loader);
            if (rl == null) throw new IOException("Couldn't find loader");
            return new ResolvedLink(rl.loadResource(link, enc, def));
        } catch (SafetyException e) {
            throw e;
        } catch (IOException e) {
            ResolvedLink rl = localCache.getIfPresent(link);
            if (rl != null) return rl;
            else return new ResolvedLink(e);
        }
    }

    public void putLocalResource(Link key, byte[] value) {
        localCache.put(key, new ResolvedLink(value));
    }

    public void clearServerData() {
        serverModels.clear();
    }

    public void setModel(GP forPlayer, byte[] data, boolean forced) {
        if (data == null) {
            Key key = new Key(forPlayer, null);
            serverModels.remove(key);
            invalidateAll(key);
        } else {
            serverModels.put(new Key(forPlayer, null), data);
            Player<?> player = reloadPlayer(forPlayer, PLAYER_UNIQUE);
            player.forcedSkin = forced;
        }
    }

    public Player<?> getLoadedPlayer(GP forPlayer) {
        Key key = new Key(forPlayer, PLAYER_UNIQUE);
        return cache.getIfPresent(key);
    }

    private void invalidateAll(Key key) {
        cache.asMap().keySet().removeIf(key::equals);
    }

    public void execute(Runnable task) {
        THREAD_POOL.execute(task);
    }

    private class Key {
        private UUID uuid;
        private GP profile;
        private String uniqueKey;

        public Key(GP player, String unique) {
            this.profile = player;
            this.uuid = getUUID.apply(player);
            this.uniqueKey = unique;
        }

        public Key(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Key other = (Key) obj;
            if (uuid == null) {
                if (other.uuid != null) return false;
            } else if (!uuid.equals(other.uuid)) return false;
            if (uniqueKey == null || other.uniqueKey == null) return true;
            if (!uniqueKey.equals(other.uniqueKey)) return false;
            return true;
        }
    }

    public Player<?> reloadPlayer(GP gprofile, String unique) {
        Key key = new Key(gprofile, null);
        invalidateAll(key);
        return loadPlayer(gprofile, unique);
    }
}
