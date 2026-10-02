package com.xiaomian124.nationalday.command;

import com.xiaomian124.nationalday.NationalDay;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class NationalDayCommand implements CommandExecutor, TabCompleter {

    private final NationalDay plugin;

    public NationalDayCommand(NationalDay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red>用法：/nationalday reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();

            sender.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<green>NationalDay 插件配置文件已重新加载！"));

            boolean active = plugin.getNationalDayManager().isCurrentlyActive();
            String status = active
                    ? "<yellow>状态：<gray>当前正处于国庆节期间"
                    : "<yellow>状态：<gray>当前不在国庆节期间";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(status));
            return true;
        }

        sender.sendMessage(MiniMessage.miniMessage()
                .deserialize("<red>未知子命令，可用：reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("reload".startsWith(prefix)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }
}