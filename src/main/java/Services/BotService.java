package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

    // private boolean firedTeleporter;
    private boolean startafterburner;
    private Integer headingTeleporter;

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

	public void getData()
	{
		// OBJECTS DATA
		foodList = gameState.getGameObjects()
			.stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
			.sorted(Comparator
					.comparing(item -> getDistanceBetween(bot, item)))
			.collect(Collectors.toList());
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
		teleporterList = gameState.getGameObjects()
			.stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
			.sorted(Comparator
					.comparing(item -> getDistanceBetween(bot, item)))
			.collect(Collectors.toList());

		// For escaping
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

		// SORT PLAYER DR YG TERDEKET SM KITA
        nearPlayerList = gameState.getPlayerGameObjects()
                .stream().filter(player -> !player.id.equals(bot.id))
                .sorted(Comparator
                        .comparing(player -> getDistanceBetween(bot, player)))
                .collect(Collectors.toList());
	}

    public void computeNextPlayerAction(PlayerAction playerAction) {

        //playerAction.action = PlayerActions.FORWARD;
        // Ini semua objek dalam game (asteroid, dst)
        // gameState.getGameObjects().forEach(obj -> obj.display());

        // Ini semua player dalam game
        //  gameState.getPlayerGameObjects().forEach(obj -> obj.display());

        // Ini kita,
        // bot.display();

        // fungsi cari makanan terdekat
        //if (!gameState.getGameObjects().isEmpty()) {
        //    var foodList = gameState.getGameObjects()
        //            .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
        //            .sorted(Comparator
        //                    .comparing(item -> getDistanceBetween(bot, item)))
        //            .collect(Collectors.toList());

        //    playerAction.heading = getHeadingBetween(foodList.get(0));
        //}
        //this.playerAction = playerAction;

        // Attack mode ga bisa jalan menghindar
        // Panggil fungsi attack dulu, kalau dia mau ngelakuin sesuatu
        // nanti dia overwrite perintah feeding module di atas ^^
        // TODO FUNC: get degree evade obstacle
		
		// Get sorted data
		getData();
		
		// PARAMETERS
		Integer botSize = bot.getSize(); // the radius of space ship
		Integer mapSize = gameState.world.radius;
		Integer closestBotSize = 0;
		double closestBotDistance = 0;
		double closestSalvoDistance = 100;
		
		// If game just started
		if (!gameState.getGameObjects().isEmpty())
		{
			closestBotSize = nearPlayerList.get(0).getSize();
			closestBotDistance = getDistanceBetween(bot, nearPlayerList.get(0)) - closestBotSize - botSize;
			if (!torpedoList.isEmpty())
				closestSalvoDistance = getDistanceBetween(bot, torpedoList.get(0)) - torpedoList.get(0).getSize() - botSize;
		}


		System.out.println("\nSIZE\t\t: " + botSize);
		System.out.println("MAP SIZE\t: " + mapSize); 


		// Pick MODES
		if (closestBotSize > botSize && closestBotDistance <= 3 * botSize)
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
		}
		else if (botSize >= 100 && closestBotDistance <= 3 * botSize)
		{
			System.out.print("ATTACK MODE ");
			attackMode();
		}
		else if (botSize <= 60 && (mapSize == null || mapSize > 800) && closestBotDistance >= 50)
		{
			System.out.print("EAT MODE ");
			eatMode();
		}
		else {	
			System.out.print("FIND MODE ");

			if(nearPlayerList.size() == 0){
				return;
			}

			var target = bot;

			for (int i = 0; i < nearPlayerList.size(); i++)
				if (nearPlayerList.get(i).getSize() <= botSize)
					target = nearPlayerList.get(i);

			// smaller bot exists
			if (target != bot)
			{
				System.out.println(target.getId());

				playerAction.action = PlayerActions.FORWARD;
				playerAction.heading = getHeadingBetween(target);
			}
			else
			{
				eatMode();
			}
		}	
		


        // PUT URGENT ACTIONS HERE
        // Overwrite whatever mode above made.
        if (!teleporterList.isEmpty()) {
            System.out.println("Checking fired teleporter");
            boolean shouldDetonate = shouldDetonateTeleporter();
            if (shouldDetonate) {
                System.out.println("teleported!");
                this.playerAction.action = PlayerActions.TELEPORT;
                // firedTeleporter = false;
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

		return;
	}

	public void defendMode()
	{
		//	var nearestSalvo = torpedoList.get(0);

		//	int salvoAngle = getHeadingBetween(nearestSalvo);
		//	int salvoSpeed = nearestSalvo.getSpeed();
		//	double salvoDistance = getDistanceBetween(bot, nearestSalvo);

		//	var nearestTeleporter = teleporterList.get(0);

		//	int teleporterAngle = getHeadingBetween(nearestTeleporter);
		//	int teleporterSpeed = nearestTeleporter.getSpeed();
		//	double teleporterDistance = getDistanceBetween(bot, nearestTeleporter);
	
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
			//playerAction.setAction(playerActions.ACTIVATESHIELD);

		// Check if we can escape

		// Check if shield is available
		
		// Check if we have teleporter
	}


    public void escapeMode(){
        var ESCAPEDISTANCE = 100;

        if(nearPlayerList.size() == 0){
            return;
        }
        var nearestBot = nearPlayerList.get(0);
		
		// jauhi musuh terdekat
        var distance = getDistanceBetween(bot, nearestBot) - nearestBot.getSize() - bot.getSize();

		// hitung angle
		var angle = nearestBot.getCurrentHeading();

        var distanceAsteroid = getDistanceBetween(bot, asteroidList.get(0)) - bot.getSize();

		System.out.println(angle);


        // Kalau ada bot dalam range escape dengan ukuran yang lebih besar
        if (distance <= ESCAPEDISTANCE && nearestBot.getSize() > bot.getSize()){
            this.playerAction.heading = (angle + 45) % 360;
            // System.out.println("Initiate After Burner");
            // nearestBot.display();
            // this.playerAction.action= PlayerActions.STARTAFTERBURNER;
            // this.startafterburner = true;
            // System.out.println(this.playerAction.heading);
            return;
        }
        
        //Jika sudah aman
        // if ( distance > ESCAPEDISTANCE || nearestBot.getSize() <= bot.getSize()){
            // this.playerAction.action= PlayerActions.STOPAFTERBURNER;
            // System.out.println("Turning Off After Burner");
            // this.startafterburner = false;
            // nearestBot.display();
            // System.out.println(this.playerAction.heading);
        //     return;
        // }

        if(distanceAsteroid < distance){
            this.playerAction.heading = getHeadingBetween(asteroidList.get(0));
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
        // Cari teleporter yang arahnya sama kayak yang kita tembak
        var myTeleporter = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && Math.abs(item.getCurrentHeading() - headingTeleporter) < 4)
                .collect(Collectors.toList());


        // Teleporter dah hilang
        if (myTeleporter.size() == 0) {
            // firedTeleporter = false;
        } else {
            var teleporter = myTeleporter.get(0);
            System.out.println("Found teleporter");
            teleporter.display();
            System.out.println(headingTeleporter);
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
