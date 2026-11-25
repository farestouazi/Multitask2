package multitask2.view;

import com.github.forax.zen.ApplicationContext;

import multitask2.model.Model;
import multitask2.model.Slot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Vue générale de la partie : dessine l'arrière-plan, le temps écoulé et les
 * vues de chaque mini-jeu, séparées par une fine ligne.
 */
public final class GeneralView implements View {

  /** Couleur de fond de la fenêtre. */
  public static final Color BACKGROUND = new Color(24, 26, 32);
  /** Couleur de la fine ligne séparant les jeux. */
  /** Hauteur réservée à la zone supérieure de l'interface, en pixels. */
  private static final int HUD_HEIGHT = 50;

  private final ApplicationContext context;
  private final Viewers viewers;
  private final int width;
  private final int height;

  public GeneralView(ApplicationContext context, Viewers viewers) {
    this.context = Objects.requireNonNull(context);
    this.viewers = Objects.requireNonNull(viewers);
    var screen = context.getScreenInfo();
    width = screen.width();
    height = screen.height();
    super();
  }

  public void render(Model model) {
    Objects.requireNonNull(model);
    context.renderFrame(graphics -> draw(graphics, model));
  }

  private void draw(Graphics2D graphics, Model model) {
    graphics.setColor(BACKGROUND);
    graphics.fillRect(0, 0, width, height);
    drawScore(graphics, model);

    var slots = model.slots();
    var n = slots.size();
    if (n == 0) {
      return;
    }

    var area = new Rectangle(0, HUD_HEIGHT, width, height - HUD_HEIGHT);
    var cells = layout(n, area);

    for (var i = 0; i < n; i++) {
      drawSlot(graphics, slots.get(i), model, cells.get(i));
    }

    // fines lignes de séparation, dessinées par-dessus les jeux
    graphics.setColor(Color.BLACK);
    graphics.setStroke(new BasicStroke(1));
    for (var cell : cells) {
      graphics.drawRect(cell.x, cell.y, cell.width - 1, cell.height - 1);
    }
  }

  /**
   * Découpe la zone en n cellules qui la couvrent exactement.
   * Cas particulier : avec 3 jeux, le premier occupe toute la moitié gauche
   * et les deux autres sont empilés à droite.
   */
  private static List<Rectangle> layout(int n, Rectangle area) {
    var cells = new ArrayList<Rectangle>(n);

    if (n == 3) {
      var half = area.width / 2;
      var halfHeight = area.height / 2;
      cells.add(new Rectangle(area.x, area.y, half, area.height));
      cells.add(new Rectangle(area.x + half, area.y, area.width - half, halfHeight));
      cells.add(new Rectangle(area.x + half, area.y + halfHeight, area.width - half,
          area.height - halfHeight));
      return cells;
    }

    // grille classique ; la dernière ligne est étirée si elle est incomplète
    var cols = (int) Math.ceil(Math.sqrt(n));
    var rows = (int) Math.ceil((double) n / cols);
    for (var row = 0; row < rows; row++) {
      var y0 = area.y + row * area.height / rows;
      var y1 = area.y + (row + 1) * area.height / rows;
      var inRow = Math.min(cols, n - row * cols);
      for (var col = 0; col < inRow; col++) {
        var x0 = area.x + col * area.width / inRow;
        var x1 = area.x + (col + 1) * area.width / inRow;
        cells.add(new Rectangle(x0, y0, x1 - x0, y1 - y0));
      }
    }
    return cells;
  }

  private void drawScore(Graphics2D graphics, Model model) {
    graphics.setColor(Color.WHITE);
    graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 18f));
    graphics.drawString("Temps : " + model.time() + " s", 10, 30);
  }

  private void drawSlot(Graphics2D graphics, Slot slot, Model model, Rectangle bounds) {
    if (!slot.active()) {
      var remaining = Math.max(0, slot.activationTime() - model.time());
      graphics.setColor(Color.GRAY);
      graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 14f));
      centerText(graphics, "Dans " + remaining + " s...", bounds);
      return;
    }

    viewers.view(slot.game()).render(graphics, bounds);
  }

  private static void centerText(Graphics2D graphics, String text, Rectangle bounds) {
    var fm = graphics.getFontMetrics();
    var x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
    var y = bounds.y + bounds.height / 2;
    graphics.drawString(text, x, y);
  }
}