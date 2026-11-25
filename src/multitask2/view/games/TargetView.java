package multitask2.view.games;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Objects;

import multitask2.model.games.Target;

public final class TargetView implements GameView {

  private static final Color BACKGROUND = new Color(0xC3EEB9);
  private static final Color FIELD_BORDER = new Color(0x9ABC92);
  private static final Color TIMER_COLOR = new Color(0x9ABC92);
  private static final Color PLAYER_COLOR = new Color(0x338020);

  private static final double MARGIN = 10;
  private static final double ZOOM_RATIO = 1.0;

  private final Target target;

  public TargetView(Target target) {
    Objects.requireNonNull(target);
    this.target = target;
    super();
  }
  
  private static double zoom(Rectangle area, double refHalfW, double refHalfH, double margin) {
    var fitW = (area.width / 2.0 - margin) / refHalfW;
    var fitH = (area.height / 2.0 - margin) / refHalfH;
    return Math.max(0.05, Math.min(1.0, Math.min(fitW, fitH))) * ZOOM_RATIO;
  }

  @Override
  public void render(Graphics2D graphics, Rectangle area) {
    GameView.checkInput(graphics, target, area);

    // on travaille sur une copie pour ne pas modifier le contexte de la fac
    var g = (Graphics2D) graphics.create();
    try {
      g.clip(area);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // fond
      g.setColor(BACKGROUND);
      g.fillRect(area.x, area.y, area.width, area.height);

      // zoom fixe : indépendant de la taille de la zone
      var pxPerUnit = zoom(area, Target.FIELD_WIDTH / 2, Target.FIELD_HEIGHT / 2, MARGIN);
      var cx = area.x + area.width / 2.0;
      var cy = area.y + area.height / 2.0;

      // limites du terrain
      var fieldW = Target.FIELD_WIDTH * pxPerUnit;
      var fieldH = Target.FIELD_HEIGHT * pxPerUnit;
      g.setColor(FIELD_BORDER);
      g.setStroke(new BasicStroke(2));
      g.draw(new Ellipse2D.Double(0, 0, 0, 0)); // (voir note) à supprimer si inutile
      g.drawRect((int) (cx - fieldW / 2), (int) (cy - fieldH / 2), (int) fieldW, (int) fieldH);

      // cible : cercle vide avec le compte à rebours au centre
      var tx = cx + target.targetX() * pxPerUnit;
      var ty = cy + target.targetY() * pxPerUnit;
      var tr = Target.TARGET_RADIUS * pxPerUnit;
      g.setColor(Color.BLACK);
      g.setStroke(new BasicStroke((float) (4 * pxPerUnit)));
      g.draw(new Ellipse2D.Double(tx - tr, ty - tr, 2 * tr, 2 * tr));

      g.setColor(TIMER_COLOR);
      g.setFont(new Font("SansSerif", Font.BOLD, (int) (32 * pxPerUnit)));
      var text = Integer.toString((int) Math.max(0, target.timeLeft()));
      var fm = g.getFontMetrics();
      g.drawString(text,
          (float) (tx - fm.stringWidth(text) / 2.0),
          (float) (ty + (fm.getAscent() - fm.getDescent()) / 2.0));

      // joueur
      var px = cx + target.playerX() * pxPerUnit;
      var py = cy + target.playerY() * pxPerUnit;
      var pr = Target.PLAYER_RADIUS * pxPerUnit;
      g.setColor(PLAYER_COLOR);
      g.fill(new Ellipse2D.Double(px - pr, py - pr, 2 * pr, 2 * pr));
    } finally {
      g.dispose();
    }
  }
}