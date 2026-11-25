package multitask2.controller;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Écran de fin de partie : affiche le score et propose de rejouer ou de revenir au menu. */
public final class GameOverScreen implements Screen {

  /** Score de la partie terminée, en secondes. */
  private final int score;
  /** Choix proposés au joueur. */
  private final Choices choices;

  /**
   * Crée l'écran de fin.
   *
   * @param score    score de la partie, en secondes
   * @param newParty fabrique d'une nouvelle partie, utilisée par « Rejouer »
   * @throws NullPointerException     si {@code newParty} est {@code null}
   * @throws IllegalArgumentException si {@code score} est négatif
   */
  public GameOverScreen(int score, Supplier<Party> newParty) {
    Objects.requireNonNull(newParty);
    if (score < 0) {
      throw new IllegalArgumentException("The score cannot be negative.");
    }
    this.score = score;
    choices = new Choices(List.of(
        new Choice("Rejouer", () -> new PlayScreen(newParty)),
        new Choice("Menu", () -> new MenuScreen(newParty))));
  }

  @Override
  public Screen update(double dt, Input input) {
    Objects.requireNonNull(input);
    return choices.update(input, this);
  }

  /**
   * Retourne le score de la partie terminée.
   *
   * @return score en secondes
   */
  public int score() {
    return score;
  }

  /**
   * Retourne les choix proposés, pour l'affichage.
   *
   * @return choix de l'écran de fin
   */
  public Choices choices() {
    return choices;
  }
}
