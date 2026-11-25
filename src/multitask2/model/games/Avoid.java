package multitask2.model.games;

import multitask2.controller.Input;
import multitask2.model.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.github.forax.zen.KeyboardEvent.Key;

/**
 * Mini-jeu d'esquive : le joueur déplace une barre placée au centre de l'aire
 * de jeu pour éviter des missiles qui arrivent des deux côtés sur l'une des
 * rangées.
 *
 * <p>
 * La cadence d'apparition et la vitesse des missiles augmentent avec la
 * difficulté. Un missile qui atteint la barre sur la rangée du joueur met fin à
 * la partie ; sur une autre rangée, il disparaît.
 * </p>
 */
public final class Avoid implements Game {

  /** Largeur de l'aire de jeu dans les coordonnées du modèle. */
  public static final double WIDTH = 100;
  /** Nombre de rangées disponibles pour le joueur et les missiles. */
  public static final int ROWS = 5;
  /** Demi-largeur de la barre du joueur, au centre, dans les coordonnées du modèle. */
  public static final double BAR_HALF_WIDTH = 3;

  /** Abscisse du centre de l'aire de jeu, où se trouve la barre. */
  private static final double CENTER = WIDTH / 2;
  /** Vitesse initiale d'un missile, en unités par seconde. */
  private static final double BASE_MISSILE_SPEED = 16;
  /** Augmentation de la vitesse des missiles par point de difficulté. */
  private static final double MISSILE_SPEED_STEP = 5;
  /** Vitesse maximale d'un missile, en unités par seconde. */
  private static final double MAX_MISSILE_SPEED = 80;
  /** Intervalle initial entre deux apparitions de missiles, en secondes. */
  private static final double BASE_SPAWN_INTERVAL = 1.5;
  /** Intervalle minimal entre deux apparitions de missiles, en secondes. */
  private static final double MIN_SPAWN_INTERVAL = 0.45;
  /** Diminution de l'intervalle d'apparition par point de difficulté. */
  private static final double SPAWN_INTERVAL_STEP = 0.15;
  
  /** Longueur d'un missile, de sa pointe à sa base, dans les coordonnées du modèle. */
  public static final double MISSILE_LENGTH = 6;

  /** Côté de l'aire de jeu d'où arrive un missile. */
  public enum Side {
    /** Le missile arrive de la gauche et se déplace vers la droite. */
    LEFT(1),
    /** Le missile arrive de la droite et se déplace vers la gauche. */
    RIGHT(-1);

    /** Sens du déplacement horizontal : {@code 1} vers la droite, {@code -1} vers la gauche. */
    private final int direction;

    Side(int direction) {
      this.direction = direction;
    }

    /**
     * Retourne le sens du déplacement horizontal des missiles de ce côté.
     *
     * @return {@code 1} vers la droite, {@code -1} vers la gauche
     */
    public int direction() {
      return direction;
    }
  }

  /**
   * Missile qui se rapproche du centre sur une rangée fixe.
   *
   * @param distance distance entre le missile et le centre, dans les coordonnées
   *                 du modèle
   * @param row      indice de la rangée occupée par le missile
   * @param side     côté d'où arrive le missile
   */
  public record Missile(double distance, int row, Side side) {

    /**
     * Retourne l'abscisse du missile dans les coordonnées du modèle.
     *
     * @return abscisse entre {@code 0} et {@link Avoid#WIDTH}
     */
    public double x() {
      return CENTER - side.direction() * distance;
    }

    /**
     * Crée un missile rapproché du centre, en conservant sa rangée et son côté.
     *
     * @param dx distance dont le missile se rapproche
     * @return nouvelle valeur immuable représentant le missile déplacé
     */
    private Missile approach(double dx) {
      return new Missile(distance - dx, row, side);
    }
  }

  /** Rangée actuellement occupée par le joueur. */
  private int row;
  /** Missiles présents dans l'aire de jeu. */
  private final List<Missile> missiles;
  /** Temps restant avant la prochaine apparition d'un missile, en secondes. */
  private double spawnTimer;
  /** Générateur utilisé pour choisir aléatoirement la rangée et le côté des missiles. */
  private final Random random;

  /**
   * Crée le jeu avec le joueur sur la rangée centrale et initialise le compte à
   * rebours.
   */
  public Avoid() {
    row = ROWS / 2;
    missiles = new ArrayList<>();
    spawnTimer = 1;
    random = new Random();
    super();
  }

  /**
   * Traite les changements de rangée, fait apparaître et avance les missiles,
   * puis vérifie si l'un d'eux a atteint la barre du joueur.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param input      état des entrées clavier
   * @param difficulty niveau de difficulté global
   * @return {@code true} si aucun missile n'a atteint la barre sur la rangée du
   *         joueur, {@code false} en cas de collision
   * @throws IllegalArgumentException si {@code dt} ou {@code difficulty} est
   *                                  négatif
   * @throws NullPointerException     si {@code input} est {@code null}
   */
  @Override
  public boolean update(double dt, Input input, double difficulty) {
    Game.checkInput(dt, input, difficulty);

    readInput(input);
    spawnMissiles(dt, difficulty);
    moveMissiles(dt, difficulty);
    return checkBar();
  }

  /**
   * Déplace le joueur d'au plus une rangée en réponse à un nouvel appui sur la
   * flèche haut ou bas. Le déplacement est limité à l'intervalle des rangées
   * disponibles.
   *
   * @param input état des entrées clavier
   */
  private void readInput(Input input) {
    var dir = (input.isKeyJustPressed(Key.UP) ? 1 : 0) + (input.isKeyJustPressed(Key.DOWN) ? -1 : 0);
    if (dir != 0) {
      row = Game.withinBounds(row + dir, 0, ROWS - 1);
    }
  }

  /**
   * Diminue le compte à rebours et fait apparaître un missile lorsque celui-ci
   * devient strictement négatif.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param difficulty niveau de difficulté global
   */
  private void spawnMissiles(double dt, double difficulty) {
    spawnTimer -= dt;
    if (spawnTimer < 0) {
      missiles.add(newMissile());
      spawnTimer = spawnInterval(difficulty);
    }
  }

  /**
   * Crée un missile au bord de l'aire de jeu, sur une rangée et un côté tirés au
   * hasard.
   *
   * @return nouveau missile, à sa distance maximale du centre
   */
  private Missile newMissile() {
    var side = random.nextBoolean() ? Side.LEFT : Side.RIGHT;
    return new Missile(CENTER, random.nextInt(ROWS), side);
  }

  /**
   * Rapproche chaque missile du centre à la vitesse déterminée par la
   * difficulté.
   *
   * @param dt         durée du pas de simulation, en secondes
   * @param difficulty niveau de difficulté global
   */
  private void moveMissiles(double dt, double difficulty) {
    var speed = missileSpeed(difficulty);
    missiles.replaceAll(m -> m.approach(dt * speed));
  }

  /**
   * Calcule l'intervalle entre deux apparitions de missiles.
   *
   * @param difficulty niveau de difficulté global
   * @return intervalle en secondes, jamais inférieur à {@code MIN_SPAWN_INTERVAL}
   */
  private static double spawnInterval(double difficulty) {
    return Math.max(MIN_SPAWN_INTERVAL, BASE_SPAWN_INTERVAL - difficulty * SPAWN_INTERVAL_STEP);
  }

  /**
   * Calcule la vitesse des missiles.
   *
   * @param difficulty niveau de difficulté global
   * @return vitesse en unités par seconde, jamais supérieure à
   *         {@code MAX_MISSILE_SPEED}
   */
  private static double missileSpeed(double difficulty) {
    return Math.min(MAX_MISSILE_SPEED, BASE_MISSILE_SPEED + difficulty * MISSILE_SPEED_STEP);
  }

  /**
   * Retire les missiles sortis de l'aire de jeu, puis détecte si un missile
   * touche la barre sur la rangée du joueur.
   *
   * @return {@code false} si un missile a touché le joueur ; {@code true} sinon
   */
  private boolean checkBar() {
    missiles.removeIf(Avoid::exited);
    return missiles.stream().noneMatch(m -> touchesBar(m) && m.row() == row);
  }

  /**
   * Indique si un missile recouvre la barre, de l'instant où sa pointe l'atteint
   * à celui où sa base la quitte.
   *
   * @param missile missile à tester
   * @return {@code true} si le missile recouvre la barre
   */
  private static boolean touchesBar(Missile missile) {
    var distance = missile.distance();
    return distance <= BAR_HALF_WIDTH && distance >= -(BAR_HALF_WIDTH + MISSILE_LENGTH);
  }

  /**
   * Indique si un missile est entièrement sorti de l'aire de jeu, par le bord
   * opposé à celui d'où il est parti.
   *
   * @param missile missile à tester
   * @return {@code true} si le missile n'est plus visible
   */
  private static boolean exited(Missile missile) {
    return missile.distance() < -(CENTER + MISSILE_LENGTH);
  }

  /**
   * Retourne la rangée actuellement occupée par le joueur.
   *
   * @return indice de rangée, compris entre {@code 0} et {@code ROWS - 1}
   */
  public int row() {
    return row;
  }

  /**
   * Retourne les missiles actuellement présents.
   *
   * <p>
   * La liste ne peut pas être modifiée par l'appelant, mais elle reflète les
   * modifications effectuées lors des mises à jour suivantes du jeu.
   * </p>
   *
   * @return vue non modifiable de la liste des missiles
   */
  public List<Missile> missiles() {
    return Collections.unmodifiableList(missiles);
  }
}