package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NA_StargazerFIDConfig implements FleetInteractionDialogPluginImpl.FIDConfigGen {
    public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
        FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

        config.alwaysAttackVsAttack = true;
        //config.alwaysPursue = true;
        //config.alwaysHarry = true;
        config.showTransponderStatus = false;
        //config.showEngageText = false;
        config.lootCredits = false;


        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) return;

                float mult = context.computePlayerContribFraction();
                float p = Global.getSettings().getFloat("salvageHullmodProb");

                CampaignFleetAPI fleet = (CampaignFleetAPI) dialog.getInteractionTarget();

                FleetEncounterContextPlugin.DataForEncounterSide data = context.getDataFor(fleet);
                List<FleetMemberAPI> losses = new ArrayList<FleetMemberAPI>();
                for (FleetEncounterContextPlugin.FleetMemberData fmd : data.getOwnCasualties()) {
                    losses.add(fmd.getMember());
                }

                Random random = Misc.getRandom(Misc.getSalvageSeed(fleet), 7);
                //random = new Random();

                boolean gaveNebula = Global.getSector().getPlayerFaction().knowsHullMod("na_stargazerstars");
                boolean gaveRage = Global.getSector().getPlayerFaction().knowsHullMod("na_stargazerrage");

                for (FleetMemberAPI member : losses) {
                    if (member.getHullSpec().hasTag("stargazer_hull") || (member.getHullSpec().getBaseHull() != null && member.getHullSpec().getBaseHull().hasTag("stargazer_hull"))) {
                        int rolls = 0;
                        switch (member.getHullSpec().getHullSize()) {
                            case CAPITAL_SHIP: rolls = 20; break;
                            case CRUISER: rolls = 12; break;
                            case DESTROYER: rolls = 6; break;
                            case FRIGATE: rolls = 3; break;
                        }

                        for (int i = 0; i < rolls; i++) {

                            // stardust nebula
                            if (random.nextFloat() < p && random.nextFloat() < mult) {
                                if (!gaveNebula) {
                                    String id = "na_stargazerstars";
                                    gaveNebula = true;
                                    HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
                                    if (spec.isHidden() || spec.isHiddenEverywhere()) continue;
                                    //if (spec.isAlwaysUnlocked()) continue;
                                    if (spec.hasTag(Tags.HULLMOD_NO_DROP)) continue;

                                    salvage.addHullmods(id, 1);
                                } else if (!gaveRage && random.nextFloat() < 0.5f) {
                                        String id = "na_stargazerrage";
                                        gaveRage = true;
                                        HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
                                        if (spec.isHidden() || spec.isHiddenEverywhere()) continue;
                                        //if (spec.isAlwaysUnlocked()) continue;
                                        if (spec.hasTag(Tags.HULLMOD_NO_DROP)) continue;

                                        salvage.addHullmods(id, 1);
                                  }

                            }
                        }
                    }
                }
            }

            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                bcc.fightToTheLast = true;

                /*if (bcc.getOtherFleet() != null) {
                    for (FleetMemberAPI curr : bcc.getOtherFleet().getMembersWithFightersCopy()) {
                        if (curr.getHullSpec().hasTag(Tags.THREAT_FABRICATOR)) {
                            bcc.forceObjectivesOnMap = true;
                            break;
                        }
                    }
                }*/

                Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredStargazer", true);
                //Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredWeird", true); // not setting this as it might break ZGR dialogue for now
                //bcc.enemyDeployAll = true;
            }
        };
        return config;
    }
}





