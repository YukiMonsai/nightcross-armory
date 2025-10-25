package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class EncoreDance extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships with officers";
    }

    private static final float MISSILE_RELOAD_PERC = .30f;
    private static final float RELOAD_PERC = 1.0f;
    private static final float SIZE_SCALE = .25f;


    public static final String ID = "na_sic_encore";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("After disabling an enemy ship, reload %s of base ammunition for missile weapons, and %s of base ammunition for ballistic and energy weapons.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(MISSILE_RELOAD_PERC * 100) + "%",
                "" + (int)(RELOAD_PERC * 100) + "%");

        tooltipMakerAPI.addSpacer(10f);

        tooltipMakerAPI.addPara("If the destroyed ship was of a smaller size class, the bonuses are reduced by %s per size difference. Partial missiles are converted into a chance to regenerate an extra missile.",
                0f, Misc.getGrayColor(), Misc.getGrayColor(),
                "" + (int)(SIZE_SCALE * 100) + "%");

    }


    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (ship.getCaptain() == null || (ship.getCaptain().getPortraitSprite().equals("graphics/portraits/portrait_generic_grayscale.png"))) return;


        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        var listeners = engine.getListenerManager().getListeners(NA_EncoreDMGListener.class);
        boolean found = false;

        for (NA_EncoreDMGListener listener : listeners) {
            if (listener.side == ship.getOwner()) {
                found = true;
                return;
            }
        }
        engine.getListenerManager().addListener(new NA_EncoreDMGListener(ship.getOwner()));
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {


    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/


    protected class NA_EncoreDMGListener implements HullDamageAboutToBeTakenListener {
        public NA_EncoreDMGListener(int side) {
            this.side = side;
        }
        int side= 0;
        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {

            if (param instanceof ShipAPI killer) {
                //if (param != pilotedShip) return false
                if (ship.isFighter()) return false;
                if (ship.getOwner() == side) return false;
                if (killer.getCaptain() == null || (killer.getCaptain().getPortraitSprite().equals("graphics/portraits/portrait_generic_grayscale.png"))) return false; // MUST have officer
                if (ship.getHitpoints() <= 0 && !ship.hasTag("sc_na_encore_counted")) {
                    ship.addTag("sc_na_encore_counted");

                    // do the reload
                    float sizeDifference = 0;
                    if (killer.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                        if (ship.getHullSize() == ShipAPI.HullSize.CRUISER) sizeDifference = 1;
                        else
                        if (ship.getHullSize() == ShipAPI.HullSize.DESTROYER) sizeDifference = 2;
                        else
                        if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE) sizeDifference = 3;
                    } else
                    if (killer.getHullSize() == ShipAPI.HullSize.CRUISER) {
                        if (ship.getHullSize() == ShipAPI.HullSize.DESTROYER) sizeDifference = 1;
                        else
                        if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE) sizeDifference = 2;
                    } else
                    if (killer.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                        if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE) sizeDifference = 1;
                    }

                    if (killer == Global.getCombatEngine().getPlayerShip()) {
                        Global.getSoundPlayer().playSound("system_forgevats", 1f, 0.5f, killer.getLocation(), Misc.ZERO);
                    }

                    float scale = 1f - sizeDifference * SIZE_SCALE;
                    var weapons = killer.getAllWeapons();
                    for (WeaponAPI weapon : weapons) {
                        if (weapon.getType() == WeaponAPI.WeaponType.ENERGY
                                || weapon.getType() == WeaponAPI.WeaponType.BALLISTIC) {
                            float ammo_min = weapon.getSpec().getMaxAmmo() * scale * RELOAD_PERC;
                            float remainder = (float) (ammo_min - Math.floor(ammo_min));
                            ammo_min = (float) Math.floor(ammo_min);

                            int bonus = remainder > 0 ? (Math.random() < remainder ? 1 : 0) : 0;

                            weapon.getAmmoTracker().setAmmo((int) Math.min(weapon.getAmmoTracker().getMaxAmmo(),
                                    weapon.getAmmoTracker().getAmmo() + ammo_min + bonus));
                        }
                        else
                        if (weapon.getType() == WeaponAPI.WeaponType.MISSILE
                                || weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY) {
                            float ammo_min = weapon.getSpec().getMaxAmmo() * scale * MISSILE_RELOAD_PERC;
                            float remainder = (float) (ammo_min - Math.floor(ammo_min));
                            ammo_min = (float) Math.floor(ammo_min);

                            int bonus = remainder > 0 ? (Math.random() < remainder ? 1 : 0) : 0;

                            weapon.getAmmoTracker().setAmmo((int) Math.min(weapon.getAmmoTracker().getMaxAmmo(),
                                    weapon.getAmmoTracker().getAmmo() + ammo_min + bonus));
                        }
                    }
                    //stacks.add(MomentumStacks(duration))

                    /*var existing = ship.getListeners(NA_EncorseAdvanceListener.class).stream().findFirst();

                    if (!existing.isEmpty()) {
                        existing.get().duration = maxTime;
                    } else {
                        doer.addListener(new NA_EncorseAdvanceListener(doer, maxTime))
                    }*/

                }
            }

            return false;
        }

    }


}

