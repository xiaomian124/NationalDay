package com.xiaomian124.nationalday.listener;

import com.xiaomian124.nationalday.NationalDay;
import com.xiaomian124.nationalday.item.ItemManager;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final NationalDay plugin;

    public EntityDeathListener(NationalDay plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWardenDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() != EntityType.WARDEN) return;

        if (!plugin.getNationalDayManager().isCurrentlyActive()) return;

        int chance = plugin.getConfig().getInt("warden-drop-chance", 100);

        if (chance >= 100) {
            event.getDrops().add(ItemManager.createNationalFlag());
        } else if (chance > 0 && Math.random() * 100 < chance) {
            event.getDrops().add(ItemManager.createNationalFlag());
        }
    }
}