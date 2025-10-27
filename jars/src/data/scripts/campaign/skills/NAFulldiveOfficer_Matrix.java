package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.combat.plugins.NA_CombatECMPlugin;
import data.scripts.stardust.NA_StargazerStardust;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;

import static data.scripts.stardust.NA_StargazerHull.STARGAZER_RED;

public class NAFulldiveOfficer_Matrix extends NAFulldiveOfficer {

    public static float RANGE_BONUS = 15f;
    public static float RANGE_EXTRA = 25f;
    public static float ZERO_FLUX_PEN = -25f;
    public static float HARDFLUX_DISS = 0.01f;
    public static float EMP_SCALE_MAXAT = 20f;
    public static float ECM = 3f;


    public static class ECMBuff implements ShipSkillEffect {
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

    public static class RangeBoostDeprecated implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
            stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getBallisticWeaponRangeBonus().unmodify(id);
            stats.getEnergyWeaponRangeBonus().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "+" + (int)(RANGE_BONUS) + "% ballistic and energy weapon range";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }




    public static class Penalty implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            if (stats.getEntity() instanceof ShipAPI ship) {
                NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
                if (swarm != null)
                    stats.getZeroFluxSpeedBoost().modifyPercent(id, ZERO_FLUX_PEN);
            }
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getZeroFluxSpeedBoost().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "Also decreases zero flux bonus by " + (int)(ZERO_FLUX_PEN) + "%";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }


    public static class NebulaBonus extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
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
            stats.getEnergyWeaponRangeBonus().unmodify(NEBULA_BONUS_ID);
            stats.getBallisticWeaponRangeBonus().unmodify(NEBULA_BONUS_ID);
        }

        public String getEffectDescription(float level) {
            //return "+1-4" + "% to ECM rating of ships, depending on ship size";
            return null;//"Up to +" + (int)EMP_SCALE + "% damage to engines and weapons, depending on ECM advantage. Max at +10% net ECM rating.";
        }


        Color hc2 = new Color(255, 75, 138);
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            info.addPara("\nIf this shielded ship has a %s and at least %s, consumes %s per second while hard flux is over %s, to convert %s current hard flux into soft flux.", 0f, hc, STARGAZER_RED,
                    "Stardust Nebula", "10 Stardust", "4 Stardust", "50%", "4%");
        }

        public String getEffectPerLevelDescription() {
            return null;
        }
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }

        public static String NEBULA_BONUS_ID = "na_fulldivematrixnebulabuff";

        public static class NA_GhostMatrixListener implements AdvanceableListener {
            protected ShipAPI ship;
            protected IntervalUtil timer = new IntervalUtil(0.2f, 0.3f);
            public NA_GhostMatrixListener(ShipAPI ship) {
                this.ship = ship;
            }

            public void advance(float amount) {
                CombatEngineAPI engine = Global.getCombatEngine();

                if (timer.intervalElapsed() && ship.getShield() != null && ship.getShield().isOn()) {
                    if (ship.getHardFluxLevel() > 0.5f) {
                        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
                        if (swarm != null && swarm.getNumActiveMembers() >= 10) {
                            timer.randomize();
                            timer.setElapsed(0f);

                            NA_StargazerStardust.SwarmMember fragment = null;
                            float maxDist = 0;
                            WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = swarm.getPicker(true, true);
                            while (!picker.isEmpty()) {
                                NA_StargazerStardust.SwarmMember p = picker.pickAndRemove();
                                float dist = Misc.getDistance(p.loc, swarm.getAttachedTo().getLocation());
                                if (swarm.params.generateOffsetAroundAttachedEntityOval) {
                                    dist -= Misc.getTargetingRadius(p.loc, swarm.getAttachedTo(), false) + swarm.params.maxOffset - ship.getCollisionRadius()*2 * 0.5f;
                                }
                                if (dist > ship.getCollisionRadius()*2) continue;
                                dist = Misc.getDistance(p.loc, MathUtils.getPointOnCircumference(ship.getShieldCenterEvenIfNoShield(),
                                        ship.getShieldRadiusEvenIfNoShield(), ship.getShield().getFacing()));
                                if (dist > maxDist) {
                                    fragment = p;
                                    maxDist = dist;
                                }
                            }




                            if (fragment != null) {
                                swarm.removeMember(fragment);
                                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                                params.flickerRateMult = 0.5f;
                                EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
                                        fragment.loc, ship, MathUtils.getPointOnCircumference(ship.getShieldCenterEvenIfNoShield(),
                                                ship.getShieldRadiusEvenIfNoShield(),
                                                ship.getShield().getFacing()
                                                        + MathUtils.getRandomNumberInRange(-5f - ship.getShield().getActiveArc()*0.5f, 5f + ship.getShield().getActiveArc()*0.5f)),
                                        ship, 15f,
                                        ship.getShield().getInnerColor(),
                                        new Color(255, 222, 234, 150), params
                                );
                                arc.setSingleFlickerMode(true);
                                float current = ship.getFluxTracker().getHardFlux();
                                ship.getFluxTracker().setHardFlux(current * (1f - HARDFLUX_DISS));
                                ship.getFluxTracker().setCurrFlux(current);
                            }
                        }
                    }
                } else {
                    timer.advance(amount);
                }

            }

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
            stats.getEnergyWeaponRangeBonus().unmodify(ECM_BONUS_ID);
            stats.getBallisticWeaponRangeBonus().unmodify(ECM_BONUS_ID);
        }

        public String getEffectDescription(float level) {
            //return "+1-4" + "% to ECM rating of ships, depending on ship size";
            return null;//"Up to +" + (int)EMP_SCALE + "% damage to engines and weapons, depending on ECM advantage. Max at +10% net ECM rating.";
        }


        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);


            info.addPara("Up to %s additional weapon range, based on ECM advantage. Max at %s net ECM rating.", 0f, hc, hc,
                    "+" + (int)(RANGE_EXTRA) + "%", (int)(100*EMP_SCALE_MAXAT) + "%");
        }

        public String getEffectPerLevelDescription() {
            return null;
        }
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }

        public static String ECM_BONUS_ID = "na_fulldivematrixecmbuff";

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
                float ecmdmgboost = Math.max(0, Math.min(1f, (pTotal - eTotal)/EMP_SCALE_MAXAT));

                if (ecmdmgboost < 0) ecmdmgboost = 0;

                ship.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*RANGE_EXTRA/100f);
                ship.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*RANGE_EXTRA/100f);

                String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
                if (ship == Global.getCombatEngine().getPlayerShip()) Global.getCombatEngine().maintainStatusForPlayerShip("NA_FulldiveMatrix", icon, "Stargazer Matrix",
                        ((int)(ecmdmgboost * RANGE_EXTRA)) +  "% energy and ballistic range due to ECM bonus", false);


            }


            //yoinked from ElectronicWarfareScript.java
            private Integer [] getTotalAndMaximum(int owner) {
                return NA_CombatECMPlugin.get(owner);
            }

        }

    }
}

