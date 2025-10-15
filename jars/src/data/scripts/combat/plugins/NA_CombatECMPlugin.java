package data.scripts.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.BattleObjectives;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.ElectronicWarfareScript;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.HashMap;
import java.util.List;

public class NA_CombatECMPlugin implements EveryFrameCombatPlugin {
    protected static HashMap<Integer, Integer[]> data = new HashMap<Integer, Integer[]>();

    public static Integer[] get(int Owner) {
        if (!data.containsKey(Owner)) data.put(Owner, calcGet(Owner));
        return data.get(Owner);
    }

    public static Integer[] calcGet(int Owner) {
        CombatEngineAPI engine = Global.getCombatEngine();

        CombatFleetManagerAPI manager = engine.getFleetManager(Owner);

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

        return new Integer[] {(Integer)((int) total), (Integer)((int) max), (Integer) counter};
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        // clear so we can re-memoize in future
        data = new HashMap<Integer, Integer[]>();
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {

    }

    @Override
    public void init(CombatEngineAPI engine) {

    }
}
