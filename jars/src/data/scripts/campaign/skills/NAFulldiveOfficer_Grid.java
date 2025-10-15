package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.combat.plugins.NA_CombatECMPlugin;
import data.scripts.stardust.NA_StargazerHull;
import data.scripts.stardust.NA_StargazerStardust;

import java.awt.*;

public class NAFulldiveOfficer_Grid extends NAFulldiveOfficer  {



    public static final float SHIELD_ARC_BONUS = 30f;
    //public static final float SHIELD_DMG_BONUS = 5f;
    public static final float FLUX_DISS = 20f;
    public static final float ECM = 2f;

    public static float SHIELD_SCALE = 20f;
    public static float EMP_SCALE_MAXAT = 20f;

    public static float EMP_VULNERABILITY = 50f;



    public static class EMPVuln implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getEmpDamageTakenMult().modifyPercent(id, EMP_VULNERABILITY);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getEmpDamageTakenMult().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "Also increases EMP damage taken by " + (int)(EMP_VULNERABILITY) + "%";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }


    public static class Level2 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getShieldArcBonus().modifyFlat(id, SHIELD_ARC_BONUS);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getShieldArcBonus().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "+" + (int)(SHIELD_ARC_BONUS) + " degrees to shield arc";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class Level4 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

            stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ECM);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

            stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "+" + (int)(ECM) + "% to ecm rating";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }


    public static class ECMBonus extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.addListener(new NA_GhostMatrixListener(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.removeListenerOfClass(NA_GhostMatrixListener.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getShieldDamageTakenMult().unmodify(ECM_BONUS_ID);
        }

        public String getEffectDescription(float level) {
            //return "+1-4" + "% to ECM rating of ships, depending on ship size";
            return null;//"Up to +" + (int)EMP_SCALE + "% damage to engines and weapons, depending on ECM advantage. Max at +10% net ECM rating.";
        }


        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);


            info.addPara("Up to %s shield damage taken, based on ECM advantage. Max at %s net ECM rating.", 0f, hc, hc,
                    "-" + (int)(SHIELD_SCALE) + "%", (int)(100*EMP_SCALE_MAXAT) + "%");
        }

        public String getEffectPerLevelDescription() {
            return null;
        }
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }

        public static String ECM_BONUS_ID = "na_fulldivegridecmbuff";

        public static class NA_GhostMatrixListener implements AdvanceableListener {
            protected ShipAPI ship;
            public NA_GhostMatrixListener(ShipAPI ship) {
                this.ship = ship;
            }

            public void advance(float amount) {
                CombatEngineAPI engine = Global.getCombatEngine();


                Integer [] player = getTotalAndMaximum(0);
                Integer [] enemy = getTotalAndMaximum(1);
                float pTotal = player[0];
                float eTotal = enemy[0];
                float ecmdmgboost = (int) Math.round(Math.max(0, Math.min(1f, (pTotal - eTotal)/EMP_SCALE_MAXAT)));

                if (ecmdmgboost < 0) ecmdmgboost = 0;

                ship.getMutableStats().getShieldDamageTakenMult().modifyMult(ECM_BONUS_ID, 1f - ecmdmgboost*SHIELD_SCALE/100f);

                String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
                if (ship == Global.getCombatEngine().getPlayerShip()) Global.getCombatEngine().maintainStatusForPlayerShip("NA_FulldiveGrid", icon, "Stargazer Grid",
                        "-" + ((int)(ecmdmgboost * SHIELD_SCALE)) +  "% shield damage taken due to ECM bonus", false);


            }


            //yoinked from ElectronicWarfareScript.java
            private Integer [] getTotalAndMaximum(int owner) {
                return NA_CombatECMPlugin.get(owner);
            }

        }

    }



    public static class Level5 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.addListener(new NA_GhostGridListener(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.removeListenerOfClass(NA_GhostGridListener.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getFluxDissipation().unmodify(BONUS_ID);
        }

        public String getEffectDescription(float level) {
            return null;//"+" + (int)(OVERLOAD_RATE) + "% flux dissipation while overloaded, +" + (int)(VENT_RATE) + "% while venting";
        }

        Color hc2 = new Color(255, 75, 125);
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);


            info.addPara("\nIf this ship has a %s, gain up to +" + (int)(FLUX_DISS) + "%% flux dissipation depending on flux level, max at 100%% flux.", 0f, hc, NA_StargazerHull.STARGAZER_RED,
                    "Stardust Nebula");
        }
        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }


        public static String BONUS_ID = "na_fulldiveghost_grid_buff";

        public static class NA_GhostGridListener implements AdvanceableListener {
            protected ShipAPI ship;
            public NA_GhostGridListener(ShipAPI ship) {
                this.ship = ship;
            }

            public void advance(float amount) {
                CombatEngineAPI engine = Global.getCombatEngine();

                if (NA_StargazerStardust.getSwarmFor(ship) != null)
                    ship.getMutableStats().getFluxDissipation().modifyPercent(BONUS_ID, ship.getFluxLevel() * FLUX_DISS);
                else
                    ship.getMutableStats().getFluxDissipation().unmodify(BONUS_ID);
            }
        }
    }

}

