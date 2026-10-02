package com.xiaomian124.nationalday.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.xiaomian124.nationalday.util.Keys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public final class ItemManager {

    private ItemManager() {}

    public static ItemStack createNationalFlag() {
        ItemStack item = new ItemStack(Material.RED_BANNER);
        BannerMeta meta = (BannerMeta) item.getItemMeta();

        meta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.BRICKS));
        meta.addPattern(new Pattern(DyeColor.RED, PatternType.SMALL_STRIPES));
        meta.addPattern(new Pattern(DyeColor.RED, PatternType.BORDER));
        meta.addPattern(new Pattern(DyeColor.RED, PatternType.DIAGONAL_UP_LEFT));
        meta.addPattern(new Pattern(DyeColor.RED, PatternType.HALF_VERTICAL_RIGHT));
        meta.addPattern(new Pattern(DyeColor.RED, PatternType.RHOMBUS));

        meta.displayName(MiniMessage.miniMessage()
                .deserialize("<!italic><yellow>国庆旗帜"));
        meta.lore(List.of(
                MiniMessage.miniMessage()
                        .deserialize("<!italic><gray>国庆节新物品，类似于信标")
        ));

        meta.getPersistentDataContainer().set(
                Keys.IS_NATIONAL_FLAG, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isNationalFlag(ItemStack item) {
        if (item == null || item.getType() != Material.RED_BANNER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer()
                .has(Keys.IS_NATIONAL_FLAG, PersistentDataType.BYTE);
    }

    public static ItemStack createPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!italic> "));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCustomHead(String base64Value) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
        profile.setProperty(new ProfileProperty("textures", base64Value));
        meta.setPlayerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }

    public static boolean isLockedPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer()
                .has(Keys.LOCKED_POTION, PersistentDataType.BYTE);
    }
}