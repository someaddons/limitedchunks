package com.limitedchunks.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Collections;
import java.util.List;

public class CommonConfiguration
{
    public final ForgeConfigSpec.IntValue                            chunkunloadnoplayer;
    public final ForgeConfigSpec.BooleanValue                        debugLog;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> excludedtickets;

    protected CommonConfiguration(final ForgeConfigSpec.Builder builder)
    {
        builder.push("Chunk settings");

        builder.comment("How long a player can keep chunkloaded chunks active after logging out. Default: 10 min");
        chunkunloadnoplayer = builder.defineInRange("chunkunloadnoplayer", 10, 0, 2000);

        builder.comment("Print log messages for which chunk tickets are unloaded where. Default: false");
        debugLog = builder.define("debugmessage", false);

        builder.comment("List of excluded ticket/chunkload types, these are mod-specific. : e.g. format :  [\"mekanism\", \"player\"]");
        excludedtickets = builder.defineList("excludedtickets", Collections.emptyList(), e -> e instanceof String);

        // Escapes the current category level
        builder.pop();
    }
}
