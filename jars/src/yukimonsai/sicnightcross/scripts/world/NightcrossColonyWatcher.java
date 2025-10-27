package yukimonsai.sicnightcross.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

public class NightcrossColonyWatcher implements EconomyTickListener {

    public static final String MEMKEY_NIGHTCROSS_COLONIZED = "$NightcrossColonized";
    public static final String MEMKEY_NIGHTCROSS_DECIVILIZED = "$NightcrossDecivilized";

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (Global.getSector().getEntityById("nightcross") != null
                && Global.getSector().getEntityById("nightcross").getMarket() != null
                && Global.getSector().getEntityById("nightcross").getMarket().hasSpaceport()) {
            if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_NIGHTCROSS_COLONIZED)) {
                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_NIGHTCROSS_COLONIZED, true);
                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_NIGHTCROSS_DECIVILIZED, false);
            }
        } else if (Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_NIGHTCROSS_COLONIZED)
            && (Global.getSector().getEconomy().getMarket("na_elevator") == null
        || !(Global.getSector().getEconomy().getMarket("na_elevator").getFaction() != null && Global.getSector().getEconomy().getMarket("na_elevator").getFaction().equals(Factions.INDEPENDENT)))) {
            if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_NIGHTCROSS_DECIVILIZED)) {
                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_NIGHTCROSS_DECIVILIZED, true);
            }
        }
    }

    @Override
    public void reportEconomyMonthEnd() {

    }


}
