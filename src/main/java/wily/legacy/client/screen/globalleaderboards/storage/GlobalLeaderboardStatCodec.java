package wily.legacy.client.screen.globalleaderboards.storage;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import wily.factoryapi.FactoryAPIPlatform;

public final class GlobalLeaderboardStatCodec {
   private static final String DELIMITER = "|";

   private GlobalLeaderboardStatCodec() {
   }

   public static String encode(Stat<?> stat) {
      Identifier typeId = BuiltInRegistries.STAT_TYPE.getKey(stat.getType());
      if (typeId == null) {
         return "";
      }

      Identifier valueId = encodeValueId(stat);
      return valueId == null ? "" : typeId + DELIMITER + valueId;
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   public static Stat<?> decode(String encoded) {
      if (encoded == null || encoded.isBlank()) {
         return null;
      }

      int separator = encoded.indexOf(DELIMITER);
      if (separator <= 0 || separator >= encoded.length() - 1) {
         return null;
      }

      Identifier typeId = Identifier.tryParse(encoded.substring(0, separator));
      Identifier valueId = Identifier.tryParse(encoded.substring(separator + 1));
      if (typeId == null || valueId == null) {
         return null;
      }

      StatType type = FactoryAPIPlatform.getRegistryValue(typeId, BuiltInRegistries.STAT_TYPE);
      if (type == null) {
         return null;
      }

      Object value = FactoryAPIPlatform.getRegistryValue(valueId, type.getRegistry());
      return value == null ? null : type.get(value);
   }

   public static Map<String, Integer> encodeMap(Object2IntMap<Stat<?>> statsMap) {
      LinkedHashMap<String, Integer> encoded = new LinkedHashMap<>();
      statsMap.object2IntEntrySet().forEach(entry -> {
         String key = encode(entry.getKey());
         if (!key.isBlank() && entry.getIntValue() > 0) {
            encoded.put(key, entry.getIntValue());
         }
      });
      return encoded;
   }

   public static Object2IntOpenHashMap<Stat<?>> decodeMap(Map<String, Integer> values) {
      Object2IntOpenHashMap<Stat<?>> decoded = new Object2IntOpenHashMap<>();
      if (values == null) {
         return decoded;
      }

      values.forEach((key, value) -> {
         Stat<?> stat = decode(key);
         if (stat != null && value != null && value > 0) {
            decoded.put(stat, value);
         }
      });
      return decoded;
   }

   private static Identifier encodeValueId(Stat<?> stat) {
      Registry<?> registry = stat.getType().getRegistry();
      Object value = stat.getValue();
      return registry == null || value == null ? null : ((Registry<Object>)registry).getKey(value);
   }
}
