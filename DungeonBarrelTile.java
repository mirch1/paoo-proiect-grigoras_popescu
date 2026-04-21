package PaooGame.Tiles;
import PaooGame.Graphics.Assets;


public class DungeonBarrelTile extends Tile {
    public DungeonBarrelTile(int id) {
        super(Assets.dungeonBarrel, id);
    }

    @Override
    public boolean IsSolid() {
        return true;
    }
}
