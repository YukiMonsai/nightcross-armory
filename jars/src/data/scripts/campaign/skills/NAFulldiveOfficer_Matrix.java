package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.BattleObjectives;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.campaign.skills.ElectronicWarfareScript;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.stardust.NA_StargazerStardust;
import data.scripts.stardust.NA_StargazerStars;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

import static data.scripts.stardust.NA_StargazerHull.STARGAZER_RED;

public class NAFulldiveOfficer_Matrix extends NAFulldiveOfficer {

    public static float RANGE_BONUS = 15f;
    public static float RANGE_EXTRA = 15f;
    public static float ZERO_FLUX_PEN = -50f;
    public static float EMP_SCALE = 100f;
    public static float EMP_SCALE_MAXAT = 0.1f;
    public static float HARDFLUX_DISS = 0.01f;



    public static class Level2 implements ShipSkillEffect {
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




    public static class Level4 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getZeroFluxSpeedBoost().modifyPercent(id, ZERO_FLUX_PEN);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getZeroFluxSpeedBoost().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "" + (int)(ZERO_FLUX_PEN) + " to zero flux bonus";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }


    public static class Level3 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
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
            stats.getDamageToTargetWeaponsMult().unmodify(ECM_BONUS_ID);
            stats.getDamageToTargetEnginesMult().unmodify(ECM_BONUS_ID);
            stats.getEnergyWeaponRangeBonus().unmodify(ECM_BONUS_ID);
            stats.getBallisticWeaponRangeBonus().unmodify(ECM_BONUS_ID);
        }

        public String getEffectDescription(float level) {
            //return "+1-4" + "% to ECM rating of ships, depending on ship size";
            return null;//"Up to +" + (int)EMP_SCALE + "% damage to engines and weapons, depending on ECM advantage. Max at +10% net ECM rating.";
        }

        Color hc2 = new Color(255, 75, 138);
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);


            info.addPara("Up to %s damage to engines and weapons, and %s additional weapon range, based on ECM advantage. Max at %s net ECM rating.", 0f, hc, hc,
                    "+" + (int)(EMP_SCALE) + "%", "+" + (int)(RANGE_EXTRA) + "%", (int)(100*EMP_SCALE_MAXAT) + "%");
            info.addPara("\nIf this shielded ship has a %s and at least %s, consumes %s per second while hard flux is over %s, to convert %s current hard flux into soft flux.", 0f, hc, STARGAZER_RED,
                    "Stardust Nebula", "10 Stardust", "4 Stardust", "50%", "4%");
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
                                ship.getFluxTracker().setHardFlux(current * 0.99f);
                                ship.getFluxTracker().setCurrFlux(current);
                            }
                        }
                    }
                } else {
                    timer.advance(amount);
                }

                int [] player = getTotalAndMaximum(engine.getFleetManager(0));
                int [] enemy = getTotalAndMaximum(engine.getFleetManager(1));
                float pTotal = player[0];
                float eTotal = enemy[0];
                float ecmdmgboost = (int) Math.round(Math.max(0, Math.min(1f, (pTotal - eTotal)/EMP_SCALE_MAXAT)));

                if (ecmdmgboost < 0) ecmdmgboost = 0;

                ship.getMutableStats().getDamageToTargetWeaponsMult().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*EMP_SCALE/100f);
                ship.getMutableStats().getDamageToTargetEnginesMult().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*EMP_SCALE/100f);
                ship.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*RANGE_EXTRA/100f);
                ship.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(ECM_BONUS_ID, 1f + ecmdmgboost*RANGE_EXTRA/100f);

                String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_electronic_warfare");
                if (ship == Global.getCombatEngine().getPlayerShip()) Global.getCombatEngine().maintainStatusForPlayerShip("NA_FulldiveMatrix", icon, "Stargazer Matrix",
                        ((int)(ecmdmgboost * EMP_SCALE)) + "% damage to weapon/engines, +" + ((int)(ecmdmgboost * RANGE_EXTRA)) +  "% energy and ballistic range", false);


            }


            //yoinked from ElectronicWarfareScript.java
            private int [] getTotalAndMaximum(CombatFleetManagerAPI manager) {

                float max = 0f;
                for (PersonAPI commander : manager.getAllFleetCommanders()) {
                    max = Math.max(max, ElectronicWarfareScript.BASE_MAXIMUM + commander.getStats().getDynamic().getValue(Stats.ELECTRONIC_WARFARE_MAX, 0f));
                }


                float total = 0f;
                List<DeployedFleetMemberAPI> deployed = manager.getDeployedCopyDFM();
                float canCounter = 0f;
                for (DeployedFleetMemberAPI member : deployed) {
                    if (member.isFighterWing()) continue;
                    if (member.isStationModule()) continue;
                    float curr = member.getShip().getMutableStats().getDynamic().getValue(Stats.ELECTRONIC_WARFARE_FLAT, 0f);
                    total += curr;

                    canCounter += member.getShip().getMutableStats().getDynamic().getValue(Stats.SHIP_BELONGS_TO_FLEET_THAT_CAN_COUNTER_EW, 0f);
                }

                for (BattleObjectiveAPI obj : Global.getCombatEngine().getObjectives()) {
                    if (obj.getOwner() == manager.getOwner() && BattleObjectives.SENSOR_JAMMER.equals(obj.getType())) {
                        total += ElectronicWarfareScript.PER_JAMMER;
                    }
                }

                int counter = 0;
                if (canCounter > 0) counter = 1;

                return new int [] {(int) total, (int) max, counter};
            }

        }

    }
}

