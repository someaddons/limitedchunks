package com.limitedchunks.event;

import com.limitedchunks.LimitedChunksMod;
import com.limitedchunks.Utils.IChunkTicketManagerAccessor;
import com.limitedchunks.Utils.ITicketManagerGetter;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

/**
 * Handler to catch server tick events
 */
public class EventHandler
{
    /**
     * Contains the positions that are/will be loaded by a player
     */
    static Map<RegistryKey<World>, Long2ObjectOpenHashMap<UUID>> posToPlayerID = new HashMap<>();
    static Map<RegistryKey<World>, HashMap<UUID, LongSet>>       playerIDToPos = new HashMap<>();
    /**
     * Queue to recheck
     */
    static Map<RegistryKey<World>, Queue<ChunkPosAndTime>>       unloadQue     = new HashMap<>();

    public static Set<String> excludedTickets = new HashSet<>();

    private static Long2ObjectOpenHashMap<UUID> getPosToPlayerIDMap(final RegistryKey<World> worldID)
    {
        return posToPlayerID.computeIfAbsent(worldID, k -> new Long2ObjectOpenHashMap());
    }

    private static HashMap<UUID, LongSet> getPlayerIDToPosMap(final RegistryKey<World> worldID)
    {
        return playerIDToPos.computeIfAbsent(worldID, k -> new HashMap<>());
    }

    public static void onWorldTick(final ServerWorld world)
    {

        Queue<ChunkPosAndTime> queue = unloadQue.get(world.getRegistryKey());
        if (queue == null || queue.isEmpty())
        {
            return;
        }


        final ChunkPosAndTime current = queue.peek();
        if (current != null && current.time < world.getServer().getTimeReference())
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
    private static void checkLoadedAndClear(final long pos, final ServerWorld world)
    {
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = getPosToPlayerIDMap(world.getRegistryKey());
        if (!(worldPositionsLoaded.containsKey(pos)))
        {
            return;
        }

        final UUID ownerUUID = worldPositionsLoaded.get(pos);
        if (ownerUUID != null)
        {
            final PlayerEntity player = world.getServer().getPlayerManager().getPlayer(ownerUUID);
            if (player != null)
            {
                return;
            }
        }

        final SortedArraySet<ChunkTicket<?>> ticketsE =
          ((IChunkTicketManagerAccessor) ((ITicketManagerGetter) world.getChunkManager()).getTicketManager()).getTicketsByPosition().get(pos);
        if (ticketsE == null)
        {
            return;
        }

        final List<ChunkTicket<?>> ticketsToRemove = new ArrayList<>();
        for (final ChunkTicket<?> ticket : ticketsE)
        {
            if (ticket == null)
            {
                continue;
            }

            if (!excludedTickets.contains(ticket.getType().toString()))
            {
                if (LimitedChunksMod.config.getCommonConfig().debugLog)
                {
                    LimitedChunksMod.LOGGER.info("Unloading ticket:" + ticket.getType().toString() + " at chunkpos:" + new ChunkPos(pos));
                }
                ticketsToRemove.add(ticket);
            }
            else if (ticket.getType() == ChunkTicketType.PLAYER)
            {
                final ChunkPos chunkPos = new ChunkPos(pos);
                final PlayerEntity closeset = world.getClosestPlayer(chunkPos.x << 4, 0, chunkPos.z << 4, -1, null);
                if (closeset != null)
                {
                    worldPositionsLoaded.put(pos, closeset.getUuid());
                    return;
                }
            }
        }

        for (final ChunkTicket<?> ticket : ticketsToRemove)
        {
            ((IChunkTicketManagerAccessor) ((ITicketManagerGetter) world.getChunkManager()).getTicketManager()).removeTicketAccessor(pos, ticket);
        }
    }

    public static void initDefaultExcludes()
    {
        excludedTickets = new HashSet<>();
        excludedTickets.add(ChunkTicketType.POST_TELEPORT.toString());
        excludedTickets.add(ChunkTicketType.PLAYER.toString());
        excludedTickets.add(ChunkTicketType.START.toString());
        excludedTickets.add(ChunkTicketType.UNKNOWN.toString());
        excludedTickets.add(ChunkTicketType.PORTAL.toString());
    }

    public static void onChunkLoad(final ServerWorld world, final WorldChunk worldChunk)
    {
        final PlayerEntity closeset = world.getClosestPlayer(worldChunk.getPos().x << 4, 0, worldChunk.getPos().z << 4, -1, null);
        final long pos = worldChunk.getPos().toLong();

        if (closeset != null)
        {
            getPlayerIDToPosMap(world.getRegistryKey()).computeIfAbsent(closeset.getUuid(), k -> new LongOpenHashSet()).add(pos);
            getPosToPlayerIDMap(world.getRegistryKey()).put(pos, closeset.getUuid());
        }
        else
        {
            getPosToPlayerIDMap(world.getRegistryKey()).put(pos, null);

            // Que chunk to unload/check on later, no player associated
            final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(world.getRegistryKey(), s -> new PriorityQueue<>());
            quedChunks.add(new ChunkPosAndTime(pos, world.getServer().getTimeReference() + LimitedChunksMod.config.getCommonConfig().chunkunloadnoplayer * 1000 * 60));
        }
    }

    public static void onChunkUnLoad(final ServerWorld world, final WorldChunk worldChunk)
    {
        final long pos = worldChunk.getPos().toLong();
        final UUID playerID = getPosToPlayerIDMap(world.getRegistryKey()).remove(pos);

        if (playerID != null)
        {
            Set<Long> positions = getPlayerIDToPosMap(world.getRegistryKey()).get(playerID);
            if (positions != null)
            {
                positions.remove(pos);
            }
        }
    }

    public static void onPlayerLeave(final ServerPlayerEntity player)
    {
        final ServerWorld world = (ServerWorld) player.getWorld();

        for (final Map.Entry<RegistryKey<World>, HashMap<UUID, LongSet>> dimEntry : playerIDToPos.entrySet())
        {
            final HashMap<UUID, LongSet> playerToPosMap = dimEntry.getValue();
            if (playerToPosMap == null)
            {
                continue;
            }

            final LongSet chunksFromPlayer = playerToPosMap.remove(player.getUuid());
            if (chunksFromPlayer == null)
            {
                continue;
            }

            final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(dimEntry.getKey(), s -> new PriorityQueue<>());
            for (final long pos : chunksFromPlayer)
            {
                quedChunks.add(new ChunkPosAndTime(pos,
                  world.getServer().getTimeReference() + (long) LimitedChunksMod.config.getCommonConfig().chunkunloadnoplayer * 1000 * 60));
            }
        }
    }

    /**
     * Reset all values
     */
    public static void onServerAboutToStart(final MinecraftServer server)
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
