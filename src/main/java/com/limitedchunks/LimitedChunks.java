package com.limitedchunks;

import com.limitedchunks.config.Configuration;
import com.limitedchunks.event.EventHandler;
import com.limitedchunks.event.ModEventHandler;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Constants.MOD_ID)
public class LimitedChunks
{
    public static final Logger LOGGER = LogManager.getLogger();

    /**
     * The config instance.
     */
    private static Configuration config;

    public LimitedChunks()
    {
        config = new Configuration();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventHandler.class);
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ModEventHandler.class);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    public static Configuration getConfig()
    {
        return config;
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("Limited chunkloading initialized");
    }
}
