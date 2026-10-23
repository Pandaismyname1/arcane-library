package com.panda.arcane.platform;

import com.panda.arcane.knowledge.Knowledge;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** What common code needs from the mod loader. Each loader module provides one, found with {@link Services}. */
public interface ArcanePlatform {
    /** "neoforge" or "fabric". */
    String loaderName();

    /** What the player has learned. Saved with the player and kept on death. */
    Knowledge knowledge(Player player);

    /** Stores what the player has learned and syncs it to that player's client. */
    void setKnowledge(Player player, Knowledge knowledge);

    /** Where a traveller came into the Library from. Saved with the entity and kept on death. */
    Optional<GlobalPos> portalReturn(Entity entity);

    void setPortalReturn(Entity entity, Optional<GlobalPos> pos);

    /** Whether a mob may break blocks: the mobGriefing rule, plus whatever the loader lets other mods decide. */
    boolean canGrief(ServerLevel level, Entity entity);

    /** The display name of the mod that owns a namespace, or the namespace itself. */
    String modName(String namespace);

    /** A builder for a creative tab of the mod's own, which the loader places among its pages. */
    net.minecraft.world.item.CreativeModeTab.Builder tabBuilder();
}
