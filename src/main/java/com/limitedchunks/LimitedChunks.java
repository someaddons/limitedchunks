package com.limitedchunks;

import com.cupboard.config.CupboardConfig;
import com.limitedchunks.config.CommonConfiguration;
import com.limitedchunks.event.EventHandler;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public LimitedChunks()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventHandler.class);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (c, b) -> true));
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("Limited chunkloading initialized");
    }
}
