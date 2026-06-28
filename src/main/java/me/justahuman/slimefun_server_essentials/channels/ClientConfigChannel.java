package me.justahuman.slimefun_server_essentials.channels;

import me.justahuman.slimefun_server_essentials.SlimefunServerEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 接收客户端上报的配置（receiveServerPayloads）。
 * 服务端据此决定是否向该玩家发送各 registry channel 的 Payload。
 */
public class ClientConfigChannel extends AbstractChannel {
    private static final Map<UUID, Boolean> RECEIVE_PAYLOADS = new HashMap<>();

    @Override
    public void onMessageReceived(@Nonnull Player player, @Nonnull byte[] message) {
        if (message.length < 1) {
            return;
        }
        boolean receive = message[0] != 0;
        RECEIVE_PAYLOADS.put(player.getUniqueId(), receive);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        super.onPluginMessageReceived(channel, player, message);
    }

    /**
     * 查询某玩家是否愿意接收服务端 Payload。默认为 true（未上报时按 true 处理）。
     */
    public static boolean shouldReceivePayloads(@Nonnull Player player) {
        return RECEIVE_PAYLOADS.getOrDefault(player.getUniqueId(), Boolean.TRUE);
    }

    @EventHandler
    @Override
    public void onQuit(PlayerQuitEvent event) {
        super.onQuit(event);
        RECEIVE_PAYLOADS.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public String getChannel() {
        return "slimefun_server_essentials:client_config";
    }
}
