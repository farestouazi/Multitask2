package multitask2.controller;

import java.util.Objects;

import multitask2.model.Model;
import multitask2.view.Viewers;

/**
 * Une partie : le modèle qui contient les mini-jeux et le registre des vues
 * associées à ces mêmes mini-jeux.
 *
 * @param model   modèle de la partie
 * @param viewers vues des mini-jeux de ce modèle
 */
public record Party(Model model, Viewers viewers) {

  /**
   * Crée une partie.
   *
   * @throws NullPointerException si un des arguments est {@code null}
   */
  public Party {
    Objects.requireNonNull(model);
    Objects.requireNonNull(viewers);
  }
}
