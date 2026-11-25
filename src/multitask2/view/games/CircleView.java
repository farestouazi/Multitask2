package multitask2.view.games;

import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.Objects;

import multitask2.model.games.Circle;

public final class CircleView implements GameView {

  private static final Color BACKGROUND = new Color(0xFAF1B8);
  private static final Color ARC_COLOR = new Color(0x837801);

  /** Zoom fixe : 1 px par unité du modèle, soit tes pixels d'origine. */
  private static final double PX_PER_UNIT = 1.0;

  /** Épaisseur de l'arc, en unités du modèle. */
  private static final double ARC_THICKNESS = 10;

  private final Circle circle;

  public CircleView(Circle circle) {
    Objects.requireNonNull(circle);
    this.circle = circle;
    super();
  }

  @Override
  public void render(Graphics2D graphics, Rectangle area) {
    GameView.checkInput(graphics, circle, area);

    // on travaille sur une copie pour ne pas modifier le contexte de la fac
    var g = (Graphics2D) graphics.create();
    try {
      g.clip(area);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // fond
      g.setColor(BACKGROUND);
      g.fillRect(area.x, area.y, area.width, area.height);

      // zoom fixe : indépendant de la taille de la zone
      var pxPerUnit = PX_PER_UNIT;

      var cx = area.x + area.width / 2.0;
      var cy = area.y + area.height / 2.0;

      // arc bouclier
      var radius = Circle.ARC_RADIUS * pxPerUnit;
      g.setColor(ARC_COLOR);
      g.setStroke(new BasicStroke((float) (ARC_THICKNESS * pxPerUnit),
                                  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.draw(new Arc2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius,
                              circle.angle(), Circle.ARC_ANGLE_DEGREES, Arc2D.OPEN));

      // fusée : triangle pointant vers le centre
      // fusée : triangle pointant vers le centre, avec fondu à l'arrivée
      // fusées : triangles pointant vers le centre, avec fondu à l'arrivée
      var oldTransform = g.getTransform();
      var oldComposite = g.getComposite();
      for (var rocket : circle.rockets()) {
        var rx = cx + rocket.x() * pxPerUnit;
        var ry = cy + rocket.y() * pxPerUnit;

        g.setComposite(AlphaComposite.SrcOver.derive((float) rocket.opacity()));
        g.translate(rx, ry);
        g.rotate(rocket.angle());

        var scale = pxPerUnit;
        int[] xPoints = { (int) (12 * scale), (int) (-6 * scale), (int) (-6 * scale) };
        int[] yPoints = { 0, (int) (10 * scale), (int) (-10 * scale) };
        g.setColor(Color.BLACK);
        g.fillPolygon(xPoints, yPoints, 3);

        g.setTransform(oldTransform);
      }
      g.setComposite(oldComposite);
      
    } finally {
      g.dispose();
    }
  }
}