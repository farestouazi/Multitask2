package multitask2.controller;

/**
 * Écran de l'application : menu, partie ou fin de partie.
 *
 * <p>
 * À chaque image, l'écran courant traite les entrées et renvoie l'écran à
 * utiliser ensuite (lui-même s'il ne change pas).
 * </p>
 */
public sealed interface Screen permits MenuScreen, PlayScreen, GameOverScreen, QuitScreen {

  /**
   * Fait évoluer l'écran d'un pas de simulation.
   *
   * @param dt    durée du pas, en secondes
   * @param input état courant des entrées clavier
   * @return l'écran à utiliser pour l'image suivante
   * @throws NullPointerException si {@code input} est {@code null}
   */
  Screen update(double dt, Input input);

  /**
   * Indique si l'application doit s'arrêter.
   *
   * @return {@code true} si la boucle principale doit se terminer
   */
  default boolean finished() {
    return false;
  }
}
