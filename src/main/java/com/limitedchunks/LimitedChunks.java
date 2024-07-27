package com.limitedchunks;

import com.cupboard.config.CupboardConfig;
import com.limitedchunks.config.CommonConfiguration;
import com.limitedchunks.event.EventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
    public static CupboardConfig<CommonConfiguration> config = new CupboardConfig<>(Constants.MOD_ID, new CommonConfiguration());

    public LimitedChunks(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(EventHandler.class);
        NeoForge.EVENT_BUS.addListener(this::commandRegister);
    }

    @SubscribeEvent
    public void commandRegister(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(new Command().build());
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("Limited chunkloading initialized");
    }
}
