package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;


public class NA_Sop_Trail implements EveryFrameWeaponEffectPlugin {

    public final float COLOR_RAMP = 50f;
    private float id = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship != null) {

            if (id == 0) id = MagicTrailPlugin.getUniqueID();


            MagicTrailPlugin.addTrailMemberAdvanced(
                    ship, /* linkedEntity */
                    id, /* ID */
                    Global.getSettings().getSprite("na_trails", "na_particletrail"), /* sprite */
                    weapon.getLocation(), /* position */
                    80f, /* startSpeed */
                    0f, /* endSpeed */
                    weapon.getCurrAngle(), /* angle */
                    0f, /* startAngularVelocity */
                    0f, /* endAngularVelocity */
                    50f, /* startSize */
                    75f, /* endSize */
                    new Color(100, 100, 255), /* startColor */
                    ship.isPhased() ? new Color(255, 175, 225) : new Color(100, 100, 255), /* endColor */
                    0.5f, /* opacity */
                    0.5f, /* inDuration */
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
