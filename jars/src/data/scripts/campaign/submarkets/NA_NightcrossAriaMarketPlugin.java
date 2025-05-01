package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;

public class NA_NightcrossAriaMarketPlugin extends BaseSubmarketPlugin {

    private final RepLevel MIN_STANDING = RepLevel.INHOSPITABLE;
    private final RepLevel MIN_STANDING_RES =  RepLevel.FRIENDLY;
    private final RepLevel MIN_STANDING_EXP = RepLevel.NEUTRAL;

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }


    @Override
    public float getTariff() {
        return 2.0f;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        if (market.getFaction() == Global.getSector().getFaction("independent")) {
            return "Not available if controlled by independents";
        }
        if (!level.isAtWorst(MIN_STANDING)) {
            return "Requires: " + market.getFaction().getDisplayName() + " - "
                    + MIN_STANDING.getDisplayName().toLowerCase();
        }
        return super.getTooltipAppendix(ui);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        if (market.getFaction() == Global.getSector().getFaction("independent")) {
            return false;
        }
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        return level.isAtWorst(MIN_STANDING);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;

        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0f;

            getCargo().getMothballedShips().clear();

            float quality = -0.5f;

            FactionAPI faction = Global.getSector().getFaction("nightcross");
            FactionDoctrineAPI doctrineOverride = faction.getDoctrine().clone();
            addShips(faction.getId(),
                    100f, // combat
                    0f, // freighter
                    0f, // tanker
                    0f, // transport
                    0f, // liner
                    0f, // utilityPts
                    null, // qualityOverride
                    quality, // qualityMod
                    FactionAPI.ShipPickMode.PRIORITY_THEN_ALL,
                    doctrineOverride);

            pruneWeapons(0f);

            WeightedRandomPicker<WeaponSpecAPI> picker = new WeightedRandomPicker<WeaponSpecAPI>();
            for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
                if (!spec.hasTag("nightcross_bp_rare") && !spec.hasTag("nightcross_bp_heavy") && !spec.hasTag("nightcross_bp_fast")) continue;
                if (spec.hasTag(Tags.NO_DROP)) continue;
                float w = spec.getRarity();
                if (spec.hasTag("nightcross_bp_rare")) w *= 0.35f;
                else if (spec.hasTag("nightcross_bp_restricted")) w *= 0.1f;
                picker.add(spec, w);
            }

            pickAndAddWeapons(5, 9, picker);


            WeightedRandomPicker<FighterWingSpecAPI> fpicker = new WeightedRandomPicker<FighterWingSpecAPI>();
            for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
                if (!spec.hasTag("nightcross_bp_rare") && !spec.hasTag("nightcross_bp_heavy") && !spec.hasTag("nightcross_bp_fast")) continue;
                if (spec.hasTag(Tags.NO_DROP)) continue;
                float w = spec.getRarity();
                if (spec.hasTag("nightcross_bp_rare")) w *= 0.35f;
                else if (spec.hasTag("nightcross_bp_restricted")) w *= 0.1f;
                fpicker.add(spec, w);
            }

            pickAndAddFighters(1,2, fpicker);


            pruneShips(0.5f);
        }

        getCargo().sort();
    }
    protected void pickAndAddWeapons(int min, int max, WeightedRandomPicker<WeaponSpecAPI> picker) {
        int types = MathUtils.getRandomNumberInRange(min, max);
        for (int i = 0; i < types; i++) {
            WeaponSpecAPI spec = picker.pick();
            if (spec == null) return;

            int count = 1;
            switch (spec.getSize()) {
                case LARGE: count = MathUtils.getRandomNumberInRange(2, 4); break;
                case MEDIUM: count = MathUtils.getRandomNumberInRange(3, 6); break;
                case SMALL: count = MathUtils.getRandomNumberInRange(4, 8); break;
            }
            cargo.addWeapons(spec.getWeaponId(), count);
        }

    }
    protected void pickAndAddFighters(int min, int max, WeightedRandomPicker<FighterWingSpecAPI> picker) {
        int types = MathUtils.getRandomNumberInRange(min, max);
        for (int i = 0; i < types; i++) {
            FighterWingSpecAPI spec = picker.pick();
            if (spec == null) return;

            int count = 1;
            cargo.addFighters(spec.getId(), count);
        }

    }

    private RepLevel getRequiredLevelAssumingLegal(FleetMemberAPI member, TransferAction action) {
        int tier = -1;
        if (member.getHullSpec()  == null) return RepLevel.VENGEFUL;
        switch (member.getHullSpec().getHullSize()) {
            case CAPITAL_SHIP: tier = 4; break;
            case FRIGATE: tier = 1; break;
            case DESTROYER: tier = 2; break;
            case CRUISER: tier = 3; break;
            default: tier = 0; break;
        }

        if (tier >= 0) {
            if (action == TransferAction.PLAYER_BUY) {
                if (tier > 3) {
                    return RepLevel.COOPERATIVE;
                }
                if (tier == 3) {
                    return RepLevel.FRIENDLY;
                }
                if (tier == 2) {
                    return RepLevel.WELCOMING;
                }
                if (tier == 1) {
                    return RepLevel.FAVORABLE;
                }
                return RepLevel.NEUTRAL;
            }
            return RepLevel.VENGEFUL;
        }
        return RepLevel.VENGEFUL;
    }

    private RepLevel getRequiredLevelAssumingLegal(CargoStackAPI stack, TransferAction action) {
        int tier = -1;
        boolean restricted = false;
        boolean rare = false;
        if (stack.isWeaponStack()) {
            WeaponSpecAPI weaponSpec = stack.getWeaponSpecIfWeapon();
            tier = weaponSpec.getTier();

            if (weaponSpec.hasTag("nightcross_bp_rare")) rare = true;
            else if (weaponSpec.hasTag("nightcross_bp_restricted")) restricted = true;
        } else if (stack.isFighterWingStack()) {
            FighterWingSpecAPI fighterSpec = stack.getFighterWingSpecIfWing();
            tier = fighterSpec.getTier();
            if (fighterSpec.hasTag("nightcross_bp_rare")) rare = true;
            else if (fighterSpec.hasTag("nightcross_bp_restricted")) restricted = true;
        }

        if (tier >= 0 || rare) {
            if (action == TransferAction.PLAYER_BUY) {
                if (tier > 3) {
                    return RepLevel.COOPERATIVE;
                }
                if (tier == 3 || restricted) {
                    return RepLevel.FRIENDLY;
                }
                if (tier == 2) {
                    return RepLevel.FAVORABLE;
                }
                return RepLevel.NEUTRAL;
            }
            return RepLevel.VENGEFUL;
        }
        return RepLevel.VENGEFUL;
    }


    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        if (level.isAtWorst(getRequiredLevelAssumingLegal(stack, action))) {
            return action == TransferAction.PLAYER_SELL;
        }
        return true;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        if (level.isAtWorst(getRequiredLevelAssumingLegal(member, action))) {
            return action == TransferAction.PLAYER_SELL;
        }
        return true;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        RepLevel minstanding = getRequiredLevelAssumingLegal(member, action);
        if (level.isAtWorst(minstanding)) {
            return action != TransferAction.PLAYER_SELL ? "" : "No sales allowed.";
        }

        return "Requires: " + market.getFaction().getDisplayName() + " - "
                + minstanding.getDisplayName().toLowerCase();
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        RepLevel minstanding = getRequiredLevelAssumingLegal(stack, action);
        if (level.isAtWorst(minstanding)) {
            return action != TransferAction.PLAYER_SELL ? "" : "No sales allowed.";
        }

        return "Requires: " + market.getFaction().getDisplayName() + " - "
                + minstanding.getDisplayName().toLowerCase();
    }
    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }
}
