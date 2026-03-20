//? if >=1.21.11 {
/*package wily.legacy.mixin.base;

import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GameRuleCategory.class)
public interface GameRuleCategoryAccessor {
    @Accessor("SORT_ORDER")
    static List<GameRuleCategory> getSortOrder() {
        throw new AssertionError();
    }
}
*///?}
