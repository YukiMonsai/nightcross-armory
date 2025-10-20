package data.scripts.campaign.plugins;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

public class NA_SettingsListener implements LunaSettingsListener {

    public static boolean na_stargazer_abyss = LunaSettings.getBoolean("Nightcross", "na_stargazer_abyss");
    public static boolean na_stargazer_gen = LunaSettings.getBoolean("Nightcross", "na_stargazer_gen");
    public static boolean na_stargazer_spawn = LunaSettings.getBoolean("Nightcross", "na_stargazer_spawn");
    public static boolean na_pascal_system = LunaSettings.getBoolean("Nightcross", "na_pascal_system");
    public static boolean na_faction_nex = LunaSettings.getBoolean("Nightcross", "na_faction_nex");
    public static boolean na_faction_gear = LunaSettings.getBoolean("Nightcross", "na_faction_gear");
    public static boolean na_faction_merc = LunaSettings.getBoolean("Nightcross", "na_faction_merc");
    public static boolean na_faction_gearrare = LunaSettings.getBoolean("Nightcross", "na_faction_gearrare");
    public static boolean na_ships_spawn = LunaSettings.getBoolean("Nightcross", "na_ships_spawn");
    public static boolean na_bounties = LunaSettings.getBoolean("Nightcross", "na_bounties");







    //Gets called whenever settings are saved in the campaign or the main menu.
    @Override
    public void settingsChanged(String modID) {
        na_stargazer_abyss = LunaSettings.getBoolean("Nightcross", "na_stargazer_abyss");
        na_stargazer_gen = LunaSettings.getBoolean("Nightcross", "na_stargazer_gen");
        na_stargazer_spawn = LunaSettings.getBoolean("Nightcross", "na_stargazer_spawn");
        na_pascal_system = LunaSettings.getBoolean("Nightcross", "na_pascal_system");
        na_faction_nex = LunaSettings.getBoolean("Nightcross", "na_faction_nex");
        na_faction_gear = LunaSettings.getBoolean("Nightcross", "na_faction_gear");
        na_faction_merc = LunaSettings.getBoolean("Nightcross", "na_faction_merc");
        na_faction_gearrare = LunaSettings.getBoolean("Nightcross", "na_faction_gearrare");
        na_ships_spawn = LunaSettings.getBoolean("Nightcross", "na_ships_spawn");
        na_bounties = LunaSettings.getBoolean("Nightcross", "na_bounties");
    }
}