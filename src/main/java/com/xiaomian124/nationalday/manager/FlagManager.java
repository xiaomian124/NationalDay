package com.xiaomian124.nationalday.manager;

import com.xiaomian124.nationalday.NationalDay;
import com.xiaomian124.nationalday.gui.FlagGuiHolder;
import com.xiaomian124.nationalday.item.ItemManager;
import com.xiaomian124.nationalday.util.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagManager {

    public static final String CONFIRM_HEAD =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDMxMmNhNDYzMmRlZjVmZmFmMmViMGQ5ZDdjYzdiNTVhNTBjNGUzOTIwZDkwMzcyYWFiMTQwNzgxZjVkZmJjNCJ9fX0=";
    public static final String CANCEL_HEAD =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzU1MmRjYjA1MGVmYzljYWI0MjdmNDYwNDhmMThmYzgwMzQ3NWY3In19fQ==";

    public static final int MAX_DURATION_SECONDS = 60 * 60;

    private static final Particle.DustOptions AMBIENT_RED_DUST =
            new Particle.DustOptions(Color.RED, 0.8f);

    private final NationalDay plugin;
    private final Map<Location, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final NamespacedKey lastActivateKey;

    public FlagManager(NationalDay plugin) {
        this.plugin = plugin;
        this.lastActivateKey = new NamespacedKey(plugin, "last_activate");
    }

    public NamespacedKey getLastActivateKey() { return lastActivateKey; }

    public void setupGui(Inventory inv) {
        ItemStack black = ItemManager.createPane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < 45; i++) {
            if (isFunctionalSlot(i)) continue;
            inv.setItem(i, black);
        }

        ItemStack confirm = ItemManager.createCustomHead(CONFIRM_HEAD);
        ItemMeta cm = confirm.getItemMeta();
        cm.displayName(MiniMessage.miniMessage().deserialize("<!italic><green>确认"));
        confirm.setItemMeta(cm);
        inv.setItem(FlagGuiHolder.CONFIRM_SLOT, confirm);

        ItemStack cancel = ItemManager.createCustomHead(CANCEL_HEAD);
        ItemMeta xm = cancel.getItemMeta();
        xm.displayName(MiniMessage.miniMessage().deserialize("<!italic><red>取消"));
        cancel.setItemMeta(xm);
        inv.setItem(FlagGuiHolder.CANCEL_SLOT, cancel);
    }

    private boolean isFunctionalSlot(int slot) {
        return slot == FlagGuiHolder.POTION_SLOT
                || slot == FlagGuiHolder.TIME_SLOT
                || slot == FlagGuiHolder.RANGE_SLOT
                || slot == FlagGuiHolder.LEVEL_SLOT;
    }

    public void syncActivatedState(FlagGuiHolder holder) {
        Block block = holder.getFlagBlock();
        if (!(block.getState() instanceof Banner banner)) return;
        PersistentDataContainer pdc = banner.getPersistentDataContainer();
        if (!pdc.has(Keys.ACTIVATED, PersistentDataType.BYTE)) return;

        holder.setActivated(true);
        int range = pdc.getOrDefault(Keys.RANGE, PersistentDataType.INTEGER, 8);
        int bonus = pdc.getOrDefault(Keys.LEVEL_BONUS, PersistentDataType.INTEGER, 0);
        holder.setRangeUpgrades((range - 8) / 8);
        holder.setLevelUpgrades(bonus);

        if (holder.getRangeUpgrades() >= 8) {
            invShowMax(holder, FlagGuiHolder.RANGE_SLOT, "范围");
        }
        if (holder.getLevelUpgrades() >= 9) {
            invShowMax(holder, FlagGuiHolder.LEVEL_SLOT, "等级");
        }

        int dur = pdc.getOrDefault(Keys.DURATION, PersistentDataType.INTEGER, 0);
        if (dur >= MAX_DURATION_SECONDS) {
            invShowMax(holder, FlagGuiHolder.TIME_SLOT, "时间");
        }

        String typesStr = pdc.get(Keys.EFFECT_TYPES, PersistentDataType.STRING);
        if (typesStr != null && !typesStr.isEmpty()) {
            String[] keys = typesStr.split(",");
            PotionEffectType firstType = Registry.EFFECT.get(
                    NamespacedKey.fromString(keys[0]));
            if (firstType != null) {
                int rng = pdc.getOrDefault(Keys.RANGE, PersistentDataType.INTEGER, 8);
                int bn = pdc.getOrDefault(Keys.LEVEL_BONUS, PersistentDataType.INTEGER, 0);
                int baseAmp = getFirstBaseAmp(pdc, 0);
                int finalLvl = Math.min(baseAmp + bn + 1, 10);
                holder.getInventory().setItem(FlagGuiHolder.POTION_SLOT,
                        createLockedPotion(firstType, keys.length, dur, rng, finalLvl));
            }
        }
    }

    private void spawnAmbientParticles(Block block) {
        Location base = block.getLocation().add(0.5, 1.2, 0.5);
        World world = base.getWorld();
        if (world == null) return;

        for (int i = 0; i < 2; i++) {
            double ox = (Math.random() - 0.5) * 1.2;
            double oy = Math.random() * 0.8;
            double oz = (Math.random() - 0.5) * 1.2;
            world.spawnParticle(Particle.DUST,
                    base.getX() + ox, base.getY() + oy, base.getZ() + oz,
                    1, 0, 0, 0, 0, AMBIENT_RED_DUST);
        }
    }

    private ItemStack createLockedPotion(PotionEffectType type, int effectCount,
                                         int duration, int range, int level) {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();

        Component name = Component.translatable(type.translationKey())
                .decoration(TextDecoration.ITALIC, false)
                .color(NamedTextColor.YELLOW);
        if (effectCount > 1) {
            name = name.append(Component.text(" 等 " + effectCount + " 种")
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.GRAY));
        }
        meta.displayName(name);

        meta.lore(List.of(
                Component.text("持续：").color(NamedTextColor.YELLOW)
                        .append(Component.text(formatDuration(duration))
                                .color(NamedTextColor.RED))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("范围 ").color(NamedTextColor.YELLOW)
                        .append(Component.text(String.valueOf(range))
                                .color(NamedTextColor.RED))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("等级 ").color(NamedTextColor.YELLOW)
                        .append(Component.text(toRoman(level))
                                .color(NamedTextColor.RED))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("已激活，无法取出").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.getPersistentDataContainer().set(
                Keys.LOCKED_POTION, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void invShowMax(FlagGuiHolder holder, int slot, String name) {
        ItemStack maxPane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = maxPane.getItemMeta();
        meta.displayName(MiniMessage.miniMessage()
                .deserialize("<!italic><red><bold>Max"));
        meta.lore(List.of(MiniMessage.miniMessage()
                .deserialize("<!italic><gray>已经达到" + name + "最大上限")));
        maxPane.setItemMeta(meta);
        holder.getInventory().setItem(slot, maxPane);
    }

    public void updateRangeSlot(FlagGuiHolder holder) {
        if (holder.getRangeUpgrades() >= 8) {
            invShowMax(holder, FlagGuiHolder.RANGE_SLOT, "范围");
        }
    }

    public void updateLevelSlot(FlagGuiHolder holder) {
        if (holder.getLevelUpgrades() >= 9) {
            invShowMax(holder, FlagGuiHolder.LEVEL_SLOT, "等级");
        }
    }

    public void updateTimeSlot(FlagGuiHolder holder) {
        if (!(holder.getFlagBlock().getState() instanceof Banner banner)) return;
        int dur = banner.getPersistentDataContainer()
                .getOrDefault(Keys.DURATION, PersistentDataType.INTEGER, 0);
        if (dur >= MAX_DURATION_SECONDS) {
            invShowMax(holder, FlagGuiHolder.TIME_SLOT, "时间");
        }
    }

    public boolean activate(FlagGuiHolder holder, Player player) {
        if (!holder.isActivated()) return doFirstActivation(holder, player);
        return doUpgrade(holder, player);
    }

    private boolean doFirstActivation(FlagGuiHolder holder, Player player) {
        Inventory inv = holder.getInventory();
        ItemStack potionItem = inv.getItem(FlagGuiHolder.POTION_SLOT);
        if (potionItem == null || potionItem.getType() != Material.POTION) return false;
        if (ItemManager.isLockedPotion(potionItem)) return false;

        PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
        List<PotionEffect> effects = new ArrayList<>();
        if (potionMeta.getBasePotionType() != null) {
            effects.addAll(potionMeta.getBasePotionType().getPotionEffects());
        }
        effects.addAll(potionMeta.getCustomEffects());
        if (effects.isEmpty()) return false;

        ItemStack timeItem = inv.getItem(FlagGuiHolder.TIME_SLOT);
        int durationSeconds = getDurationFromMaterial(
                timeItem != null ? timeItem.getType() : Material.AIR);
        if (durationSeconds <= 0) return false;
        durationSeconds = Math.min(durationSeconds, MAX_DURATION_SECONDS);

        int rangeUp = holder.getRangeUpgrades();
        int levelUp = holder.getLevelUpgrades();

        ItemStack rangeCheck = inv.getItem(FlagGuiHolder.RANGE_SLOT);
        ItemStack levelCheck = inv.getItem(FlagGuiHolder.LEVEL_SLOT);
        if (rangeCheck != null && rangeCheck.getType() == Material.NETHER_STAR
                && rangeUp < 8) {
            rangeUp++;
        }
        if (levelCheck != null && levelCheck.getType() == Material.WITHER_SKELETON_SKULL
                && levelUp < 9) {
            levelUp++;
        }

        StringBuilder typesB = new StringBuilder();
        StringBuilder ampsB = new StringBuilder();
        for (int i = 0; i < effects.size(); i++) {
            PotionEffect e = effects.get(i);
            if (i > 0) { typesB.append(","); ampsB.append(","); }
            typesB.append(e.getType().getKey().toString());
            ampsB.append(Math.min(e.getAmplifier(), 9));
        }

        Block block = holder.getFlagBlock();
        Banner banner = (Banner) block.getState();
        PersistentDataContainer pdc = banner.getPersistentDataContainer();
        pdc.set(Keys.IS_NATIONAL_FLAG, PersistentDataType.BYTE, (byte) 1);
        pdc.set(Keys.EFFECT_TYPES, PersistentDataType.STRING, typesB.toString());
        pdc.set(Keys.EFFECT_AMPS, PersistentDataType.STRING, ampsB.toString());
        pdc.set(Keys.DURATION, PersistentDataType.INTEGER, durationSeconds);
        pdc.set(Keys.RANGE, PersistentDataType.INTEGER, 8 + rangeUp * 8);
        pdc.set(Keys.LEVEL_BONUS, PersistentDataType.INTEGER, levelUp);
        pdc.set(Keys.ACTIVATED, PersistentDataType.BYTE, (byte) 1);
        pdc.set(lastActivateKey, PersistentDataType.LONG, System.currentTimeMillis());
        banner.update();

        inv.setItem(FlagGuiHolder.POTION_SLOT, null);
        inv.setItem(FlagGuiHolder.TIME_SLOT, null);
        consumeOrReturn(inv, FlagGuiHolder.RANGE_SLOT, Material.NETHER_STAR, player);
        consumeOrReturn(inv, FlagGuiHolder.LEVEL_SLOT, Material.WITHER_SKELETON_SKULL, player);

        holder.setActivated(true);
        holder.setConfirmed(true);

        scheduleActivation(block, player, false, null, 0, 8 + rangeUp * 8);
        return true;
    }

    private boolean doUpgrade(FlagGuiHolder holder, Player player) {
        Inventory inv = holder.getInventory();
        Block block = holder.getFlagBlock();
        Banner banner = (Banner) block.getState();
        PersistentDataContainer pdc = banner.getPersistentDataContainer();

        ItemStack timeItem = inv.getItem(FlagGuiHolder.TIME_SLOT);
        ItemStack rangeItem = inv.getItem(FlagGuiHolder.RANGE_SLOT);
        ItemStack levelItem = inv.getItem(FlagGuiHolder.LEVEL_SLOT);

        int oldDuration = pdc.getOrDefault(Keys.DURATION, PersistentDataType.INTEGER, 0);

        boolean hasTime = timeItem != null
                && getDurationFromMaterial(timeItem.getType()) > 0
                && oldDuration < MAX_DURATION_SECONDS;
        boolean hasRange = rangeItem != null && rangeItem.getType() == Material.NETHER_STAR
                && holder.getRangeUpgrades() < 8;
        boolean hasLevel = levelItem != null && levelItem.getType() == Material.WITHER_SKELETON_SKULL
                && holder.getLevelUpgrades() < 9;

        if (!hasTime && !hasRange && !hasLevel) {
            consumeOrReturn(inv, FlagGuiHolder.TIME_SLOT, Material.AIR, player);
            consumeOrReturn(inv, FlagGuiHolder.RANGE_SLOT, Material.AIR, player);
            consumeOrReturn(inv, FlagGuiHolder.LEVEL_SLOT, Material.AIR, player);
            return false;
        }

        int oldRange = pdc.getOrDefault(Keys.RANGE, PersistentDataType.INTEGER, 8);
        int newRange = oldRange;
        int oldBonus = pdc.getOrDefault(Keys.LEVEL_BONUS, PersistentDataType.INTEGER, 0);
        int baseAmp = getFirstBaseAmp(pdc, 0);

        List<String> upgradeMsgs = new ArrayList<>();

        if (hasTime) {
            int addSec = getDurationFromMaterial(timeItem.getType());
            int newDur = Math.min(oldDuration + addSec, MAX_DURATION_SECONDS);
            pdc.set(Keys.DURATION, PersistentDataType.INTEGER, newDur);
            upgradeMsgs.add("<yellow>持续 <red>" + formatDuration(oldDuration)
                    + "</red> ⇨ <red>" + formatDuration(newDur) + "</red>");
            inv.setItem(FlagGuiHolder.TIME_SLOT, null);
            if (newDur >= MAX_DURATION_SECONDS) {
                invShowMax(holder, FlagGuiHolder.TIME_SLOT, "时间");
            }
        } else {
            consumeOrReturn(inv, FlagGuiHolder.TIME_SLOT, Material.AIR, player);
        }

        if (hasRange) {
            newRange = Math.min(oldRange + 8, 8 + 8 * 8);
            pdc.set(Keys.RANGE, PersistentDataType.INTEGER, newRange);
            upgradeMsgs.add("<yellow>范围 <red>" + oldRange
                    + "</red> ⇨ <red>" + newRange + "</red>");
            inv.setItem(FlagGuiHolder.RANGE_SLOT, null);
            holder.setRangeUpgrades(holder.getRangeUpgrades() + 1);
        } else {
            consumeOrReturn(inv, FlagGuiHolder.RANGE_SLOT, Material.AIR, player);
        }

        if (hasLevel) {
            int newBonus = Math.min(oldBonus + 1, 9);
            pdc.set(Keys.LEVEL_BONUS, PersistentDataType.INTEGER, newBonus);
            int oldLvl = Math.min(baseAmp + oldBonus + 1, 10);
            int newLvl = Math.min(baseAmp + newBonus + 1, 10);
            upgradeMsgs.add("<yellow>等级 <red>" + toRoman(oldLvl)
                    + "</red> ⇨ <red>" + toRoman(newLvl) + "</red>");
            inv.setItem(FlagGuiHolder.LEVEL_SLOT, null);
            holder.setLevelUpgrades(holder.getLevelUpgrades() + 1);
        } else {
            consumeOrReturn(inv, FlagGuiHolder.LEVEL_SLOT, Material.AIR, player);
        }

        banner.update();
        holder.setConfirmed(true);

        scheduleActivation(block, player, true, upgradeMsgs, oldRange, newRange);
        return true;
    }

    private void consumeOrReturn(Inventory inv, int slot, Material expected, Player player) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        inv.setItem(slot, null);

        if (item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        if (item.getType() == Material.RED_STAINED_GLASS_PANE) return;

        if (expected != Material.AIR && item.getType() == expected) return;

        giveItem(player, item);
    }

    private void scheduleActivation(Block block, Player player, boolean isUpgrade,
                                    List<String> upgradeMsgs, int oldRange, int newRange) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startEffectTask(block);
            playActivationBurst(block);

            Location soundLoc = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().playSound(soundLoc,
                    Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 1.0f);

            if (player.isOnline()) {
                player.sendActionBar(MiniMessage.miniMessage()
                        .deserialize("<green><bold>✔ 效果已应用"));
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                if (isUpgrade && upgradeMsgs != null && !upgradeMsgs.isEmpty()) {
                    String joined = String.join(" <dark_gray>|</dark_gray> ", upgradeMsgs);
                    player.sendActionBar(MiniMessage.miniMessage().deserialize(joined));
                } else {
                    Banner b = (Banner) block.getState();
                    PersistentDataContainer pdc = b.getPersistentDataContainer();
                    int dur = pdc.getOrDefault(Keys.DURATION, PersistentDataType.INTEGER, 0);
                    int range = pdc.getOrDefault(Keys.RANGE, PersistentDataType.INTEGER, 8);
                    int bonus = pdc.getOrDefault(Keys.LEVEL_BONUS, PersistentDataType.INTEGER, 0);
                    int baseAmp = getFirstBaseAmp(pdc, 0);
                    int lvl = Math.min(baseAmp + bonus + 1, 10);
                    player.sendActionBar(MiniMessage.miniMessage().deserialize(
                            "<yellow>持续 <red>" + formatDuration(dur)
                                    + "</red> <dark_gray>|</dark_gray> 范围 <red>" + range
                                    + "</red> <dark_gray>|</dark_gray> 等级 <red>"
                                    + toRoman(lvl) + "</red>"));
                }

                if (newRange > oldRange) {
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                            showRangeExpansion(block, oldRange, newRange), 20L);
                }
            }, 60L);
        }, 30L);
    }

    private int getFirstBaseAmp(PersistentDataContainer pdc, int currentBonus) {
        String amps = pdc.get(Keys.EFFECT_AMPS, PersistentDataType.STRING);
        if (amps == null || amps.isEmpty()) return 0;
        try {
            return Integer.parseInt(amps.split(",")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void startEffectTask(Block block) {
        Location blockLoc = block.getLocation().clone();
        cancelTask(blockLoc);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!(block.getState() instanceof Banner banner)) {
                    cancelTask(blockLoc); return;
                }
                PersistentDataContainer pdc = banner.getPersistentDataContainer();
                if (!pdc.has(Keys.ACTIVATED, PersistentDataType.BYTE)) {
                    cancelTask(blockLoc); return;
                }

                int remaining = pdc.getOrDefault(Keys.DURATION,
                        PersistentDataType.INTEGER, 0);
                if (remaining <= 0) {
                    pdc.remove(Keys.ACTIVATED);
                    pdc.remove(Keys.DURATION);
                    banner.update();
                    cancelTask(blockLoc);

                    for (Player p : block.getWorld().getPlayers()) {
                        if (p.getOpenInventory().getTopInventory().getHolder()
                                instanceof FlagGuiHolder gh
                                && gh.getFlagBlock().equals(block)) {
                            gh.setActivated(false);
                            gh.getInventory().setItem(FlagGuiHolder.POTION_SLOT, null);
                            gh.getInventory().setItem(FlagGuiHolder.TIME_SLOT, null);
                        }
                    }
                    return;
                }
                pdc.set(Keys.DURATION, PersistentDataType.INTEGER, remaining - 1);
                banner.update();

                String typesStr = pdc.get(Keys.EFFECT_TYPES, PersistentDataType.STRING);
                String ampsStr = pdc.get(Keys.EFFECT_AMPS, PersistentDataType.STRING);
                if (typesStr == null || ampsStr == null) return;

                String[] types = typesStr.split(",");
                String[] amps = ampsStr.split(",");
                if (types.length != amps.length) return;

                int bonus = pdc.getOrDefault(Keys.LEVEL_BONUS, PersistentDataType.INTEGER, 0);
                int range = pdc.getOrDefault(Keys.RANGE, PersistentDataType.INTEGER, 8);
                Location center = block.getLocation().add(0.5, 0.5, 0.5);
                double rangeSq = (double) range * range;

                for (Player p : block.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(center) > rangeSq) continue;
                    for (int i = 0; i < types.length; i++) {
                        PotionEffectType type = Registry.EFFECT.get(
                                NamespacedKey.fromString(types[i]));
                        if (type == null) continue;
                        int baseAmp;
                        try { baseAmp = Integer.parseInt(amps[i]); }
                        catch (NumberFormatException e) { continue; }
                        int amp = Math.min(baseAmp + bonus, 9);
                        p.addPotionEffect(new PotionEffect(
                                type, 40, amp, true, true, true));
                    }
                }

                for (Player p : block.getWorld().getPlayers()) {
                    if (p.getOpenInventory().getTopInventory().getHolder()
                            instanceof FlagGuiHolder gh
                            && gh.getFlagBlock().equals(block)) {
                        PotionEffectType firstType = Registry.EFFECT.get(
                                NamespacedKey.fromString(types[0]));
                        if (firstType == null) continue;
                        int dur = pdc.getOrDefault(Keys.DURATION,
                                PersistentDataType.INTEGER, 0);
                        int finalLvl = Math.min(
                                Integer.parseInt(amps[0]) + bonus + 1, 10);
                        gh.getInventory().setItem(FlagGuiHolder.POTION_SLOT,
                                createLockedPotion(firstType, types.length,
                                        dur, range, finalLvl));

                        if (dur >= MAX_DURATION_SECONDS) {
                            invShowMax(gh, FlagGuiHolder.TIME_SLOT, "时间");
                        }
                    }
                }

                spawnAmbientParticles(block);

            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeTasks.put(blockLoc, task);
    }

    public void cancelTask(Location loc) {
        BukkitTask t = activeTasks.remove(loc);
        if (t != null) t.cancel();
    }

    public void stopEffect(Block block) {
        cancelTask(block.getLocation());
        if (block.getState() instanceof Banner banner) {
            PersistentDataContainer pdc = banner.getPersistentDataContainer();
            pdc.remove(Keys.ACTIVATED);
            pdc.remove(Keys.DURATION);
            banner.update();
        }
    }

    public void shutdown() {
        for (BukkitTask t : activeTasks.values()) t.cancel();
        activeTasks.clear();
    }

    public void playActivationBurst(Block block) {
        Location center = block.getLocation().add(0.5, 1.0, 0.5);
        World world = center.getWorld();
        if (world == null) return;

        new BukkitRunnable() {
            int tick = 0;
            final int expandTicks = 15;

            @Override
            public void run() {
                if (tick >= expandTicks) {
                    spawnBurst(world, center, 3.0);
                    cancel();
                    return;
                }
                double progress = tick / (double) expandTicks;
                double radius = 0.3 + progress * 2.2;
                spawnColorSphere(world, center, radius);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnColorSphere(World world, Location center, double radius) {
        int points = Math.max(20, (int) (radius * 40));
        for (int i = 0; i < points; i++) {
            double phi = Math.acos(1 - 2.0 * (i + 0.5) / points);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);

            world.spawnParticle(Particle.DUST,
                    center.getX() + x, center.getY() + y, center.getZ() + z,
                    1, 0, 0, 0, 0,
                    new Particle.DustOptions(pickColor(), 1.2f),
                    true);
        }
    }

    private void spawnBurst(World world, Location center, double radius) {
        int points = 120;
        for (int i = 0; i < points; i++) {
            double phi = Math.acos(1 - 2.0 * (i + 0.5) / points);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.cos(phi);
            double z = Math.sin(phi) * Math.sin(theta);

            world.spawnParticle(Particle.DUST,
                    center.getX() + x * 0.5,
                    center.getY() + y * 0.5,
                    center.getZ() + z * 0.5,
                    1,
                    x * 0.3, y * 0.3, z * 0.3,
                    0.15,
                    new Particle.DustOptions(pickColor(), 1.5f),
                    true);
        }

        world.spawnParticle(Particle.END_ROD,
                center.getX(), center.getY(), center.getZ(),
                30, 1.5, 1.5, 1.5, 0.2, null, true);
    }

    private Color pickColor() {
        double r = Math.random();
        if (r < 0.34) return Color.RED;
        if (r < 0.67) return Color.ORANGE;
        return Color.YELLOW;
    }

    public void showRangeExpansion(Block block, int oldRange, int newRange) {
        if (newRange <= oldRange) return;
        Location center = block.getLocation().add(0.5, 0.15, 0.5);
        World world = center.getWorld();
        if (world == null) return;

        new BukkitRunnable() {
            double radius = Math.max(1.5, oldRange);
            @Override
            public void run() {
                if (radius > newRange) {
                    for (int k = 0; k < 6; k++) {
                        final int delay = k;
                        Bukkit.getScheduler().runTaskLater(plugin,
                                () -> spawnRingFlash(world, center, newRange), delay);
                    }
                    cancel();
                    return;
                }
                spawnRing(world, center, radius);
                radius += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void spawnRing(World world, Location center, double radius) {
        int points = (int) Math.max(64, radius * 14);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            world.spawnParticle(Particle.END_ROD,
                    x, center.getY() + 0.15, z,
                    1, 0, 0, 0, 0, null, true);

            world.spawnParticle(Particle.DUST,
                    x, center.getY(), z,
                    1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.RED, 1.6f),
                    true);
        }
    }

    private void spawnRingFlash(World world, Location center, double radius) {
        int points = (int) Math.max(96, radius * 18);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY() + 0.2;

            world.spawnParticle(Particle.FLAME,
                    x, y, z, 1, 0, 0.05, 0, 0.01, null, true);
        }
    }

    public int getDurationFromMaterial(Material material) {
        return switch (material) {
            case IRON_INGOT -> 2 * 60;
            case GOLD_INGOT -> 5 * 60;
            case DIAMOND -> 10 * 60;
            case EMERALD -> 13 * 60;
            case NETHERITE_INGOT -> 20 * 60;
            default -> 0;
        };
    }

    private String formatDuration(int seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) {
            int m = seconds / 60;
            int s = seconds % 60;
            return s > 0 ? m + "m" + s + "s" : m + "m";
        }
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        return m > 0 ? h + "h" + m + "m" : h + "h";
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    public void giveItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    public void returnAllItems(Player player, Inventory inv) {
        int[] slots = {
                FlagGuiHolder.POTION_SLOT, FlagGuiHolder.TIME_SLOT,
                FlagGuiHolder.RANGE_SLOT, FlagGuiHolder.LEVEL_SLOT
        };
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            if (ItemManager.isLockedPotion(item)) continue;
            if (item.getType() == Material.BLACK_STAINED_GLASS_PANE) continue;
            if (item.getType() == Material.RED_STAINED_GLASS_PANE) continue;
            giveItem(player, item);
            inv.setItem(slot, null);
        }
    }
}