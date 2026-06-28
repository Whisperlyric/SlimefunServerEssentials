package me.justahuman.slimefun_server_essentials.channels;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.justahuman.slimefun_server_essentials.SlimefunServerEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

public class LoadingStateChannel extends AbstractChannel {
    @EventHandler @Override
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!event.getChannel().equals(getChannel())) {
            return;
        }

        Player player = event.getPlayer();
        // 延迟 10 ticks（500ms）后发送，等待客户端 ClientConfigPayload 到达
        // 因为 MC|Register 在 Configuration phase 触发，而 client_config 在 Play phase 才发送
        SlimefunServerEssentials.getInstance().getServer().getScheduler().runTaskLater(
                SlimefunServerEssentials.getInstance(),
                () -> sendLoadingState(player),
                10L
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
}
