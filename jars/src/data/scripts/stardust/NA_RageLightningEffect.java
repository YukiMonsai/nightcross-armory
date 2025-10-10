package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Multiple instances of this plugin - one for every projectile (on hit), and one for each weapon.
 *
 * The goal is for the on-hit effect to fire off a lightning arc in case of a hit, and for the onfire/every frame copy
 * of the plugin to fire off a lightning arc in case there is a miss.
 *
 * @author Alex
 *
 */
public class NA_RageLightningEffect implements OnHitEffectPlugin, OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, NA_StardustWeapon {


    public static Color RIFT_LIGHTNING_COLOR = new Color(255,25,75,255);
    public static float RIFT_LIGHTNING_SPEED = 10000f;

    //	public static String RIFT_LIGHTNING_PROJ_TAG = "rift_lightning_proj_tag";
    public static String RIFT_LIGHTNING_DAMAGE_REMOVER = "rift_lightning_damage_remover";
    public static String RIFT_LIGHTNING_FIRED_TAG = "rift_lightning_fired_tag";
    public static String RIFT_LIGHTNING_SOURCE_WEAPON = "rift_lightning_source_weapon";
    public static String RIFT_LIGHTNING_SOURCE_POS = "rift_lightning_source_pos";

    @Override
    public int getNumFragmentsToFire() {
        return 2;
    }

    public static class FiredLightningProjectile {
        public DamagingProjectileAPI projectile;
        public Vector2f origin;
    }


//	/**
//	 * The actual damage is dealt by the rift explosion.
//	 * (Removing this: setting multiplier to 0 on projectile instead)
//	 * @author Alex
//	 *
//	 */
//	public static class RiftLightningBaseDamageNegator implements DamageDealtModifier {
//		@Override
//		public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
//			if (param instanceof DamagingProjectileAPI) {
//				DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
//				if (proj.getCustomData().containsKey(RIFT_LIGHTNING_PROJ_TAG)) {
//					damage.getModifier().modifyMult(RIFT_LIGHTNING_PROJ_TAG, 0f);
//					return RIFT_LIGHTNING_PROJ_TAG;
//				}
//			}
//			return null;
//		}
//	}

    protected java.util.List<FiredLightningProjectile> fired = new ArrayList<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
//		if (!fired.isEmpty()) {
//			System.out.println("FIRED");
//		}
        if (engine != null && engine.isInFastTimeAdvance()) {
            return;
        }
        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(weapon.getShip());
        int active = swarm == null ? 0 : swarm.getNumActiveMembers();
        int required = getNumFragmentsToFire();
        boolean disable = active < required;
        weapon.setForceDisabled(disable);

        showNoFragmentSwarmWarning(weapon, weapon.getShip());

        List<FiredLightningProjectile> remove = new ArrayList<>();

        float maxRange = weapon.getRange();
        for (FiredLightningProjectile data : fired) {
            Vector2f from = (Vector2f) data.projectile.getCustomData().get(RIFT_LIGHTNING_SOURCE_POS);
            if (from == null) from = data.projectile.getSpawnLocation();
            float dist = Misc.getDistance(from, data.projectile.getLocation());
            boolean firedAlready = data.projectile.getCustomData().containsKey(RIFT_LIGHTNING_FIRED_TAG);
            if (dist > maxRange || firedAlready) {
                remove.add(data);
                if (!firedAlready) {
                    fireArc(data.projectile, weapon, null, null, from);
                }
            }
        }
        fired.removeAll(remove);
    }


    protected static NA_StargazerStardust.SwarmMember pickPrimaryFragment(NA_StargazerStardust sourceSwarm, Vector2f loc, float range) {
        return pickOuterFragmentWithinRange(sourceSwarm, range, loc);
    }


    protected static NA_StargazerStardust.SwarmMember pickOuterFragmentWithinRange(NA_StargazerStardust sourceSwarm, float range, Vector2f otherLoc) {
        NA_StargazerStardust.SwarmMember best = null;
        float minDist = Float.MAX_VALUE;
        WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = sourceSwarm.getPicker(true, true);

        return picker.pick();
    }


    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
//		if (weapon.getShip() != null &&
//				!weapon.getShip().hasListenerOfClass(RiftLightningBaseDamageNegator.class)) {
//			weapon.getShip().addListener(new RiftLightningBaseDamageNegator());
//		}
        //projectile.setCustomData(RIFT_LIGHTNING_PROJ_TAG, true);
        if (projectile.getSource() != null) {
        // stargazer hullmod
            NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(weapon.getShip());
            int active = swarm == null ? 0 : swarm.getNumActiveMembers();
            int required = getNumFragmentsToFire();
            if (active >= required) {
                NA_StargazerStardust.SwarmMember fragment = pickPrimaryFragment(swarm, weapon.getFirePoint(0), 1200);
                if (fragment == null) {
                    return;
                }
            }
        } else return;

        for (int i = 1; i < getNumFragmentsToFire(); i++) {
            NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(weapon.getShip());
            NA_StargazerStardust.SwarmMember fragment = pickOuterFragmentWithinRange(swarm, 1200, weapon.getFirePoint(0));
            if (fragment != null) {
                swarm.removeMember(fragment);

                Vector2f from = weapon.getFirePoint(0);

                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                params.segmentLengthMult = 4f;

                params.glowSizeMult = 0.5f;
                params.brightSpotFadeFraction = 0.33f;
                params.brightSpotFullFraction = 0.5f;
                params.movementDurMax = 0.2f;
                params.flickerRateMult = 0.35f;

                float dist = Misc.getDistance(from, from);
                float minBright = 100f;
                if (dist * params.brightSpotFullFraction < minBright) {
                    params.brightSpotFullFraction = minBright / Math.max(minBright, dist);
                }

                float thickness = 40f;

                EmpArcEntityAPI arc = engine.spawnEmpArcVisual(fragment.loc, weapon.getShip(),
                        from,
                        weapon.getShip(),
                        thickness, // thickness
                        new Color(112, 0, 0),
                        new Color(255, 241, 244),
                        params
                );
                //arc.setCoreWidthOverride(thickness * coreWidthMult);
                arc.setSingleFlickerMode(true);
                arc.setUpdateFromOffsetEveryFrame(true);
            }
        }


        projectile.getDamage().getModifier().modifyMult(RIFT_LIGHTNING_DAMAGE_REMOVER, 0f);
        projectile.setCustomData(RIFT_LIGHTNING_SOURCE_WEAPON, weapon);

        FiredLightningProjectile data = new FiredLightningProjectile();
        data.projectile = projectile;
        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(weapon.getShip());
        NA_StargazerStardust.SwarmMember fragment = pickOuterFragmentWithinRange(swarm, 1200, weapon.getFirePoint(0));
        if (fragment != null) {
            data.origin = fragment.loc;
            projectile.setCustomData(RIFT_LIGHTNING_SOURCE_POS, fragment.loc);

            //swarm.removeMember(fragment);

            Vector2f from = weapon.getFirePoint(0);

            EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
            params.segmentLengthMult = 4f;

            params.glowSizeMult = 0.5f;
            params.brightSpotFadeFraction = 0.33f;
            params.brightSpotFullFraction = 0.5f;
            params.movementDurMax = 0.2f;
            params.flickerRateMult = 0.8f;

            float dist = Misc.getDistance(from, from);
            float minBright = 100f;
            if (dist * params.brightSpotFullFraction < minBright) {
                params.brightSpotFullFraction = minBright / Math.max(minBright, dist);
            }

            float thickness = 80f;

            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
                    from,weapon.getShip(),
                    fragment.loc, weapon.getShip(),
                    thickness, // thickness
                    new Color(186, 10, 10),
                    new Color(255, 241, 244),
                    params
            );
            //arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode(true);
            arc.setUpdateFromOffsetEveryFrame(true);
        }

        fired.add(data);
    }


    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        WeaponAPI weapon = (WeaponAPI) projectile.getCustomData().get(RIFT_LIGHTNING_SOURCE_WEAPON);
        if (weapon == null) return;

        Vector2f from = (Vector2f) projectile.getCustomData().get(RIFT_LIGHTNING_SOURCE_POS);;
        if (from == null) {
            from = projectile.getSpawnLocation();
        }

        fireArc(projectile, weapon, point, target, from);
    }

    public static void fireArc(DamagingProjectileAPI projectile, WeaponAPI weapon, Vector2f point, CombatEntityAPI target, Vector2f from) {
        boolean firedAlready = projectile.getCustomData().containsKey(RIFT_LIGHTNING_FIRED_TAG);
        if (firedAlready) return;

        projectile.setCustomData(RIFT_LIGHTNING_FIRED_TAG, true);

        CombatEngineAPI engine = Global.getCombatEngine();

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;


        float dist = Float.MAX_VALUE;
        if (point != null) dist = Misc.getDistance(from, point);

        float maxRange = weapon.getRange();
        if (dist > maxRange || point == null) {
            dist = maxRange * (0.5f + 0.5f * (float) Math.random());
            if (projectile.didDamage()) {
                dist = maxRange;
            }
            point = Misc.getUnitVectorAtDegreeAngle(projectile.getFacing());
            point.scale(dist);
            Vector2f.add(point, from, point);
        }

        float arcSpeed = RIFT_LIGHTNING_SPEED;

        DwellerShroud shroud = DwellerShroud.getShroudFor(ship);
        if (shroud != null) {
            float angle = Misc.getAngleInDegrees(ship.getLocation(), point);
            from = Misc.getUnitVectorAtDegreeAngle(angle + 90f - 180f * (float) Math.random());
            from.scale((0.5f + (float) Math.random() * 0.25f) * shroud.getShroudParams().maxOffset);
            Vector2f.add(ship.getLocation(), from, from);
        }


        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 8f;
        params.zigZagReductionFactor = 0.2f;
        params.fadeOutDist = 50f;
        params.minFadeOutMult = 10f;
//		params.flickerRateMult = 0.7f;
        params.flickerRateMult = 0.3f;
//		params.flickerRateMult = 0.05f;
//		params.glowSizeMult = 3f;
//		params.brightSpotFullFraction = 0.5f;

        params.movementDurOverride = Math.max(0.05f, dist / arcSpeed);

        //Color color = weapon.getSpec().getGlowColor();
        Color color = RIFT_LIGHTNING_COLOR;
        EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArcVisual(from, ship, point, null,
                40f, // thickness
                color,
                new Color(255,255,255,255),
                params
        );
        arc.setCoreWidthOverride(10f);

        arc.setRenderGlowAtStart(false);
        arc.setFadedOutAtStart(true);
        arc.setSingleFlickerMode(true);

        spawnMine(ship, point, params.movementDurOverride * 0.8f); // - 0.05f);

    }

    public static void spawnMine(ShipAPI source, Vector2f mineLoc, float delay) {
        CombatEngineAPI engine = Global.getCombatEngine();


        //Vector2f currLoc = mineLoc;
        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                "naai_ragelightning_minelayer",
                mineLoc,
                (float) Math.random() * 360f, null);
        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponAPI.WeaponType.ENERGY, false, mine.getDamage());
        }


        float fadeInTime = 0.05f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);

        float liveTime = Math.max(delay, 0f);
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
        mine.addDamagedAlready(source);
        mine.setNoMineFFConcerns(true);
        if (liveTime <= 0.016f) {
            mine.explode();
        }
    }

}
