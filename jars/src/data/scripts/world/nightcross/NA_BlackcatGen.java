package data.scripts.world.nightcross;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.NightcrossID;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.scripts.world.nightcross.NA_StargazerFleets.createStargazerFleet;

public class NA_BlackcatGen implements SectorGeneratorPlugin {
    // generates happy friendly ai fleets to greet the player on their adventures
    public static void initFactionRelationships(SectorAPI sector) {
    }

    public boolean BlackcatGenerated = false;

    @Override
    public void generate(SectorAPI sector) {


        generate_blackcat(sector);
    }
    public void init(SectorAPI sector) {
        
    }


    public boolean generate_blackcat(SectorAPI sector) {



        //BlackcatGenerated = true;
        return BlackcatGenerated;
    }




}
