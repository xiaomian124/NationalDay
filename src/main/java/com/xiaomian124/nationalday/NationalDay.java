package com.xiaomian124.nationalday;

import com.xiaomian124.nationalday.command.NationalDayCommand;
import com.xiaomian124.nationalday.item.ItemManager;
import com.xiaomian124.nationalday.listener.EntityDeathListener;
import com.xiaomian124.nationalday.listener.FlagInteractListener;
import com.xiaomian124.nationalday.listener.GuiClickListener;
import com.xiaomian124.nationalday.listener.StartupListener;
import com.xiaomian124.nationalday.manager.FlagManager;
import com.xiaomian124.nationalday.manager.NationalDayManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class NationalDay extends JavaPlugin {

    private static NationalDay instance;
    private FlagManager flagManager;
    private NationalDayManager nationalDayManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        flagManager = new FlagManager(this);
        nationalDayManager = new NationalDayManager(this);
        nationalDayManager.loadConfig();
        nationalDayManager.start();

        registerRecipe();

        getServer().getPluginManager().registerEvents(new FlagInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);

        StartupListener startup = new StartupListener(this);
        getServer().getPluginManager().registerEvents(startup, this);

        getServer().getScheduler().runTaskLater(this, startup::scanAllLoadedChunks, 20L);

        NationalDayCommand command = new NationalDayCommand(this);
        if (getCommand("nationalday") != null) {
            getCommand("nationalday").setExecutor(command);
            getCommand("nationalday").setTabCompleter(command);
        }

        getLogger().info("NationalDay 插件已启用");
    }

    @Override
    public void onDisable() {
        if (nationalDayManager != null) {
            nationalDayManager.stop();
        }
        if (flagManager != null) {
            flagManager.shutdown();
        }
        getLogger().info("NationalDay 插件已禁用");
    }

    // config
    public void reloadPluginConfig() {
        reloadConfig();
        nationalDayManager.reload();
    }

    private void registerRecipe() {
        NamespacedKey key = new NamespacedKey(this, "national_flag_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, ItemManager.createNationalFlag());

        recipe.shape("WWW", "WNW", "WWW");
        recipe.setIngredient('W', Material.RED_WOOL);
        recipe.setIngredient('N', Material.NETHER_STAR);

        Bukkit.addRecipe(recipe);
    }

    public static NationalDay getInstance() {
        return instance;
    }

    public FlagManager getFlagManager() {
        return flagManager;
    }

    public NationalDayManager getNationalDayManager() {
        return nationalDayManager;
    }
}