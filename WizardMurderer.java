import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.powerbot.concurrent.Task;
import org.powerbot.concurrent.strategy.Strategy;
import org.powerbot.game.api.ActiveScript;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.methods.Game;
import org.powerbot.game.api.methods.Settings;
import org.powerbot.game.api.methods.Tabs;
import org.powerbot.game.api.methods.Walking;
import org.powerbot.game.api.methods.Widgets;
import org.powerbot.game.api.methods.input.Mouse;
import org.powerbot.game.api.methods.interactive.NPCs;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.node.GroundItems;
import org.powerbot.game.api.methods.node.SceneEntities;
import org.powerbot.game.api.methods.tab.Inventory;
import org.powerbot.game.api.methods.tab.Skills;
import org.powerbot.game.api.methods.widget.Bank;
import org.powerbot.game.api.methods.widget.Camera;
import org.powerbot.game.api.util.Filter;
import org.powerbot.game.api.util.Random;
import org.powerbot.game.api.util.Time;
import org.powerbot.game.api.wrappers.Area;
import org.powerbot.game.api.wrappers.Tile;
import org.powerbot.game.api.wrappers.interactive.NPC;
import org.powerbot.game.api.wrappers.node.GroundItem;
import org.powerbot.game.api.wrappers.node.Item;
import org.powerbot.game.api.wrappers.node.SceneObject;
import org.powerbot.game.bot.event.listener.PaintListener;

@Manifest(authors = { "Striker" }, name = "WizardMurderer", description = "Kills Wizards near Port Sarim.", version = 1.0)
public class WizardMurderer extends ActiveScript implements PaintListener {
	
	final static int[] MAGES = {2709, 2710, 2711, 2712};
	final static int[] RUNE_IDS = {556, 557, 554, 555};
	final static int WATER_TALLY = 1444;
	static int FOOD_ID = 0;
	final static int BOX = 14664;
	final static int BOOTH = 11758;
    static boolean guiDone = false;
    static boolean lootRunes = false;
    static boolean looting = false;
    static boolean hover = false;
    static boolean lootWhenNo = false;
    static boolean lootTalis = false;
	
	public final Tile[] bankPath = {
			new Tile(2999, 3267, 0), new Tile(3002, 3271, 0),
			new Tile(3004, 3276, 0), new Tile(3005, 3281, 0),
			new Tile(3006, 3286, 0), new Tile(3007, 3291, 0),
			new Tile(3007, 3296, 0), new Tile(007, 3301, 0),
			new Tile(3008, 3306, 0), new Tile(3008, 3311, 0),
			new Tile(3008, 3316, 0), new Tile(3008, 3321, 0),
			new Tile(3007, 3326, 0), new Tile(3007, 3331, 0),
			new Tile(3007, 3336, 0), new Tile(3007, 3341, 0),
			new Tile(3007, 3346, 0), new Tile(3008, 3351, 0),
			new Tile(3008, 3356, 0), new Tile(3013, 3358, 0),
			new Tile(3012, 3356, 0) 
	};
	
	public final Tile[] magePath = {
			new Tile(3013, 3356, 0), new Tile(3009, 3353, 0),
			new Tile(3007, 3348, 0), new Tile(3007, 3343, 0),
			new Tile(3007, 3338, 0), new Tile(3006, 3333, 0),
			new Tile(3006, 3328, 0), new Tile(3006, 3323, 0),
			new Tile(3006, 3318, 0), new Tile(3006, 3313, 0),
			new Tile(3006, 3308, 0), new Tile(3006, 3303, 0),
			new Tile(3006, 3298, 0), new Tile(3005, 3293, 0),
			new Tile(3005, 3288, 0), new Tile(3006, 3283, 0),
			new Tile(3006, 3278, 0), new Tile(3005, 3273, 0),
			new Tile(3002, 3269, 0) 
	};
	
	long startTime, timeran;
	
	int startExpR, startExpA, startExpS, startExpD, expGained, expGainedR, expGainedA, expGainedD, expGainedS, expH;
	String training = "...";
	
	final Area area = new Area(new Tile(2989, 3281, 0), new Tile(3010, 3257, 0));
	final Tile bankTile = new Tile(3012, 3356, 0);
	final Tile mageTile = new Tile(3000, 3266, 0);

	protected void setup() {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					gui gui = new gui();
					gui.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		startExpR = Skills.getExperience(Skills.RANGE);
		startExpA = Skills.getExperience(Skills.ATTACK);
		startExpS = Skills.getExperience(Skills.STRENGTH);
		startExpD = Skills.getExperience(Skills.DEFENSE);	
		startTime = System.currentTimeMillis();

			provide(new hasFood());
			provide(new noFood());	
	}
	
	public class hasFood extends Strategy implements Task {

		@Override
		public void run() {
			setRun();
			setAutoRetaliate();
			if (!atMages()) {				
				Walking.newTilePath(magePath).traverse();
			} else {
				rapeMages();
			}
			if (needEat()) {
				eat();
			}
		}
		
		@Override
		public boolean validate() {
			Item food = Inventory.getItem(FOOD_ID);
			return food != null && guiDone;		
		}				
	}
	
	public class noFood extends Strategy implements Task {

		@Override
		public void run() {
			if (!atBank()) {
				Walking.newTilePath(bankPath).traverse();
			} else {
				doBank();
			}			
		}
		
		@Override
		public boolean validate() {
			Item food = Inventory.getItem(FOOD_ID);
			return food == null && guiDone;		
		}		
		
	}
	
	private boolean atMages() {
		return area.contains(Players.getLocal().getLocation());
	}
	private boolean needEat() {
		return Players.getLocal().getHpPercent() <= 30 + Random.nextInt(-1, 10);
	}
	
	public boolean atBank() {
		SceneObject booth = SceneEntities.getNearest(BOOTH);
		return booth != null && booth.isOnScreen();
	}
																//attacking method
	public void rapeMages() {
		NPC mages = NPCs.getNearest(new Filter<NPC>() {
			
			@Override
			public boolean accept(NPC npc) {
				if (npc.getId() == MAGES[0] || npc.getId() == MAGES[1] || npc.getId() == MAGES[2] || npc.getId() == MAGES[3]) {
					if (!npc.isInCombat()) {
						return true;
					}
				}				
			return false;
			
			}
		});
		if (Players.getLocal().getInteracting() == null) {
			if (lootRunes && !lootWhenNo) {
				loot();
			}
			if (mages != null) {
				if (mages.isOnScreen()) {					
					mages.interact("Attack");
					Time.sleep(Random.nextInt(1000, 2000));				
				} else {
					Tile tile = mages.getLocation();
					Camera.turnTo(mages);
					Time.sleep(Random.nextInt(200, 300));
					if (!mages.isOnScreen()) {
						tile.clickOnMap();
						Time.sleep(Random.nextInt(1000, 2000));
					}
				}
			} else {
				if (lootWhenNo) {
					loot();
				}
			}
		} else {
			if (hover) {
				if (mages.isOnScreen()) {
					if (!mages.contains(Mouse.getLocation()) && !mages.isMoving()) {
						mages.hover();
						Time.sleep(Random.nextInt(100, 200));
					}
				} else {
					Camera.turnTo(mages);
				}
			}
		}
	}														
															//banking method
	private void doBank() {
		if (Inventory.getItem(BOX) == null) {
			if (!Bank.isOpen()) {
				SceneObject booth = SceneEntities.getNearest(BOOTH);
				if (booth != null) {
					booth.interact("Bank");
					Time.sleep(Random.nextInt(1000, 2000));
					if (Players.getLocal().isMoving()) {
						Time.sleep(Random.nextInt(100, 200));
					}
				}
			} else {
				if (Inventory.getCount() == 0) {
					if (Bank.getItem(FOOD_ID) != null) {
						Bank.withdraw(FOOD_ID, 28);
						Time.sleep(Random.nextInt(200, 500));
					} else {
						log.info("Out of food, stopping script...");
						Bank.close();
						Time.sleep(Random.nextInt(200, 500));
						Game.logout(true);
						Time.sleep(3000);
						stop();
					}
				} else {
					Bank.depositInventory();
					Time.sleep(Random.nextInt(200, 500));
				}					
			}
		} else {
			Inventory.getItem(BOX).getWidgetChild().interact("Drop");
			Time.sleep(Random.nextInt(300, 500));
		}
	}
																//eating method
	private void eat() {
		if (Tabs.getCurrent() == Tabs.INVENTORY) {
			Item food = Inventory.getItem(FOOD_ID);
			if (food != null) {
				food.getWidgetChild().interact("Eat");	
				if (Players.getLocal().isInCombat()) {
					Time.sleep(Random.nextInt(3000, 3500));
				} else {
					Time.sleep(Random.nextInt(100, 200));
				}
			}
		} else {
			Tabs.INVENTORY.open();
			Time.sleep(Random.nextInt(100, 200));
		}
	}
															//looting
	private void loot() {
		if (!Inventory.isFull()) {
			GroundItem loot = GroundItems.getNearest(RUNE_IDS);
			if (loot != null) {
				if (loot.isOnScreen() && !Players.getLocal().isMoving()) {
					loot.interact("Take");
					Time.sleep(Random.nextInt(300, 700));
				} else {
					Tile tile = loot.getLocation();
					Camera.turnTo(loot);
					Time.sleep(Random.nextInt(200, 300));
					if (!loot.isOnScreen()) {
						tile.clickOnMap();
						Time.sleep(Random.nextInt(1000, 2000));
					}
				}
			}
			if (lootTalis) {
				GroundItem tally = GroundItems.getNearest(WATER_TALLY);
				if (tally != null) {
					if (tally.isOnScreen()) {
					tally.interact("Take");
					Time.sleep(Random.nextInt(300, 700));
					} else {
						Tile tiles = tally.getLocation();
						Camera.turnTo(tally);
						Time.sleep(Random.nextInt(200, 300));
						if (!tally.isOnScreen()) {
							tiles.clickOnMap();
							Time.sleep(Random.nextInt(1000, 2000));
						}
					}
				}
			}
		}
	}
		
																//set run
	private void setRun() {
		if (!Walking.isRunEnabled()) {
			if (Walking.getEnergy() > 40 + Random.nextInt(0, 10)) {
				Walking.setRun(true);
				Time.sleep(Random.nextInt(400, 700));
			}
		}
	}
	
	private void setAutoRetaliate() {
		if (Settings.get(172) == 1) {
			if (!Tabs.ATTACK.isOpen()) {
				Tabs.ATTACK.open();
				Widgets.get(884, 13).click(true);
				Time.sleep(Random.nextInt(200, 500));
			}
		}
	}
	
	private final Color color1 = new Color(0, 0, 0, 192);
    private final Color color2 = new Color(255, 51, 51);
    private final Color color3 = new Color(240, 240, 240);
    private final Color color4 = new Color(0, 204, 102, 197);

    private final Font font1 = new Font("Arial", 0, 11);
    															//paint, made using Enfilade's Easel
    public void onRepaint(Graphics g1) {
    	timeran = System.currentTimeMillis() - startTime;

        long millis = timeran;
        long hours = millis / (1000 * 60 * 60);
        millis -= hours * (1000 * 60 * 60);
        long minutes = millis / (1000 * 60);
        millis -= minutes * (1000 * 60);
        long seconds = millis / 1000;
        
        expGainedR = Skills.getExperience(Skills.RANGE) - startExpR;
        expGainedA = Skills.getExperience(Skills.ATTACK) - startExpA;
        expGainedD = Skills.getExperience(Skills.DEFENSE) - startExpD;
        expGainedS = Skills.getExperience(Skills.STRENGTH) - startExpS;
        if (expGainedR > 0) {
        	training = "Ranged";
        	expGained = expGainedR;
        } else if (expGainedA > 0) {
        	training = "Attack";
        	expGained = expGainedA;
        } else if (expGainedD > 0) {
        	training = "Defence";
        	expGained = expGainedD;
        } else if (expGainedS > 0) {
        	training = "Strength";
        	expGained = expGainedS;
        }
        expH = (int) ((expGained) * 3600000D / timeran);
        
        Graphics2D g = (Graphics2D)g1;
        g.setColor(color1);
        g.fillRect(370, 11, 134, 52);
        g.setFont(font1);
        g.setColor(color2);
        g.drawString("WizardMurderer by Striker", 375, 26);
        g.setColor(color3);
        g.drawString("Exp Gained: " + expGained, 386, 40);
        g.drawString("Exp/h: " + expH, 386, 55);
        g.setColor(color1);
        g.fillRect(418, 315, 91, 17);
        g.setColor(color4);
        g.drawString("Time ran: " + hours + ":" + minutes + ":" + seconds, 423, 327);
        g.setColor(color1);
        g.fillRect(418, 297, 91, 17);
        g.setColor(color4);
        g.drawString("Training " + training , 423, 309);
    }   
}
