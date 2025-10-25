package data.scripts.stardust;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NAModPlugin;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCAptitudeSpec;

import java.awt.*;

public class NA_StargazerHull extends NA_StargazerStars {

    protected final String ID = "na_stargazerhullmod";


    public NA_StargazerHull() {
        super();
    }

    public static Color STARGAZER_RED = new Color(202, 28, 62);


    public static float MODULE_DAMAGE_TAKEN_MULT = 0.5f;
    public static float FLUX_DISS_PER_DMOD = 0.03f;
    public static float DMOD_EFFECT = 0.25f;
    public static float CR_PER_DMOD = 0.02f;
    public static float ZERO_FLUX_BOOST = 20f;
    public static float MAX_DMOD = 5f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

        float dmods = 0;
        if (stats.getFleetMember() != null) {
            dmods = DModManager.getNumDMods(stats.getFleetMember().getVariant());
        }
        if (stats.getVariant() != null) {
            dmods = DModManager.getNumDMods(stats.getVariant());
        }
        if (dmods > MAX_DMOD) dmods = MAX_DMOD;
        //stats.getEngineDamageTakenMult().modifyMult(id, MODULE_DAMAGE_TAKEN_MULT);
        //stats.getWeaponDamageTakenMult().modifyMult(id, MODULE_DAMAGE_TAKEN_MULT);
        stats.getMaxCombatReadiness().modifyMult(id, 1f + CR_PER_DMOD * dmods, "Stargazer");
        stats.getFluxDissipation().modifyMult(id, 1f + FLUX_DISS_PER_DMOD * dmods);
        stats.getFluxCapacity().modifyMult(id, 1f + FLUX_DISS_PER_DMOD * dmods);
        stats.getZeroFluxSpeedBoost().modifyFlat(id, ZERO_FLUX_BOOST);
        stats.getAllowZeroFluxAtAnyLevel().modifyFlat(id, 1f);
        stats.getDynamic().getMod(Stats.DMOD_EFFECT_MULT).modifyMult(id, 1f - DMOD_EFFECT);

        // hidden stat bonus boo
        // supposed to simulate derelict operations
        // this is high tech ships with derelict ops :toocool:
        boolean shouldGiveFakeDerelictOps = true;
        if (NAModPlugin.hasSiC) {
            if (stats.getFleetMember() != null && stats.getFleetMember().getFleetData() != null && stats.getFleetMember().getFleetData().getFleet() != null
                    && stats.getFleetMember().getFleetData().getFleet() != null) {

                SCData data = SCUtils.getFleetData(stats.getFleetMember().getFleetData().getFleet());
                if (data.hasAptitudeInFleet("sc_improvisation")) shouldGiveFakeDerelictOps = false;
            }
        }
        if (shouldGiveFakeDerelictOps && stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(1f) > 0.99f) {
            if (stats.getFleetMember() != null && stats.getFleetMember().getFleetData() != null && stats.getFleetMember().getFleetData().getFleet() != null
                    && stats.getFleetMember().getFleetData().getFleet().getFaction() != null && stats.getFleetMember().getFleetData().getFleet().getFaction().getId().equals(NightcrossID.FACTION_STARGAZER)) {

                stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 1f/(1f + dmods * 0.05f));
                stats.getSuppliesPerMonth().modifyMult(id, 1f/(1f + dmods * 0.05f));
            }
        }

    }


    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color t = Misc.getTextColor();
        Color g = Misc.getGrayColor();

        tooltip.addPara("A frozen soul cast out from this world. What then, is there to do"
                + " but watch the flames burn out, one by one...", STARGAZER_RED, opad);

        tooltip.addSectionHeading("Campaign", Alignment.MID, opad);
        tooltip.addPara("Sensor profile reduced by %s.", opad, h, "50%");

        tooltip.addSectionHeading("Combat", Alignment.MID, opad);
        tooltip.addPara("- %s flux dissipation and capacity per d-mod (max 5)."
                        + "\n- %s max combat readiness per d-mod (max 5)."
                        + "\n- %s effects of d-mods."
                        //+ "\n- Weapon and engine damage taken is reduced by %s."
                        + "\n- Zero-flux speed boost increased by %s and is allowed at any level, as long"
                        + " as the ship isn't generating flux.",
                opad, h,
                "+" + (int) Math.round((FLUX_DISS_PER_DMOD) * 100f) + "%",
                "+" + (int) Math.round((CR_PER_DMOD) * 100f) + "%",
                "-" + (int) Math.round((DMOD_EFFECT) * 100f) + "%",
                //"" + (int) Math.round((MODULE_DAMAGE_TAKEN_MULT) * 100f) + "%",
                "" + (int) Math.round(ZERO_FLUX_BOOST));


    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
    public float getTooltipWidth() {
        return super.getTooltipWidth();
    }
}
