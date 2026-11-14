package multitask2.view;

import multitask2.controller.Choices;
import multitask2.controller.GameOverScreen;
import multitask2.controller.MenuScreen;
import multitask2.controller.Party;
import multitask2.controller.PlayScreen;
import multitask2.controller.QuitScreen;
import multitask2.controller.Screen;


import com.github.forax.zen.ApplicationContext;

import multitask2.model.Model;
import multitask2.model.Slot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Objects;

/**
 * Vue générale de la partie : dessine l'arrière-plan, le temps écoulé et une
 * grille contenant les vues de chaque mini-jeu.
 */
public final class GeneralView implements View {

  /** Couleur de fond de la fenêtre. */
  public static final Color BACKGROUND = new Color(24, 26, 32);
  /** Hauteur réservée à la zone supérieure de l'interface, en pixels. */
  private static final int HUD_HEIGHT = 50;
  /** Espacement utilisé entre les zones dessinées, en pixels. */
  private static final int PADDING = 10;
  /** Marge autour de la grille et de la zone d'informations, en pixels. */
  private static final int MARGIN = 20;

  /**
   * Contexte graphique qui fournit la taille d'écran et réalise le rendu des
   * images.
   */
  private final ApplicationContext context;
  /** Largeur de l'écran relevée à la construction, en pixels. */
  private final int width;
  /** Hauteur de l'écran relevée à la construction, en pixels. */
  private final int height;

  /**
   * Crée la vue et mémorise les dimensions de l'écran courant.
   *
   * @param context contexte graphique utilisé pour le rendu
   * @param viewers registre permettant de retrouver la vue de chaque mini-jeu
   * @throws NullPointerException si {@code context} ou {@code viewers} est
   *                              {@code null}
   */
  /**
   * Crée la vue et mémorise les dimensions de l'écran courant.
   *
   * @param context contexte graphique utilisé pour le rendu
   * @throws NullPointerException si {@code context} est {@code null}
   */
  public GeneralView(ApplicationContext context) {
    this.context = Objects.requireNonNull(context);
    var screen = context.getScreenInfo();
    width = screen.width();
    height = screen.height();
    super();
  }

  /**
   * Demande au contexte graphique de dessiner une image représentant l'état
   * actuel du modèle.
   *
   * @param model modèle contenant le temps de partie et les mini-jeux à afficher
   * @throws NullPointerException si {@code model} est {@code null}
   */
  public void render(Screen screen) {
	    Objects.requireNonNull(screen);
	    context.renderFrame(graphics -> draw(graphics, screen));
	 }

  /**
   * Dessine le fond, l'affichage du temps puis la grille des mini-jeux. Les
   * cellules sont réparties sur un nombre de colonnes et de lignes visant une
   * grille approximativement carrée.
   *
   * @param graphics surface graphique de l'image en cours
   * @param model    modèle contenant l'état à dessiner
   */
//  private void draw(Graphics2D graphics, Model model) {
//    graphics.setColor(BACKGROUND);
//    graphics.fillRect(0, 0, width, height);
//    drawScore(graphics, model);
//
//    var slots = model.slots();
//    var n = slots.size();
//    if (n > 0) {
//      var cols = (int) Math.ceil(Math.sqrt(n));
//      var rows = (int) Math.ceil((double) n / cols);
//      var areaWidth = width - 2 * MARGIN - PADDING;
//      var areaHeight = height - HUD_HEIGHT - 2 * MARGIN - PADDING;
//      var cellWidth = areaWidth / cols;
//      var cellHeight = areaHeight / rows;
//
//      for (var i = 0; i < n; i++) {
//        var col = i % cols;
//        var row = i / cols;
//        var x = PADDING + MARGIN + col * cellWidth;
//        var y = HUD_HEIGHT + MARGIN + row * cellHeight;
//        var bounds = new Rectangle(x, y, cellWidth - PADDING, cellHeight - PADDING);
//        drawSlot(graphics, slots.get(i), model, bounds);
//      }
//    }
//  }
  
  /**
   * Dessine le fond puis l'écran courant.
   *
   * @param graphics surface graphique de l'image en cours
   * @param screen   écran à dessiner
   */
  private void draw(Graphics2D graphics, Screen screen) {
    graphics.setColor(BACKGROUND);
    graphics.fillRect(0, 0, width, height);
    switch (screen) {
    case PlayScreen play -> drawParty(graphics, play.party());
    case MenuScreen menu ->
        drawChoices(graphics, "Multitask 2", "Haut / Bas puis Espace", menu.choices());
    case GameOverScreen over ->
        drawChoices(graphics, "Perdu !", "Score : " + over.score() + " s", over.choices());
    case QuitScreen _ -> { }
    }
  }

  /**
   * Dessine le temps écoulé puis la grille des mini-jeux d'une partie. Les
   * cellules sont réparties sur un nombre de colonnes et de lignes visant une
   * grille approximativement carrée.
   *
   * @param graphics surface graphique de l'image en cours
   * @param party    partie à dessiner
   */
  private void drawParty(Graphics2D graphics, Party party) {
	    var model = party.model();
	    drawScore(graphics, model);
	    var active = model.slots().stream().filter(Slot::active).toList();
	    var area = new Rectangle(MARGIN, HUD_HEIGHT + MARGIN, width - 2 * MARGIN,
	        height - HUD_HEIGHT - 2 * MARGIN);
	    var cells = Tiling.split(active.size(), area);
	    for (var i = 0; i < cells.size(); i++) {
	      drawSlot(graphics, active.get(i), party, inset(cells.get(i)));
	    }
	  }
  
  private static Rectangle inset(Rectangle cell) {
	    var half = PADDING / 2;
	    return new Rectangle(cell.x + half, cell.y + half, cell.width - PADDING, cell.height - PADDING);
	  }

  /**
   * Dessine un titre, un sous-titre et une liste de choix, le choix sélectionné
   * étant mis en évidence.
   *
   * @param graphics surface graphique de l'image en cours
   * @param title    titre affiché en grand
   * @param subtitle texte affiché sous le titre
   * @param choices  choix à afficher
   */
  private void drawChoices(Graphics2D graphics, String title, String subtitle, Choices choices) {
    graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 40f));
    graphics.setColor(Color.WHITE);
    drawCentered(graphics, title, height / 4);
    graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 24f));
    drawCentered(graphics, subtitle, height / 4 + 50);
    var labels = choices.labels();
    for (var i = 0; i < labels.size(); i++) {
      var selected = i == choices.selected();
      graphics.setColor(selected ? Color.YELLOW : Color.GRAY);
      var text = selected ? "> " + labels.get(i) + " <" : labels.get(i);
      drawCentered(graphics, text, height / 2 + i * 40);
    }
  }

  /**
   * Dessine un texte centré horizontalement sur l'écran, à une hauteur donnée.
   *
   * @param graphics surface graphique utilisée pour le dessin
   * @param text     texte à afficher
   * @param y        ordonnée de la ligne de base du texte, en pixels
   */
  private void drawCentered(Graphics2D graphics, String text, int y) {
    centerText(graphics, text, new Rectangle(0, y, width, 0));
  }

  /**
   * Dessine le temps entier écoulé depuis le début de la partie dans la zone
   * supérieure.
   *
   * @param graphics surface graphique de l'image en cours
   * @param model    modèle fournissant le temps écoulé
   */
  private void drawScore(Graphics2D graphics, Model model) {
    graphics.setColor(Color.WHITE);
    graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 18f));
    String text = String.format("Temps : " + model.time() + " s");
    graphics.drawString(text, PADDING + MARGIN, 30 + MARGIN);
  }

  /**
   * Dessine la cellule d'un mini-jeu. Un jeu inactif affiche un compte à rebours
   * ; un jeu actif affiche un contour puis délègue son contenu à la vue
   * spécialisée correspondante.
   *
   * @param graphics surface graphique de l'image en cours
   * @param slot     emplacement contenant le mini-jeu et son état d'activation
   * @param model    modèle utilisé pour calculer le compte à rebours
   * @param bounds   rectangle de la cellule à dessiner
   */
  private void drawSlot(Graphics2D graphics, Slot slot, Party party, Rectangle bounds) {
	    graphics.setColor(new Color(40, 42, 54));
	    graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);
	    graphics.setColor(new Color(90, 200, 120));
	    graphics.setStroke(new BasicStroke(3));
	    graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);
	    var inner = new Rectangle(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4);
	    party.viewers().view(slot.game()).render(graphics, inner);
	  }
  /**
   * Dessine un texte horizontalement centré dans un rectangle, à mi-hauteur de
   * celui-ci.
   *
   * @param graphics surface graphique utilisée pour le dessin
   * @param text     texte à afficher
   * @param bounds   rectangle dans lequel centrer le texte
   */
  private static void centerText(Graphics2D graphics, String text, Rectangle bounds) {
    var fm = graphics.getFontMetrics();
    var x = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
    var y = bounds.y + bounds.height / 2;
    graphics.drawString(text, x, y);
  }
}