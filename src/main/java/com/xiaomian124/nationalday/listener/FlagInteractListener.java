package com.xiaomian124.nationalday.listener;

import com.xiaomian124.nationalday.NationalDay;
import com.xiaomian124.nationalday.gui.FlagGuiHolder;
import com.xiaomian124.nationalday.item.ItemManager;
import com.xiaomian124.nationalday.util.Keys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class FlagInteractListener implements Listener {

    private final NationalDay plugin;

    public FlagInteractListener(NationalDay plugin) { this.plugin = plugin; }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!ItemManager.isNationalFlag(item)) return;
        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof Banner banner)) return;
        PersistentDataContainer pdc = banner.getPersistentDataContainer();
        pdc.set(Keys.IS_NATIONAL_FLAG, PersistentDataType.BYTE, (byte) 1);
        banner.update();
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.RED_BANNER) return;
        if (!(block.getState() instanceof Banner banner)) return;
        if (!banner.getPersistentDataContainer()
                .has(Keys.IS_NATIONAL_FLAG, PersistentDataType.BYTE)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        FlagGuiHolder holder = new FlagGuiHolder(block);
        plugin.getFlagManager().setupGui(holder.getInventory());
        plugin.getFlagManager().syncActivatedState(holder);
        player.openInventory(holder.getInventory());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.RED_BANNER) return;
        if (!(block.getState() instanceof Banner banner)) return;
        if (!banner.getPersistentDataContainer()
                .has(Keys.IS_NATIONAL_FLAG, PersistentDataType.BYTE)) return;

        Player player = event.getPlayer();

        boolean wasActive = banner.getPersistentDataContainer()
                .has(Keys.ACTIVATED, PersistentDataType.BYTE);
        if (wasActive) {
            Location soundLoc = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().playSound(soundLoc,
                    Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.0f);
            plugin.getFlagManager().stopEffect(block);
        }

        event.setDropItems(false);
        block.getWorld().dropItemNaturally(
                block.getLocation(), ItemManager.createNationalFlag());

        if (player.getOpenInventory().getTopInventory().getHolder()
                instanceof FlagGuiHolder gh
                && gh.getFlagBlock().equals(block)) {
            player.closeInventory();
        }
    }
}