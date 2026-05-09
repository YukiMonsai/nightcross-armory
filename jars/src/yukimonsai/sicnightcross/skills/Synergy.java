package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
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
        tooltipMakerAPI.addPara("Gain up to %s energy weapon flux cost and %s missile rate of fire and hitpoints, based on the ordnance point ratio with the other weapon type." +
                        "\n Synergy weapons count as both.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(FLUX_COST_ENERGY) + "%",
                "+" + (int)(BONUS_MISSILE) + "%");

        tooltipMakerAPI.addSpacer(10f);

        tooltipMakerAPI.addPara("For example, 100 OP of missile weapons and 50 OP of energy weapons grants %s energy weapon flux cost and %s missile fire rate and hitpoints.", 0f, Misc.getGrayColor(), Misc.getNegativeHighlightColor(),
                "-" + (int)(FLUX_COST_ENERGY * 0.67) + "%",
                "+" + (int)(BONUS_MISSILE * 0.33) + "%" );

    }


    @Override
    public void advanceInCombat(SCData data, ShipAPI ship, Float amount) {

    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {

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
            ship.getMutableStats().getMissileRoFMult().unmodify(ID);
        }
    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
