package wily.legacy.config;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyConfigWidget;
import wily.legacy.network.CommonConfigSyncPayload;
import wily.legacy.util.LegacyComponents;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.*;
import java.util.stream.Collectors;

public interface LegacyConfig<T> extends Bearer<T> {
    StorageHandler COMMON_STORAGE = new StorageHandler("legacy/common.json");

    Logger LOGGER = LogManager.getLogger("legacy_config");

    Map<LegacyConfig<?>, Supplier<LegacyConfigWidget<?>>> displayMap = new HashMap<>();

    String getKey();

    T defaultValue();

    default void reset(){
        set(defaultValue());
    }

    Codec<T> codec();

    StorageAccess getStorageAccess();

    ArbitrarySupplier<LegacyConfigDisplay<T>> getDisplay();

    static <T> void setAndSync(LegacyConfig<T> config, T value){
        config.set(value);
        sync(config);
    }

    static <T> void sync(LegacyConfig<T> config){
        CommonConfigSyncPayload payload = CommonConfigSyncPayload.of(config);
        if (Legacy4J.currentServer == null) {
            CommonNetwork.sendToServer(payload);
        } else CommonNetwork.sendToPlayers(Legacy4J.currentServer.getPlayerList().getPlayers(), CommonConfigSyncPayload.of(config));
    }

    interface StorageAccess {
        void save();

        default <T,E> DataResult<E> encode(LegacyConfig<T> config, DynamicOps<E> ops) {
            return config.codec().encodeStart(ops,config.get());
        }

        default <T> DataResult<T> decode(LegacyConfig<T> config, Dynamic<?> dynamic) {
            DataResult<T> result = config.codec().parse(dynamic);
            result.result().ifPresent(v->whenParsed(config,v));
            return result;
        }

        default <T> void whenParsed(LegacyConfig<T> config, T newValue) {
            config.set(newValue);
        }
    }

    static <T> void saveOptionAndConsume(LegacyConfig<T> config, T newValue, Consumer<T> consumer) {
        config.set(newValue);
        config.save();
        consumer.accept(newValue);
    }

    class StorageHandler implements StorageAccess {
        public final File file;
        public final Map<String, LegacyConfig<?>> configMap;

        public StorageHandler(File file, Map<String, LegacyConfig<?>> configMap){
            this.file = file;
            this.configMap = configMap;
        }

        public StorageHandler(File file){
            this(file, new HashMap<>());
        }

        public StorageHandler(String configDirectoryFile){
            this(FactoryAPI.getConfigDirectory().resolve(configDirectoryFile).toFile());
        }

        @Override
        public void save() {
            LegacyConfig.save(file, configMap);
        }

        public void load(){
            LegacyConfig.load(file, configMap);
        }

        public <T> void decodeConfigs(Dynamic<T> dynamic, Consumer<LegacyConfig<?>> afterDecode){
            LegacyConfig.decodeConfigs(configMap, dynamic, afterDecode);
        }

        public <T> T encodeConfigs(DynamicOps<T> ops){
            return LegacyConfig.encodeConfigs(configMap, ops);
        }

        public <T> LegacyConfig<T> register(String key, LegacyConfig<T> config){
            configMap.put(key,config);
            return config;
        }
        public <T> LegacyConfig<T> register(LegacyConfig<T> config){
            return register(config.getKey(), config);
        }

    }


    default void save(){
        getStorageAccess().save();
    }

    default DataResult<T> decode(Dynamic<?> dynamic) {
        return getStorageAccess().decode(this,dynamic);
    }

    default <E> DataResult<E> encode(DynamicOps<E> ops) {
        return getStorageAccess().encode(this,ops);
    }

    static <T> LegacyConfig<T> create(String key, ArbitrarySupplier<LegacyConfigDisplay<T>> display, T defaultValue, Bearer<T> bearer, Codec<T> codec, Consumer<T> consumer, StorageAccess access){
        return new LegacyConfig<>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public T defaultValue() {
                return defaultValue;
            }

            @Override
            public Codec<T> codec() {
                return codec;
            }

            @Override
            public StorageAccess getStorageAccess() {
                return access;
            }

            @Override
            public ArbitrarySupplier<LegacyConfigDisplay<T>> getDisplay() {
                return display;
            }


            @Override
            public void set(T t) {
                bearer.set(t);
                consumer.accept(t);
            }

            @Override
            public T get() {
                return bearer.get();
            }
        };
    }

    static <T> LegacyConfig<T> createWithWidget(LegacyConfig<T> config, Supplier<LegacyConfigWidget<T>> legacyConfigWidgetSupplier){
        if (FactoryAPI.isClient()) displayMap.put(config,()->legacyConfigWidgetSupplier.get());
        return config;
    }

    static LegacyConfig<Boolean> createCommonBoolean(String key, ArbitrarySupplier<LegacyConfigDisplay<Boolean>> display, boolean defaultValue){
        return createCommonBoolean(key, display, defaultValue, b->{});
    }
    static LegacyConfig<Boolean> createCommonBoolean(String key, ArbitrarySupplier<LegacyConfigDisplay<Boolean>> display,  boolean defaultValue, Consumer<Boolean> booleanConsumer){
        return createBoolean(key, display, defaultValue, booleanConsumer, COMMON_STORAGE);
    }

    static LegacyConfig<Boolean> createBoolean(String key, ArbitrarySupplier<LegacyConfigDisplay<Boolean>> display, boolean defaultValue, Consumer<Boolean> consumer, StorageAccess access){
        return createWithWidget(create(key, display, defaultValue, Bearer.of(defaultValue), Codec.BOOL, consumer, access), ()->LegacyConfigWidget.createCommonTickBox(display.get().tooltip()));
    }

    static LegacyConfig<Integer> createInteger(String key, ArbitrarySupplier<LegacyConfigDisplay<Integer>> display, BiFunction<Component,Integer,Component> captionFunction, int min, int max, int defaultValue, Consumer<Integer> consumer, StorageAccess access){
        return createInteger(key, display, captionFunction, min, ()-> max, Integer.MAX_VALUE, defaultValue, consumer, access);
    }

    static LegacyConfig<Integer> createInteger(String key, ArbitrarySupplier<LegacyConfigDisplay<Integer>> display, BiFunction<Component,Integer,Component> captionFunction, int min, IntSupplier max, int maxEncodable, int defaultValue, Consumer<Integer> consumer, StorageAccess access){
        return createWithWidget(create(key, display, defaultValue, Bearer.of(defaultValue), Codec.intRange(min,maxEncodable), consumer, access),()->LegacyConfigWidget.createCommonIntegerSlider(display.get().tooltip(),captionFunction,min,max));
    }

    static LegacyConfig<Double> createDouble(String key, ArbitrarySupplier<LegacyConfigDisplay<Double>> display, BiFunction<Component,Double,Component> captionFunction, double defaultValue, Consumer<Double> consumer, StorageAccess access) {
        return createWithWidget(create(key, display, defaultValue, Bearer.of(defaultValue), Codec.DOUBLE, consumer, access), ()->LegacyConfigWidget.createCommonSlider(display.get().tooltip(),captionFunction));
    }
    static <T> LegacyConfig<T> create(String key, ArbitrarySupplier<LegacyConfigDisplay<T>> display, BiFunction<Component,T,Component> captionFunction, Codec<T> codec, Function<Integer,T> valueGetter, Function<T, Integer> valueSetter, Supplier<Integer> valuesSize, T defaultValue, Consumer<T> consumer, StorageAccess access) {
        return createWithWidget(create(key, display, defaultValue, Bearer.of(defaultValue), codec, consumer, access), ()->LegacyConfigWidget.createCommonSliderFromInt(display.get().tooltip(),captionFunction,valueGetter,valueSetter,valuesSize));
    }
    static <T> LegacyConfig<T> create(String key, ArbitrarySupplier<LegacyConfigDisplay<T>> display, BiFunction<Component,T,Component> captionFunction, Function<Integer,T> valueGetter, Function<T, Integer> valueSetter, Supplier<Integer> valuesSize, T defaultValue, Consumer<T> consumer, StorageAccess access) {
        return create(key, display, captionFunction, Codec.INT.xmap(valueGetter, valueSetter), valueGetter, valueSetter, valuesSize, defaultValue, consumer, access);
    }

    LegacyConfig<Boolean> legacyCauldrons = COMMON_STORAGE.register(createCommonBoolean("legacy.mixin.base.cauldron", ()-> new LegacyConfigDisplay<>("legacyCauldrons" , b-> LegacyComponents.NEEDS_RESTART), true, b-> {}));
    LegacyConfig<Boolean> legacyPistons = COMMON_STORAGE.register(createCommonBoolean("legacy.mixin.base.piston", ()-> new LegacyConfigDisplay<>("legacyPistons" , b-> LegacyComponents.NEEDS_RESTART), true, b-> {}));
    LegacyConfig<Boolean> legacyCombat = COMMON_STORAGE.register(createCommonBoolean("legacyCombat", ()-> new LegacyConfigDisplay<>(LegacyConfig.legacyCombat.getKey()), true, b-> {}));
    LegacyConfig<Boolean> legacySwordBlocking = COMMON_STORAGE.register(createCommonBoolean("legacySwordBlocking", ()-> new LegacyConfigDisplay<>(LegacyConfig.legacySwordBlocking.getKey()), false, b-> {}));


    static <T> void decodeConfigs(Map<String,? extends LegacyConfig<?>> configs, Dynamic<T> dynamic, Consumer<LegacyConfig<?>> afterDecode){
        dynamic.asMapOpt().result().ifPresent(m->m.forEach(p-> p.getFirst().asString().result().ifPresent(s-> {
            LegacyConfig<?> config = configs.get(s);
            config.decode(p.getSecond());
            afterDecode.accept(config);
        })));
    }

    static <T> T encodeConfigs(Map<String,? extends LegacyConfig<?>> configs, DynamicOps<T> ops){
        return ops.createMap(configs.entrySet().stream().collect(Collectors.toMap(e-> ops.createString(e.getKey()), e-> e.getValue().encode(ops).result().orElseThrow())));
    }

    static void load(File file, Map<String,? extends LegacyConfig<?>> configs){
        if (!file.exists()) {
            save(file, configs);
            return;
        }
        try (BufferedReader r = Files.newReader(file, Charsets.UTF_8)){
            GsonHelper.parse(r).asMap().forEach((s,e)->{
                LegacyConfig<?> config = configs.get(s);
                if (config == null) {
                    LOGGER.warn("Config named as {} from {} config file wasn't found",s, file.toString());
                } else config.decode(new Dynamic<>(JsonOps.INSTANCE,e));
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to load the config {}: {}",file.toString(),e);
        }
    }

    static void save(File file, Map<String,? extends LegacyConfig<?>> configs){
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        try (JsonWriter w = new JsonWriter(Files.newWriter(file, Charsets.UTF_8))){
            w.setSerializeNulls(false);
            w.setIndent("  ");
            JsonObject obj = new JsonObject();
            configs.forEach((s,config)-> config.encode(JsonOps.INSTANCE).resultOrPartial(error-> LOGGER.warn("Failed to save config named as {} from {} config file: {}",s, file.toString(),error)).ifPresent(e-> obj.add(s,e)));
            GsonHelper.writeValue(w,obj, Comparator.naturalOrder());
        } catch (IOException e) {
            LOGGER.warn("Failed to save the config {}: {}",file.toString(),e);
        }
    }
}
