package com.xiaomian124.nationalday.listener;

import com.xiaomian124.nationalday.NationalDay;
import com.xiaomian124.nationalday.util.Keys;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

public class StartupListener implements Listener {

    private final NationalDay plugin;

    public StartupListener(NationalDay plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        scanChunk(event.getChunk());
    }

    public void scanAllLoadedChunks() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scanChunk(chunk);
            }
        }
    }

    private void scanChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Banner banner)) continue;
            if (!banner.getPersistentDataContainer()
                    .has(Keys.ACTIVATED, PersistentDataType.BYTE)) continue;

            plugin.getFlagManager().startEffectTask(banner.getBlock());
        }
    }
}