package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.ElectronicWarfareScript;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.plugins.NA_CombatECMPlugin;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class Science extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships";
    }

    public static float MALFUNCTIONMOD = -50f;
    public static float ECMPER_SHIP_BONUS = 1f;

    private static final float DAMAGE_NERF_MULT = 0.5f;
    private static final float EMP_SCALE_MAXAT = ElectronicWarfareScript.BASE_MAXIMUM / DAMAGE_NERF_MULT;



    public static final String ID = "na_sic_science";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("Every deployed ship contributes %s to the ECM rating* of the fleet**", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(ECMPER_SHIP_BONUS) + "%");
        tooltipMakerAPI.addPara("While the flagship is deployed, having an ECM advantage over the enemy fleet decreases the enemy fleet's energy and ballistic weapon damage***", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(ECMPER_SHIP_BONUS) + "%");

        tooltipMakerAPI.addSpacer(10f);

        int max = (int) ElectronicWarfareScript.BASE_MAXIMUM;

        tooltipMakerAPI.addPara("*Enemy weapon range is reduced by the total ECM rating of your deployed ships, " +
                "up to a maximum of %s. This penalty is reduced by the ratio " +
                "of the enemy ECM rating to yours." + "Does not apply to fighters, affects all weapons including missiles.", 0f, Misc.getGrayColor(), Misc.getHighlightColor(),
                (int)(max) + "%"
                );
        tooltipMakerAPI.addPara("**This effect does not apply to ships with the 'Safety Overrides' hullmod", 0f, Misc.getGrayColor(), Misc.getGrayColor());
        tooltipMakerAPI.addPara("***Enemy ballistic and energy weapon damage is reduced by %s of the total ECM rating of your deployed ships, " +
                        "up to a maximum of %s. This penalty is reduced by the ratio " +
                        "of the enemy ECM rating to yours.", 0f, Misc.getGrayColor(), Misc.getHighlightColor(),

                (int)(DAMAGE_NERF_MULT * 100f) + "%",
                (int)(max) + "%"

        );

    }

    @Override
    public void advanceInCombat(SCData data, ShipAPI ps, Float amount) {
        if (ps == Global.getCombatEngine().getPlayerShip()) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                float max = (int) ElectronicWarfareScript.BASE_MAXIMUM;
                int enemySide = data.isPlayer() ? 1 : 0;
                Integer [] player = getTotalAndMaximum(data.isPlayer() ? 0 : 1);
                Integer [] enemy = getTotalAndMaximum(enemySide);
                float pTotal = player[0];
                float eTotal = enemy[0];
                float ecmdmgnerf = Math.max(0, Math.min(1f, (pTotal - eTotal)/EMP_SCALE_MAXAT));

                if (ecmdmgnerf < 0) ecmdmgnerf = 0;

                if (ecmdmgnerf > 0)
                    for (DeployedFleetMemberAPI member : engine.getFleetManager(enemySide).getDeployedCopyDFM()) {
                        ShipAPI ship = member.getShip();
                        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult(ID, 1f - 0.01f * ecmdmgnerf * max);
                        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(ID, 1f - 0.01f * ecmdmgnerf * max);

                        String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
                        if (ship == Global.getCombatEngine().getPlayerShip()) Global.getCombatEngine().maintainStatusForPlayerShip(ID, icon, "Scientific (dis)Advantage",
                                ((int)(ecmdmgnerf * max)) +  "% energy and ballistic damage due to enemy ECM bonus", false);
                    }

            }
        }
    }

    //yoinked from ElectronicWarfareScript.java
    private Integer [] getTotalAndMaximum(int owner) {
        return NA_CombatECMPlugin.get(owner);
    }
    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {

        if (!variant.hasHullMod(HullMods.SAFETYOVERRIDES)) {

            stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ECMPER_SHIP_BONUS);
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
