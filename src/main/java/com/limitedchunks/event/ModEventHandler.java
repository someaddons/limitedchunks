package com.limitedchunks.event;

import com.limitedchunks.LimitedChunks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ModEventHandler
{
    @SubscribeEvent
    public static void onConfigChanged(ModConfigEvent event)
    {
        EventHandler.initDefaultExcludes();
        EventHandler.excludedTickets.addAll(LimitedChunks.getConfig().getCommonConfig().excludedtickets.get());
    }
}
