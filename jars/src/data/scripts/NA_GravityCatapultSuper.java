package data.scripts;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;
import java.util.List;

public class NA_GravityCatapultSuper extends NA_GravityCatapult {

    protected static final String IMPACT_SOUND = "hit_solid";
    protected static final String SLASH_SOUND = "na_system_catapult_super_slash";

    protected static final Color EXPLOSION_COLOR = new Color(200, 23, 253, 255);
    protected static final float EXPLOSION_VISUAL_RADIUS = 150;

    public static float TIME_SLASH = 0.12f; // ms for jump
    IntervalUtil slashInterval = new IntervalUtil(TIME_SLASH, TIME_SLASH);
    int slashIndex = 1;
    boolean doSlash = false;



    public static float DMG_AREA = 175;
    public static float DMG_AMT = 250f;
    public static DamageType DMG_TYPE = DamageType.FRAGMENTATION;

    private ShipAPI ship = null;



    private static final float ELECTROTOXIN_MULT = 0.04f;
    private static final float ELECTROTOXIN_DURATION = 8f;
    private static final float ELECTROTOXIN_INTERVAL = 0.5f;
    private static final float ELECTROTOXIN_EMP = 40f;


    public static Color JITTER_UNDER_COLOR = new Color(48, 0, 246,155);


    public static Color TEXT_COLOR = new Color(223, 175,255,255);


    public static class TargetData {
        public ShipAPI target;
        public ShipAPI ship;
        public EveryFrameCombatPlugin targetEffectPlugin;
        public float malfunctionMult = ELECTROTOXIN_MULT;
        public Object KEY_TARGET = new Object();
        public IntervalUtil elapsed = new IntervalUtil(ELECTROTOXIN_DURATION, ELECTROTOXIN_DURATION);
        public IntervalUtil empInterval = new IntervalUtil(ELECTROTOXIN_INTERVAL, ELECTROTOXIN_INTERVAL);
        public TargetData(ShipAPI ship, ShipAPI target) {

            this.ship = ship;
            this.target = target;
        }
    }

    public static void applyElectrotoxin(ShipAPI ship, final ShipAPI target) {
        final String targetDataKey = target.getId() + "_electrotoxin_target_data";

        Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
        if (targetDataObj == null) {
            Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(ship, target));
            if (target != null) {
                targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
                boolean text = targetDataObj == null;
                ((TargetData) targetDataObj).target = target;
                if (text && (target.getFluxTracker().showFloaty() ||
                        ship == Global.getCombatEngine().getPlayerShip() ||
                        target == Global.getCombatEngine().getPlayerShip())) {
                    target.getFluxTracker().showOverloadFloatyIfNeeded("Electrotoxin applied!!", TEXT_COLOR, 4f, true);
                }
            }
        } else {
            ((TargetData) targetDataObj).target = target;
            ((TargetData) targetDataObj).elapsed.setElapsed(0f); // reset
        }

        if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;

        final TargetData targetData = (TargetData) targetDataObj;
        targetData.malfunctionMult = ELECTROTOXIN_MULT;

        if (targetData.targetEffectPlugin == null) {
            targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (Global.getCombatEngine().isPaused()) return;
                    if (targetData.target == Global.getCombatEngine().getPlayerShip()) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(targetData.KEY_TARGET,
                                "graphics/icons/hullsys/high_energy_focus.png",
                                "Electrotoxin",
                                "+" + (int)((targetData.malfunctionMult) * 100f) + "% malfunction chance", true);
                    }

                    if (targetData.elapsed.intervalElapsed() || !targetData.target.isAlive()) {
                        targetData.target.getMutableStats().getWeaponMalfunctionChance().unmodify(targetDataKey);
                        targetData.target.getMutableStats().getEngineMalfunctionChance().unmodify(targetDataKey);
                        Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
                        Global.getCombatEngine().getCustomData().remove(targetDataKey);
                    } else {
                        targetData.elapsed.advance(amount);
                        targetData.empInterval.advance(amount);
                        if (targetData.empInterval.intervalElapsed()) {
                            targetData.empInterval = new IntervalUtil(ELECTROTOXIN_INTERVAL, ELECTROTOXIN_INTERVAL);
                            // do a minor arc based on hullsize
                            for (int i = 0; i < Math.max(1, NAUtils.shipSize(targetData.target)); i++) {
                                Global.getCombatEngine().spawnEmpArcPierceShields(
                                        targetData.ship, targetData.target.getLocation(), targetData.target, targetData.target,
                                        DamageType.FRAGMENTATION,
                                        ELECTROTOXIN_EMP, // damage
                                        ELECTROTOXIN_EMP, // emp
                                        targetData.target.getCollisionRadius(), // max range
                                        null,
                                        15f,
                                        new Color(204, 198, 255, 255), /* startColor */
                                        new Color(138, 42, 222, 255) /* endColor */
                                );
                            }
                        }
                        targetData.target.getMutableStats().getWeaponMalfunctionChance().modifyFlat(targetDataKey, targetData.malfunctionMult * 5f);
                        targetData.target.getMutableStats().getEngineMalfunctionChance().modifyFlat(targetDataKey, targetData.malfunctionMult);

                        targetData.target.setJitterUnder(this, JITTER_UNDER_COLOR, 0.7f, 3, 10f, 45f);
                    }
                }
            };
            Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
        }
    }


    public void doDmg(Vector2f point) {
        if (ship == null) return;

        boolean playedOnce = false;

        List<ShipAPI> enemiesNearby = NAUtils.getEnemyShipsWithinRange(ship, point, DMG_AREA, true);
        List<MissileAPI> missilesNearby = NAUtils.getMissilesWithinRange(point, DMG_AREA);

        for (MissileAPI missile : missilesNearby) {
            // just do the damage
            Global.getCombatEngine().applyDamage(
                    missile, missile.getLocation(), DMG_AMT, DMG_TYPE, 0f, false, false, ship
            );
            if (!playedOnce) {
                playedOnce = true;
                Global.getSoundPlayer().playSound(IMPACT_SOUND, 1f, 1.2f, point, Misc.ZERO);
            }
        }
        for (ShipAPI trg : enemiesNearby) {
            if (trg.isPhased()) continue;
            Vector2f tpoint = CollisionUtils.getNearestPointOnBounds(
                    MathUtils.getPointOnCircumference(trg.getLocation(), trg.getCollisionRadius(),
                            VectorUtils.getAngle(trg.getLocation(), point)), trg);
            Global.getCombatEngine().applyDamage(
                    trg, tpoint, DMG_AMT, DMG_TYPE, DMG_AMT, false, true, ship, false
            );
            applyElectrotoxin(ship, trg);


            Global.getCombatEngine().spawnEmpArc(
                    ship, point, trg, trg,
                    DamageType.FRAGMENTATION,
                    DMG_AMT, // damage
                    DMG_AMT, // emp
                    DMG_AREA, // max range
                    "tachyon_lance_emp_impact",
                    10f,
                    new Color(204, 198, 255, 255), /* startColor */
                    new Color(138, 42, 222, 255) /* endColor */
            );

        }
    }

    float s1_id = MagicTrailPlugin.getUniqueID();
    float s2_id = MagicTrailPlugin.getUniqueID();
    float s3_id = MagicTrailPlugin.getUniqueID();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        super.apply(stats, id, state, effectLevel); // do the regular, but ALSO do the slash if target is hostile

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        this.ship = ship;
        String shipID = id + "_" + ship.getId();

        if (effectLevel == 0) {
            slashInterval.setElapsed(0f);
            doSlash = true;
        } else if (state == State.ACTIVE) { // active
            // set the target

            ShipAPI target = null;


            // there should be a target
            String key = ID + "_data_" + ship.getId();
            NA_GravityCatapultData data = (NA_GravityCatapultData) ship.getCustomData().get(key);
            if (data != null) {
                target = data.target;

                if (ship != null && target != null) {
                    float amount = Global.getCombatEngine().getElapsedInLastFrame();


                    //advance the timer for slash
                    slashInterval.advance(amount);


                    // engage warp drive



                    if (slashInterval.intervalElapsed()) {

                        List<ShipAPI> enemiesNearby = NAUtils.getEnemyShipsWithinRange(ship, ship.getLocation(), DMG_AREA, true);
                        List<MissileAPI> missilesNearby = NAUtils.getMissilesWithinRange(ship.getLocation(), DMG_AREA);
                        if (enemiesNearby.size() > 0 || missilesNearby.size() > 0) {
                            doSlash = true;
                            Global.getSoundPlayer().playSound(SLASH_SOUND, 1f, 0.3f, ship.getLocation(), Misc.ZERO);
                        } else {
                            doSlash = false;
                        }

                        MagicTrailPlugin.cutTrailsOnEntity(ship);

                        slashIndex *= -1;
                        slashInterval = new IntervalUtil(TIME_SLASH, TIME_SLASH);


                        // do damage
                        doDmg(ship.getLocation());
                    } else if (doSlash){
                        float angle = VectorUtils.getAngle(data.targetLoc, data.initialLoc);
                        MagicTrailPlugin.addTrailMemberAdvanced(
                                ship, /* linkedEntity */
                                s1_id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                MathUtils.getPointOnCircumference(ship.getLocation(), 250f/TIME_SLASH * (TIME_SLASH/2 - slashInterval.getElapsed()),
                                        angle + 80 * slashIndex), /* position */
                                75f, /* startSpeed */
                                15f, /* endSpeed */
                                angle + (slashIndex * 45f), /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                35f, /* startSize */
                                25f, /* endSize */
                                new Color(216, 213, 246, 255), /* startColor */
                                new Color(138, 42, 222, 255), /* endColor */
                                1f, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                2.4f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                25f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                new Vector2f(), /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                        MagicTrailPlugin.addTrailMemberAdvanced(
                                ship, /* linkedEntity */
                                s2_id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                MathUtils.getPointOnCircumference(ship.getLocation(), 250f/TIME_SLASH * (TIME_SLASH/2 - slashInterval.getElapsed()), angle + 100 * slashIndex), /* position */
                                100f, /* startSpeed */
                                0, /* endSpeed */
                                angle + (slashIndex * 45f), /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                35f, /* startSize */
                                25f, /* endSize */
                                new Color(204, 198, 255, 255), /* startColor */
                                new Color(189, 42, 222, 255), /* endColor */
                                1f, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                2.2f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                25f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                new Vector2f(), /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                        MagicTrailPlugin.addTrailMemberAdvanced(
                                ship, /* linkedEntity */
                                s3_id, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
                                MathUtils.getPointOnCircumference(ship.getLocation(), 250f/TIME_SLASH * (TIME_SLASH/2 - slashInterval.getElapsed()), angle + 90 * slashIndex), /* position */
                                75f, /* startSpeed */
                                25f, /* endSpeed */
                                angle + (slashIndex * 45f), /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                42f, /* startSize */
                                30f, /* endSize */
                                new Color(204, 198, 255, 255), /* startColor */
                                new Color(42, 45, 222, 255), /* endColor */
                                1f, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                1.9f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                25f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                new Vector2f(), /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                }

            }

        }
    }

}
