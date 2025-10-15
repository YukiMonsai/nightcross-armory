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
    public static boolean na_combatui_enable = LunaSettings.getBoolean("Nightcross", "na_combatui_enable");
    public static boolean na_combatui_pause = LunaSettings.getBoolean("Nightcross", "na_combatui_pause");
    public static boolean na_combatui_compact = LunaSettings.getBoolean("Nightcross", "na_combatui_compact");
    public static boolean na_combatui_colorblind = LunaSettings.getBoolean("Nightcross", "na_combatui_colorblind");
    public static float tacticalRenderHeightOffset = LunaSettings.getFloat("Nightcross", "na_combatui_height");
    public static float tacticalRenderSideOffset = LunaSettings.getFloat("Nightcross", "na_combatui_side");






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
        na_combatui_enable = LunaSettings.getBoolean("Nightcross", "na_combatui_enable");
        na_combatui_pause = LunaSettings.getBoolean("Nightcross", "na_combatui_pause");
        na_combatui_compact = LunaSettings.getBoolean("Nightcross", "na_combatui_compact");
        na_combatui_colorblind = LunaSettings.getBoolean("Nightcross", "na_combatui_colorblind");

        tacticalRenderHeightOffset = LunaSettings.getFloat("Nightcross", "na_combatui_height");
        tacticalRenderSideOffset = LunaSettings.getFloat("Nightcross", "na_combatui_side");
    }
}