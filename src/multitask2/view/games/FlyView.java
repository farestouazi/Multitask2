package multitask2.view.games;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;

import multitask2.model.games.Fly;

/**
 * Vue graphique du mini-jeu {@link Fly} : dessine un fond rose uni sur toute la
 * zone fournie, de fines barres noires qui arrivent de la droite et le vaisseau
 * triangulaire du joueur.
 * Il n'y a aucun contour : seules les barres sont noires.
 */
public final class FlyView implements GameView {

  /** Couleur du fond, relevée sur le jeu original. */
  private static final Color BACKGROUND = new Color(0xFEC0E5);
  /** Couleur du vaisseau, relevée sur le jeu original. */
  private static final Color SHIP = new Color(0xA20262);
  /** Couleur des barres d'obstacle, relevée sur le jeu original. */
  private static final Color OBSTACLE = Color.BLACK;
  /** Diamètre d'arrondi des coins du fond, en pixels. */
  private static final int CORNER_ARC = 8;

  /**
   * Conversion entre les coordonnées du modèle et les pixels de la zone de
   * dessin.
   *
   * @param area zone disponible pour le dessin
   */
  private record Layout(Rectangle area) {

    /** @return nombre de pixels par unité horizontale du modèle */
    double scaleX() {
      return area.width / Fly.WIDTH;
    }

    /** @return nombre de pixels par unité verticale du modèle */
    double scaleY() {
      return area.height / Fly.HEIGHT;
    }

    /**
     * @param x abscisse dans les coordonnées du modèle
     * @return abscisse correspondante, en pixels
     */
    int toX(double x) {
      return area.x + (int) (x * scaleX());
    }

    /**
     * @param y ordonnée dans les coordonnées du modèle, l'ordonnée 0 étant en bas
     * @return ordonnée correspondante, en pixels
     */
    int toY(double y) {
      return area.y + area.height - (int) (y * scaleY());
    }
  }

  /** État du mini-jeu de vol lu pour produire le dessin. */
  private final Fly flyer;

  /**
   * Crée une vue associée au mini-jeu fourni.
   *
   * @param flyer mini-jeu dont l'état doit être représenté
   * @throws NullPointerException si {@code flyer} est {@code null}
   */
  public FlyView(Fly flyer) {
    Objects.requireNonNull(flyer);
    this.flyer = flyer;
    super();
  }

  /**
   * Dessine le fond, les barres puis le vaisseau. Les coordonnées du modèle sont
   * mises à l'échelle pour tenir dans le rectangle fourni. Le dessin se fait sur
   * une copie du contexte graphique, limitée à la zone fournie, pour ne pas
   * modifier les réglages utilisés par le reste de l'affichage.
   *
   * @param graphics contexte graphique à utiliser
   * @param area     zone disponible pour le dessin
   * @throws NullPointerException si {@code graphics}, le jeu associé ou
   *                              {@code area} est nul
   */
  @Override
  public void render(Graphics2D graphics, Rectangle area) {
    GameView.checkInput(graphics, flyer, area);

    var g = (Graphics2D) graphics.create();
    try {
      g.setClip(area);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      var layout = new Layout(area);
      drawBackground(g, layout);
      flyer.obstacles().forEach(obstacle -> drawObstacle(g, layout, obstacle));
      drawShip(g, layout);
    } finally {
      g.dispose();
    }
  }

  /**
   * Dessine le fond uni.
   *
   * @param g      contexte graphique
   * @param layout conversion en pixels
   */
  private static void drawBackground(Graphics2D g, Layout layout) {
    var area = layout.area();
    g.setColor(BACKGROUND);
    g.fillRoundRect(area.x, area.y, area.width, area.height, CORNER_ARC, CORNER_ARC);
  }

  /**
   * Dessine une barre d'obstacle : une partie au-dessus de l'ouverture et une
   * partie en dessous.
   *
   * @param g        contexte graphique
   * @param layout   conversion en pixels
   * @param obstacle barre à dessiner
   */
  private static void drawObstacle(Graphics2D g, Layout layout, Fly.Obstacle obstacle) {
    var area = layout.area();
    var x = layout.toX(obstacle.x());
    var width = (int) (Fly.OBSTACLE_WIDTH * layout.scaleX());
    var top = layout.toY(obstacle.gapTop());
    var bottom = layout.toY(obstacle.gapBottom());
    g.setColor(OBSTACLE);
    g.fillRect(x, area.y, width, top - area.y);
    g.fillRect(x, bottom, width, area.y + area.height - bottom);
  }

  /**
   * Dessine le vaisseau sous la forme d'un triangle plein dont la pointe est
   * tournée vers la droite, dans le sens du vol.
   *
   * @param g      contexte graphique
   * @param layout conversion en pixels
   */
  private void drawShip(Graphics2D g, Layout layout) {
    var left = layout.toX(Fly.PLAYER_X - Fly.PLAYER_RADIUS);
    var right = layout.toX(Fly.PLAYER_X + Fly.PLAYER_RADIUS);
    var top = layout.toY(flyer.y() + Fly.PLAYER_RADIUS);
    var bottom = layout.toY(flyer.y() - Fly.PLAYER_RADIUS);
    g.setColor(SHIP);
    g.fillPolygon(new int[] { left, left, right }, new int[] { top, bottom, (top + bottom) / 2 }, 3);
  }
}
