package yukimonsai.sicstargazer.skills.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class CR extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }


    private static final float ENERGY_RANGE = 0.08f;
    private static final float BALLISTIC_DMG = 0.05f;
    private static final float BALLISTIC_RECOIL = 0.15f;
    private static final float PROJSPEED = 0.15f;


    public static final String ID = "na_sic_targeting";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s energy weapon range", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(ENERGY_RANGE * 100) + "%");
        tooltipMakerAPI.addPara("%s ballistic weapon damage", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(BALLISTIC_DMG * 100) + "%");
        tooltipMakerAPI.addPara("%s weapon recoil", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "-" + (int)(BALLISTIC_RECOIL * 100) + "%");
        tooltipMakerAPI.addPara("%s projectile speed", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(PROJSPEED * 100) + "%");
    }



    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
        stats.getEnergyWeaponRangeBonus().modifyPercent(ID, ENERGY_RANGE);
        stats.getBallisticWeaponDamageMult().modifyPercent(ID, BALLISTIC_DMG);
        stats.getRecoilPerShotMult().modifyPercent(ID, -BALLISTIC_RECOIL);
        stats.getRecoilDecayMult().modifyPercent(ID, BALLISTIC_RECOIL);
        stats.getEnergyProjectileSpeedMult().modifyPercent(ID, PROJSPEED);
        stats.getBallisticProjectileSpeedMult().modifyPercent(ID, PROJSPEED);



    }

    /*
    @Override
    public void onActivation(SCData data) {
    }

    @Override
    public void onDeactivation(SCData data) {
    }*/
}
