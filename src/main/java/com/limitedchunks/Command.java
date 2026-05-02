package com.limitedchunks;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

public class Command
{
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(Constants.MOD_ID)
            .requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
            .then(Commands.literal("listActiveTickets")
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                    .executes(context ->
                    {
                        final BlockPos pos = BlockPosArgument.getBlockPos(context, "position");
                        final ChunkPos chunkPos = ChunkPos.containing(pos);
                        final ServerLevel level = context.getSource().getLevel();

                        level.getChunkSource().distanceManager.runAllUpdates(level.getChunkSource().chunkMap);
                        context.getSource().sendSystemMessage(Component.literal("Chunk tickets at: " + pos.toShortString()));

                        final List<Ticket> tickets = level.getChunkSource().distanceManager.ticketStorage.tickets.get(chunkPos.pack());
                        if (tickets == null || tickets.isEmpty())
                        {
                            context.getSource().sendSystemMessage(Component.literal("None").withStyle(ChatFormatting.YELLOW));
                            return 1;
                        }

                        for (final Ticket ticket : tickets)
                        {
                            if (ticket != null)
                            {
                                context.getSource().sendSystemMessage(Component.literal(ticket.toString()).withStyle(ChatFormatting.YELLOW));
                            }
                        }

                        return 1;
                    })))
            .then(Commands.literal("listnonplayerchunktickets")
                .executes(context ->
                {
                    final ServerLevel level = context.getSource().getLevel();

                    level.getChunkSource().distanceManager.runAllUpdates(level.getChunkSource().chunkMap);
                    context.getSource().sendSystemMessage(Component.literal("Listing all chunks and their tickets, which are not near a player: "));

                    int count = 0;

                    for (Long2ObjectMap.Entry<List<Ticket>> entry : level.getChunkSource().distanceManager.ticketStorage.tickets.long2ObjectEntrySet())
                    {
                        if (entry.getValue() == null)
                        {
                            continue;
                        }

                        boolean hasPLayer = false;
                        for (final Ticket ticket : entry.getValue())
                        {
                            if (ticket != null && ticket.getType() == TicketType.PLAYER_LOADING)
                            {
                                hasPLayer = true;
                                break;
                            }
                        }

                        if (!hasPLayer)
                        {
                            context.getSource()
                                .sendSystemMessage(Component.literal("Chunk tickets at:" + ChunkPos.unpack(entry.getLongKey()).getWorldPosition()));
                            count++;
                            for (final Ticket ticket : entry.getValue())
                            {
                                if (ticket != null)
                                {
                                    context.getSource()
                                        .sendSystemMessage(Component.literal(ticket.toString()).withStyle(ChatFormatting.YELLOW));
                                }
                            }
                        }
                    }

                    context.getSource().sendSystemMessage(Component.literal("Total amount: " + count));

                    return 1;
                }));
    }
}
