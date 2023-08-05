package com.limitedchunks.config;

import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.limitedchunks.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class CommonConfiguration implements ICommonConfig
{
    public int          chunkunloadnoplayer = 10;
    public boolean      debugLog            = false;
    public List<String> excludedtickets     = new ArrayList<>();

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry = new JsonObject();
        entry.addProperty("desc:", "How long a player can keep chunkloaded chunks active after logging out."
                                     + " default:10, min 0, max 2000");
        entry.addProperty("chunkunloadnoplayer", chunkunloadnoplayer);
        root.add("chunkunloadnoplayer", entry);

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:", "Print log messages for which chunk tickets are unloaded where. Default: false");
        entry2.addProperty("debugLog", debugLog);
        root.add("debugLog", entry2);

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "List of excluded ticket/chunkload types, these are mod-specific. : e.g. format :  [\"mekanism\", \"player\"]");
        final JsonArray list3 = new JsonArray();
        for (final String name : excludedtickets)
        {
            list3.add(name);
        }
        entry3.add("excludedtickets", list3);
        root.add("excludedtickets", entry3);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        chunkunloadnoplayer = data.get("chunkunloadnoplayer").getAsJsonObject().get("chunkunloadnoplayer").getAsInt();
        debugLog = data.get("debugLog").getAsJsonObject().get("debugLog").getAsBoolean();
        excludedtickets = new ArrayList<>();
        for (final JsonElement element : data.get("excludedtickets").getAsJsonObject().get("excludedtickets").getAsJsonArray())
        {
            excludedtickets.add(element.getAsString());
        }

        EventHandler.initDefaultExcludes();
        EventHandler.excludedTickets.addAll(excludedtickets);
    }
}
