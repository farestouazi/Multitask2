package multitask2.model.games;

import com.github.forax.zen.KeyboardEvent.Key;
import java.util.random.RandomGenerator;
import multitask2.controller.Input;
import multitask2.model.Game;

public final class Balance implements Game {

  /** Demi-longueur de la piste, en unités du modèle. */
  public static final double TRACK_HALF_LENGTH = 50;
  /** Valeur absolue maximale de l'inclinaison de la planche, en degrés. */
  public static final double MAX_TILT_DEGREES = 50;
  /** Rayon de la bille, en unités du modèle (20 px sur une planche de 450 px). */
  public static final double BALL_RADIUS = 21.0 * TRACK_HALF_LENGTH / 225.0;
  /** Profondeur (axe y vers le bas) à laquelle la bille est considérée comme perdue. */
  public static final double FALL_END_Y = 80;

  /** Distance au-delà du bord à partir de laquelle la bille tombe. */
  private static final double EDGE_MARGIN = BALL_RADIUS / 2;

  /** Vitesse de rotation maximale quand une touche est enfoncée, en degrés par seconde. */
  private static final double MAX_TURN_SPEED_DEG_PER_SEC = 115;
  /** Rapidité avec laquelle la vitesse de rotation rejoint sa cible (inertie). */
  private static final double INPUT_SMOOTHING = 3.0;
  /** Force avec laquelle la position de la bille fait pencher la planche. */
  private static final double BALL_WEIGHT = 0.39;
  /** Amortissement de la rotation pour éviter que la planche ne s'emballe. */
  private static final double ROTATION_DAMPING = 4.0;

  /** Accélération gravitationnelle de base utilisée par la simulation. */
  private static final double GRAVITY = 110;
  /** Frottement de la bille, par seconde. */
  private static final double FRICTION = 0.8;
  /** Amplitude des secousses aléatoires appliquées à la bille. */
  private static final double NOISE = 45;

  private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

  /** Position longitudinale de la bille sur la planche, centrée en zéro. */
  private double position;
  /** Vitesse longitudinale de la bille. */
  private double speed;
  /** Inclinaison actuelle de la planche, en degrés. */
  private double tilt;
  /** Vitesse de rotation de la planche, en degrés par seconde. */
  private double angularVelocity;

  /** Vrai quand la bille a quitté la planche et tombe dans le vide. */
  private boolean falling;
  /** Position de la bille en chute, relative au centre de la planche (y vers le bas). */
  private double fallX, fallY;
  /** Vitesse de la bille en chute. */
  private double fallVx, fallVy;

  /**
   * Crée un jeu avec la bille au centre, planche horizontale, et une légère
   * impulsion aléatoire initiale sur la bille.
   */
  public Balance() {
    position = 0;
    speed = (RANDOM.nextDouble() - 0.5) * 13;
    tilt = 0;
    angularVelocity = 0;
    falling = false;
    fallX = 0;
    fallY = 0;
    fallVx = 0;
    fallVy = 0;
    super();
  }

  @Override
  public boolean update(double dt, Input input, double difficulty) {
    Game.checkInput(dt, input, difficulty);

    readInput(dt, input);
    rotatePlank(dt);
    if (falling) {
      fall(dt);
      return fallY < FALL_END_Y;
    }
    positionAndSpeed(dt, difficulty);
    checkBorder();
    return true;
  }

  /** Calcule la vitesse de rotation visée par le joueur et s'en rapproche progressivement. */
  private void readInput(double dt, Input input) {
    var dir = (input.isKeyDown(Key.LEFT) ? -1 : 0) + (input.isKeyDown(Key.RIGHT) ? 1 : 0);
    var target = dir * MAX_TURN_SPEED_DEG_PER_SEC;
    angularVelocity += (target - angularVelocity) * INPUT_SMOOTHING * dt;
  }

  /**
   * Applique le poids de la bille (tant qu'elle est sur la planche),
   * l'amortissement, puis met à jour l'inclinaison.
   */
  private void rotatePlank(double dt) {
    if (!falling) {
      angularVelocity += position * BALL_WEIGHT * dt;
    }
    angularVelocity *= Math.max(0, 1 - ROTATION_DAMPING * dt);

    var wanted = tilt + angularVelocity * dt;
    tilt = Game.dontExceed(wanted, MAX_TILT_DEGREES);
    if (tilt != wanted) {
      angularVelocity = 0; // la planche est en butée
    }
  }

  private void positionAndSpeed(double dt, double difficulty) {
    var factor = 1 + difficulty / 4;
    speed += GRAVITY * Math.sin(Math.toRadians(tilt)) * factor * dt;
    speed += (RANDOM.nextDouble() - 0.5) * NOISE * factor * dt;
    speed *= Math.max(0, 1 - FRICTION * dt);
    position += speed * dt;
  }

  /** Si la bille dépasse le bord, elle quitte la planche et passe en chute libre. */
  private void checkBorder() {
    if (Math.abs(position) <= TRACK_HALF_LENGTH + EDGE_MARGIN) {
      return;
    }
    falling = true;
    var angle = Math.toRadians(tilt);
    var cos = Math.cos(angle);
    var sin = Math.sin(angle);
    // on tourne le point (position, -rayon) de l'angle de la planche
    fallX = position * cos + BALL_RADIUS * sin;
    fallY = position * sin - BALL_RADIUS * cos;
    // la bille partait dans la direction de la planche
    fallVx = speed * cos;
    fallVy = speed * sin;
  }

  /** Chute libre de la bille dans le repère de l'écran. */
  private void fall(double dt) {
    fallVy += GRAVITY * dt;
    fallX += fallVx * dt;
    fallY += fallVy * dt;
  }

  public double position() {
    return position;
  }

  public double tilt() {
    return tilt;
  }

  /** Retourne {@code true} si la bille a quitté la planche. */
  public boolean isFalling() {
    return falling;
  }

  /** Abscisse de la bille en chute, relative au centre de la planche. */
  public double fallX() {
    return fallX;
  }

  /** Ordonnée de la bille en chute, relative au centre de la planche (vers le bas). */
  public double fallY() {
    return fallY;
  }
}