package com.panda.arcane.registry;

import com.panda.arcane.ArcaneMod;
import com.panda.arcane.knowledge.Knowledge;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArcaneMod.MODID);

    /** What the player has learned. Saved, kept on death, synced to the owning player only. */
    public static final Supplier<AttachmentType<Knowledge>> KNOWLEDGE = ATTACHMENTS.register("knowledge",
            () -> AttachmentType.builder(() -> Knowledge.EMPTY)
                    .serialize(Knowledge.CODEC.fieldOf("tiers"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, Knowledge.STREAM_CODEC)
                    .build());

    /** Where a traveller came from, so the Library portal can lead back. Not synced. */
    public static final Supplier<AttachmentType<java.util.Optional<net.minecraft.core.GlobalPos>>> PORTAL_RETURN = ATTACHMENTS.register("portal_return",
            () -> AttachmentType.<java.util.Optional<net.minecraft.core.GlobalPos>>builder(() -> java.util.Optional.empty())
                    .serialize(net.minecraft.core.GlobalPos.CODEC.optionalFieldOf("pos"))
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
