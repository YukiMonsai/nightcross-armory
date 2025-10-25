package yukimonsai.sicstargazer.skills.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class ECCCM extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }


    private static final float MISSILE_HP = 25f;
    private static final float MISSILE_FLARE = 20f;
    private static final float PD_RANGE = 100f;
    private static final float PD_DMG = 25f;
    private static final float MISSILE_RATE = 20f;
    private static final float MISSILE_RATE_NOREGEN = 0.1f;


    protected static float MIN_SMALL = 32;
    protected static float MIN_MED = 64;
    protected static float MIN_LARGE = 128;


    public static final String ID = "na_sic_ecccm";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s missile hitpoints", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(MISSILE_HP) + "%");
        tooltipMakerAPI.addPara("%s chance for missiles to ignore flares", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(MISSILE_FLARE) + "%");
        tooltipMakerAPI.addPara("%s range of point defense weapons", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(PD_RANGE) + "%");
        tooltipMakerAPI.addPara("%s damage to missiles and fighters", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(PD_DMG) + "%");

        tooltipMakerAPI.addSpacer(10f);

        tooltipMakerAPI.addPara("On ships with officers, missiles with 32/64/128 or more base max ammo (e.g swarmer, locust) recover %s of their total ammunition each 60 seconds. " +
                        "\nThis timer does not advance while the ship is overloaded or venting.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "" + (int)(MISSILE_RATE_NOREGEN * 100) + "%");


    }


    public static class NA_ECCCMSiCListener implements AdvanceableListener {
        protected ShipAPI ship;
        protected IntervalUtil timer = new IntervalUtil(60f, 60f);
        public NA_ECCCMSiCListener(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (ship.getFluxTracker().isOverloadedOrVenting()) amount = 0;
            timer.advance(amount);
            if (timer.intervalElapsed()) {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                        if ((weapon.getSize() == WeaponAPI.WeaponSize.SMALL
                                && weapon.getSpec().getMaxAmmo() >= MIN_SMALL)
                        || (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM
                                && weapon.getSpec().getMaxAmmo() >= MIN_MED)
                        || (weapon.getSize() == WeaponAPI.WeaponSize.LARGE
                                && weapon.getSpec().getMaxAmmo() >= MIN_LARGE)) {
                            weapon.getAmmoTracker().setAmmo((int) Math.min(weapon.getAmmoTracker().getMaxAmmo(),
                                    weapon.getAmmoTracker().getAmmo() + MISSILE_RATE_NOREGEN * weapon.getAmmoTracker().getMaxAmmo()));
                        }
                    }
                }


                timer.setElapsed(0f);
            }

        }

    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (ship.getCaptain() == null || (ship.getCaptain().getPortraitSprite().equals("graphics/portraits/portrait_generic_grayscale.png"))) return;
        if (!ship.hasListenerOfClass(NA_ECCCMSiCListener.class)) {
            ship.addListener(new NA_ECCCMSiCListener(ship));
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

        stats.getMissileHealthBonus().modifyPercent(id, MISSILE_HP);
        stats.getNonBeamPDWeaponRangeBonus().modifyFlat(id, PD_RANGE);
        stats.getBeamPDWeaponRangeBonus().modifyFlat(id, PD_RANGE);
        stats.getEccmChance().modifyPercent(id, MISSILE_FLARE);
        stats.getDamageToFighters().modifyPercent(id, PD_DMG);
        stats.getDamageToMissiles().modifyPercent(id, PD_DMG);



    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
