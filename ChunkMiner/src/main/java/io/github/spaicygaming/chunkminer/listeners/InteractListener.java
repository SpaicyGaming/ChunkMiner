package io.github.spaicygaming.chunkminer.listeners;

import com.massivecraft.factions.*;
import io.github.spaicygaming.chunkminer.ChunkMiner;
import io.github.spaicygaming.chunkminer.Miner;
import io.github.spaicygaming.chunkminer.util.ChatUtil;
import io.github.spaicygaming.chunkminer.util.Const;
import io.github.spaicygaming.chunkminer.util.MinerItem;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class InteractListener implements Listener {

    private ChunkMiner main;
    private MinerItem minerItem;

    /**
     * The maximum amount of ChunkMiners a player can place at once
     */
    private int maxMinersAmountAtOnce;

    public InteractListener(ChunkMiner main) {
        this.main = main;
        minerItem = main.getMinerItem();
        maxMinersAmountAtOnce = main.getConfig().getInt("MainSettings.maxAmount");
    }

    @EventHandler
    public void onMinerPlace(PlayerInteractEvent event) {
        // Check the Action
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        // Return if the item is not a ChunkMiner
        if (!minerItem.isSimilar(event.getItem())) return;


        // Cancel the event
        event.setCancelled(true);

        // Update the inventory to prevent glitchy items
        Player player = event.getPlayer();
        player.updateInventory();

        // Return is the player does not have permission
        if (!player.hasPermission(Const.PERM_PLACE)) {
            player.sendMessage(ChatUtil.c("noPlacePerms").replace("{perm}", Const.PERM_PLACE));
            // To prevent glitchy items
            return;
        }

        // Return if the player tried to place the miner in a blacklisted world
        String worldName = player.getWorld().getName();
        if (!player.hasPermission("chunkminer.bypass.gamemodes")
                && main.getConfig().getStringList("MainSettings.blacklistedWorlds").contains(worldName)) {
            player.sendMessage(ChatUtil.c("blacklistedWorld").replace("{world}", worldName));
            return;
        }

        // Return if the player is in a not allowed gamemode (specified in the config.yml)
        if (!player.hasPermission("chunkminer.bypass.worlds") && !allowedGamemode(player)) {
            player.sendMessage(ChatUtil.c("notAllowedGamemode")
                    .replace("{gamemode}", ChatUtil.capitalizeFirstChar(player.getGameMode().toString())));
            return;
        }

        // Return if the player has reached the max amount of miners he can place at once
        Set<Chunk> playerOperations = main.getActiveOperations().get(player);
        if (playerOperations != null && playerOperations.size() == maxMinersAmountAtOnce) {
            player.sendMessage(ChatUtil.c("maxAmountReached").replace("{max_amount}", Integer.toString(maxMinersAmountAtOnce)));
            return;
        }

        // FactionsUUID checks
        if (Const.FACTIONS_HOOK && main.isFactionsInstalled())
            if (!canBuildHereFactions(player, event.getClickedBlock().getLocation())) {
                player.sendMessage(ChatUtil.c("notAllowedHereFactions"));
                return;
            }

        /*
         * TODO: add a confirm gui
         */

        // Action start message
        player.sendMessage(ChatUtil.c("minerPlaced"));

        // The chunk the player placed the miner in
        Chunk chunk = event.getClickedBlock().getLocation().getChunk();

        // Return if there is currently an active process in the chunk
        if (currentlyProcessed(chunk)) {
            player.sendMessage(ChatUtil.c("currentlyProcessed"));
            return;
        }

        // Scan the chunk
        Miner miner = new Miner(chunk, player, main.getWorldGuard());
        // If the player is not allowed to build in this region...
        if (!miner.scan()) {
            player.sendMessage(ChatUtil.c("notAllowedHere"));
            return;
        }

        // Return if the chunk is already mined
        if (miner.getBlocksAmount() == 0) {
            player.sendMessage(ChatUtil.c("chunkAlreadyMined"));
            return;
        }

        // Remove the ChunkMiner item from player's hand
        player.getInventory().setItemInHand(removeOneItem(player.getInventory().getItemInHand()));
        player.updateInventory(); // To prevent glitchy items

        // Mine the chunk
        miner.mine();

        // Action finished Message
        player.sendMessage(ChatUtil.c("minerSuccess"));

        // Notify staffers
        notifyStaffers(player.getName(), chunk);
    }

    /**
     * Check whether the player is in a allowed gamemode
     * to place ChunkMiners
     *
     * @param player The player
     * @return true if he is
     */
    private boolean allowedGamemode(Player player) {
        return !main.getConfig().getStringList("MainSettings.blockedGamemodes").contains(player.getGameMode().toString());
    }

    /**
     * Check whether the player is allowed by FactionsUUID
     * to build at that location
     *
     * @param player   The player who placed the miner
     * @param location The location of the block he interacted with
     * @return true if he is
     */
    private boolean canBuildHereFactions(Player player, Location location) {
        final String configSectionName = "MainSettings.hooks.FactionsUUID.allow";

        // The FPlayer who placed the miner
        FPlayer factionsPlayer = FPlayers.getInstance().getByPlayer(player);

        // Player is bypassing
        if (Conf.playersWhoBypassAllProtection.contains(factionsPlayer.getName()) || factionsPlayer.isAdminBypassing())
            return true;

        // Faction at miner location
        Faction otherFaction = Board.getInstance().getFactionAt(new FLocation(location));

        // Return true if it's wilderness
        if (otherFaction.isWilderness())
            return true;

        // Own claim
        if (factionsPlayer.getFactionId().equals(otherFaction.getId())) {
            return main.getConfig().getStringList(configSectionName + ".roles")
                    .contains(factionsPlayer.getRole().toString().toUpperCase());
        }

        // Not own claim
        return false;
    }

    /**
     * Check whether there is already an operation in that chunk
     *
     * @param chunk The chun to check
     * @return true if the chunk is involved in an operation
     */
    private boolean currentlyProcessed(Chunk chunk) {
        for (Set<Chunk> chunkSet : main.getActiveOperations().values())
            for (Chunk ch : chunkSet)
                if (chunk.equals(ch))
                    return true;

        return false;
    }

    /**
     * Decrease by one the amount of items in the ItemStack
     *
     * @param item The ItemStack
     * @return the ItemStack with one item less
     */
    private ItemStack removeOneItem(ItemStack item) {
        ItemStack tempItem = item;
        int amount = tempItem.getAmount();

        if (amount == 1) {
            tempItem = null;
        } else {
            tempItem.setAmount(amount - 1);
        }
        return tempItem;
    }

    /**
     * Notify all staffers that the player used a ChunkMiner
     *
     * @param playerName The name of the player who placed the ChunkMiner
     * @param chunk      The chunk mined
     */
    private void notifyStaffers(String playerName, Chunk chunk) {
        for (Player staffer : main.getServer().getOnlinePlayers()) {
            if (!staffer.hasPermission(Const.PERM_NOTIFY_ON_USE))
                continue;

            // TODO: don't send the message to the player who placed the miner (if he is a staffer)

            staffer.sendMessage(ChatUtil.c("minerNotifyStaff").replace("{playerName}", playerName)
                    .replace("{world}", chunk.getWorld().getName())
                    .replace("{x}", String.valueOf(chunk.getX()))
                    .replace("{z}", String.valueOf(chunk.getZ())));
        }
    }


}
