package multitask2.view;

import multitask2.controller.Screen;

/** Interface de la vue générale chargée de représenter visuellement l'écran courant. */
public interface View {

  /**
   * Dessine une image correspondant à l'écran courant.
   *
   * @param screen écran dont l'état doit être affiché
   */
  void render(Screen screen);
}