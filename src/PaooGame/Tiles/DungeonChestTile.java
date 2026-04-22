package PaooGame.Tiles;
import PaooGame.Graphics.Assets;


public class DungeonChestTile extends Tile {
    public DungeonChestTile(int id) {
        super(Assets.dungeonChest, id);
    }

    @Override
    public boolean IsSolid() {
        return true;
    }
}
