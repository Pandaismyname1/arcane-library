package com.panda.arcane.item;

import com.panda.arcane.ArcaneMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Opens the Codex screen on the client: a list of everything the player has learned. */
public class CodexItem extends Item {
    public CodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ArcaneMod.openCodex.run();
        }
        return InteractionResult.SUCCESS;
    }
}
