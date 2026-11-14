package multitask2.controller;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Un choix proposé au joueur : un texte et l'écran qu'il ouvre quand il est
 * validé.
 *
 * @param label texte affiché
 * @param next  fabrique de l'écran ouvert quand le choix est validé
 */
public record Choice(String label, Supplier<Screen> next) {

  /**
   * Crée un choix.
   *
   * @throws NullPointerException si un des arguments est {@code null}
   */
  public Choice {
    Objects.requireNonNull(label);
    Objects.requireNonNull(next);
  }
}
