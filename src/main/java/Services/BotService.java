package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    // Default game states
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

    // Fired or have not fired afterburner
    private boolean startafterburner;

    // Direction of fired teleporter
    private Integer headingTeleporter = -1;

    // List of game objects
	public List<GameObject> foodList;
	public List<GameObject> superFoodList;
	public List<GameObject> nearPlayerList;
	public List<GameObject> torpedoList;
	public List<GameObject> teleporterList;
	public List<GameObject> asteroidList;
	public List<GameObject> gasCloudList;

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }

    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
		// Get list of map objects
		populateObjectData();
		
		// PARAMETERS
		Integer botSize = bot.getSize(); // the radius of spaceship
		Integer mapSize = gameState.world.radius;
		Integer closestBotSize = 0;
		double closestBotDistance = 0;
		double closestSalvoDistance = 100;
		
		// If not start of game, get info on size and distances
		if (!gameState.getGameObjects().isEmpty())
		{
			closestBotSize = nearPlayerList.get(0).getSize();
			closestBotDistance = getDistanceBetween(bot, nearPlayerList.get(0)) - closestBotSize - botSize;
			if (!torpedoList.isEmpty())
				closestSalvoDistance = getDistanceBetween(bot, torpedoList.get(0)) - torpedoList.get(0).getSize() - botSize;
		}


		System.out.println("\nSIZE\t\t: " + botSize);
		System.out.println("MAP SIZE\t: " + mapSize);

        // Mode picking logic
        boolean isDanger = closestBotSize > botSize && closestBotDistance <= 3 * botSize;
		boolean isStrong = botSize >= 100 && closestBotDistance <= 3 * botSize;

        // Pick MODES
		if (isDanger)
		{
			if (closestSalvoDistance <= 50.0)
			{
				System.out.println("\t\t\tDEFEND MODE ");
				defendMode();
			}
			else
			{
				System.out.print("ESCAPE MODE ");
				escapeMode();
			}
		} else if (isStrong) {
			System.out.print("ATTACK MODE ");
			attackMode();
		}
		else
		{
			System.out.print("EAT MODE ");
			eatMode();
		}	

        // Check if we have fired a teleporter and should teleport.
        if (!teleporterList.isEmpty()) {
            System.out.println("Checking fired teleporter");
            boolean shouldDetonate = shouldDetonateTeleporter();
            if (shouldDetonate) {
                System.out.println("teleported!");
                this.playerAction.action = PlayerActions.TELEPORT;
                this.headingTeleporter = -1;
            }
        }


    }

	public void eatMode()
	{
		int botSize = bot.getSize();
		playerAction.action = PlayerActions.FORWARD;

		// Make sure game has started
		if (!gameState.getGameObjects().isEmpty())
		{
            // Calculate the distances between food and choose the most profitable

			double foodDistance = getDistanceBetween(bot, foodList.get(0)) - botSize - foodList.get(0).getSize();
			double superFoodDistance = getDistanceBetween(bot, superFoodList.get(0)) - botSize - superFoodList.get(0).getSize();

			System.out.print(" " + foodDistance + " " + superFoodDistance);

			// priority scale food : superfood == 1 : 2
			if (superFoodDistance <= foodDistance)
			{
				System.out.println(" SUPERFOOD");
				playerAction.heading = getHeadingBetween(superFoodList.get(0));
			}
			else
			{
				System.out.println(" FOOD");
				playerAction.heading = getHeadingBetween(foodList.get(0));
			}
		}
	}

	public void defendMode()
	{
		// activate shield
		if (bot.getSize() > 200)
		{
			var nearestBot = nearPlayerList.get(0);
			int angle = nearestBot.getCurrentHeading();

			playerAction.heading = (angle + 60) % 360;
		}
		if (bot.getShieldCount() > 0)
		{
			System.out.println( "SHIELD ON");
			playerAction.action = PlayerActions.ACTIVATESHIELD;
		}
	}


    public void escapeMode(){
        var ESCAPEDISTANCE = 100;

        if (nearPlayerList.size() == 0){
            return;
        }
        var nearestBot = nearPlayerList.get(0);
		
		// jauhi musuh terdekat
        var distance = getDistanceBetween(bot, nearestBot) - nearestBot.getSize() - bot.getSize();

		// hitung angle
		var angle = nearestBot.getCurrentHeading();

        var distanceAsteroid = getDistanceBetween(bot, asteroidList.get(0)) - bot.getSize() - asteroidList.get(0).getSize();

		System.out.println(angle);

        // Kalau ada bot dalam range escape dengan ukuran yang lebih besar
        if (distance <= ESCAPEDISTANCE && nearestBot.getSize() > bot.getSize()){
            this.playerAction.heading = (angle + 45) % 360;
            if (bot.getSize() > nearestBot.getSize()*2) {
                System.out.println("Initiate After Burner");
                this.playerAction.action= PlayerActions.STARTAFTERBURNER;
                this.startafterburner = true;
            } else {
                System.out.println("Turning Off After Burner");
                this.playerAction.action= PlayerActions.STOPAFTERBURNER;
                this.startafterburner = false;
            }
            return;
        }
        
        // Jika sudah aman
        if ( (distance > ESCAPEDISTANCE || nearestBot.getSize() <= bot.getSize()) && this.startafterburner){
            System.out.println("Turning Off After Burner");
            this.playerAction.action= PlayerActions.STOPAFTERBURNER;
            this.startafterburner = false;
        }

        if (distanceAsteroid < distance){
            this.playerAction.heading = getHeadingBetween(asteroidList.get(0));
            System.out.println("Heading to Asteroid");
        } 
    }

    public void attackMode(){
        var PURSUEDISTANCE = 175;
        var FIRINGDISTANCE = 250; 

        if (nearPlayerList.size() == 0) {
            return;
        }

        var nearestBot = nearPlayerList.get(0);
        var distance = getDistanceBetween(bot, nearestBot) - nearestBot.getSize() - bot.getSize();
        var angle = getHeadingBetween(nearestBot);

        // Kalo ada salvo dan kita dalam range firing, fire salvo
        if (this.bot.torpedoSalvoCount > 0 && distance < FIRINGDISTANCE && nearestBot.getSize() > 15) {
            this.playerAction.heading = getHeadingBetween(nearestBot);
            System.out.println("Shooting salvo");
            nearestBot.display();
            this.playerAction.action = PlayerActions.FIRETORPEDOES;
            System.out.println(this.playerAction.heading);
            return;
        }

        // Kalo ada bot deket kita yang lebih kecil, kejar dia.
        var deltaAngle = Math.abs(playerAction.heading - angle) > 5;
        if (this.bot.size > nearestBot.getSize() + 10 && distance < PURSUEDISTANCE && deltaAngle) {
            System.out.println("Targeting player :");
            nearestBot.display();
            this.playerAction.action = PlayerActions.FORWARD;
            this.playerAction.heading = angle;
            return;
        }

        // Cari apakah ada yang worth it buat di teleport, sekali pake makan 20 size.
        // Player yang kita teleportin harus > 40
        var teleportPlayerList = gameState.getPlayerGameObjects()
                .stream().filter(player -> !player.id.equals(bot.id) && player.getSize() > 40 && player.getSize() < bot.getSize() - 80)
                .sorted(Comparator
                        .comparing(player -> getDistanceBetween(bot, player)))
                .collect(Collectors.toList());

        if (teleportPlayerList.size() > 0 && this.bot.getTeleporterCount() > 0 && teleporterList.isEmpty()) {
            var targetBot = teleportPlayerList.get(0);
            var teleportAngle = getHeadingBetween(targetBot);
            System.out.println("FIRED TELEPORTER");
            this.playerAction.action = PlayerActions.FIRETELEPORT;
            this.playerAction.heading = teleportAngle;
            this.headingTeleporter = teleportAngle;
        }
    }

    public boolean shouldDetonateTeleporter() {
        if (!teleporterList.isEmpty()) {
            var teleporter = teleporterList.get(0);
            System.out.println("Found teleporter");
            teleporter.display();
            System.out.println(headingTeleporter);

            // Check if teleporter is now near a player
            var playerNearby = gameState.getPlayerGameObjects().stream()
                    .filter(player -> !player.id.equals(bot.id) &&
                            getDistanceBetween(teleporter, player) < bot.getSize() + player.getSize() &&
                            player.getSize() < bot.getSize() - 10)
                    .collect(Collectors.toList());

            if (playerNearby.size() == 0) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public void populateObjectData()
    {
        // Find foods in map
        foodList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        // Find superfoods in map
        superFoodList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.SUPERFOOD)
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        // Check if torpedo fired at us
        torpedoList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TORPEDOSALVO &&
                        getHeadingBetween(item) > 30)
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        // Find teleporter fired by us
        teleporterList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && Math.abs(item.getCurrentHeading() - headingTeleporter) < 2)
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        // For list of asteroids and gas cloud
        asteroidList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.ASTEROIDFIELD)
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());
        gasCloudList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD)
                .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        // get list of nearest players
        nearPlayerList = gameState.getPlayerGameObjects()
                .stream().filter(player -> !player.id.equals(bot.id))
                .sorted(Comparator
                        .comparing(player -> getDistanceBetween(bot, player)))
                .collect(Collectors.toList());
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }


    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }


    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }


    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

}
