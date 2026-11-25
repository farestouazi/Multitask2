package multitask2.model.games;

import multitask2.controller.Input;
import multitask2.model.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.github.forax.zen.KeyboardEvent.Key;

/**
 * Mini-jeu de vol : le joueur pilote un vaisseau à hauteur variable, qui monte
 * tant que la touche est maintenue et descend dès qu'elle est relâchée, pour
 * passer dans l'ouverture de murs qui arrivent de la droite.
 *
 * <p>
 * La vitesse des murs augmente avec la difficulté, leur cadence d'apparition
 * aussi, et leur ouverture se réduit. Pour que le jeu reste toujours faisable,
 * l'ouverture d'un mur ne s'éloigne jamais de celle du mur précédent de plus
 * que ce que le vaisseau peut parcourir entre les deux. Toucher un mur met fin
 * à la partie ; toucher le sol ou le plafond est sans danger.
 * </p>
 */
public final class Fly implements Game {

  /** Touche du mini-jeu : la maintenir pour monter, la relâcher pour descendre. */
  public static final Key KEY = Key.SPACE;

  /** Largeur de l'aire de jeu dans les coordonnées du modèle. */
  public static final double WIDTH = 200;
  /** Hauteur de l'aire de jeu dans les coordonnées du modèle, l'ordonnée 0 étant en bas. */
  public static final double HEIGHT = 100;
  /** Abscisse fixe du vaisseau dans les coordonnées du modèle. */
  public static final double PLAYER_X = 40;
  /** Rayon du vaisseau, utilisé pour le dessiner et pour détecter les collisions. */
  public static final double PLAYER_RADIUS = 4;
  /** Épaisseur d'une barre d'obstacle dans les coordonnées du modèle. */
  public static final double OBSTACLE_WIDTH = 4;

  /** Vitesse verticale du vaisseau, en montée comme en descente, en unités par seconde. */
  private static final double VERTICAL_SPEED = 40;
  /** Vitesse initiale des murs, en unités par seconde. */
  private static final double BASE_OBSTACLE_SPEED = 60;
  /** Augmentation de la vitesse des murs par point de difficulté. */
  private static final double OBSTACLE_SPEED_STEP = 6;
  /** Vitesse maximale des murs, en unités par seconde. */
  private static final double MAX_OBSTACLE_SPEED = 140;
  /** Intervalle initial entre deux apparitions de murs, en secondes. */
  private static final double BASE_SPAWN_INTERVAL = 2.0;
  /** Intervalle minimal entre deux apparitions de murs, en secondes. */
  private static final double MIN_SPAWN_INTERVAL = 1.2;
  /** Diminution de l'intervalle d'apparition par point de difficulté. */
  private static final double SPAWN_INTERVAL_STEP = 0.1;
  /** Demi-hauteur initiale de l'ouverture d'un mur. */
  private static final double BASE_GAP_HALF = 20;
  /** Demi-hauteur minimale de l'ouverture d'un mur. */
  private static final double MIN_GAP_HALF = 12;
  /** Diminution de la demi-hauteur de l'ouverture par point de difficulté. */
  private static final double GAP_HALF_STEP = 0.5;
  /** Distance minimale entre une ouverture et le sol ou le plafond. */
  private static final double GAP_MARGIN = 2;
  /** Part de la distance parcourable entre deux murs dont l'ouverture peut s'éloigner. */
  private static final double REACH_RATIO = 0.6;

  /**
   * Mur vertical percé d'une ouverture, qui se déplace vers la gauche.
   *
   * @param x         abscisse du bord gauche du mur, dans les coordonnées du modèle
   * @param gapCenter ordonnée du centre de l'ouverture
   * @param gapHalf   demi-hauteur de l'ouverture
   */
  public record Obstacle(double x, double gapCenter, double gapHalf) {

    /**
     * Retourne l'ordonnée du bas de l'ouverture.
     *
     * @return ordonnée du bord inférieur de l'ouverture
     */
    public double gapBottom() {
      return gapCenter - gapHalf;
    }

    /**
     * Retourne l'ordonnée du haut de l'ouverture.
     *
     * @return ordonnée du bord supérieur de l'ouverture
     */
    public double gapTop() {
      return gapCenter + gapHalf;
    }

    /**
     * Crée un mur déplacé vers la gauche, en conservant son ouverture.
     *
     * @param dx distance dont le mur avance
     * @return nouvelle valeur immuable représentant le mur déplacé
     */
    private Obstacle moveLeft(double dx) {
      return new Obstacle(x - dx, gapCenter, gapHalf);
    }
  }

  /** Ordonnée du centre du vaisseau. */
  private double y;
  /** Murs présents dans l'aire de jeu. */
  private final List<Obstacle> obstacles;
  /** Temps restant avant la prochaine apparition d'un mur, en secondes. */
  private double spawnTimer;
  /** Ordonnée du centre de l'ouverture du dernier mur créé. */
  private double lastGapCenter;
  /** Générateur utilisé pour choisir l'ouverture des murs. */
  private final Random random;

  /**
   * Crée le jeu avec le vaisseau à mi-hauteur et initialise le compte à rebours.
   */
  public Fly() {
    y = HEIGHT / 2;
    obstacles = new ArrayList<>();
    spawnTimer = 1;
    lastGapCenter = HEIGHT / 2;
    random = new Random();
    super();
  }

  /**
   * Déplace le vaisseau, fait apparaître et avance les murs, puis vérifie si le
   * vaisseau en touche un.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param input      état des entrées clavier
   * @param difficulty niveau de difficulté global
   * @return {@code true} si le vaisseau ne touche aucun mur, {@code false} en
   *         cas de collision
   * @throws IllegalArgumentException si {@code dt} ou {@code difficulty} est
   *                                  négatif
   * @throws NullPointerException     si {@code input} est {@code null}
   */
  @Override
  public boolean update(double dt, Input input, double difficulty) {
    Game.checkInput(dt, input, difficulty);

    movePlayer(dt, input);
    spawnObstacles(dt, difficulty);
    moveObstacles(dt, difficulty);
    return obstacles.stream().noneMatch(this::hits);
  }

  /**
   * Fait monter le vaisseau si la touche est maintenue, descendre sinon, sans
   * sortir de l'aire de jeu.
   *
   * @param dt    durée du pas de simulation, en secondes
   * @param input état des entrées clavier
   */
  private void movePlayer(double dt, Input input) {
    var direction = input.isKeyDown(KEY) ? 1 : -1;
    y = Math.clamp(y + direction * VERTICAL_SPEED * dt, PLAYER_RADIUS, HEIGHT - PLAYER_RADIUS);
  }

  /**
   * Diminue le compte à rebours et fait apparaître un mur à droite de l'aire de
   * jeu lorsque celui-ci devient strictement négatif.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param difficulty niveau de difficulté global
   */
  private void spawnObstacles(double dt, double difficulty) {
    spawnTimer -= dt;
    if (spawnTimer < 0) {
      obstacles.add(newObstacle(difficulty));
      spawnTimer = spawnInterval(difficulty);
    }
  }

  /**
   * Crée un mur au bord droit de l'aire de jeu. Son ouverture est tirée au
   * hasard autour de celle du mur précédent, sans s'en éloigner de plus que le
   * vaisseau ne peut parcourir entre deux murs, et sans toucher le sol ou le
   * plafond.
   *
   * @param difficulty niveau de difficulté global
   * @return nouveau mur
   */
  private Obstacle newObstacle(double difficulty) {
    var half = gapHalf(difficulty);
    var reach = VERTICAL_SPEED * spawnInterval(difficulty) * REACH_RATIO;
    var candidate = lastGapCenter + (2 * random.nextDouble() - 1) * reach;
    lastGapCenter = Math.clamp(candidate, half + GAP_MARGIN, HEIGHT - half - GAP_MARGIN);
    return new Obstacle(WIDTH, lastGapCenter, half);
  }

  /**
   * Fait avancer chaque mur vers la gauche à la vitesse déterminée par la
   * difficulté, puis retire ceux qui sont entièrement sortis de l'aire de jeu.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param difficulty niveau de difficulté global
   */
  private void moveObstacles(double dt, double difficulty) {
    var speed = obstacleSpeed(difficulty);
    obstacles.replaceAll(o -> o.moveLeft(dt * speed));
    obstacles.removeIf(o -> o.x() + OBSTACLE_WIDTH < 0);
  }

  /**
   * Indique si le vaisseau touche un mur : il doit pour cela recouvrir le mur
   * horizontalement sans être entièrement dans l'ouverture.
   *
   * @param obstacle mur à tester
   * @return {@code true} s'il y a collision
   */
  private boolean hits(Obstacle obstacle) {
    var overlaps = obstacle.x() < PLAYER_X + PLAYER_RADIUS
        && obstacle.x() + OBSTACLE_WIDTH > PLAYER_X - PLAYER_RADIUS;
    var insideGap = y - PLAYER_RADIUS >= obstacle.gapBottom()
        && y + PLAYER_RADIUS <= obstacle.gapTop();
    return overlaps && !insideGap;
  }

  /**
   * Calcule l'intervalle entre deux apparitions de murs.
   *
   * @param difficulty niveau de difficulté global
   * @return intervalle en secondes, jamais inférieur à {@code MIN_SPAWN_INTERVAL}
   */
  private static double spawnInterval(double difficulty) {
    return Math.max(MIN_SPAWN_INTERVAL, BASE_SPAWN_INTERVAL - difficulty * SPAWN_INTERVAL_STEP);
  }

  /**
   * Calcule la vitesse des murs.
   *
   * @param difficulty niveau de difficulté global
   * @return vitesse en unités par seconde, jamais supérieure à
   *         {@code MAX_OBSTACLE_SPEED}
   */
  private static double obstacleSpeed(double difficulty) {
    return Math.min(MAX_OBSTACLE_SPEED, BASE_OBSTACLE_SPEED + difficulty * OBSTACLE_SPEED_STEP);
  }

  /**
   * Calcule la demi-hauteur de l'ouverture des murs.
   *
   * @param difficulty niveau de difficulté global
   * @return demi-hauteur, jamais inférieure à {@code MIN_GAP_HALF}
   */
  private static double gapHalf(double difficulty) {
    return Math.max(MIN_GAP_HALF, BASE_GAP_HALF - difficulty * GAP_HALF_STEP);
  }

  /**
   * Retourne l'ordonnée du centre du vaisseau.
   *
   * @return ordonnée comprise entre {@code PLAYER_RADIUS} et
   *         {@code HEIGHT - PLAYER_RADIUS}
   */
  public double y() {
    return y;
  }

  /**
   * Retourne les murs actuellement présents.
   *
   * <p>
   * La liste ne peut pas être modifiée par l'appelant, mais elle reflète les
   * modifications effectuées lors des mises à jour suivantes du jeu.
   * </p>
   *
   * @return vue non modifiable de la liste des murs
   */
  public List<Obstacle> obstacles() {
    return Collections.unmodifiableList(obstacles);
  }
}
