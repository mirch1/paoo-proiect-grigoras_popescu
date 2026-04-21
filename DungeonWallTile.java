package PaooGame.Tiles;
import PaooGame.Graphics.Assets;


public class DungeonWallTile extends Tile {
    public DungeonWallTile(int id) {
        super(Assets.dungeonWall, id);
    }

    @Override
    public boolean IsSolid() {
        return true;
    }
}
