package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

    private boolean firedTeleporter;
    private boolean startafterburner;
    private Integer headingTeleporter;

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

        playerAction.action = PlayerActions.FORWARD;
        // Ini semua objek dalam game (asteroid, dst)
        // gameState.getGameObjects().forEach(obj -> obj.display());

        // Ini semua player dalam game
        // gameState.getPlayerGameObjects().forEach(obj -> obj.display());

        // Ini kita,
//        bot.display();

        // fungsi cari makanan terdekat
        if (!gameState.getGameObjects().isEmpty()) {
            var foodList = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                    .sorted(Comparator
                            .comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());

            playerAction.heading = getHeadingBetween(foodList.get(0));
        }
        this.playerAction = playerAction;

        // Attack mode ga bisa jalan menghindar
        // Panggil fungsi attack dulu, kalau dia mau ngelakuin sesuatu
        // nanti dia overwrite perintah feeding module di atas ^^
        // TODO FUNC: get degree evade obstacle
        if (bot.getSize() > 20) {
            attackMode();
        }

        // PUT URGENT ACTIONS HERE
        // Overwrite whatever mode above made.
        if (firedTeleporter) {
            System.out.println("Checking fired teleporter");
            boolean shouldDetonate = shouldDetonateTeleporter();
            if (shouldDetonate) {
                System.out.println("teleported!");
                this.playerAction.action = PlayerActions.TELEPORT;
                firedTeleporter = false;
            }
        }
    }


    public void escapeMode(){
        var ESCAPEDISTANCE = 100;

        // Sort Player dari yang terdekat (credits: William)
        var nearPlayerList = gameState.getPlayerGameObjects()
                .stream().filter(player -> !player.id.equals(bot.id))
                .sorted(Comparator
                        .comparing(player -> getDistanceBetween(bot, player)))
                .collect(Collectors.toList());
        if(nearPlayerList.size() == 0){
            return;
        }
        var nearestBot = nearPlayerList.get(0);
        var distance = getDistanceBetween(bot, nearestBot) - nearestBot.getSize() - bot.getSize();
        var angle = getHeadingBetween(nearestBot);

        // Kalau ada bot dalam range escape dengan ukuran yang lebih besar
        if(bot.getSize() > 100 && distance <= ESCAPEDISTANCE && nearestBot.getSize() > bot.getSize()){
            this.playerAction.heading = angle;
            System.out.println("Initiate After Burner");
            nearestBot.display();
            this.playerAction.action= PlayerActions.STARTAFTERBURNER;
            this.startafterburner = true;
            System.out.println(this.playerAction.heading);
            return;
        }
        
        //Jika sudah aman
        if((bot.getSize()< 100 || distance > ESCAPEDISTANCE || nearestBot.getSize() <= bot.getSize()) && this.startafterburner){
            this.playerAction.action= PlayerActions.STOPAFTERBURNER;
            System.out.println("Turning Off After Burner");
            this.startafterburner = false;
            nearestBot.display();
            System.out.println(this.playerAction.heading);
            return;
        }
    }

    public void attackMode(){
        var PURSUEDISTANCE = 175;
        var FIRINGDISTANCE = 250;

        // SORT PLAYER DR YG TERDEKET SM KITA
        var nearPlayerList = gameState.getPlayerGameObjects()
                .stream().filter(player -> !player.id.equals(bot.id))
                .sorted(Comparator
                        .comparing(player -> getDistanceBetween(bot, player)))
                .collect(Collectors.toList());

        if (nearPlayerList.size() == 0) {
            return;
        }
        var nearestBot = nearPlayerList.get(0);
        var distance = getDistanceBetween(bot, nearestBot)-nearestBot.getSize() - bot.getSize();
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
                .stream().filter(player -> !player.id.equals(bot.id) && player.getSize() > 40 && player.getSize() < bot.getSize() - 25)
                .sorted(Comparator
                        .comparing(player -> getDistanceBetween(bot, player)))
                .collect(Collectors.toList());

        if (teleportPlayerList.size() > 0 && this.bot.getTeleporterCount() > 0 && firedTeleporter == false) {
            this.firedTeleporter = true;
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
            firedTeleporter = false;
        } else {
            var teleporter = myTeleporter.get(0);
            System.out.println("Found teleporter");
            teleporter.display();
            System.out.println(headingTeleporter);
            var playerNearby = gameState.getPlayerGameObjects().stream()
                    .filter(player -> !player.id.equals(bot.id) &&
                            getDistanceBetween(teleporter, player) < bot.getSize() + player.getSize() &&
                            player.getSize() - 10 < bot.getSize())
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
