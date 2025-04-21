package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;


public class NA_Wound_Red implements EveryFrameWeaponEffectPlugin {

    public final float COLOR_RAMP = 50f;
    private float id = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship != null) {
            float fluxperc = ship.getFluxTracker().getFluxLevel();
            float hardflux = ship.getFluxTracker().getHardFlux()/Math.max(1, ship.getFluxTracker().getMaxFlux());


            if (id == 0) id = MagicTrailPlugin.getUniqueID();

            Color trailColor = new Color(
                    (int) Math.max(75f, Math.min(225f, 175 + 2*COLOR_RAMP * hardflux)),
                    (int) Math.max(75f, Math.min(225f, 75 + 0.5f*COLOR_RAMP * (fluxperc - hardflux))),
                    (int) Math.max(175f, Math.min(225f, 125f - COLOR_RAMP * (fluxperc - hardflux))));

            MagicTrailPlugin.addTrailMemberAdvanced(
                    ship, /* linkedEntity */
                    id, /* ID */
                    Global.getSettings().getSprite("na_trails", "na_particletrail"), /* sprite */
                    weapon.getLocation(), /* position */
                    80f, /* startSpeed */
                    0f, /* endSpeed */
                    weapon.getCurrAngle() + MathUtils.getRandomNumberInRange(-5f, 5f), /* angle */
                    0f, /* startAngularVelocity */
                    0f, /* endAngularVelocity */
                    25f, /* startSize */
                    45f, /* endSize */
                    trailColor, /* startColor */
                    ship.isPhased() ? new Color(255, 175, 225) : new Color(100, 100, 255), /* endColor */
                    0.5f, /* opacity */
                    0.1f, /* inDuration */
                    0.25f, /* mainDuration */
                    0.5f, /* outDuration */
                    GL11.GL_SRC_ALPHA, /* blendModeSRC */
                    GL11.GL_ONE_MINUS_SRC_ALPHA, /* blendModeDEST */
                    256f, /* textureLoopLength */
                    16f, /* textureScrollSpeed */
                    -1, /* textureOffset */
                    Misc.ZERO, /* offsetVelocity */
                    null, /* advancedOptions */
                    CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                    1f /* frameOffsetMult */
            );

            MagicTrailPlugin.addTrailMemberAdvanced(
                    ship, /* linkedEntity */
                    id, /* ID */
                    Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                    weapon.getLocation(), /* position */
                    50f, /* startSpeed */
                    0f, /* endSpeed */
                    weapon.getCurrAngle() + 10f, /* angle */
                    0f, /* startAngularVelocity */
                    0f, /* endAngularVelocity */
                    70f, /* startSize */
                    15f, /* endSize */
                    trailColor, /* startColor */
                    Color.RED, /* endColor */
                    0.5f, /* opacity */
                    0.1f, /* inDuration */
                    0.25f, /* mainDuration */
                    0.5f, /* outDuration */
                    GL11.GL_SRC_ALPHA, /* blendModeSRC */
                    GL11.GL_ONE_MINUS_SRC_ALPHA, /* blendModeDEST */
                    256f, /* textureLoopLength */
                    16f, /* textureScrollSpeed */
                    -1, /* textureOffset */
                    Misc.ZERO, /* offsetVelocity */
                    null, /* advancedOptions */
                    CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                    1f /* frameOffsetMult */
            );


        }
    }
}
