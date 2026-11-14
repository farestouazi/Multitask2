package multitask2.view.games;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;
import java.awt.BasicStroke;

import multitask2.model.games.Avoid;

/**
 * Vue graphique du mini-jeu {@link Avoid} : dessine un fond bleu avec ses
 * rangées, les missiles qui arrivent des deux côtés et la barre du joueur au
 * centre.
 */
public final class AvoidView implements GameView {

	  /** Couleur du fond, relevée sur le jeu original. */
	  private static final Color BACKGROUND = new Color(0xD0D5FE);
	  /**
	   * Couleur de remplissage de la case de la barre où se trouve le joueur (étage
	   * actuel), relevée sur le jeu original.
	   */
	  private static final Color BAR = new Color(0x043F9F);
	  /** Couleur du contour des cases de la barre. */
	  private static final Color BAR_OUTLINE = Color.BLACK;
	  /** Couleur des missiles, relevée sur le jeu original. */
	  private static final Color MISSILE = Color.BLACK;
  /**
   * Positions et tailles en pixels, déduites de la zone de dessin.
   *
   * @param area zone disponible pour le dessin
   */
  private record Layout(Rectangle area) {

    /** @return nombre de pixels par unité horizontale du modèle */
    double scaleX() {
      return area.width / Avoid.WIDTH;
    }

    /** @return hauteur d'une rangée, en pixels */
    int rowHeight() {
      return area.height / Avoid.ROWS;
    }

    /** @return demi-hauteur d'un missile, en pixels */
    int missileHalfSize() {
      return Math.max(4, rowHeight() * 3 / 10);
    }

    /** @return longueur d'un missile, de sa pointe à sa base, en pixels */
    int missileLength() {
      return (int) (Avoid.MISSILE_LENGTH * scaleX());
    }
    
    /**
     * @param x abscisse dans les coordonnées du modèle
     * @return abscisse correspondante, en pixels
     */
    int toX(double x) {
      return area.x + (int) (x * scaleX());
    }

    /**
     * @param row indice de rangée
     * @return ordonnée du haut de la rangée, en pixels
     */
    int rowTop(int row) {
      return area.y + area.height - (row + 1) * rowHeight();
    }

    /**
     * @param row indice de rangée
     * @return ordonnée du centre de la rangée, en pixels
     */
    int rowCenterY(int row) {
      return rowTop(row) + rowHeight() / 2;
    }
  }

  /** État du mini-jeu d'esquive lu pour produire le dessin. */
  private final Avoid avoider;

  /**
   * Crée une vue associée au mini-jeu fourni.
   *
   * @param avoider mini-jeu dont l'état doit être représenté
   * @throws NullPointerException si {@code avoider} est {@code null}
   */
  public AvoidView(Avoid avoider) {
    Objects.requireNonNull(avoider);
    this.avoider = avoider;
    super();
  }

  /**
   * Dessine le fond, les missiles puis la barre du joueur. Les coordonnées du
   * modèle sont mises à l'échelle pour tenir dans le rectangle fourni. Le dessin
   * se fait sur une copie du contexte graphique, limitée à la zone fournie,
   * pour ne pas modifier les réglages utilisés par le reste de l'affichage.
   *
   * @param graphics contexte graphique à utiliser
   * @param area     zone disponible pour le dessin
   * @throws NullPointerException si {@code graphics}, le jeu associé ou
   *                              {@code area} est nul
   */
  @Override
  public void render(Graphics2D graphics, Rectangle area) {
    GameView.checkInput(graphics, avoider, area);

    var g = (Graphics2D) graphics.create();
    try {
      g.setClip(area);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      var layout = new Layout(area);
      drawBackground(g, layout);
      drawBar(g, layout);
      avoider.missiles().forEach(missile -> drawMissile(g, layout, missile));
    } finally {
      g.dispose();
    }
  }

  /**
   * Dessine le fond bleu, les rangées alternées et la ligne centrale.
   *
   * @param g      contexte graphique
   * @param layout positions en pixels
   */
  private static void drawBackground(Graphics2D g, Layout layout) {
	    var area = layout.area();
	    g.setColor(BACKGROUND);
	    g.fillRoundRect(area.x, area.y, area.width, area.height, 14, 14);
	  }
  /**
   * Dessine un missile sous la forme d'un triangle bleu dont la pointe, dirigée
   * vers le centre, marque la position du missile.
   *
   * @param g       contexte graphique
   * @param layout  positions en pixels
   * @param missile missile à dessiner
   */
  private static void drawMissile(Graphics2D g, Layout layout, Avoid.Missile missile) {
    var tipX = layout.toX(missile.x());
    var y = layout.rowCenterY(missile.row());
    var half = layout.missileHalfSize();
    var baseX = tipX - missile.side().direction() * layout.missileLength();
    var xs = new int[] { baseX, baseX, tipX };
    var ys = new int[] { y - half, y + half, y };
    g.setColor(MISSILE);
    g.fillPolygon(xs, ys, 3);

  }

  /**
   * Dessine la barre du joueur au centre, sur sa rangée, avec un halo.
   *
   * @param g      contexte graphique
   * @param layout positions en pixels
   */
  private void drawBar(Graphics2D g, Layout layout) {
	    var halfWidth = (int) (Avoid.BAR_HALF_WIDTH * layout.scaleX());
	    var x = layout.toX(Avoid.WIDTH / 2) - halfWidth;
	    g.setStroke(new BasicStroke(2f));
	    for (var row = 0; row < Avoid.ROWS; row++) {
	      var y = layout.rowTop(row);
	      if (row == avoider.row()) {
	        g.setColor(BAR);
	        g.fillRect(x, y, 2 * halfWidth, layout.rowHeight());
	      }
	      g.setColor(BAR_OUTLINE);
	      g.drawRect(x, y, 2 * halfWidth, layout.rowHeight());
	    }
	  }
}