package multitask2.model.games;

import com.github.forax.zen.KeyboardEvent.Key;
import java.util.random.RandomGenerator;
import multitask2.controller.Input;
import multitask2.model.Game;

public final class Target implements Game {

  /** Largeur du terrain, en unités du modèle (le centre est en (0, 0)). */
  public static final double FIELD_WIDTH = 600;
  /** Hauteur du terrain, en unités du modèle. */
  public static final double FIELD_HEIGHT = 400;
  /** Rayon du joueur. */
  public static final double PLAYER_RADIUS = 15;
  /** Rayon de la cible. */
  public static final double TARGET_RADIUS = 40;

  /** Vitesse du joueur, en unités par seconde. */
  private static final double PLAYER_SPEED = 300;
  /** Temps accordé pour atteindre chaque cible, en secondes. */
  private static final double TIME_PER_TARGET = 11;
  /** Marge minimale entre une cible et le bord du terrain. */
  private static final double TARGET_MARGIN = 10;
  /** Facteur appliqué en diagonale pour ne pas aller plus vite (1 / racine de 2). */
  private static final double DIAGONAL = Math.sqrt(0.5);

  private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

  /** Position du joueur, relative au centre du terrain (y vers le bas). */
  private double playerX, playerY;
  /** Position de la cible, relative au centre du terrain. */
  private double targetX, targetY;
  /** Temps restant pour atteindre la cible actuelle, en secondes. */
  private double timeLeft;

  /** Crée un jeu avec le joueur au centre et une première cible au hasard. */
  public Target() {
    playerX = 0;
    playerY = 0;
    targetX = randomTargetX();
    targetY = randomTargetY();
    timeLeft = TIME_PER_TARGET;
    super();
  }

  @Override
  public boolean update(double dt, Input input, double difficulty) {
    Game.checkInput(dt, input, difficulty);

    var factor = 1 + difficulty / 4;
    movePlayer(dt, input);
    checkTarget();
    timeLeft -= dt * factor;
    return timeLeft > 0;
  }

  /** Déplacement dans 8 directions, à vitesse constante, borné au terrain. */
  private void movePlayer(double dt, Input input) {
    var dx = (input.isKeyDown(Key.D) ? 1 : 0) - (input.isKeyDown(Key.A) ? 1 : 0);
    var dy = (input.isKeyDown(Key.S) ? 1 : 0) - (input.isKeyDown(Key.W) ? 1 : 0);
    var norm = (dx != 0 && dy != 0) ? DIAGONAL : 1;

    playerX = Game.dontExceed(playerX + dx * norm * PLAYER_SPEED * dt,
        FIELD_WIDTH / 2 - PLAYER_RADIUS);
    playerY = Game.dontExceed(playerY + dy * norm * PLAYER_SPEED * dt,
        FIELD_HEIGHT / 2 - PLAYER_RADIUS);
  }

  /** Si le joueur touche la cible : un point, une nouvelle cible, et le chrono repart. */
  private void checkTarget() {
    var distance = Math.hypot(playerX - targetX, playerY - targetY);
    if (distance <= PLAYER_RADIUS + TARGET_RADIUS) {
      targetX = randomTargetX();
      targetY = randomTargetY();
      timeLeft = TIME_PER_TARGET;
    }
  }

  // static : utilisables dans le constructeur avant super()
  private static double randomTargetX() {
    var range = FIELD_WIDTH / 2 - TARGET_RADIUS - TARGET_MARGIN;
    return (RANDOM.nextDouble() * 2 - 1) * range;
  }

  private static double randomTargetY() {
    var range = FIELD_HEIGHT / 2 - TARGET_RADIUS - TARGET_MARGIN;
    return (RANDOM.nextDouble() * 2 - 1) * range;
  }

  public double playerX() {
    return playerX;
  }

  public double playerY() {
    return playerY;
  }

  public double targetX() {
    return targetX;
  }

  public double targetY() {
    return targetY;
  }

  /** Temps restant pour la cible actuelle, en secondes. */
  public double timeLeft() {
    return timeLeft;
  }
}