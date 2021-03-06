package com.redomar.game.level;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.redomar.game.Game;
import com.redomar.game.entities.Entity;
import com.redomar.game.entities.Player;
import com.redomar.game.entities.PlayerMP;
import com.redomar.game.gfx.Screen;
import com.redomar.game.level.tiles.Tile;
import com.redomar.game.lib.utils.Vector2i;
import com.redomar.game.net.packets.Packet01Disconnect;
import com.redomar.game.script.PrintTypes;
import com.redomar.game.script.Printing;

public class LevelHandler {

	private byte[] tiles;
	private int width;
	private int height;
	private List<Entity> entities = new ArrayList<Entity>();
	private List<Entity> entities_p = new ArrayList<Entity>();
	private String imagePath;
	private BufferedImage image;
	private Printing print;

	private Comparator<Node> nodeSorter = new Comparator<Node>() {

		public int compare(Node n0, Node n1) {
			if(n1.fCost < n0.fCost) return +1;
			if(n1.fCost > n0.fCost) return -1;
			return 0;
		}
		
	};
	
	public LevelHandler(String imagePath) {

		if (imagePath != null) {
			this.imagePath = imagePath;
			this.loadLevelFromFile();
		} else {
			tiles = new byte[width * height];
			this.width = 64;
			this.height = 64;
			this.generateLevel();
		}
		
		print = new Printing();
	}

	private void loadLevelFromFile() {
		try {
			this.image = ImageIO.read(Level.class.getResource(this.imagePath));
			this.width = image.getWidth();
			this.height = image.getHeight();
			tiles = new byte[width * height];
			this.loadTiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadTiles() {
		int[] tileColours = this.image.getRGB(0, 0, width, height, null, 0,
				width);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				tileCheck: for (Tile t : Tile.getTiles()) {
					if (t != null
							&& t.getLevelColour() == tileColours[x + y * width]) {
						this.tiles[x + y * width] = t.getId();
						break tileCheck;
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void saveLevelToFile() {
		try {
			ImageIO.write(image, "png",
					new File(Level.class.getResource(this.imagePath).getFile()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void alterTile(int x, int y, Tile newTile) {
		this.tiles[x + y * width] = newTile.getId();
		image.setRGB(x, y, newTile.getLevelColour());
	}

	private void generateLevel() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (x * y % 10 < 7) {
					tiles[x + y * width] = Tile.getGrass().getId();
				} else {
					tiles[x + y * width] = Tile.getStone().getId();
				}
			}
		}
	}

	public synchronized List<Entity> getEntities() {
		return this.entities;
	}
	
	public synchronized List<Entity> getProjectileEntities() {
		return this.entities_p;
	}

	public void tick() {
		for (Entity e : getEntities()) {
			e.tick();
		}
		
		for (Entity e : getProjectileEntities()) {
			e.tick();
		}
		
		for (Tile t : Tile.getTiles()) {
			if (t == null) {
				break;
			}
			t.tick();
		}

	}

	public void renderTiles(Screen screen, int xOffset, int yOffset) {
		if (xOffset < 0) {
			xOffset = 0;
		}
		if (xOffset > ((width << 3) - screen.getWidth())) {
			xOffset = ((width << 3) - screen.getWidth());
		}
		if (yOffset < 0) {
			yOffset = 0;
		}
		if (yOffset > ((height << 3) - screen.getHeight())) {
			yOffset = ((height << 3) - screen.getHeight());
		}

		screen.setOffset(xOffset, yOffset);

		for (int y = (yOffset >> 3); y < (yOffset + screen.getHeight() >> 3) + 1; y++) {
			for (int x = (xOffset >> 3); x < (xOffset + screen.getWidth() >> 3) + 1; x++) {
				getTile(x, y).render(screen, this, x << 3, y << 3);
			}
		}
	}

	public void renderEntities(Screen screen) {
		for (Entity e : getEntities()) {
			e.render(screen);
		}
	}
	
	public void renderProjectileEntities(Screen screen){
		for (Entity e : getProjectileEntities()){
			e.render(screen);
		}
	}

	public Tile getTile(int x, int y) {
		if (0 > x || x >= width || 0 > y || y >= height) {
			return Tile.getVoid();
		}
		return Tile.getTiles()[tiles[x + y * width]];
	}

	public void addEntity(Entity entity) {
		this.getEntities().add(entity);
		print.print("Added "+entity.getName()+" Entity", PrintTypes.LEVEL);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void addProjectileEntities(Entity entity) {
		this.getProjectileEntities().add(entity);
	}

	public void removeEntity(Entity entity) {
		this.getEntities().remove(entity);
		print.print("Removed "+entity.getName()+" Entity", PrintTypes.LEVEL);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void removeProjectileEntities(Entity entity) {
		this.getProjectileEntities().remove(entity);
	}

	public void removeEntity(String username) {
		int index = 0;
		for (Entity e : getEntities()) {
			if (e instanceof PlayerMP
					&& ((PlayerMP) e).getUsername().equalsIgnoreCase(username)) {
				break;
			}
			index++;
		}
		this.getEntities().remove(index);
		Packet01Disconnect packet = new Packet01Disconnect(Game.getPlayer().getUsername());
		packet.writeData(Game.getSocketClient());
	}

	private int getPlayerMPIndex(String username) {
		int index = 0;
		for (Entity e : getEntities()) {
			if (e instanceof PlayerMP
					&& ((PlayerMP) e).getUsername().equalsIgnoreCase(username)) {
				break;
			}
			index++;
		}
		return index;
	}

	public void movePlayer(String username, int x, int y, int numSteps,
			boolean isMoving, int movingDir) {
		int index = getPlayerMPIndex(username);
		PlayerMP player = (PlayerMP) this.getEntities().get(index);
		player.setX(x);
		player.setY(y);
		player.setNumSteps(numSteps);
		player.setMoving(isMoving);
		player.setMovingDir(movingDir);
	}
	
	public List<Node> findPath(Vector2i start, Vector2i goal){
		List<Node> openList = new ArrayList<Node>();
		List<Node> closedList = new ArrayList<Node>();
		Node current = new Node(start, null, 0, getDistance(start, goal));
		openList.add(current);
		while(openList.size() > 0){
			Collections.sort(openList, nodeSorter);
			current = openList.get(0);
			if(current.tile.equals(goal)){
				List<Node> path = new ArrayList<Node>();
				while (current.parent != null) {
					path.add(current);
					current = current.parent;
				}
				openList.clear();
				closedList.clear();
				return path;
			}
			openList.remove(current);
			closedList.add(current);
			for(int i = 0; i < 9; i++){
				if(i == 4) continue;
				int x = current.tile.getX();
				int y = current.tile.getY();
				int xi = (i % 3) - 1;
				int yi = (i / 3) - 1;
				Tile at = getTile(x + xi,y + yi);
				if(at == null) continue;
				if(at.isSolid()) continue;
				Vector2i a = new Vector2i(x + xi, y + yi);
				double gCost = current.gCost + (getDistance(current.tile, a) == 1 ? 1 : 0.95);
				double hCost = getDistance(a, goal);
				Node node = new Node(a, current, gCost, hCost);
				if(isVectorInList(closedList, a) && gCost >= node.gCost) continue;
				if(!isVectorInList(openList, a) || gCost < node.gCost) openList.add(node);
			}
		}
		closedList.clear();
		return null;
	}
	
	private boolean isVectorInList(List<Node> list, Vector2i vector){
		for(Node n : list){
			if(n.tile.equals(vector)) return true;
		}
		return false;
	}
	
	private double getDistance(Vector2i tile, Vector2i goal){
		double dx = tile.getX() - goal.getX();
		double dy = tile.getY() - goal.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public List<Entity> getEntities(Entity e, int radius){
		List<Entity> result = new ArrayList<Entity>();
		int ex = (int) e.getX();
		int ey = (int) e.getY();
		for (int i = 0; i < entities.size(); i++) {
			Entity entity = entities.get(i);
			int x = (int) entity.getX();
			int y = (int) entity.getY();
			
			int dx = Math.abs(x - ex);
			int dy = Math.abs(y - ey);
			
			double distance = Math.sqrt((dx*2) + (dy*2));
			if(distance <= radius){
				result.add(entity);
			}
		}
		return result;
	}
	
	public List<Player> getPlayers(Entity e, int radius){
		List<Entity> entities = getEntities(e, radius);
		List<Player> result = new ArrayList<Player>();
		for (int i = 0; i < entities.size(); i++) {
			if (entities.get(i) instanceof Player) {
				result.add((Player) entities.get(i));
			}
		}
		return result;
	}

}
