package wily.legacy.CustomModelSkins.cpm.client;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.world.entity.Avatar;

public class Platform {
    public static boolean isSitting(Avatar player) {
        return player.isPassenger();
    }

    public static Channel getChannel(Connection conn) {
        try {
            var f = Connection.class.getDeclaredField("channel");
            f.setAccessible(true);
            return (Channel) f.get(conn);
        } catch (Throwable e) {
            return null;
        }
    }
}
