package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StargazerMissiles extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }


    private static final float SHIELD_MISSILE_DMG = 10f;
    private static final float SHIELD_MISSILE_FLUX = 25f;
    private static final float SHIELD_MISSILE_HP = 25f;
    private static final float MISSILE_ROF = 10f;


    public static final String ID = "na_sic_overwhelm";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s bonus damage vs. shields with missiles and synergy weapons", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(SHIELD_MISSILE_DMG) + "%");
        tooltipMakerAPI.addPara("%s flux cost of missiles and synergy weapons", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(SHIELD_MISSILE_FLUX) + "%");
        tooltipMakerAPI.addPara("%s missile hitpoints", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(SHIELD_MISSILE_HP) + "%");
        tooltipMakerAPI.addPara("%s missile rate of fire", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(MISSILE_ROF) + "%");


    }


    public static class NA_AtomizeMod implements DamageDealtModifier, AdvanceableListener {
        protected ShipAPI ship;
        public NA_AtomizeMod(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!ship.isAlive() || target == null) {
                return null;
            }
            if (!shieldHit) return null;
            //if (ship.getCaptain() == null || (ship.getCaptain().isDefault())) return null;

            if ((param instanceof DamagingProjectileAPI proj
                    && proj.getWeapon() != null && (
                    proj.getWeapon().getType() == WeaponAPI.WeaponType.MISSILE
                        || proj.getWeapon().getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
                    ))
                    ||
                    (param instanceof BeamAPI beam
                            && beam.getWeapon() != null && (
                            beam.getWeapon().getType() == WeaponAPI.WeaponType.MISSILE
                                    || beam.getWeapon().getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
                    ))
                ) {
                    damage.getModifier().modifyPercent(ID, SHIELD_MISSILE_DMG);
                    return ID;
                }
                return null;
            }

            boolean inited = false;

            private void init(ShipAPI ship){
                if (inited) return;
                for(WeaponAPI weapon : getSynergy(ship)){
                    if(!weapon.isDecorative()){
                        if (weapon.isBeam() && !weapon.isBurstBeam()){
                            beams.add(weapon);
                        }
                        else{
                            fluxRefunded.put(weapon, true);
                        }
                    }
                }
                inited = true;
            }

            private final HashMap<WeaponAPI, Boolean> fluxRefunded = new HashMap<>();
            private final List<WeaponAPI> beams = new ArrayList<>();

            @Override
            public void advance(float amount) {

                if (!ship.isAlive()) return;
                init(ship);



                // Code based on Knights of Ludd, thanks selkie and co.
                float dissipationBuff = 0f;
                float flatFluxRefund = 0f;


                boolean firing = false;
                for(WeaponAPI weapon : beams){
                    if(weapon.isFiring()) {
                        dissipationBuff += weapon.getFluxCostToFire() * (100f - SHIELD_MISSILE_FLUX)/100f;
                        firing = true;

                    }
                }
                for(WeaponAPI weapon : fluxRefunded.keySet()){
                    if(weapon.isFiring()){
                        if(!fluxRefunded.get(weapon)){
                            fluxRefunded.put(weapon, true);
                            firing = true;
                            flatFluxRefund += weapon.getFluxCostToFire() * (100f - SHIELD_MISSILE_FLUX)/100f;
                        }
                    } else if(!weapon.isInBurst()){
                        fluxRefunded.put(weapon, false);
                    }
                }
                float maxFluxRefund = ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux();
                ship.getFluxTracker().decreaseFlux(Math.min(maxFluxRefund, flatFluxRefund));
                ship.getMutableStats().getFluxDissipation().modifyFlat(ID, dissipationBuff);

                //ship.getMutableStats().getMissileRoFMult().modifyMult(ID, ROF_PENALTY);

            }




            public static boolean weaponIsSynergy(WeaponAPI weapon) {
                return weapon != null
                        && (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
                        && weapon.getSpec().getType() == WeaponAPI.WeaponType.ENERGY);
            }


            // ONLY weapons that are synergy but not missiles--i.e. energy weapons with synergy mount type
            public static List<WeaponAPI> getSynergy(ShipAPI carrier) {
                List<WeaponAPI> result = new ArrayList<WeaponAPI>();

                for (WeaponAPI weapon : carrier.getAllWeapons()) {
                    if (
                            weaponIsSynergy(weapon)
                    ) {
                        result.add(weapon);
                    }
                }

                return result;
            }

    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        //if (ship.getCaptain() == null || (ship.getCaptain().isDefault())) return;
        if (!ship.hasListenerOfClass(NA_AtomizeMod.class)) {
            ship.addListener(new NA_AtomizeMod(ship));
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

        stats.getMissileHealthBonus().modifyPercent(ID, SHIELD_MISSILE_HP);
        stats.getMissileRoFMult().modifyPercent(ID, MISSILE_ROF);
        stats.getMissileWeaponFluxCostMod().modifyPercent(ID, MISSILE_ROF);
    }





    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
