package yukimonsai.sicnightcross.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;

import java.util.HashMap;
import java.util.Random;

import com.fs.starfarer.api.characters.PersonAPI;
import second_in_command.specs.SCOfficer;

public class addXO implements EconomyTickListener {
    String prev;

    @Override
    public void reportEconomyTick(int iterIndex) {

    }

    @Override
    public void reportEconomyMonthEnd() {
        MarketAPI market = Global.getSector().getEconomy().getMarket("na_graveyard_station");
        if(market == null) market = Global.getSector().getEconomy().getMarket("ilm"); // try for a vanilla market instead
        if(market == null) return; // If the market doesn't exist, just quit

        if(prev != null)
            market.getCommDirectory().removeEntry(prev); // Clear last month's XO
        prev = null;

        Random rand = new Random();
        //if(rand.nextFloat() > .7f) return; // 70% chance to have XO in market each month


        // Make a random Dustkeeper
        PersonAPI person = market.getFaction().createRandomPerson(); // Random gender
        person.setFaction("nightcross");
        person.getMemoryWithoutUpdate().set("$sc_officer_aptitude","sc_nightcross");
        person.getMemoryWithoutUpdate().set("$sc_hireable", true);
        person.setPostId("executive_officer_sc_nightcross");

        prev = market.getCommDirectory().addPerson(person);
    }
}
