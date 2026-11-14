package multitask2.view.games;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;

import multitask2.model.games.Jump;

/**
 * Vue graphique du mini-jeu {@link Jump} : dessine un fond violet clair uni, un
 * sol noir interrompu par les trous et le demi-cercle du joueur. Il n'y a aucun
 * contour autour du fond.
 */
public final class JumpView implements GameView {

  /** Couleur du fond, relevée sur le jeu original. */
  private static final Color BACKGROUND = new Color(0xDFB9F9);
  /** Couleur du demi-cercle, relevée sur le jeu original. */
  private static final Color PLAYER = new Color(0x600D97);
  /** Couleur du sol, relevée sur le jeu original. */
  private static final Color GROUND = Color.BLACK;
  /** Diamètre d'arrondi des coins du fond, en pixels. */
  private static final int CORNER_ARC = 8;
  /** Épaisseur du sol, en unités du modèle. */
  private static final double GROUND_THICKNESS = 1.5;

  /**
   * Conversion entre les coordonnées du modèle et les pixels de la zone de
   * dessin.
   *
   * @param area zone disponible pour le dessin
   */
  private record Layout(Rectangle area) {

    /** @return nombre de pixels par unité horizontale du modèle */
    double scaleX() {
      return area.width / Jump.WIDTH;
    }

    /** @return nombre de pixels par unité verticale du modèle */
    double scaleY() {
      return area.height / Jump.HEIGHT;
    }

    /**
     * @param x abscisse dans les coordonnées du modèle
     * @return abscisse correspondante, en pixels
     */
    int toX(double x) {
      return area.x + (int) Math.round(x * scaleX());
    }

    /**
     * @param y ordonnée dans les coordonnées du modèle, l'ordonnée 0 étant en bas
     * @return ordonnée correspondante, en pixels
     */
    int toY(double y) {
      return area.y + area.height - (int) Math.round(y * scaleY());
    }
  }

  /** État du mini-jeu de saut lu pour produire le dessin. */
  private final Jump jumper;

  /**
   * Crée une vue associée au mini-jeu fourni.
   *
   * @param jumper mini-jeu dont l'état doit être représenté
   * @throws NullPointerException si {@code jumper} est {@code null}
   */
  public JumpView(Jump jumper) {
    Objects.requireNonNull(jumper);
    this.jumper = jumper;
    super();
  }

  /**
   * Dessine le fond, le sol puis le demi-cercle. Les coordonnées du modèle sont
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
    GameView.checkInput(graphics, jumper, area);

    var g = (Graphics2D) graphics.create();
    try {
      g.setClip(area);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      var layout = new Layout(area);
      drawBackground(g, layout);
      drawGround(g, layout);
      drawPlayer(g, layout);
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
   * Dessine le sol : une ligne noire sur toute la largeur, interrompue par
   * chaque trou. Le sol est tracé de gauche à droite, de trou en trou.
   *
   * @param g      contexte graphique
   * @param layout conversion en pixels
   */
  private void drawGround(Graphics2D g, Layout layout) {
    g.setColor(GROUND);
    var start = 0.0;
    for (var hole : jumper.holes()) {
      fillGround(g, layout, start, hole.x());
      start = hole.end();
    }
    fillGround(g, layout, start, Jump.WIDTH);
  }

  /**
   * Dessine un tronçon de sol entre deux abscisses du modèle, s'il est visible.
   *
   * @param g      contexte graphique
   * @param layout conversion en pixels
   * @param from   abscisse de début du tronçon
   * @param to     abscisse de fin du tronçon
   */
  private static void fillGround(Graphics2D g, Layout layout, double from, double to) {
    var left = layout.toX(Math.max(0, from));
    var right = layout.toX(Math.min(Jump.WIDTH, to));
    if (right > left) {
      var top = layout.toY(Jump.GROUND_Y);
      var thickness = Math.max(2, (int) Math.round(GROUND_THICKNESS * layout.scaleY()));
      g.fillRect(left, top, right - left, thickness);
    }
  }

  /**
   * Dessine le demi-cercle : sa partie plate repose sur le sol, ou sur la
   * hauteur de saut, et sa partie arrondie est tournée vers le haut.
   *
   * @param g      contexte graphique
   * @param layout conversion en pixels
   */
  private void drawPlayer(Graphics2D g, Layout layout) {
    var radiusX = (int) Math.round(Jump.PLAYER_RADIUS * layout.scaleX());
    var radiusY = (int) Math.round(Jump.PLAYER_RADIUS * layout.scaleY());
    var centerX = layout.toX(Jump.PLAYER_X);
    var baseY = layout.toY(Jump.GROUND_Y + jumper.height());
    g.setColor(PLAYER);
    g.fillArc(centerX - radiusX, baseY - radiusY, 2 * radiusX, 2 * radiusY, 0, 180);
  }
}
