package yukimonsai.sicnightcross.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.ElectronicWarfareScript;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.plugins.NA_CombatECMPlugin;
import data.scripts.stardust.NA_StargazerHull;
import data.scripts.weapons.NA_RKKVRenderer;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicUI;
import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class StargazerECM extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships with AI cores";
    }

    public static float MALFUNCTIONMOD = 0.1f;
    public static float ENGINE_MALFUNCTIONMOD = 0.01f;
    public static float ECMPER_SHIP_BONUS = 1f;
    public static float AURA_SIZE = 1000f;


    private static final float EMP_SCALE_MAXAT = ElectronicWarfareScript.BASE_MAXIMUM;



    public static final String ID = "na_sic_ecm";

    @Override
    public void addTooltip(SCData scData, TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addPara("Each AI core deployed contributes %s to the ECM rating* of the fleet*", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                (int)(ECMPER_SHIP_BONUS) + "%");
        tooltipMakerAPI.addPara("Grants a 1000 su aura to the flagship that causes malfunctions based on the fleet's ECM advantage (maximum effect at 10%% advantage).", 0f, Misc.getHighlightColor(), Misc.getHighlightColor());

        tooltipMakerAPI.addSpacer(10f);

        int max = (int) ElectronicWarfareScript.BASE_MAXIMUM;

        tooltipMakerAPI.addPara("*Enemy weapon range is reduced by the total ECM rating of your deployed ships, " +
                "up to a maximum of %s. This penalty is reduced by the ratio " +
                "of the enemy ECM rating to yours." + "Does not apply to fighters, affects all weapons including missiles.", 0f, Misc.getGrayColor(), Misc.getHighlightColor(),
                (int)(max) + "%"
                );

    }

    @Override
    public void advanceInCombat(SCData data, ShipAPI ps, Float amount) {
        if (layerRenderer == null || layerRenderer.engine != Global.getCombatEngine()) {
            if (layerRenderer != null)
                layerRenderer.engine = null;
            // make sure old one gets disposed

            layerRenderer = new StargazerECMRenderer(Global.getCombatEngine());
            Global.getCombatEngine().addLayeredRenderingPlugin(layerRenderer);
        }

        if (data.getFleet().getFlagship() != null && ps.getFleetMember() != null && data.getFleet().getFlagship().getId().equals(ps.getFleetMember().getId())) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                int enemySide = data.isPlayer() ? 1 : 0;
                Integer [] player = getTotalAndMaximum(data.isPlayer() ? 0 : 1);
                Integer [] enemy = getTotalAndMaximum(enemySide);
                float pTotal = player[0];
                float eTotal = enemy[0];
                float ecmdmgnerf = Math.max(0, Math.min(1f, (pTotal - eTotal)/EMP_SCALE_MAXAT));

                if (ecmdmgnerf < 0) ecmdmgnerf = 0;

                if (ecmdmgnerf > 0) {
                    if (layerRenderer != null) {
                        layerRenderer.toRender.put(ps, ps.getOwner());
                    }
                    for (DeployedFleetMemberAPI member : engine.getFleetManager(enemySide).getDeployedCopyDFM()) {
                        ShipAPI ship = member.getShip();
                        float distSquared = Math.max(100f, MathUtils.getDistanceSquared(ps.getLocation(), ship.getLocation()) - ship.getCollisionRadius() * ship.getCollisionRadius());
                        float mag = 0f;
                        if (distSquared < 1000000) {
                            mag = (float) Math.max(0.5f, 1f - Math.sqrt(distSquared)/AURA_SIZE);
                        }

                        if (mag == 0) continue;

                        ship.getMutableStats().getWeaponMalfunctionChance().modifyFlat(ID, mag * 0.01f * ecmdmgnerf * MALFUNCTIONMOD);
                        ship.getMutableStats().getEngineDamageTakenMult().modifyMult(ID, mag * 1f - 0.01f * ecmdmgnerf * ENGINE_MALFUNCTIONMOD);

                        String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
                        if (ship == Global.getCombatEngine().getPlayerShip())
                            Global.getCombatEngine().maintainStatusForPlayerShip(ID, icon, "WARNING: COGNITOHAZARD",
                                    ((int) (ecmdmgnerf * MALFUNCTIONMOD)) + "% weapon malfunction chance" +
                                            "\n" + ((int) (ecmdmgnerf * MALFUNCTIONMOD)) + "% engine malfunction chance", false);
                    }
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

        if (stats.getFleetMember() != null && stats.getFleetMember().getCaptain() != null && stats.getFleetMember().getCaptain().isAICore()) {
            stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ECMPER_SHIP_BONUS);
        }

    }

    public static StargazerECMRenderer layerRenderer;

    @Override
    public void advance(SCData data, Float amunt) {
        if (layerRenderer != null && (layerRenderer.engine != Global.getCombatEngine() || Global.getCombatEngine() == null)) {
            layerRenderer.engine = null;
            layerRenderer = null;
            // end
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
        if (layerRenderer == null || layerRenderer.engine != Global.getCombatEngine()) {
            if (layerRenderer != null)
                layerRenderer.engine = null;
            // make sure old one gets disposed

            layerRenderer = new StargazerECMRenderer(Global.getCombatEngine());
            Global.getCombatEngine().addLayeredRenderingPlugin(layerRenderer);
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
