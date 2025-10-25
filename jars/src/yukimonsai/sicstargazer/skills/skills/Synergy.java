package yukimonsai.sicstargazer.skills.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class Synergy extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }


    private static final float BONUS_MISSILE = 40f;
    private static final float FLUX_COST_ENERGY = 20f;


    public static final String ID = "na_sic_synergy";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("Gain up to %s energy weapon flux cost and %s missile rate of fire and hitpoints, based on the ordnance point ratio with the other weapon type. Missile weapons boost energy weapons and vice versa." +
                        "\n Synergy weapons count as both.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(FLUX_COST_ENERGY) + "%",
                "+" + (int)(BONUS_MISSILE) + "%");


    }


    @Override
    public void advanceInCombat(SCData data, ShipAPI ship, Float amount) {
        float energyOP = 0;
        float missileOP = 0;
        if (ship.isAlive()) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY) {
                    energyOP += weapon.getSpec().getOrdnancePointCost(null);
                    missileOP += weapon.getSpec().getOrdnancePointCost(null);
                } else
                if (weapon.getType() == WeaponAPI.WeaponType.ENERGY) {
                    energyOP += weapon.getSpec().getOrdnancePointCost(null);
                } else
                if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                    missileOP += weapon.getSpec().getOrdnancePointCost(null);
                }
            }
        }

        if (missileOP + energyOP > 0) {
            ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent(ID, -missileOP / (energyOP + missileOP) * FLUX_COST_ENERGY);
            ship.getMutableStats().getMissileHealthBonus().modifyPercent(ID, energyOP / (energyOP + missileOP) * BONUS_MISSILE);
            ship.getMutableStats().getMissileRoFMult().modifyPercent(ID, energyOP / (energyOP + missileOP) * BONUS_MISSILE);
        } else {
            ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
            ship.getMutableStats().getMissileHealthBonus().unmodify(ID);
            ship.getMutableStats().getMissileWeaponDamageMult().unmodify(ID);
        }
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
}
