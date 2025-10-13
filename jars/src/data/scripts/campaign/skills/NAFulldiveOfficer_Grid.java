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

import java.awt.*;

public class NAFulldiveOfficer_Grid extends NAFulldiveOfficer  {



    public static final float SHIELD_ARC_BONUS = 30f;
    public static final float SHIELD_DMG_BONUS = 5f;
    public static final float FLUX_DISS = 20f;
    public static final float ECM = 2f;


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
    public static class Level3 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getShieldDamageTakenMult().modifyMult(id, 1f - (0.01f * SHIELD_DMG_BONUS));
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getShieldDamageTakenMult().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "-" + (int)(SHIELD_DMG_BONUS) + "% shield damage taken.";
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
            return "+" + (int)(ECM) + " to ecm rating";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
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


            info.addPara("Up to +%s flux dissipation depending on flux level, max at %s flux.", 0f, hc, hc,
                    "" + (int)(FLUX_DISS) + "%", "100%");
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

                ship.getMutableStats().getFluxDissipation().modifyPercent(BONUS_ID, ship.getFluxLevel() * FLUX_DISS);

            }
        }
    }

}

