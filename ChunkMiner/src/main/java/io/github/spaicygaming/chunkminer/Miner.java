package io.github.spaicygaming.chunkminer;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import io.github.spaicygaming.chunkminer.util.Const;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Miner {

    /**
     * Main class instance
     */
    private ChunkMiner main;

    /**
     * The chunk the player placed the miner
     */
    private Chunk chunk;

    /**
     * The player who placed the miner
     */
    private Player player;

    /**
     * WorldGuard instance
     */
    private WorldGuardPlugin worldGuard;

    public Miner(Chunk chunk, Player player, WorldGuardPlugin worldGuard) {
        this.main = ChunkMiner.getInstance();

        this.chunk = chunk;
        this.player = player;
        this.worldGuard = worldGuard;
    }

    /**
     * The blocks inside the chunk to replace with AIR.
     */
    private Set<Block> blocksToRemove = new HashSet<>();

    /**
     * Scan the blocks inside the chunk and add
     * them in {@link #blocksToRemove}.
     * <p>
     * While the operation is in progress the chunk is added
     * to the Set containing currently processed chunks.
     *
     * @return false if the player is not allowed to build in this region
     */
    public boolean scan() {
//		long actionStart = System.currentTimeMillis();
        // Add the processed chunk to the Set (see the javadocs for more information)
        operationStarted();

        World world = chunk.getWorld();

        int x = chunk.getX() << 4;
        int z = chunk.getZ() << 4;

        for (int xx = x; xx < x + 16; xx++)
            for (int zz = z; zz < z + 16; zz++)
                for (int y = Const.MIN_HEIGHT; y < world.getMaxHeight(); y++) {
                    Block currBlock = world.getBlockAt(xx, y, zz);

                    // Check if there is WorldGuard
                    if (worldGuard != null)
                        // Check if the block is in a WorldGuard's protected region and
                        // if the player is allowed to build in that region
                        if (Const.WORLDGUARD_HOOK && !worldGuard.canBuild(player, currBlock))
                            return false;

                    // Skip blocks made of ignored materials and Air
                    if (ignoreMaterial(currBlock.getType()) || currBlock.getType() == Material.AIR)
                        continue;

                    // Add the blocks to the set of blocks to remove on #mine()
                    blocksToRemove.add(currBlock);
                }

        // Remove the chunk from the set
        operationFinished();

//      System.out.println("Scan process took " + (System.currentTimeMillis() - actionStart) + "ms");
        return true;
    }

    /**
     * Check whether blocks with that material should not be removed
     *
     * @param material The material to remove
     * @return true if ignored
     */
    private boolean ignoreMaterial(Material material) {
        return Const.IGNORED_MATERIALS.contains(material);
    }

    /**
     * Returns the amount of blocks in the Chunk that can be removed.
     * {@link #mine()} must be called before.
     *
     * @return the amount of blocks
     */
    public int getBlocksAmount() {
        return blocksToRemove.size();
    }

    /**
     * Replace all the blocks with AIR.
     * <p>
     * While the operation is in progress the chunk is added
     * to the Set containing currently processed chunks.
     */
    public void mine() {
        operationStarted();
        blocksToRemove.forEach(block -> block.setType(Material.AIR));
        operationFinished();
    }

    /**
     * Add the {@link #chunk} in the Set containing all the chunks in which there is an active operation
     * started by the player
     */
    private void operationStarted() {
        Map<Player, Set<Chunk>> operations = main.getActiveOperations();

        if (operations.containsKey(player)) {
            operations.get(player).add(chunk);
        } else {
            operations.put(player, new HashSet<>(Arrays.asList(chunk)));
        }
    }

    /**
     * Remove the {@link #chunk} from the Set in the main class
     * containing all the chunks in which there is an active operation
     */
    private void operationFinished() {
        Map<Player, Set<Chunk>> operations = main.getActiveOperations();

        if (operations.get(player).size() > 1) {
            operations.get(player).remove(chunk);
        } else {
            operations.remove(player);
        }
    }

}
