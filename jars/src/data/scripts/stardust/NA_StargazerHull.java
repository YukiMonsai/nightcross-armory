package data.scripts.stardust;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class NA_StargazerHull extends NA_StargazerStars {

    protected final String ID = "na_stargazerhullmod";


    public NA_StargazerHull() {
        super();
    }

    public static Color STARGAZER_RED = new Color(202, 28, 62);


    public static float MODULE_DAMAGE_TAKEN_MULT = 0.5f;
    public static float ZERO_FLUX_BOOST = 20f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEngineDamageTakenMult().modifyMult(id, MODULE_DAMAGE_TAKEN_MULT);
        stats.getWeaponDamageTakenMult().modifyMult(id, MODULE_DAMAGE_TAKEN_MULT);
        stats.getZeroFluxSpeedBoost().modifyFlat(id, ZERO_FLUX_BOOST);
        stats.getAllowZeroFluxAtAnyLevel().modifyFlat(id, 1f);
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
        tooltip.addPara("- Weapon and engine damage taken is reduced by %s."
                        + "\n- Zero-flux speed boost increased by %s and is allowed at any level, as long"
                        + " as the ship isn't generating flux.",
                opad, h,
                "" + (int) Math.round((1f - MODULE_DAMAGE_TAKEN_MULT) * 100f) + "%",
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
