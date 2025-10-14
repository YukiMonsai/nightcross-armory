package data.scripts.campaign.rulecmd.nca;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.FleetMemberData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SalvageDefenderModificationPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Based on code from HMI
public class NA_NightcrossStargazerDefenderCMD extends BaseCommandPlugin {

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        final SectorEntityToken entity = dialog.getInteractionTarget();
        final MemoryAPI memory = getEntityMemory(memoryMap);

        final CampaignFleetAPI defenders = memory.getFleet("$defenderFleet");
        if (defenders == null) return false;

        dialog.setInteractionTarget(defenders);
        defenders.getMemoryWithoutUpdate().set("$entity.na_stargazerdefCMD", true);

        final FIDConfig config = new FIDConfig();
        config.leaveAlwaysAvailable = true;
        config.showCommLinkOption = true;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = true;
        config.impactsEnemyReputation = true;
        config.pullInAllies = true; // yes: want option of bringing Courser
        config.pullInEnemies = true;
        config.pullInStations = false;
        config.lootCredits = true;

        config.firstTimeEngageOptionText = "Engage the automated forces";
        config.afterFirstTimeEngageOptionText = "Re-engage the automated forces";
        config.noSalvageLeaveOptionText = "Continue";

        config.dismissOnLeave = false;
        config.printXPToDialog = true;

        long seed = memory.getLong(MemFlags.SALVAGE_SEED);
        config.salvageRandom = Misc.getRandom(seed, 75);


        Global.getSector().getCampaignUI().restartEncounterMusic(defenders);

        final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);

        final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
        config.delegate = new BaseFIDDelegate() {

            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {
                // nothing in there we care about keeping; clearing to reduce savefile size
                defenders.getMemoryWithoutUpdate().clear();
                // there's a "standing down" assignment given after a battle is finished that we don't care about
                defenders.clearAssignments();
                defenders.deflate();

                dialog.setPlugin(originalPlugin);
                dialog.setInteractionTarget(entity);

                //Global.getSector().getCampaignUI().clearMessages();

                if (plugin.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                    if (context.didPlayerWinEncounterOutright()) {

                        SDMParams p = new SDMParams();
                        p.entity = entity;
                        p.factionId = defenders.getFaction().getId();

                        SalvageDefenderModificationPlugin plugin = Global.getSector().getGenericPlugins().pickPlugin(
                                SalvageDefenderModificationPlugin.class, p);
                        if (plugin != null) {
                            plugin.reportDefeated(p, entity, defenders);
                        }

                        memory.unset("$hasDefenders");
                        memory.unset("$defenderFleet");
                        memory.set("$defenderFleetDefeated", true);
                        entity.removeScriptsOfClass(FleetAdvanceScript.class);
                        FireBest.fire(null, dialog, memoryMap, "BeatDefendersContinue");
                    } else {
                        boolean persistDefenders = false;
                        if (context.isEngagedInHostilities()) {
                            persistDefenders |= !Misc.getSnapshotMembersLost(defenders).isEmpty();
                            for (FleetMemberAPI member : defenders.getFleetData().getMembersListCopy()) {
                                if (member.getStatus().needsRepairs()) {
                                    persistDefenders = true;
                                    break;
                                }
                            }
                        }

                        if (persistDefenders) {
                            if (!entity.hasScriptOfClass(FleetAdvanceScript.class)) {
                                defenders.setDoNotAdvanceAI(true);
                                defenders.setContainingLocation(entity.getContainingLocation());
                                // somewhere far off where it's not going to be in terrain or whatever
                                defenders.setLocation(1000000, 1000000);
                                entity.addScript(new FleetAdvanceScript(defenders));
                            }
                            memory.expire("$defenderFleet", 10); // defenders may have gotten damaged; persist them for a bit
                        }
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }


            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                //bcc.objectivesAllowed = false;
                bcc.enemyDeployAll = true;
            }


        };


        dialog.setPlugin(plugin);
        plugin.init(dialog);

        return true;
    }


}




