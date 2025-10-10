package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.stardust.NA_StargazerStardust;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NA_ForgeVats2 extends BaseShipSystemScript {
    public static final Object KEY_JITTER = new Object();
    public static final Color JITTER_COLOR = new Color(255, 108, 229,155);


    private IntervalUtil beamTimer = new IntervalUtil(0.15f, 0.3f);


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }


        if (effectLevel > 0) {
            float jitterLevel = effectLevel * 0.1f;

            boolean firstTime = false;
            final String fightersKey = ship.getId() + "_na_forgevats2";
            List<WeaponAPI> weapons = null;
            if (!Global.getCombatEngine().getCustomData().containsKey(fightersKey)) {
                weapons = getWeapons(ship);
                Global.getCombatEngine().getCustomData().put(fightersKey, weapons);
                firstTime = true;
            } else {
                weapons = (List<WeaponAPI>) Global.getCombatEngine().getCustomData().get(fightersKey);
            }
            if (weapons == null) { // shouldn't be possible, but still
                weapons = new ArrayList<WeaponAPI>();
            }

            for (WeaponAPI weapon : weapons) {

                float maxRangeBonus = 10f;
                if (weapon.getSpec().getSize() == WeaponAPI.WeaponSize.LARGE) maxRangeBonus = 50;
                else if (weapon.getSpec().getSize() == WeaponAPI.WeaponSize.MEDIUM) maxRangeBonus = 25;
                float jitterRangeBonus = 5f + jitterLevel * maxRangeBonus;

                //if (firstTime) {
                    //Global.getSoundPlayer().playSound("system_forgevats", 1f, 0.5f, weapon.getLocation(), ship.getVelocity());
                    ship.setJitter(KEY_JITTER, JITTER_COLOR, jitterLevel, 10, 0f, jitterRangeBonus);
                //}

                if (effectLevel == 1) {
                    int max = weapon.getAmmoTracker().getMaxAmmo();
                    float percent = 0.5f;
                    float reload = (float) Math.ceil(max*percent);
                    weapon.getAmmoTracker().setAmmo(
                            (int) Math.min(max, Math.max(0, weapon.getAmmoTracker().getAmmo() + reload))
                    );

                }
            }



            if (beamTimer.intervalElapsed()) {
                beamTimer.randomize();
                /*ship.addAfterimage(new Color(255, 0, 75, (int) (25 + 50 * effectLevel)),
                        0,
                        0,
                        -ship.getVelocity().x,
                        -ship.getVelocity().y,
                        2f, 0.25f, 0.5f, 0.5f,
                        true,
                        true,
                        true
                );*/

                NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
                if (swarm != null) {
                    boolean realshock = false;
                    if (MathUtils.getRandomNumberInRange(effectLevel, 1f) < 0.5f) {
                        // fake shock
                    } else {
                        // real shock

                        WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker2 = swarm.getPicker(true, true);
                        NA_StargazerStardust.SwarmMember fragment = picker2.pick();
                        if (fragment != null) {
                            List<CombatEntityAPI> nearbyTargets = NAUtils.getEntitiesWithinRange(ship.getLocation(), 1200f);
                            ShipAPI shp = ship;
                            nearbyTargets.removeIf(entry -> (entry.getOwner() == shp.getOwner()));
                            if (!nearbyTargets.isEmpty()) {
                                WeightedRandomPicker<CombatEntityAPI> picker = new WeightedRandomPicker<>();
                                for (CombatEntityAPI entity : nearbyTargets) {
                                    float size = 0;
                                    if (entity instanceof MissileAPI || entity instanceof CombatAsteroidAPI) size = 0.25f;
                                    if (entity instanceof ShipAPI) {
                                        switch (((ShipAPI) entity).getHullSize()) {
                                            case FRIGATE: size = 1; break;
                                            case CRUISER: size = 3.5f; break;
                                            case CAPITAL_SHIP: size = 6f; break;
                                            case DESTROYER: size = 2f; break;
                                            case FIGHTER: size = 0.4f; break;
                                        }
                                    }
                                    picker.add(entity, size);
                                }

                                CombatEntityAPI target = picker.pick();
                                if (target != null) {
                                    realshock = true;
                                    Global.getCombatEngine().spawnEmpArc(
                                            ship, fragment.loc, null, target, DamageType.HIGH_EXPLOSIVE, 150f + 75f * ship.getFluxLevel(),
                                            150f + 75f * ship.getFluxLevel(), 1500f, "na_rift_beam_explosion2", 20f + 10f * ship.getFluxLevel(),

                                            new Color(255, 8, 187, 50),
                                            new Color(253, 162, 162, 250)
                                    );
                                }
                            }
                        }

                    }
                    if (!realshock && MathUtils.getRandomNumberInRange(effectLevel, 1f) > 0.5f) {


                        WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker2 = swarm.getPicker(true, true);
                        NA_StargazerStardust.SwarmMember fragment = picker2.pick();
                        NA_StargazerStardust.SwarmMember fragment2 = picker2.pick();
                        if (fragment2 != null && fragment != null && fragment2 != fragment) {
                            Global.getCombatEngine().spawnEmpArcVisual(
                                    fragment.loc, ship, fragment2.loc, ship, 15f,
                                    new Color(255, 0, 221, 50),
                                    new Color(255, 222, 222, 150)
                            );
                        }
                    }
                }

            }
            beamTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());



        }
    }

    // Get all synergy weapons and large missiles
    public static List<WeaponAPI> getWeapons(ShipAPI carrier) {
        List<WeaponAPI> result = new ArrayList<WeaponAPI>();

        for (WeaponAPI weapon : carrier.getAllWeapons()) {
            if (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
                || weapon.getSpec().getMountType() == WeaponAPI.WeaponType.ENERGY) {
                result.add(weapon);
            }
        }

        return result;
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        final String fightersKey = ship.getId() + "_na_forgevats2";
        Global.getCombatEngine().getCustomData().remove(fightersKey);

//		for (ShipAPI fighter : getFighters(ship)) {
//			fighter.setPhased(false);
//			fighter.setCopyLocation(null, 1f, fighter.getFacing());
//		}
    }


    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }


}
















