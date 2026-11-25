package multitask2.view.games;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;

import multitask2.model.games.Balance;

public final class BalanceView implements GameView {

  // dimensions d'origine de ton jeu, en pixels
  /** Zoom fixe : 4.5 px par unité du modèle, soit ta planche d'origine de 450 px. */
  private static final double PX_PER_UNIT = 4.5;
  private static final double PLANK_THICKNESS = 22;

  private static final Color BACKGROUND = new Color(0xFEBCBC);
  private static final Color BALL_COLOR = new Color(0xAE0202);

  private final Balance balance;

  public BalanceView(Balance balance) {
    Objects.requireNonNull(balance);
    this.balance = balance;
    super();
  }

  @Override
  public void render(Graphics2D graphics, Rectangle area) {
    GameView.checkInput(graphics, balance, area);

    // on travaille sur une copie pour ne pas modifier le contexte de la fac
    var g = (Graphics2D) graphics.create();
    try {
      g.clip(area); // la bille qui tombe ne dépasse pas de la zone
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // fond
      g.setColor(BACKGROUND);
      g.fillRect(area.x, area.y, area.width, area.height);

      // tout est mis à l'échelle selon la largeur de la zone
      // zoom fixe : indépendant de la taille de la zone
      var pxPerUnit = PX_PER_UNIT;
      var plankLength = 2 * Balance.TRACK_HALF_LENGTH * pxPerUnit; // 450 px
      var thickness = PLANK_THICKNESS;
      var radius = Balance.BALL_RADIUS * pxPerUnit;                // 20 px

      var cx = area.x + area.width / 2;
      var cy = area.y + area.height / 2;

      // planche (et bille tant qu'elle est dessus), dans le repère tourné
      var oldTransform = g.getTransform();
      g.translate(cx, cy);
      g.rotate(Math.toRadians(balance.tilt()));

      g.setColor(Color.BLACK);
      g.fillRect((int) (-plankLength / 2), 0, (int) plankLength, (int) thickness);

      if (!balance.isFalling()) {
        var ballPos = balance.position() * pxPerUnit;
        g.setColor(BALL_COLOR);
        g.fillOval((int) (ballPos - radius), (int) (-2 * radius),
                   (int) (2 * radius), (int) (2 * radius));
      }
      g.setTransform(oldTransform);

      // la bille qui tombe est dessinée en coordonnées écran
      if (balance.isFalling()) {
        var fx = cx + balance.fallX() * pxPerUnit;
        var fy = cy + balance.fallY() * pxPerUnit;
        g.setColor(BALL_COLOR);
        g.fillOval((int) (fx - radius), (int) (fy - radius),
                   (int) (2 * radius), (int) (2 * radius));
      }
    } finally {
      g.dispose();
    }
  }
}