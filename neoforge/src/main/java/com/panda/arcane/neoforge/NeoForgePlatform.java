package com.panda.arcane.neoforge;

import com.panda.arcane.knowledge.Knowledge;
import com.panda.arcane.platform.ArcanePlatform;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.EventHooks;

/** {@link ArcanePlatform} on NeoForge: data attachments, NeoForge's grief hook and the mod list. */
public final class NeoForgePlatform implements ArcanePlatform {
    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public Knowledge knowledge(Player player) {
        return player.getData(NeoForgeAttachments.KNOWLEDGE);
    }

    @Override
    public void setKnowledge(Player player, Knowledge knowledge) {
        player.setData(NeoForgeAttachments.KNOWLEDGE, knowledge);
    }

    @Override
    public Optional<GlobalPos> portalReturn(Entity entity) {
        return entity.getData(NeoForgeAttachments.PORTAL_RETURN);
    }

    @Override
    public void setPortalReturn(Entity entity, Optional<GlobalPos> pos) {
        entity.setData(NeoForgeAttachments.PORTAL_RETURN, pos);
    }

    @Override
    public boolean canGrief(ServerLevel level, Entity entity) {
        return EventHooks.canEntityGrief(level, entity);
    }

    @Override
    public String modName(String namespace) {
        return ModList.get().getModContainerById(namespace).map(c -> c.getModInfo().getDisplayName()).orElse(namespace);
    }

    @Override
    public CreativeModeTab.Builder tabBuilder() {
        return CreativeModeTab.builder();
    }
}
