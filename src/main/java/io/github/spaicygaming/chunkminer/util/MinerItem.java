package io.github.spaicygaming.chunkminer.util;

import io.github.spaicygaming.chunkminer.ChunkMiner;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MinerItem {

    // Item properties
    private Material material;
    private String displayName;
    private List<String> lore;

    private ItemStack item;

    public MinerItem(ChunkMiner main) {
        main.getLogger().info("Loading ChunkMiner item properties...");
        // Initialize Item properties

        // Check whether the material in the configuration file is a valid material
        String materialName = main.getConfig().getString("MinerItem.material");
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            main.disable("The Material \"" + materialName + "\" does not exist.");
            return;
        }

        displayName = ChatUtil.color("MinerItem.displayName");
        lore = ChatUtil.color(main.getConfig().getStringList("MinerItem.lore"));

        init();

        main.getLogger().info("Item correctly loaded");
    }

    private void init() {
        item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);

        item.setItemMeta(itemMeta);
    }

    /**
     * Give chunk miners to the player
     *
     * @param player The player
     * @param amount The amount of ChunkMiners
     */
    public void give(Player player, int amount) {
        ItemStack tempItemStack = item;
        tempItemStack.setAmount(amount);
        player.getInventory().addItem(tempItemStack);
    }

    /**
     * Check whether the ItemStack is a MinerItem.
     * Ignores ItemStack amount.
     *
     * @param compareItemStack The ItemStack to compare
     * @return true if it is
     */
    public boolean isSimilar(ItemStack compareItemStack) {
        if (compareItemStack == null || !compareItemStack.hasItemMeta())
            return false;

        ItemMeta ciMeta = compareItemStack.getItemMeta();

        return compareItemStack.getType() == material
                && ciMeta.getDisplayName().equals(displayName)
                && ciMeta.getLore().equals(lore);
    }

}