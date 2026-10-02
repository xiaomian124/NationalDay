package com.xiaomian124.nationalday.listener;

import com.xiaomian124.nationalday.NationalDay;
import com.xiaomian124.nationalday.gui.FlagGuiHolder;
import com.xiaomian124.nationalday.manager.FlagManager;
import com.xiaomian124.nationalday.util.Keys;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GuiClickListener implements Listener {

    private final NationalDay plugin;

    public GuiClickListener(NationalDay plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FlagGuiHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        if (slot >= 45) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Inventory gui = holder.getInventory();
        ItemStack cursor = event.getCursor();
        boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;

        switch (slot) {
            case FlagGuiHolder.POTION_SLOT -> {
                if (holder.isActivated()) return;
                if (!hasCursor || cursor.getType() != Material.POTION) return;

                ItemStack existing = gui.getItem(slot);
                if (existing != null && existing.getType() != Material.AIR) {
                    plugin.getFlagManager().giveItem(player, existing);
                }

                ItemStack one = cursor.clone();
                one.setAmount(1);
                gui.setItem(slot, one);

                setCursor(event, cursor, 1);

                player.playSound(player.getLocation(),
                        Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1.0f, 1.0f);
            }
            case FlagGuiHolder.TIME_SLOT -> {
                if (!hasCursor) return;

                if (holder.isActivated()) {
                    Banner banner = (Banner) holder.getFlagBlock().getState();
                    int currentDur = banner.getPersistentDataContainer()
                            .getOrDefault(Keys.DURATION, PersistentDataType.INTEGER, 0);
                    if (currentDur >= FlagManager.MAX_DURATION_SECONDS) {
                        plugin.getFlagManager().updateTimeSlot(holder);
                        return;
                    }
                }

                if (holder.isActivated()) {
                    Banner banner = (Banner) holder.getFlagBlock().getState();
                    long lastAct = banner.getPersistentDataContainer()
                            .getOrDefault(plugin.getFlagManager().getLastActivateKey(),
                                    PersistentDataType.LONG, 0L);
                    if (System.currentTimeMillis() - lastAct < 20_000) {
                        holder.setActivated(false);
                        gui.setItem(FlagGuiHolder.POTION_SLOT, null);
                        plugin.getFlagManager().stopEffect(holder.getFlagBlock());
                        return;
                    }
                }

                ItemStack existing = gui.getItem(slot);
                if (existing != null && existing.getType() != Material.AIR) {
                    plugin.getFlagManager().giveItem(player, existing);
                }

                ItemStack one = cursor.clone();
                one.setAmount(1);
                gui.setItem(slot, one);

                setCursor(event, cursor, 1);
            }
            case FlagGuiHolder.RANGE_SLOT, FlagGuiHolder.LEVEL_SLOT -> {
                if (!hasCursor) return;

                ItemStack current = gui.getItem(slot);
                if (current != null && current.getType() == Material.RED_STAINED_GLASS_PANE) {
                    return;
                }

                ItemStack existing = gui.getItem(slot);
                if (existing != null && existing.getType() != Material.AIR) {
                    plugin.getFlagManager().giveItem(player, existing);
                }

                ItemStack one = cursor.clone();
                one.setAmount(1);
                gui.setItem(slot, one);

                setCursor(event, cursor, 1);
            }
            case FlagGuiHolder.CONFIRM_SLOT -> {
                if (holder.isActivated() && !hasAnyUpgrade(holder)) return;
                boolean ok = plugin.getFlagManager().activate(holder, player);
                if (ok) {
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
                    player.closeInventory();
                }
            }
            case FlagGuiHolder.CANCEL_SLOT -> {
                plugin.getFlagManager().returnAllItems(player, gui);
                player.playSound(player.getLocation(),
                        Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
                holder.setConfirmed(true);
                player.closeInventory();
            }
            default -> {}
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof FlagGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof FlagGuiHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (holder.isConfirmed()) return;
        plugin.getFlagManager().returnAllItems(player, holder.getInventory());
    }

    private void setCursor(InventoryClickEvent event, ItemStack cursor, int amount) {
        int remain = cursor.getAmount() - amount;
        if (remain <= 0) {
            event.setCursor(null);
        } else {
            ItemStack newCursor = cursor.clone();
            newCursor.setAmount(remain);
            event.setCursor(newCursor);
        }
    }

    private boolean hasAnyUpgrade(FlagGuiHolder holder) {
        Inventory inv = holder.getInventory();

        ItemStack t = inv.getItem(FlagGuiHolder.TIME_SLOT);
        if (t != null && t.getType() != Material.AIR
                && t.getType() != Material.RED_STAINED_GLASS_PANE
                && t.getType() != Material.BLACK_STAINED_GLASS_PANE
                && plugin.getFlagManager().getDurationFromMaterial(t.getType()) > 0) {
            return true;
        }

        ItemStack r = inv.getItem(FlagGuiHolder.RANGE_SLOT);
        if (r != null && r.getType() == Material.NETHER_STAR
                && holder.getRangeUpgrades() < 8) {
            return true;
        }

        ItemStack l = inv.getItem(FlagGuiHolder.LEVEL_SLOT);
        if (l != null && l.getType() == Material.WITHER_SKELETON_SKULL
                && holder.getLevelUpgrades() < 9) {
            return true;
        }

        return false;
    }
}