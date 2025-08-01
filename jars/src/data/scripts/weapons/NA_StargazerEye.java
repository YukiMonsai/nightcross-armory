package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.opengl.GL11;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;


public class NA_StargazerEye implements EveryFrameWeaponEffectPlugin {

    public final float COLOR_RAMP = 50f;
    private float id = 0f;



    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {



        ShipAPI ship = weapon.getShip();
        if (ship != null) {


            String key = "StargazerListener" + ship.hashCode();
            String key2 = "StargazerEye" + ship.hashCode();
            Object targetDataObj = Global.getCombatEngine().getCustomData().get(key);
            if (targetDataObj == null) {
                Global.getCombatEngine().getCustomData().put(key, new NA_StargazerListener(weapon, key, ship));
            }
            Object lightobj = Global.getCombatEngine().getCustomData().get(key2);

            if (id == 0) id = MagicTrailPlugin.getUniqueID();

            if (lightobj == null) {
                StandardLight light = (StandardLight) Global.getCombatEngine().getCustomData().get(key2);
                light = new StandardLight(weapon.getLocation(), Misc.ZERO, Misc.ZERO, null);
                light.setIntensity(0.05f);
                light.setVelocity(ship.getVelocity());
                light.setSize(ship.getCollisionRadius());
                light.setColor(new Color(179, 0, 0));
                light.fadeIn(0.05f);
                light.setLifetime(1f);
                light.setAutoFadeOutTime(0.47f);
                light.setSize(ship.getCollisionRadius() * 0.8f);
                light.setSpecularMult(2.0f);

                light.attachTo(weapon.getShip());
                light.setOffset(weapon.getSlot().getLocation());
                LightShader.addLight(light);
            }



            MagicTrailPlugin.addTrailMemberAdvanced(
                    ship, /* linkedEntity */
                    id, /* ID */
                    Global.getSettings().getSprite("na_trails", "na_particletrail"), /* sprite */
                    weapon.getLocation(), /* position */
                    80f, /* startSpeed */
                    0f, /* endSpeed */
                    weapon.getCurrAngle() + weapon.getShip().getFacing(), /* angle */
                    0f, /* startAngularVelocity */
                    0f, /* endAngularVelocity */
                    80f, /* startSize */
                    250f, /* endSize */
                    new Color(255, 65, 100), /* startColor */
                    ship.isPhased() ? new Color(255, 175, 225) : new Color(200, 0, 0), /* endColor */
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
