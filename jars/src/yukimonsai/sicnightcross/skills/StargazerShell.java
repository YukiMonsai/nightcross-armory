package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

import java.awt.*;

public class StargazerShell extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all automated ships";
    }


    private static final float EMP_BUFF = 20f;
    private static final float TAKEN_BUFF = 20f;
    private static final float REPAIRTIME_PEN = 25f;


    public static final String ID = "na_sic_shell";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("%s EMP damage taken", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(100f * EMP_BUFF) + "%");
        tooltipMakerAPI.addPara("%s weapon and engine damage taken", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                "+" + (int)(100f * TAKEN_BUFF) + "%");
        tooltipMakerAPI.addPara("%s weapon and engine repair time", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(),
                "+" + (int)(100f * REPAIRTIME_PEN) + "%");
        tooltipMakerAPI.addPara("Automated ships are more likely to receive d-mods after being disabled in combat", 0f, Misc.getHighlightColor(), Misc.getHighlightColor());
        tooltipMakerAPI.addPara("Automated ships are almost always recoverable if lost in combat", 0f, Misc.getHighlightColor(), Misc.getHighlightColor());


    }


    @Override
    public void callEffectsFromSeparateSkill(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        if (Misc.isAutomated(stats)) {
            stats.getDynamic().getMod(Stats.DMOD_ACQUIRE_PROB_MOD).modifyMult(id, 1.5f);
            stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 2f);
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
        if (Misc.isAutomated(variant)) {
            stats.getEmpDamageTakenMult().modifyMult(ID, 1f - 0.01f * EMP_BUFF);
            stats.getWeaponDamageTakenMult().modifyMult(ID, 1f - 0.01f * TAKEN_BUFF);
            stats.getEngineDamageTakenMult().modifyMult(ID, 1f - 0.01f * TAKEN_BUFF);
            stats.getCombatWeaponRepairTimeMult().modifyMult(ID, 1f + 0.01f * TAKEN_BUFF);
            stats.getCombatEngineRepairTimeMult().modifyMult(ID, 1f + 0.01f * TAKEN_BUFF);
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
