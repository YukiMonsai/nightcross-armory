package data.scripts.campaign.enc;

import com.fs.graphics.H;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.HashMap;

public interface ManagerWithAbyssInterest {
    // abyss interest makes it more likely that the player will encounter one type of abyss event
    // interacting with abyss derelicts increases interest
    // It also should have some level of cross-mod compatibility using memkeys,
    // by subtracting other kinds of abyss interest from this one's

    public static final String ABYSS_INTEREST_GLOBAL_KEY = "$abyssInterestManagerKey";

    static String getGlobalAbyssManagerTag() {
        return ABYSS_INTEREST_GLOBAL_KEY;
    }

    static ManagerWithAbyssInterest getManager() {
        return null;
    }
    static float getAdjustedAbyssInterest(String key) {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (memory.contains(getGlobalAbyssManagerTag())) {
            HashMap<String, Float> map = (HashMap<String, Float>) memory.get(getGlobalAbyssManagerTag());

            float main = 0f;
            float rest = 0f;

            for (String abyssInterestKey : map.keySet()) {
                if (key.equals(abyssInterestKey)) {
                    main += map.get(abyssInterestKey);
                } else {
                    rest += map.get(abyssInterestKey);
                }

            }
            if (main == 0) return 0f;
            if (rest == 0) return main;
            return main * main / (main + rest);
        }

        return 0f;
    }



    static float currentAbyssInterest(String key) {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        if (memory.contains(getGlobalAbyssManagerTag())) {
            HashMap<String, Float> map = (HashMap<String, Float>) memory.get(getGlobalAbyssManagerTag());
            if (map.containsKey(key)) return map.get(key);
        }
        return 0f;
    }
    static void setAbyssInterest(String key, float amount) {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        HashMap<String, Float> map = null;
        if (memory.contains(getGlobalAbyssManagerTag())) {
            map = (HashMap<String, Float>) memory.get(getGlobalAbyssManagerTag());
        }
        if (map == null) {
            map = new HashMap<String, Float>();
            memory.set(getGlobalAbyssManagerTag(), map);
        }
        if (map != null) {
            map.put(key, amount);
        }
    }


}
