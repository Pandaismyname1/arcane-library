package com.panda.arcane.fabric;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.knowledge.Knowledge;
import com.panda.arcane.platform.ArcanePlatform;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.gamerules.GameRules;

/** {@link ArcanePlatform} on Fabric: Fabric API data attachments, the mobGriefing rule and Fabric Loader's mod list. */
public final class FabricPlatform implements ArcanePlatform {
    /** What the player has learned. Saved, kept on death, synced to the owning player only. */
    static final AttachmentType<Knowledge> KNOWLEDGE = AttachmentRegistry.create(ArcaneMod.id("knowledge"), builder -> builder
            .initializer(() -> Knowledge.EMPTY)
            .persistent(Knowledge.CODEC)
            .copyOnDeath()
            .syncWith(Knowledge.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

    /** Where a traveller came from, so the Library portal can lead back. Not synced. */
    static final AttachmentType<GlobalPos> PORTAL_RETURN = AttachmentRegistry.create(ArcaneMod.id("portal_return"), builder -> builder
            .persistent(GlobalPos.CODEC)
            .copyOnDeath());

    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public Knowledge knowledge(Player player) {
        return player.getAttachedOrElse(KNOWLEDGE, Knowledge.EMPTY);
    }

    @Override
    public void setKnowledge(Player player, Knowledge knowledge) {
        player.setAttached(KNOWLEDGE, knowledge);
    }

    @Override
    public Optional<GlobalPos> portalReturn(Entity entity) {
        return Optional.ofNullable(entity.getAttached(PORTAL_RETURN));
    }

    @Override
    public void setPortalReturn(Entity entity, Optional<GlobalPos> pos) {
        entity.setAttached(PORTAL_RETURN, pos.orElse(null));
    }

    @Override
    public boolean canGrief(ServerLevel level, Entity entity) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    @Override
    public String modName(String namespace) {
        return FabricLoader.getInstance().getModContainer(namespace).map(c -> c.getMetadata().getName()).orElse(namespace);
    }

    @Override
    public CreativeModeTab.Builder tabBuilder() {
        return FabricCreativeModeTab.builder();
    }
}
