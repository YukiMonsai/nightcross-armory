// important for replacing plugins and other useful things
package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityIconProvider;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.scripts.campaign.ids.NightcrossID;

public class NACampaignPlugin extends BaseCampaignPlugin {

    public String getId() {
        return "NACampaignPluginImpl";
    }

    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
        switch (commodityId) {
            case NightcrossID.TETO_CORE:
                return new PluginPick<AICoreOfficerPlugin>(new NAGhostCorePlugin(), PickPriority.MOD_SET);
            case NightcrossID.GHOST_CORE_ID:
                return new PluginPick<AICoreOfficerPlugin>(new NAGhostCorePlugin(), PickPriority.MOD_SET);
            default:
                return null;
        }
    }

}








