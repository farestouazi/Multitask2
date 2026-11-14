package multitask2.model.games;

import multitask2.controller.Input;
import multitask2.model.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.github.forax.zen.KeyboardEvent.Key;

/**
 * Mini-jeu de saut : un demi-cercle avance sur un sol percé de trous et le
 * joueur doit sauter au bon moment pour passer par-dessus. Le demi-cercle reste
 * à une abscisse fixe, ce sont les trous qui défilent vers la gauche. Tomber
 * dans un trou met fin à la partie.
 *
 * <p>
 * La vitesse du sol, la largeur des trous et leur cadence dépendent de la
 * difficulté. Pour que le jeu reste toujours faisable, la largeur d'un trou ne
 * dépasse jamais une fraction de la distance parcourue pendant un saut, et deux
 * trous sont toujours assez éloignés pour qu'on puisse atterrir puis sauter à
 * nouveau.
 * </p>
 */
public final class Jump implements Game {

  /** Touche du mini-jeu : appuyer dessus pour sauter. */
  public static final Key KEY = Key.SHIFT;

  /** Largeur de l'aire de jeu dans les coordonnées du modèle. */
  public static final double WIDTH = 200;
  /** Hauteur de l'aire de jeu dans les coordonnées du modèle, l'ordonnée 0 étant en bas. */
  public static final double HEIGHT = 100;
  /** Ordonnée du sol dans les coordonnées du modèle. */
  public static final double GROUND_Y = 35;
  /** Abscisse fixe du demi-cercle dans les coordonnées du modèle. */
  public static final double PLAYER_X = 50;
  /** Rayon du demi-cercle, utilisé pour le dessiner. */
  public static final double PLAYER_RADIUS = 6;

  /** Vitesse verticale donnée au demi-cercle au début d'un saut, en unités par seconde. */
  private static final double JUMP_SPEED = 70;
  /** Accélération de la chute, en unités par seconde carrée. */
  private static final double GRAVITY = 220;
  /** Durée d'un saut complet, en secondes. */
  private static final double AIR_TIME = 2 * JUMP_SPEED / GRAVITY;
  /** Part de la distance parcourue pendant un saut que peut mesurer un trou. */
  private static final double HOLE_RATIO = 0.6;
  /** Vitesse initiale du sol, en unités par seconde. */
  private static final double BASE_SPEED = 60;
  /** Augmentation de la vitesse du sol par point de difficulté. */
  private static final double SPEED_STEP = 4;
  /** Vitesse maximale du sol, en unités par seconde. */
  private static final double MAX_SPEED = 110;
  /** Largeur initiale d'un trou. */
  private static final double BASE_HOLE_WIDTH = 14;
  /** Augmentation de la largeur des trous par point de difficulté. */
  private static final double HOLE_WIDTH_STEP = 0.8;
  /** Délai initial avant le premier trou, en secondes. */
  private static final double FIRST_HOLE_DELAY = 1.5;
  /** Intervalle minimal initial entre deux trous, en secondes. */
  private static final double BASE_INTERVAL = 2.0;
  /** Intervalle minimal entre deux trous, quelle que soit la difficulté, en secondes. */
  private static final double MIN_INTERVAL = 1.3;
  /** Diminution de l'intervalle minimal par point de difficulté. */
  private static final double INTERVAL_STEP = 0.05;
  /** Durée aléatoire ajoutée à l'intervalle minimal, en secondes. */
  private static final double INTERVAL_JITTER = 0.8;

  /**
   * Trou du sol, qui se déplace vers la gauche.
   *
   * @param x     abscisse du bord gauche du trou, dans les coordonnées du modèle
   * @param width largeur du trou
   */
  public record Hole(double x, double width) {

    /**
     * Retourne l'abscisse du bord droit du trou.
     *
     * @return abscisse du bord droit
     */
    public double end() {
      return x + width;
    }

    /**
     * Crée un trou déplacé vers la gauche.
     *
     * @param dx distance dont le trou avance
     * @return nouvelle valeur immuable représentant le trou déplacé
     */
    private Hole moveLeft(double dx) {
      return new Hole(x - dx, width);
    }
  }

  /** Hauteur du demi-cercle au-dessus du sol, 0 lorsqu'il est posé. */
  private double height;
  /** Vitesse verticale du demi-cercle, positive vers le haut. */
  private double verticalSpeed;
  /** Trous présents dans l'aire de jeu. */
  private final List<Hole> holes;
  /** Temps restant avant la prochaine apparition d'un trou, en secondes. */
  private double spawnTimer;
  /** Générateur utilisé pour espacer les trous. */
  private final Random random;

  /**
   * Crée le jeu avec le demi-cercle posé sur le sol et sans trou.
   */
  public Jump() {
    holes = new ArrayList<>();
    spawnTimer = FIRST_HOLE_DELAY;
    random = new Random();
    super();
  }

  /**
   * Fait sauter le demi-cercle si la touche vient d'être pressée, fait défiler
   * les trous puis vérifie que le demi-cercle n'est pas tombé dans l'un d'eux.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param input      état des entrées clavier
   * @param difficulty niveau de difficulté global
   * @return {@code true} si le demi-cercle ne tombe pas, {@code false} sinon
   * @throws IllegalArgumentException si {@code dt} ou {@code difficulty} est
   *                                  négatif
   * @throws NullPointerException     si {@code input} est {@code null}
   */
  @Override
  public boolean update(double dt, Input input, double difficulty) {
    Game.checkInput(dt, input, difficulty);

    movePlayer(dt, input);
    spawnHoles(dt, difficulty);
    moveHoles(dt, difficulty);
    return !isFalling();
  }

  /**
   * Déclenche un saut si le demi-cercle est posé et que la touche vient d'être
   * pressée, puis applique la gravité.
   *
   * @param dt    durée du pas de simulation, en secondes
   * @param input état des entrées clavier
   */
  private void movePlayer(double dt, Input input) {
    if (height == 0 && input.isKeyJustPressed(KEY)) {
      verticalSpeed = JUMP_SPEED;
    }
    if (height > 0 || verticalSpeed > 0) {
      verticalSpeed -= GRAVITY * dt;
      height += verticalSpeed * dt;
    }
    if (height <= 0) {
      height = 0;
      verticalSpeed = 0;
    }
  }

  /**
   * Diminue le compte à rebours et fait apparaître un trou à droite de l'aire de
   * jeu lorsque celui-ci devient strictement négatif.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param difficulty niveau de difficulté global
   */
  private void spawnHoles(double dt, double difficulty) {
    spawnTimer -= dt;
    if (spawnTimer < 0) {
      holes.add(new Hole(WIDTH, holeWidth(difficulty)));
      spawnTimer = minInterval(difficulty) + random.nextDouble() * INTERVAL_JITTER;
    }
  }

  /**
   * Fait avancer chaque trou vers la gauche à la vitesse déterminée par la
   * difficulté, puis retire ceux qui sont entièrement sortis de l'aire de jeu.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param difficulty niveau de difficulté global
   */
  private void moveHoles(double dt, double difficulty) {
    var speed = groundSpeed(difficulty);
    holes.replaceAll(h -> h.moveLeft(dt * speed));
    holes.removeIf(h -> h.end() < 0);
  }

  /**
   * Indique si le demi-cercle est posé au-dessus d'un trou : son centre est alors
   * au-dessus du vide et il tombe.
   *
   * @return {@code true} s'il est posé sur le sol au-dessus d'un trou
   */
  private boolean isFalling() {
    return height == 0 && holes.stream().anyMatch(h -> h.x() <= PLAYER_X && PLAYER_X < h.end());
  }

  /**
   * Calcule la vitesse de défilement du sol.
   *
   * @param difficulty niveau de difficulté global
   * @return vitesse en unités par seconde, jamais supérieure à {@code MAX_SPEED}
   */
  private static double groundSpeed(double difficulty) {
    return Math.min(MAX_SPEED, BASE_SPEED + difficulty * SPEED_STEP);
  }

  /**
   * Calcule la largeur des trous, bornée pour qu'un saut puisse toujours les
   * franchir.
   *
   * @param difficulty niveau de difficulté global
   * @return largeur du trou, en unités du modèle
   */
  private static double holeWidth(double difficulty) {
    var wanted = BASE_HOLE_WIDTH + difficulty * HOLE_WIDTH_STEP;
    return Math.min(wanted, HOLE_RATIO * groundSpeed(difficulty) * AIR_TIME);
  }

  /**
   * Calcule l'intervalle minimal entre deux apparitions de trous.
   *
   * @param difficulty niveau de difficulté global
   * @return intervalle en secondes, jamais inférieur à {@code MIN_INTERVAL}
   */
  private static double minInterval(double difficulty) {
    return Math.max(MIN_INTERVAL, BASE_INTERVAL - difficulty * INTERVAL_STEP);
  }

  /**
   * Retourne la hauteur du demi-cercle au-dessus du sol.
   *
   * @return hauteur, 0 lorsqu'il est posé
   */
  public double height() {
    return height;
  }

  /**
   * Retourne les trous actuellement présents.
   *
   * <p>
   * La liste ne peut pas être modifiée par l'appelant, mais elle reflète les
   * modifications effectuées lors des mises à jour suivantes du jeu.
   * </p>
   *
   * @return vue non modifiable de la liste des trous
   */
  public List<Hole> holes() {
    return Collections.unmodifiableList(holes);
  }
}
