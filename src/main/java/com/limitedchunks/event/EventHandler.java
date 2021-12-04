package com.limitedchunks.event;

import com.limitedchunks.LimitedChunks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * Handler to catch server tick events
 */
public class EventHandler
{
    /**
     * Contains the positions that are/will be loaded by a player
     */
    static Map<ResourceKey<Level>, Long2ObjectOpenHashMap<UUID>> posToPlayerID = new HashMap<>();
    static Map<ResourceKey<Level>, HashMap<UUID, LongSet>>       playerIDToPos = new HashMap<>();
    /**
     * Queue to recheck
     */
    static Map<ResourceKey<Level>, Queue<ChunkPosAndTime>>       unloadQue     = new HashMap<>();

    public static Set<String> excludedTickets = new HashSet<>();

    private static Long2ObjectOpenHashMap<UUID> getPosToPlayerIDMap(final ResourceKey<Level> worldID)
    {
        return posToPlayerID.computeIfAbsent(worldID, k -> new Long2ObjectOpenHashMap());
    }

    private static HashMap<UUID, LongSet> getPlayerIDToPosMap(final ResourceKey<Level> worldID)
    {
        return playerIDToPos.computeIfAbsent(worldID, k -> new HashMap<>());
    }

    @SubscribeEvent
    public static void onWorldTick(final TickEvent.WorldTickEvent event)
    {
        if (event.world.isClientSide() || event.phase == TickEvent.Phase.START)
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.world;

        Queue<ChunkPosAndTime> queue = unloadQue.get(world.dimension());
        if (queue == null || queue.isEmpty())
        {
            return;
        }


        final ChunkPosAndTime current = queue.peek();
        if (current != null && current.time < world.getServer().getNextTickTime())
        {
            queue.poll();
            checkLoadedAndClear(current.pos, world);
        }
    }

    /**
     * Checks if a player is keeping the chunk active, unloads otherwise
     *
     * @param pos
     * @param world
     */
    private static void checkLoadedAndClear(final long pos, final ServerLevel world)
    {
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = getPosToPlayerIDMap(world.dimension());
        if (!(worldPositionsLoaded.containsKey(pos)))
        {
            return;
        }

        final UUID ownerUUID = worldPositionsLoaded.get(pos);
        if (ownerUUID != null)
        {
            final Player player = world.getServer().getPlayerList().getPlayer(ownerUUID);
            if (player != null)
            {
                return;
            }
        }

        final SortedArraySet<Ticket<?>> ticketsE = world.getChunkSource().distanceManager.tickets.get(pos);
        if (ticketsE == null)
        {
            return;
        }

        final List<Ticket<?>> ticketsToRemove = new ArrayList<>();
        for (final Ticket<?> ticket : ticketsE)
        {
            if (ticket == null)
            {
                continue;
            }

            if (!excludedTickets.contains(ticket.getType().toString()))
            {
                if (LimitedChunks.getConfig().getCommonConfig().debugLog.get())
                {
                    LimitedChunks.LOGGER.info("Unloading ticket:" + ticket.getType().toString() + " at chunkpos:" + new ChunkPos(pos));
                }
                ticketsToRemove.add(ticket);
            }
            else if (ticket.getType() == TicketType.PLAYER)
            {
                final ChunkPos chunkPos = new ChunkPos(pos);
                final Player closeset = world.getNearestPlayer(chunkPos.x << 4, 0, chunkPos.z << 4, -1, null);
                if (closeset != null)
                {
                    worldPositionsLoaded.put(pos, closeset.getUUID());
                    return;
                }
            }
        }

        for (final Ticket<?> ticket : ticketsToRemove)
        {
            world.getChunkSource().distanceManager.removeTicket(pos, ticket);
        }
    }

    public static void initDefaultExcludes()
    {
        excludedTickets = new HashSet<>();
        excludedTickets.add(TicketType.POST_TELEPORT.toString());
        excludedTickets.add(TicketType.PLAYER.toString());
        excludedTickets.add(TicketType.START.toString());
        excludedTickets.add(TicketType.UNKNOWN.toString());
        excludedTickets.add(TicketType.PORTAL.toString());
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event)
    {
        if (event.getWorld().isClientSide())
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.getWorld();

        final Player closeset = world.getNearestPlayer(event.getChunk().getPos().x << 4, 0, event.getChunk().getPos().z << 4, -1, null);
        final long pos = event.getChunk().getPos().toLong();

        if (closeset != null)
        {
            getPlayerIDToPosMap(world.dimension()).computeIfAbsent(closeset.getUUID(), k -> new LongOpenHashSet()).add(pos);
            getPosToPlayerIDMap(world.dimension()).put(pos, closeset.getUUID());
        }
        else
        {
            getPosToPlayerIDMap(world.dimension()).put(pos, null);

            // Que chunk to unload/check on later, no player associated
            final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(world.dimension(), s -> new PriorityQueue<>());
            quedChunks.add(new ChunkPosAndTime(pos, world.getServer().getNextTickTime() + LimitedChunks.getConfig().getCommonConfig().chunkunloadnoplayer.get() * 1000 * 60));
        }
    }

    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event)
    {
        if (event.getWorld().isClientSide())
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.getWorld();
        final long pos = event.getChunk().getPos().toLong();
        final UUID playerID = getPosToPlayerIDMap(world.dimension()).remove(pos);

        if (playerID != null)
        {
            Set<Long> positions = getPlayerIDToPosMap(world.dimension()).get(playerID);
            if (positions != null)
            {
                positions.remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(final PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity().level.isClientSide())
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.getPlayer().level;

        for (final Map.Entry<ResourceKey<Level>, HashMap<UUID, LongSet>> dimEntry : playerIDToPos.entrySet())
        {
            final HashMap<UUID, LongSet> playerToPosMap = dimEntry.getValue();
            if (playerToPosMap == null)
            {
                continue;
            }

            final LongSet chunksFromPlayer = playerToPosMap.remove(event.getPlayer().getUUID());
            if (chunksFromPlayer == null)
            {
                continue;
            }

            final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(dimEntry.getKey(), s -> new PriorityQueue<>());
            for (final long pos : chunksFromPlayer)
            {
                quedChunks.add(new ChunkPosAndTime(pos,
                  world.getServer().getNextTickTime() + LimitedChunks.getConfig().getCommonConfig().chunkunloadnoplayer.get() * 1000 * 60));
            }
        }
    }

    /**
     * Reset all values
     */
    @SubscribeEvent
    public static void onServerAboutToStart(final ServerAboutToStartEvent event)
    {
        posToPlayerID = new HashMap<>();
        playerIDToPos = new HashMap<>();
        unloadQue = new HashMap<>();
    }

    /**
     * Data holder class
     */
    private static class ChunkPosAndTime implements Comparable<ChunkPosAndTime>
    {
        final long pos;
        final long time;

        ChunkPosAndTime(final long pos, final long time)
        {
            this.pos = pos;
            this.time = time;
        }

        @Override
        public int compareTo(final ChunkPosAndTime o)
        {
            return (int) (time - o.time);
        }
    }
}
