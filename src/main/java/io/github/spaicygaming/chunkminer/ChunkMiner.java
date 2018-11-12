package io.github.spaicygaming.chunkminer;

import io.github.spaicygaming.chunkminer.cmd.CMCommands;
import io.github.spaicygaming.chunkminer.listeners.PlayerInteractListener;
import io.github.spaicygaming.chunkminer.listeners.PlayerJoinListener;
import io.github.spaicygaming.chunkminer.miner.MinersManager;
import io.github.spaicygaming.chunkminer.util.ChatUtil;
import io.github.spaicygaming.chunkminer.util.MinerItem;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkMiner extends JavaPlugin {

    private static ChunkMiner instance;
    private MinerItem minerItem;
    private UpdateChecker updateChecker;

    /**
     * Latest configuration file (config.yml) version
     */
    @SuppressWarnings("FieldCanBeLocal") // It's here for better code readability
    private final double configVersion = 1.4;

    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        checkConfigVersion();

        // Factions Hook
        hookMessage("Factions", getConfig().getBoolean("MainSettings.hooks.FactionsUUID.enabled"), isFactionsInstalled());

        // Initialize miner item
        minerItem = new MinerItem(this);

        // Initialize MinersManager
        MinersManager minersManager = new MinersManager(this);


        // Register listeners/commands
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this, minersManager, null), this);
        getCommand("chunkminer").setExecutor(new CMCommands(this));

        // Update Checker
        if (getConfig().getBoolean("CheckForUpdates")) {
            checkForUpdates();
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(getUpdateChecker()), this);
        }

        getLogger().info("ChunkMiner has been enabled!");
    }

    public static ChunkMiner getInstance() {
        return instance;
    }

    /**
     * @return the instance of MinerItem
     */
    public MinerItem getMinerItem() {
        return minerItem;
    }

    /**
     * Check the configuration file version.
     * Notify the ConsoleSender if the config.yml is outdated
     */
    private void checkConfigVersion() {
        if (getConfig().getDouble("configVersion") < configVersion) {
            ChatUtil.alert("OUTDATED config.yml FILE DETECTED, PLEASE DELETE THE OLD ONE!");
            ChatUtil.alert("You can also manually update it: https://github.com/SpaicyGaming/ChunkMiner/blob/master/ChunkMiner/src/main/resources/config.yml");
        }
    }

    /**
     * Check for updates and notify the console
     */
    private void checkForUpdates() {
        getLogger().info("Checking for updates...");
        updateChecker = new UpdateChecker(Double.parseDouble(getDescription().getVersion()));

        if (updateChecker.availableUpdate()) {
            ChatUtil.alert("New version available!");
            ChatUtil.alert("Your version: " + getUpdateChecker().getCurrentVersion());
            ChatUtil.alert("Latest version: " + getUpdateChecker().getLatestVersion());
        } else
            getLogger().info("No new version available :(");
    }

    /**
     * Return the instance of UpdateChecker.
     *
     * @return null if update checking is disabled from the config.yml
     */
    private UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    /**
     * Disable the plugin and send fancy messages to the console
     *
     * @param reason The reason why the plugin has been disabled
     */
    public void disable(String reason) {
        ConsoleCommandSender cs = getServer().getConsoleSender();

        cs.sendMessage(ChatColor.RED + ChatUtil.getSeparators('=', 70));
        cs.sendMessage(ChatColor.AQUA + "[ChunkMiner] " + ChatColor.RED + "Fatal Error!");
        cs.sendMessage(ChatColor.RED + reason);
        cs.sendMessage(ChatColor.RED + "Disabling plugin...");
        cs.sendMessage(ChatColor.RED + ChatUtil.getSeparators('=', 70));

        getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Check whether Factions plugin is installed
     *
     * @return true if it is
     */
    public boolean isFactionsInstalled() {
        Plugin factions = getServer().getPluginManager().getPlugin("Factions");

        return factions != null && factions.isEnabled();
    }

    private void hookMessage(String pluginName, boolean configCondition, boolean pluginLoaded) {
        if (configCondition) {
            if (pluginLoaded)
                getLogger().info("Hooked into " + pluginName);
            else
                getLogger().info("Can't hook into " + pluginName + ", plugin not found");
        }
    }

}