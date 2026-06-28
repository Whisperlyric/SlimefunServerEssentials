package me.justahuman.slimefun_server_essentials.channels;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.justahuman.slimefun_server_essentials.SlimefunServerEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LoadingStateChannel extends AbstractChannel {
    private final Set<UUID> scheduled = new HashSet<>();

    @EventHandler
    @Override
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        // 调用父类：将玩家加入 players 集合，执行 onRegisterChannel 回调
        super.onPlayerRegisterChannel(event);
        if (!event.getChannel().equals(getChannel())) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        // 防止重复调度
        if (!scheduled.add(playerId)) {
            return;
        }

        // 客户端注册 loading_state 频道意味着已进入 Play phase，codec 已激活。
        // 延迟 20 ticks（1000ms）等待 ClientConfigPayload 到达，再发送 loading_state。
        SlimefunServerEssentials.getInstance().getServer().getScheduler().runTaskLater(
                SlimefunServerEssentials.getInstance(),
                () -> {
                    scheduled.remove(playerId);
                    sendLoadingState(player);
                },
                20L
        );
    }

    /**
     * 根据客户端配置发送 loading_state 和 registry channels。
     * 若客户端已断开则跳过。
     */
    private void sendLoadingState(Player player) {
        if (!player.isOnline()) {
            return;
        }

        boolean shouldSend = ClientConfigChannel.shouldReceivePayloads(player);
        ByteArrayDataOutput loadingStatePacket = ByteStreams.newDataOutput();
        if (shouldSend) {
            loadingStatePacket.writeInt(SlimefunServerEssentials.getComponentTypesChannel().messages.size());
            loadingStatePacket.writeInt(SlimefunServerEssentials.getItemsChannel().messages.size());
            loadingStatePacket.writeInt(SlimefunServerEssentials.getRecipeCategoriesChannel().messages.size());
            loadingStatePacket.writeInt(SlimefunServerEssentials.getRecipeDisplaysChannel().messages.size());
        } else {
            // 客户端不接收 Payload：发送全 0，告知客户端无需等待
            loadingStatePacket.writeInt(0);
            loadingStatePacket.writeInt(0);
            loadingStatePacket.writeInt(0);
            loadingStatePacket.writeInt(0);
        }
        sendMessage(player, loadingStatePacket);

        if (shouldSend) {
            SlimefunServerEssentials.getComponentTypesChannel().scheduleMessages(player);
            SlimefunServerEssentials.getItemsChannel().scheduleMessages(player);
            SlimefunServerEssentials.getRecipeCategoriesChannel().scheduleMessages(player);
            SlimefunServerEssentials.getRecipeDisplaysChannel().scheduleMessages(player);
        }
    }

    @Override
    public String getChannel() {
        return "slimefun_server_essentials:loading_state";
    }

    @EventHandler
    @Override
    public void onQuit(PlayerQuitEvent event) {
        super.onQuit(event);
        scheduled.remove(event.getPlayer().getUniqueId());
    }
}
