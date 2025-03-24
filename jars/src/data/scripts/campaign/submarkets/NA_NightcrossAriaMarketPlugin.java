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
                picker.add(spec, w);
            }

            pickAndAddWeapons(5, 9, picker);


            WeightedRandomPicker<FighterWingSpecAPI> fpicker = new WeightedRandomPicker<FighterWingSpecAPI>();
            for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
                if (!spec.hasTag("nightcross_bp_rare") && !spec.hasTag("nightcross_bp_heavy") && !spec.hasTag("nightcross_bp_fast")
                    && !spec.hasTag("nightcross_bp_restricted")) continue;
                if (spec.hasTag(Tags.NO_DROP)) continue;
                float w = spec.getRarity();
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

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "No sales allowed.";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "No sales allowed.";
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }
}
