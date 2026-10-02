package com.xiaomian124.nationalday.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class FlagGuiHolder implements InventoryHolder {

    public static final int TIME_SLOT    = 12;
    public static final int POTION_SLOT  = 19;
    public static final int RANGE_SLOT   = 21;
    public static final int CONFIRM_SLOT = 23;
    public static final int CANCEL_SLOT  = 25;
    public static final int LEVEL_SLOT   = 30;

    private final Block flagBlock;
    private final Inventory inventory;

    private boolean activated = false;
    private boolean confirmed = false;
    private int rangeUpgrades = 0;
    private int levelUpgrades = 0;

    public FlagGuiHolder(Block flagBlock) {
        this.flagBlock = flagBlock;
        this.inventory = Bukkit.createInventory(this, 45, Component.text("国庆旗帜"));
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public Block getFlagBlock() { return flagBlock; }
    public boolean isActivated() { return activated; }
    public void setActivated(boolean v) { this.activated = v; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean v) { this.confirmed = v; }
    public int getRangeUpgrades() { return rangeUpgrades; }
    public void setRangeUpgrades(int v) { this.rangeUpgrades = v; }
    public int getLevelUpgrades() { return levelUpgrades; }
    public void setLevelUpgrades(int v) { this.levelUpgrades = v; }
}