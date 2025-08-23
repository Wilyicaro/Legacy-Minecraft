package wily.legacy.neoforge;

import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.util.ModInfo;
import wily.legacy.util.RegisterListing;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Legacy4JPlatformImpl {
    public static final Map<String,ModInfo> MOD_INFOS =  new HashMap<>();
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static TagKey<Item> getCommonItemTag(String commonTag) {
        return ItemTags.create(new ResourceLocation("forge", commonTag));
    }

    public static boolean isLoadingMod(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Collection<ModInfo> getMods() {
        LoadingModList.get().getMods().forEach(m-> getModInfo(m.getModId()));
        return MOD_INFOS.values();
    }
    public static ModInfo getModInfo(String modId) {
        return ModList.get().isLoaded(modId) ? MOD_INFOS.computeIfAbsent(modId, s-> new ModInfo() {
            IModFileInfo info = ModList.get().getModFileById(modId);
            Optional<? extends ModContainer> opt = ModList.get().getModContainerById(modId);
            @Override
            public Collection<String> getAuthors() {
                return opt.flatMap(c->c.getModInfo().getConfig().getConfigElement("authors").map(s-> Collections.singleton(String.valueOf(s)))).orElse(Collections.emptySet());
            }

            @Override
            public Optional<String> getHomepage() {
                return opt.flatMap(c->c.getModInfo().getConfig().getConfigElement("displayURL").map(String::valueOf));
            }

            @Override
            public Optional<String> getIssues() {
                return Optional.ofNullable(info instanceof ModFileInfo i ? i.getIssueURL() : null).map(URL::toString);
            }

            @Override
            public Optional<String> getSources() {
                return Optional.empty();
            }

            @Override
            public Collection<String> getCredits() {
                return opt.flatMap(c->c.getModInfo().getConfig().getConfigElement("credits").map(o-> Set.of(String.valueOf(o)))).orElse(Collections.emptySet());
            }

            @Override
            public Collection<String> getLicense() {
                return Collections.singleton(info.getLicense());
            }

            @Override
            public String getDescription() {
                return opt.map(c->c.getModInfo().getDescription()).orElse("");
            }

            @Override
            public Optional<String> getLogoFile(int i) {
                return this.info.getMods().stream().filter(m->m.getModId().equals(modId)).findFirst().flatMap(IModInfo::getLogoFile);
            }
            @Override
            public Optional<Path> findResource(String s) {
                return Optional.of(this.info.getFile().findResource(s)).filter(Files::exists);
            }

            @Override
            public String getId() {
                return modId;
            }

            @Override
            public String getVersion() {
                return opt.map(c->c.getModInfo().getVersion().toString()).orElse("");
            }

            @Override
            public String getName() {
                return opt.map(c->c.getModInfo().getDisplayName()).orElse("");
            }

        }) : null;
    }

    public static <T,V extends T> RegisterListing.Holder<V> deferredToRegisterHolder(DeferredHolder<T,V> holder){
        return new RegisterListing.Holder<>() {
            @Override
            public ResourceLocation getId() {
                return holder.getId();
            }
            @Override
            public V get() {
                return holder.get();
            }
        };
    }
    public static <T> RegisterListing<T> createLegacyRegister(String namespace, Registry<T> registry) {
        return new RegisterListing<>() {
            private final DeferredRegister<T> REGISTER = DeferredRegister.create(registry,namespace);

            @Override
            public Collection<Holder<T>> getEntries() {
                return stream().collect(Collectors.toSet());
            }
            @Override
            public Registry<T> getRegistry() {
                return registry;
            }
            @Override
            public String getNamespace() {
                return namespace;
            }
            @Override
            public void register() {
                REGISTER.register(Legacy4JForge.MOD_EVENT_BUS);
            }
            @Override
            public <V extends T> Holder<V> add(String id, Supplier<V> supplier) {
                return deferredToRegisterHolder(REGISTER.register(id,supplier));
            }
            @NotNull
            @Override
            public Iterator<Holder<T>> iterator() {
                return stream().iterator();
            }

            @Override
            public Stream<Holder<T>> stream() {
                return REGISTER.getEntries().stream().map(h->(Holder<T>) deferredToRegisterHolder(h));
            }
        };
    }

    public static Legacy4JPlatform.Loader getLoader() {
        return Legacy4JPlatform.Loader.NEOFORGE;
    }
    public static boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }


    public static<T extends CustomPacketPayload> void sendToPlayer(ServerPlayer serverPlayer, T packetHandler) {
        PacketDistributor.PLAYER.with(serverPlayer).send(packetHandler);
    }
    public static<T extends CustomPacketPayload> void sendToServer(T packetHandler) {
        PacketDistributor.SERVER.noArg().send(packetHandler);
    }

    public static Fluid getBucketFluid(BucketItem item) {
        return item.getFluid();
    }

    public static boolean isPackHidden(Pack pack) {
        return false;
    }

}
