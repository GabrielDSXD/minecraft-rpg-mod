package com.alvorecer.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** Céu dos Reinos do Éter: nuvens altas e névoa dourada que acompanha a luz do dia. */
public class AetherSky extends DimensionSpecialEffects {
    public AetherSky() {
        super(220f, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        Vec3 day = fogColor.multiply(brightness * 0.94 + 0.06, brightness * 0.94 + 0.06, brightness * 0.91 + 0.09);
        return day.lerp(new Vec3(1.0, 0.86, 0.62), 0.25 * brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }
}
