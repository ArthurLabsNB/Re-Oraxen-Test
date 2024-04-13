package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;

public class CustomBlockMiningListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCustomBlock(BlockDamageEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final Player player = event.getPlayer();

        CustomBlockMechanic mechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());
        if (mechanic == null || player.getGameMode() == GameMode.CREATIVE) return;

        event.setCancelled(true);
        OraxenPlugin.get().breakerManager().startBlockBreak(player, location, mechanic);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageAbort(BlockDamageAbortEvent event) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
    }
}
