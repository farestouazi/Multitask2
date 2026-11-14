package multitask2;

import java.util.function.Function;

import com.github.forax.zen.Application;
import com.github.forax.zen.ApplicationContext;
import multitask2.controller.Controller;
import multitask2.controller.MenuScreen;
import multitask2.controller.Party;
import multitask2.model.Game;
import multitask2.model.Model;
import multitask2.view.games.GameView;
import multitask2.view.GeneralView;
import multitask2.view.Viewers;

import multitask2.model.games.Avoid;
import multitask2.view.games.AvoidView;

import multitask2.model.games.Balance;
import multitask2.view.games.BalanceView;

import multitask2.model.games.Fly;
import multitask2.view.games.FlyView;

import multitask2.model.games.Jump;
import multitask2.view.games.JumpView;

/**
 * Point d'entrée de l'application de mini-jeux multitâches.
 *
 * <p>
 * Cette classe crée l'interface graphique et le contrôleur, démarré sur le menu
 * principal. Elle construit aussi chaque nouvelle partie en enregistrant les
 * mini-jeux avec leur vue respective.
 * </p>
 */
public final class Main {

  /**
   * Constructeur privé afin d'empêcher l'instanciation de cette classe
   * utilitaire.
   */
  private Main() {
    super();
  }

  /**
   * Lance la boucle de l'application graphique.
   *
   * @param args arguments de la ligne de commande ; ils ne sont pas utilisés
   *             actuellement
   */
  public static void main(String[] args) {
    Application.run(GeneralView.BACKGROUND, Main::game);
  }

  /**
   * Crée la vue et le contrôleur, démarré sur le menu principal, et l'exécute
   * jusqu'à ce que le joueur quitte. <br>
   * Le contexte graphique est libéré même si l'exécution du contrôleur se termine
   * à la suite d'une exception.
   *
   * @param context contexte graphique fourni par la bibliothèque Zen
   */
  private static void game(ApplicationContext context) {
    var view = new GeneralView(context);
    var controller = new Controller(new MenuScreen(Main::newParty), view, context);
    try {
      controller.run();
    } finally {
      context.dispose();
    }
  }

  /**
   * Construit une nouvelle partie avec ses mini-jeux et leurs vues. C'est ici
   * que l'on déclare un nouveau mini-jeu.
   *
   * @return nouvelle partie
   */
  private static Party newParty() {
    var model = new Model();
    var viewers = new Viewers();
    addGame(model, viewers, new Balance(), 10, BalanceView::new);
    addGame(model, viewers, new Avoid(), 15, AvoidView::new);
    addGame(model, viewers, new Fly(), 0, FlyView::new);
    addGame(model, viewers, new Jump(), 5, JumpView::new);
    return new Party(model, viewers);
  }

  /**
   * Enregistre un mini-jeu dans le modèle et associe à cette instance sa vue
   * graphique.
   *
   * @param <G>     type concret du mini-jeu
   * @param model   modèle auquel ajouter le mini-jeu
   * @param viewers registre dans lequel enregistrer sa vue
   * @param game    instance du mini-jeu à enregistrer
   * @param time    temps, en secondes depuis le début de la partie, auquel
   *                activer le mini-jeu
   * @param factory fonction construisant une vue à partir de l'instance du
   *                mini-jeu
   */
  private static <G extends Game> void addGame(Model model, Viewers viewers, G game, int time,
      Function<G, GameView> factory) {
    model.addGame(game, time);
    viewers.addGame(game, factory.apply(game));
  }
}
