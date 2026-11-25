package multitask2.model.games;

import com.github.forax.zen.KeyboardEvent.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;
import multitask2.controller.Input;
import multitask2.model.Game;

public final class Circle implements Game {

  /** Rayon de l'arc, en unités du modèle (le centre du jeu est en (0, 0)). */
  public static final double ARC_RADIUS = 65;
  /** Ouverture de l'arc plein, en degrés (le reste est la trouée). */
  public static final double ARC_ANGLE_DEGREES = 280;
  /** Distance du centre à laquelle apparaissent les fusées. */
  public static final double SPAWN_DISTANCE = 250;

  /** Nombre maximal de fusées en même temps. */
  private static final int MAX_ROCKETS = 3;
  /** Délai minimal entre deux apparitions, en secondes. */
  private static final double MIN_SPAWN_INTERVAL = 2;
  /** Délai maximal entre deux apparitions, en secondes. */
  private static final double MAX_SPAWN_INTERVAL = 6;

  /** Vitesse de rotation maximale de l'arc, en degrés par seconde. */
  private static final double MAX_TURN_SPEED = 220;
  /** Rapidité avec laquelle la vitesse de rotation rejoint sa cible (inertie). */
  private static final double INPUT_SMOOTHING = 12.0;

  /** Vitesse maximale des fusées. */
  private static final double MAX_ROCKET_SPEED = 350;
  /** Vitesse initiale, très faible pour laisser la courbe exponentielle démarrer. */
  private static final double INITIAL_ROCKET_SPEED = 0.000001;
  /** Force de l'accélération exponentielle des fusées. */
  private static final double EXPONENTIAL_GROWTH_RATE = 1.5;
  /** Distance au centre à laquelle la fusée s'arrête. */
  private static final double STOP_DISTANCE = 8;
  
  /** Durée du fondu d'apparition d'une fusée, en secondes. */
  private static final double APPEAR_DURATION = 0.5;
  /** Temps pendant lequel la fusée reste immobile après son apparition (fondu inclus), en secondes. */
  private static final double SPAWN_HOLD = 1.0;

  private static final double FADE_DURATION = 0.2;
  private static final double INITIAL_HOLD_DURATION = 0;
  private static final double MIN_HOLD_DURATION = 0;
  
  private static final double HOLD_DECAY_PER_SECOND = 0.01;

  private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

  /** Une fusée en vol, arrêtée ou en train de s'effacer. */
  public static final class Rocket {
    private double x, y;
    private final double dirX, dirY;
    private double speed = INITIAL_ROCKET_SPEED;
    /** Vrai quand la collision avec l'arc a déjà été testée. */
    private boolean passedArc;
    /** Vrai quand la fusée a atteint le centre et s'est arrêtée. */
    private boolean arrived;
    private double fadeTime;
    /** Temps écoulé depuis l'apparition de la fusée, en secondes. */
    private double age;
    /** Durée d'attente de cette fusée, fixée à son arrivée au centre. */
    private double hold = INITIAL_HOLD_DURATION;

    private Rocket(double x, double y, double dirX, double dirY) {
      this.x = x;
      this.y = y;
      this.dirX = dirX;
      this.dirY = dirY;
    }

    /** Abscisse relative au centre. */
    public double x() {
      return x;
    }

    /** Ordonnée relative au centre (vers le bas). */
    public double y() {
      return y;
    }

    /** Angle de la fusée en radians, dans le repère écran (y vers le bas). */
    public double angle() {
      return Math.atan2(dirY, dirX);
    }

    /** Opacité : 1 en vol, puis décroît jusqu'à 0 pendant le fondu. */
    /** Opacité : fondu à l'apparition, puis fondu à la disparition une fois arrivée au centre. */
    public double opacity() {
      var appear = Math.min(1, age / APPEAR_DURATION);
      var disappear = arrived ? Math.max(0, 1 - fadeTime / FADE_DURATION) : 1;
      return Math.min(appear, disappear);
    }
  }

  /** Angle de départ de l'arc, en degrés, dans [0, 360). */
  private double angle;
  /** Vitesse de rotation de l'arc, en degrés par seconde. */
  private double angularVelocity;

  private final List<Rocket> rockets;
  /** Temps restant avant la prochaine tentative d'apparition, en secondes. */
  private double spawnTimer;
  /** Temps de jeu écoulé, en secondes. */
  private double elapsed;

  /** Crée un jeu avec un arc orienté au hasard ; la première fusée arrive tout de suite. */
  public Circle() {
    angle = RANDOM.nextDouble() * 360;
    angularVelocity = 0;
    rockets = new ArrayList<>();
    spawnTimer = 0;
    elapsed = 0;
    super();
  }

  @Override
  public boolean update(double dt, Input input, double difficulty) {
    Game.checkInput(dt, input, difficulty);

    var factor = 1 + difficulty / 4;
    elapsed += dt;
    var hold = Math.max(MIN_HOLD_DURATION, INITIAL_HOLD_DURATION - elapsed * HOLD_DECAY_PER_SECOND);
    readInput(dt, input);
    trySpawn(dt);

    var hit = false;
    for (var rocket : rockets) {
      moveRocket(rocket, dt, factor, hold);
      if (hitsArc(rocket)) {
        hit = true;
      }
    }
    rockets.removeIf(rocket ->
    rocket.arrived
        ? rocket.fadeTime >= rocket.hold + FADE_DURATION
        : Math.hypot(rocket.x, rocket.y) > SPAWN_DISTANCE + 50);
    return !hit;
  }

  /** Fait tourner l'arc avec de l'inertie. Q tourne dans un sens, E dans l'autre. */
  private void readInput(double dt, Input input) {
    var dir = (input.isKeyDown(Key.Q) ? 1 : 0) + (input.isKeyDown(Key.E) ? -1 : 0);
    var target = dir * MAX_TURN_SPEED;
    angularVelocity += (target - angularVelocity) * INPUT_SMOOTHING * dt;

    angle = (angle + angularVelocity * dt) % 360;
    if (angle < 0) {
      angle += 360;
    }
  }

  /**
   * Décompte le délai d'apparition. Quand il arrive à zéro, une fusée apparaît
   * s'il y a de la place, puis un nouveau délai aléatoire de 1 à 3 s démarre.
   */
  private void trySpawn(double dt) {
    spawnTimer -= dt;
    if (spawnTimer > 0) {
      return;
    }
    if (rockets.stream().filter(r -> !r.arrived).count() < MAX_ROCKETS) {
      spawnRocket();
    }
    spawnTimer = MIN_SPAWN_INTERVAL + RANDOM.nextDouble() * (MAX_SPAWN_INTERVAL - MIN_SPAWN_INTERVAL);
  }

  /** Fait apparaître une fusée à un angle aléatoire, visant le centre. */
  private void spawnRocket() {
    var spawnAngle = RANDOM.nextDouble() * 2 * Math.PI;
    var cos = Math.cos(spawnAngle);
    var sin = Math.sin(spawnAngle);
    rockets.add(new Rocket(cos * SPAWN_DISTANCE, sin * SPAWN_DISTANCE, -cos, -sin));
  }

  /** Accélération exponentielle puis déplacement ; à l'arrivée la fusée s'arrête et s'efface. */
  private static void moveRocket(Rocket rocket, double dt, double factor, double hold) {
    rocket.age += dt;

    if (rocket.arrived) {
      rocket.fadeTime += dt;
      return;
    }

    // la fusée reste immobile pendant l'attente
    if (rocket.age < SPAWN_HOLD) {
      return;
    }
    
    if (rocket.arrived) {
      rocket.fadeTime += dt;
      return;
    }

    rocket.speed += (1.0 + rocket.speed * EXPONENTIAL_GROWTH_RATE) * factor * dt;
    rocket.speed = Math.min(rocket.speed, MAX_ROCKET_SPEED * factor);

    var distance = Math.hypot(rocket.x, rocket.y);
    var step = rocket.speed * dt;

    if (distance - step <= STOP_DISTANCE) {
      var stopAt = Math.min(distance, STOP_DISTANCE);
      rocket.x = -rocket.dirX * stopAt;
      rocket.y = -rocket.dirY * stopAt;
      rocket.arrived = true;
      rocket.fadeTime = 0;
      rocket.hold = hold;
      return;
    }

    rocket.x += rocket.dirX * step;
    rocket.y += rocket.dirY * step;
  }
  /**
   * Teste la collision une seule fois, au moment où la fusée franchitu le rayon de l'arc.
   * Retourne {@code true} si elle touche la partie pleine de l'arc.
   */
  private boolean hitsArc(Rocket rocket) {
    if (rocket.arrived || rocket.passedArc || Math.hypot(rocket.x, rocket.y) > ARC_RADIUS) {
      return false;
    }
    rocket.passedArc = true;

    // angle de la fusée, y inversé pour suivre le sens anti-horaire de l'arc
    var rocketAngle = Math.toDegrees(Math.atan2(-rocket.y, rocket.x));
    // angle de la fusée mesuré depuis le début de l'arc
    var relative = ((rocketAngle - angle) % 360 + 360) % 360;
    // l'arc plein couvre [0, ARC_ANGLE_DEGREES], le reste est la trouée
    return relative < ARC_ANGLE_DEGREES;
  }

  /** Angle de départ de l'arc, en degrés. */
  public double angle() {
    return angle;
  }

  /** Fusées actuellement visibles (en vol ou en train de s'effacer). */
  public List<Rocket> rockets() {
    return Collections.unmodifiableList(rockets);
  }
}