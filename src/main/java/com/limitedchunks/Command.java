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
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;

public class Command
{
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(Constants.MOD_ID)
          .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
          .then(Commands.literal("listActiveTickets")
            .then(Commands.argument("position", BlockPosArgument.blockPos())
              .executes(context ->
              {
                  final BlockPos pos = BlockPosArgument.getBlockPos(context, "position");
                  final ChunkPos chunkPos = new ChunkPos(pos);
                  final ServerLevel level = context.getSource().getLevel();

                  level.getChunkSource().distanceManager.runAllUpdates(level.getChunkSource().chunkMap);
                  context.getSource().sendSystemMessage(Component.literal("Chunk tickets at: " + pos.toShortString()));

                  for (final Ticket<?> ticket : level.getChunkSource().distanceManager.tickets.get(chunkPos.toLong()))
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

                for (Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> entry : level.getChunkSource().distanceManager.tickets.long2ObjectEntrySet())
                {
                    boolean hasPLayer = false;
                    for (final Ticket<?> ticket : entry.getValue())
                    {
                        if (ticket != null && ticket.getType() == TicketType.PLAYER)
                        {
                            hasPLayer = true;
                            break;
                        }
                    }

                    if (!hasPLayer)
                    {
                        context.getSource()
                          .sendSystemMessage(Component.literal("Chunk tickets at:" + new ChunkPos(entry.getLongKey()).getWorldPosition()));
                        count++;
                        for (final Ticket<?> ticket : entry.getValue())
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
