package com.xiaomian124.nationalday.manager;

import com.xiaomian124.nationalday.NationalDay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalDateTime;

public class NationalDayManager {

    private final NationalDay plugin;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean initialized = false;
    private boolean currentlyActive = false;
    private BukkitTask checkTask;

    public NationalDayManager(NationalDay plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        String period = plugin.getConfig().getString(
                "national-day-period", "26.10.1.00:00-26.10.7.23:59"
        );

        if (period == null || period.isBlank()) {
            plugin.getLogger().warning("config.yml 中未设置 national-day-period！");
            initialized = false;
            return;
        }

        try {
            String[] parts = period.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("时间段必须包含一个 '-' 分隔符");
            }
            startTime = parseDateTime(parts[0]);
            endTime = parseDateTime(parts[1]);

            if (endTime.isBefore(startTime)) {
                throw new IllegalArgumentException("结束时间不能早于开始时间");
            }

            initialized = true;
            plugin.getLogger().info("国庆节时间段已加载：" + startTime + " 到 " + endTime);
        } catch (Exception e) {
            plugin.getLogger().warning("解析国庆节时间段失败：" + e.getMessage());
            initialized = false;
        }
    }

    public void reload() {
        plugin.reloadConfig();
        loadConfig();

        if (!initialized) {
            plugin.getLogger().warning("重载后国庆节时间段无效，请检查 config.yml！");
            return;
        }

        tick();
    }

    private LocalDateTime parseDateTime(String input) {
        input = input.trim();
        String[] parts = input.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("日期格式错误：" + input + "（应为 yy.M.d.HH:mm）");
        }

        int year = 2000 + Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);

        String[] timeParts = parts[3].split(":");
        if (timeParts.length != 2) {
            throw new IllegalArgumentException("时间格式错误：" + parts[3]);
        }
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        return LocalDateTime.of(year, month, day, hour, minute);
    }

    public void start() {
        if (!initialized) {
            plugin.getLogger().warning("国庆节管理器未初始化，将跳过启动。");
            return;
        }

        currentlyActive = isInPeriod(LocalDateTime.now());

        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        long secondsUntilStart = Duration.between(now, startTime).getSeconds();
        long secondsUntilEnd = Duration.between(now, endTime).getSeconds();

        boolean shouldBeActive = isInPeriod(now);

        if (secondsUntilStart > 0 && secondsUntilStart <= 10) {
            sendActionBar("<yellow>国庆节还有 <red>" + secondsUntilStart + "</red> 秒开始");
            playCountdownSound();
        }
        else if (secondsUntilEnd > 0 && secondsUntilEnd <= 10) {
            sendActionBar("<yellow>国庆节还有 <red>" + secondsUntilEnd + "</red> 秒结束");
            playCountdownSound();
        }

        if (shouldBeActive && !currentlyActive) {
            onPeriodStart();
        } else if (!shouldBeActive && currentlyActive) {
            onPeriodEnd();
        }

        currentlyActive = shouldBeActive;
    }

    private void playCountdownSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
        }
    }

    private void onPeriodStart() {
        Component message = MiniMessage.miniMessage().deserialize(
                "<yellow>国庆节开始了，现在你可以击败监守者获得国庆旗帜！"
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            player.playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        plugin.getLogger().info("国庆节已开始，监守者掉旗帜功能已开启");
    }

    private void onPeriodEnd() {
        Component message = MiniMessage.miniMessage().deserialize(
                "<yellow>国庆节结束了，假期也结束了，期待下一次国庆节！"
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            player.playSound(player.getLocation(),
                    Sound.ENTITY_WOLF_WHINE, 1.0f, 1.0f);
        }
        plugin.getLogger().info("国庆节已结束，监守者掉旗帜功能已关闭");
    }

    private void sendActionBar(String miniMessageString) {
        Component component = MiniMessage.miniMessage().deserialize(miniMessageString);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(component);
        }
    }

    public boolean isCurrentlyActive() {
        if (!initialized) return false;
        return currentlyActive;
    }

    private boolean isInPeriod(LocalDateTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }
}