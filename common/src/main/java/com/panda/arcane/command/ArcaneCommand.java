package com.panda.arcane.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.panda.arcane.knowledge.Knowledge;
import com.panda.arcane.knowledge.KnowledgeManager;
import com.panda.arcane.logic.Mastery;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Admin and debugging commands: {@code /arcane knowledge learn|forget|list|learnall|reset}. All require game-master permission.
 */
public final class ArcaneCommand {
    private ArcaneCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, net.minecraft.commands.CommandBuildContext context) {
        dispatcher.register(Commands.literal("arcane")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("knowledge")
                        .then(Commands.literal("learn")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("enchantment", ResourceArgument.resource(context, Registries.ENCHANTMENT))
                                                .executes(c -> learn(c, 3))
                                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, Mastery.MAX_TIER))
                                                        .executes(c -> learn(c, IntegerArgumentType.getInteger(c, "tier")))))))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("enchantment", ResourceArgument.resource(context, Registries.ENCHANTMENT))
                                                .executes(ArcaneCommand::forget))))
                        .then(Commands.literal("learnall")
                                .then(Commands.argument("targets", EntityArgument.players()).executes(c -> learnAll(c, 3))
                                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, Mastery.MAX_TIER))
                                                .executes(c -> learnAll(c, IntegerArgumentType.getInteger(c, "tier"))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players()).executes(ArcaneCommand::reset)))
                        .then(Commands.literal("list")
                                .then(Commands.argument("target", EntityArgument.player()).executes(ArcaneCommand::list)))));
    }

    private static int learn(CommandContext<CommandSourceStack> c, int tier) throws CommandSyntaxException {
        Holder.Reference<Enchantment> ench = ResourceArgument.getResource(c, "enchantment", Registries.ENCHANTMENT);
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : players) KnowledgeManager.learn(p, ench.key().identifier(), Mastery.ofTier(tier));
        c.getSource().sendSuccess(() -> Component.literal("Taught " + ench.key().identifier() + " (tier " + tier + ") to " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int forget(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Holder.Reference<Enchantment> ench = ResourceArgument.getResource(c, "enchantment", Registries.ENCHANTMENT);
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : players) {
            var copy = new java.util.LinkedHashMap<>(KnowledgeManager.get(p).tiers());
            copy.remove(ench.key().identifier());
            com.panda.arcane.platform.Services.PLATFORM.setKnowledge(p, new Knowledge(copy));
        }
        c.getSource().sendSuccess(() -> Component.literal("Made " + players.size() + " player(s) forget " + ench.key().identifier()), true);
        return players.size();
    }

    private static int learnAll(CommandContext<CommandSourceStack> c, int tier) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        var enchantments = c.getSource().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (ServerPlayer p : players) {
            enchantments.listElements().forEach(ref -> KnowledgeManager.learn(p, ref.key().identifier(), Mastery.ofTier(tier)));
        }
        c.getSource().sendSuccess(() -> Component.literal("Taught every enchantment (tier " + tier + ") to " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int reset(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : players) com.panda.arcane.platform.Services.PLATFORM.setKnowledge(p, Knowledge.EMPTY);
        c.getSource().sendSuccess(() -> Component.literal("Cleared the knowledge of " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "target");
        Knowledge k = KnowledgeManager.get(p);
        c.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " knows " + k.size() + " enchantments: " + k.tiers()), false);
        return k.size();
    }
}
