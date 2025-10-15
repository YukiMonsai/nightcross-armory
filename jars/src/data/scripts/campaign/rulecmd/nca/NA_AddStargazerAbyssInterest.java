package data.scripts.campaign.rulecmd.nca;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.enc.NA_StargazerGhostManager;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class NA_AddStargazerAbyssInterest extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        float amount = 1f;
        if (!params.isEmpty() && params.get(0).isFloat(memoryMap)) {
            amount = params.get(0).getFloat(memoryMap);
        }
        NA_StargazerGhostManager.addAbyssInterest(amount);
        dialog.getInteractionTarget().getMemoryWithoutUpdate().unset(NA_StargazerGhostManager.ABYSS_INTEREST_KEY);
        showRecoverable(dialog);
        return true;
    }


    public void showRecoverable(InteractionDialogAPI dialog) {
        SectorEntityToken entity = dialog.getInteractionTarget();
        if (!(entity.getCustomPlugin() instanceof DerelictShipEntityPlugin plugin)) {
            SectorEntityToken target = entity;

            if (target.getCustomInteractionDialogImageVisual() != null) {
                dialog.getVisualPanel().showImageVisual(target.getCustomInteractionDialogImageVisual());
            } else {
                if (target.getMarket() != null) {
                    target = target.getMarket().getPlanetEntity();
                }
                if (target instanceof PlanetAPI) {
                    //Global.getSettings().setBoolean("3dPlanetBGInInteractionDialog", true);
                    if (!Global.getSettings().getBoolean("3dPlanetBGInInteractionDialog")) {
                        dialog.getVisualPanel().showPlanetInfo((PlanetAPI) target);
                    }
                    //dialog.getVisualPanel().showLargePlanet((PlanetAPI) target);

                }
//			else if (target instanceof XXXXX) {
//				dialog.getVisualPanel().showXXXXX((XXXXX) target);
//			}
            }
        } else {
            // set to abyss visual
            entity.setCustomInteractionDialogImageVisual(new InteractionDialogImageVisual("illustrations", "na_abysswreck", 480, 300));

            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, plugin.getData().ship.variantId);
            member.getStatus().applyHullFractionDamage(0.9f);
            DefaultFleetInflater.getNumDModsToAdd(Global.getSettings().getVariant(plugin.getData().ship.variantId), 4, new Random());
            member.setShipName(plugin.getData().ship.shipName);
            dialog.getVisualPanel().showFleetMemberInfo(member, false);

        }


    }

}
