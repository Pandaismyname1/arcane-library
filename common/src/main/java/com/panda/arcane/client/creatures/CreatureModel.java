package com.panda.arcane.client.creatures;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Base for the Creatures A models. The geometry comes from {@link CreatureLayers}; subclasses only animate. Parts that belong to some variants only are shown or hidden
 * from the generated "only" table every frame.
 */
public abstract class CreatureModel extends EntityModel<CreatureRenderState> {
    protected static final float DEG = Mth.DEG_TO_RAD;
    private final Map<ModelPart, String[]> only = new HashMap<>();
    private final Function<String, ModelPart> lookup;

    protected CreatureModel(ModelPart root, Map<String, String[]> onlyTable) {
        this(root, onlyTable, RenderTypes::entityCutout);
    }

    protected CreatureModel(ModelPart root, Map<String, String[]> onlyTable, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
        this.lookup = root.createPartLookup();
        onlyTable.forEach((name, variants) -> only.put(part(name), variants));
    }

    protected final ModelPart part(String name) {
        ModelPart p = lookup.apply(name);
        if (p == null) throw new IllegalStateException(getClass().getSimpleName() + " has no part '" + name + "'");
        return p;
    }

    @Override
    public final void setupAnim(CreatureRenderState state) {
        super.setupAnim(state);
        for (var e : only.entrySet()) {
            boolean show = false;
            for (String v : e.getValue()) show |= v.equals(state.variantName);
            e.getKey().visible = show;
        }
        animate(state, state.ageInTicks, state.walkAnimationPos, Math.min(1.0F, state.walkAnimationSpeed));
    }

    protected abstract void animate(CreatureRenderState s, float t, float pos, float speed);

    protected static float smooth(float a) {
        a = Mth.clamp(a, 0.0F, 1.0F);
        return a * a * (3.0F - 2.0F * a);
    }
}
