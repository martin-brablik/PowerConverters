package covers1624.lib.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal block position helper with an orientation.
 */
public class BlockPosition {

    public int x;
    public int y;
    public int z;
    public ForgeDirection orientation;

    public BlockPosition(int x, int y, int z) {
        this(x, y, z, ForgeDirection.UNKNOWN);
    }

    public BlockPosition(int x, int y, int z, ForgeDirection orientation) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.orientation = orientation;
    }

    public BlockPosition(TileEntity tile) {
        this(tile.xCoord, tile.yCoord, tile.zCoord);
    }

    /**
     * Moves this position by the given amount in the direction of its orientation.
     */
    public void moveForwards(int steps) {
        x += orientation.offsetX * steps;
        y += orientation.offsetY * steps;
        z += orientation.offsetZ * steps;
    }

    /**
     * Returns the positions next to this one, each with its orientation set to the direction it lies in.
     *
     * @param includeVertical whether the up and down neighbours are included.
     */
    public List<BlockPosition> getAdjacent(boolean includeVertical) {
        List<BlockPosition> positions = new ArrayList<BlockPosition>();
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (!includeVertical && (dir == ForgeDirection.UP || dir == ForgeDirection.DOWN)) {
                continue;
            }
            BlockPosition pos = new BlockPosition(x, y, z, dir);
            pos.moveForwards(1);
            positions.add(pos);
        }
        return positions;
    }

    /**
     * Gets the tile entity next to the given tile in the given direction, or null if that chunk is not loaded.
     */
    public static TileEntity getAdjacentTileEntity(TileEntity tile, ForgeDirection direction) {
        World world = tile.getWorldObj();
        if (world == null) {
            return null;
        }
        int x = tile.xCoord + direction.offsetX;
        int y = tile.yCoord + direction.offsetY;
        int z = tile.zCoord + direction.offsetZ;
        if (!world.blockExists(x, y, z)) {
            return null;
        }
        return world.getTileEntity(x, y, z);
    }
}
