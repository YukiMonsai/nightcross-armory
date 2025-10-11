package data.scripts.campaign.rulecmd.nca;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;

import java.util.List;
import java.util.Map;



/**
 * NotifyEvent $eventHandle <params>
 *
 */
public class NA_ZGRTurnIn extends BaseCommandPlugin {

    public static float VALUE_MULT = 3f;
    public static float REP_MULT = 0.2f;
    public static float NA_REP_MULT = -0.5f;

    protected CampaignFleetAPI playerFleet;
    protected SectorEntityToken entity;
    protected FactionAPI playerFaction;
    protected FactionAPI entityFaction;
    protected TextPanelAPI text;
    protected OptionPanelAPI options;
    protected CargoAPI playerCargo;
    protected MemoryAPI memory;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;
    protected PersonAPI person;
    protected FactionAPI faction;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        this.dialog = dialog;
        this.memoryMap = memoryMap;

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        memory = getEntityMemory(memoryMap);

        entity = dialog.getInteractionTarget();
        text = dialog.getTextPanel();
        options = dialog.getOptionPanel();

        playerFleet = Global.getSector().getPlayerFleet();
        playerCargo = playerFleet.getCargo();

        playerFaction = Global.getSector().getPlayerFaction();
        entityFaction = entity.getFaction();

        person = dialog.getInteractionTarget().getActivePerson();
        faction = person.getFaction();

        if (command.equals("selectSellableItems")) {
            selectSellableItems();
        } else if (command.equals("playerHasSellableItems")) {
            return playerHasSellableItems();
        }

        return true;
    }


    protected void selectSellableItems() {
        CargoAPI copy = getSellableItems();

        final float width = 310f;
        dialog.showCargoPickerDialog("Select items to turn in", "Confirm", "Cancel", true, width, copy, new CargoPickerListener() {
            public void pickedCargo(CargoAPI cargo) {
                if (cargo.isEmpty()) {
                    cancelledCargoSelection();
                    return;
                }

                cargo.sort();

                int bounty = 0;
                MemoryAPI mem = Global.getSector().getPlayerMemoryWithoutUpdate();

                for (CargoStackAPI stack : cargo.getStacksCopy()) {
                    playerCargo.removeItems(stack.getType(), stack.getData(), stack.getSize());
                    int num = (int) stack.getSize();
                    AddRemoveCommodity.addStackLossText(stack, text);
                    bounty += num * stack.getBaseValuePerUnit() * VALUE_MULT;
                }

                float repChange = computeReputationValue(cargo);
                float repPenalty = computeReputationPenalty(cargo);


                if (bounty > 0) {
                    playerCargo.getCredits().add(bounty);
                    AddRemoveCommodity.addCreditsGainText((int)bounty, text);

                    String soldTotalKey = "$itemValueSoldToZGRStargazer";
                    int curr = mem.getInt(soldTotalKey);
                    curr += bounty;
                    mem.set(soldTotalKey, curr);


                    soldTotalKey = "$itemValueSoldToZGRTotal";
                    curr = mem.getInt(soldTotalKey);
                    curr += bounty;
                    mem.set(soldTotalKey, curr);

                }

                if (repChange >= 1f) {
                    CoreReputationPlugin.CustomRepImpact impact = new CoreReputationPlugin.CustomRepImpact();
                    impact.delta = repChange * 0.01f;
                    Global.getSector().adjustPlayerReputation(
                            new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact,
                                    null, text, true),
                            faction.getId());

                    impact.delta *= 0.25f;
                    if (impact.delta >= 0.01f) {
                        Global.getSector().adjustPlayerReputation(
                                new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact,
                                        null, text, true),
                                person);
                    }
                }

                if (repPenalty <= -0.1f) {
                    CoreReputationPlugin.CustomRepImpact impact = new CoreReputationPlugin.CustomRepImpact();
                    impact.delta = repPenalty * 0.01f;
                    Global.getSector().adjustPlayerReputation(
                            new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact,
                                    null, text, true),
                            NightcrossID.NIGHTCROSS_ARMORY);

                }

                mem.set("$itemValueSoldToZGRJustNowStargazer", (int)bounty, 0);
                mem.set("$itemValueSoldToZGRJustNowTotal", (int)bounty, 0);
                FireBest.fire(null, dialog, memoryMap, "ZGRItemsTurnedIn");
            }
            public void cancelledCargoSelection() {
            }
            public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {

                float bounty = 0f;
                for (CargoStackAPI stack : combined.getStacksCopy()) {
                    int num = (int) stack.getSize();
                    bounty += num * stack.getBaseValuePerUnit() * VALUE_MULT;
                }

                float repChange = computeReputationValue(combined);
                float repPenalty = computeReputationPenalty(combined);

                float pad = 3f;
                float small = 5f;
                float opad = 10f;

                panel.setParaFontOrbitron();
                panel.addPara(Misc.ucFirst(faction.getDisplayName()), faction.getBaseUIColor(), 1f);
                //panel.addTitle(Misc.ucFirst(faction.getDisplayName()), faction.getBaseUIColor());
                //panel.addPara(faction.getDisplayNameLong(), faction.getBaseUIColor(), opad);
                //panel.addPara(faction.getDisplayName() + " (" + entity.getMarket().getName() + ")", faction.getBaseUIColor(), opad);
                panel.setParaFontDefault();

                panel.addImage(faction.getLogo(), width * 1f, 3f);

                panel.addPara("If you turn in the selected items, you will receive a %s bounty " +
                                "and your standing with " + faction.getDisplayNameWithArticle() + " will improve by %s points." +
                                "\nYour standing with Nightcross Armory will worsen by %s points.",
                        opad * 1f, Misc.getHighlightColor(),
                        Misc.getWithDGS(bounty) + Strings.C,
                        "" + (int) repChange,
                        "" + (int) repPenalty);
            }
        });
    }

    protected float computeReputationValue(CargoAPI cargo) {
        float rep = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            rep += getBaseRepValue(stack) * stack.getSize();
        }
        rep *= REP_MULT;
        return rep;
    }
    protected float computeReputationPenalty(CargoAPI cargo) {
        float rep = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            rep += getBaseRepValue(stack) * stack.getSize();
        }
        rep *= NA_REP_MULT;
        return rep;
    }

    public static float getBaseRepValue(CargoStackAPI stack) {
        if (stack.isWeaponStack()) {
            switch (stack.getWeaponSpecIfWeapon().getSize()) {
                case LARGE: return 3f;
                case MEDIUM: return 2f;
                case SMALL: return 1f;
            }
        }
        if (stack.isSpecialStack()) {
            return 5f;
        }
        return 1f;
    }


    protected boolean playerHasSellableItems() {
        return !getSellableItems().isEmpty();
    }

    public static boolean isStargazerStack(CargoStackAPI stack) {
        boolean match = false;
        match |= stack.isWeaponStack() && stack.getWeaponSpecIfWeapon().hasTag("stargazer");
        match |= stack.isSpecialStack() && stack.getSpecialItemSpecIfSpecial().hasTag("stargazer");
        return match;
    }

    protected CargoAPI getSellableItems() {
        CargoAPI copy = Global.getFactory().createCargo(false);
        for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
            boolean match = isStargazerStack(stack);
            if (match) {
                copy.addFromStack(stack);
            }
        }
        copy.sort();
        return copy;
    }


}















