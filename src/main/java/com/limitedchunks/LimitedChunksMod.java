package com.limitedchunks;

import com.limitedchunks.config.Configuration;
import com.limitedchunks.event.EventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LimitedChunksMod implements ModInitializer
{
    public static Configuration config;

    public static final String MODID  = "limitedchunks";
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize()
    {
        config = new Configuration();
        config.load();
        LOGGER.info(MODID + " mod initialized");
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        ServerTickEvents.END_WORLD_TICK.register(EventHandler::onWorldTick);
        ServerChunkEvents.CHUNK_LOAD.register(EventHandler::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(EventHandler::onChunkUnLoad);
        ServerLifecycleEvents.SERVER_STARTING.register(EventHandler::onServerAboutToStart);
    }
}
