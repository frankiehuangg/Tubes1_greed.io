package Models;

import Enums.*;
import java.util.*;

public class GameObject {
  public UUID id;
  public Integer size;
  public Integer speed;
  public Integer currentHeading;
  public Position position;
  public ObjectTypes gameObjectType;
  public Integer effects = 0;
  public Integer torpedoSalvoCount = 0;
  public Integer superNovaAvailable = 0;
  public Integer teleporterCount = 0;
  public Integer shieldCount = 0;

  public GameObject(UUID id, Integer size, Integer speed, Integer currentHeading, Position position, ObjectTypes gameObjectType) {
    this.id = id;
    this.size = size;
    this.speed = speed;
    this.currentHeading = currentHeading;
    this.position = position;
    this.gameObjectType = gameObjectType;
  }

  public GameObject(UUID id, Integer size, Integer speed, Integer currentHeading, Position position, ObjectTypes gameObjectType, Integer effects, Integer torpedoSalvoCount, Integer superNovaAvailable, Integer teleporterCount, Integer shieldCount) {
    this.id = id;
    this.size = size;
    this.speed = speed;
    this.currentHeading = currentHeading;
    this.position = position;
    this.gameObjectType = gameObjectType;
    this.effects = effects;
    this.torpedoSalvoCount = torpedoSalvoCount;
    this.superNovaAvailable = superNovaAvailable;
    this.teleporterCount = teleporterCount;
    this.shieldCount = shieldCount;
  }

  public Integer getTorpedoSalvoCount() {
    return torpedoSalvoCount;
  }

  public Integer getShieldCount() {
    return shieldCount;
  }

  public Integer getSuperNovaAvailable() {
    return superNovaAvailable;
  }

  public Integer getCurrentHeading() {
    return currentHeading;
  }

  public Integer getEffects() {
    return effects;
  }

  public Integer getTeleporterCount() {
    return teleporterCount;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getSpeed() {
    return speed;
  }

  public void setSpeed(int speed) {
    this.speed = speed;
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public ObjectTypes getGameObjectType() {
    return gameObjectType;
  }

  public void setGameObjectType(ObjectTypes gameObjectType) {
    this.gameObjectType = gameObjectType;
  }

  public void display() {
    System.out.printf("[id: %s, size: %d, speed: %d, " +
            "currentHeading: %d, x: %d, y: %d, type: %s, " +
            "Effects: %d, torpedo: %d, supernove: %d, teleporter: %d, shield: %d] \n",id.toString(), size, speed, currentHeading,
            position.x, position.y, gameObjectType.toString(), effects, torpedoSalvoCount, superNovaAvailable, teleporterCount, shieldCount);
  }

  public static GameObject FromStateList(UUID id, List<Integer> stateList)
  {
    Position position = new Position(stateList.get(4), stateList.get(5));
    if (stateList.size() > 9) {
      return new GameObject(id, stateList.get(0), stateList.get(1), stateList.get(2), position, ObjectTypes.valueOf(stateList.get(3)), stateList.get(6), stateList.get(7), stateList.get(8), stateList.get(9), stateList.get(10));
    } else {
      return new GameObject(id, stateList.get(0), stateList.get(1), stateList.get(2), position, ObjectTypes.valueOf(stateList.get(3)));
    }
  }
}
