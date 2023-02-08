package Enums;

public enum EffectTypes {
  AFTERBURNER(1),
  ASTEROIDFIELD(2),
  GASCLOUD(4),
  SUPERFOOD(8),
  SHIELD(16);

  public final Integer value;

  EffectTypes(Integer value) {
    this.value = value;
  }

  public static EffectTypes valueOf(Integer value) {
    for (EffectTypes objectType : EffectTypes.values()) {
      if (objectType.value == value) return objectType;
    }

    throw new IllegalArgumentException("Value not found");
  }
}
