package wily.legacy.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;

public interface LegacyPlayerInfo {
    default GameProfile getProfile(){
        return null;
    }
    int getPosition();
    void setPosition(int i);
    boolean isVisible();
    void setVisibility(boolean visible);
    boolean isExhaustionDisabled();
    void setDisableExhaustion(boolean exhaustion);
    boolean mayFlySurvival();
    void setMayFlySurvival(boolean mayFly);

    static LegacyPlayerInfo fromNetwork(FriendlyByteBuf buf){
        return new LegacyPlayerInfo() {
            int pos = buf.readVarInt();
            boolean invisible = buf.readBoolean();
            boolean exhaustion = buf.readBoolean();
            boolean mayFly = buf.readBoolean();
            public int getPosition() {
                return pos;
            }
            public void setPosition(int i) {
                pos = i;
            }
            public boolean isVisible() {
                return invisible;
            }
            public void setVisibility(boolean visible) {
                this.invisible = visible;
            }
            public boolean isExhaustionDisabled() {
                return exhaustion;
            }
            public void setDisableExhaustion(boolean exhaustion) {
                this.exhaustion = exhaustion;
            }
            public boolean mayFlySurvival() {
                return mayFly;
            }
            public void setMayFlySurvival(boolean mayFly) {
                this.mayFly = mayFly;
            }
        };
    }
    default void toNetwork(FriendlyByteBuf buf){
        buf.writeVarInt(getPosition());
        buf.writeBoolean(isVisible());
        buf.writeBoolean(isExhaustionDisabled());
        buf.writeBoolean(mayFlySurvival());
    }
    default void copyFrom(LegacyPlayerInfo info){
        this.setPosition(info.getPosition());
        this.setVisibility(info.isVisible());
        this.setDisableExhaustion(info.isExhaustionDisabled());
        this.setMayFlySurvival(info.mayFlySurvival());
    }
}
