package multitask2.controller;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Menu principal : jouer ou quitter. */
public final class MenuScreen implements Screen {

  /** Choix proposés par le menu. */
  private final Choices choices;

  /**
   * Crée le menu principal.
   *
   * @param newParty fabrique d'une nouvelle partie, utilisée par « Jouer »
   * @throws NullPointerException si {@code newParty} est {@code null}
   */
  public MenuScreen(Supplier<Party> newParty) {
    Objects.requireNonNull(newParty);
    choices = new Choices(List.of(
        new Choice("Jouer", () -> new PlayScreen(newParty)),
        new Choice("Quitter", QuitScreen::new)));
  }

  @Override
  public Screen update(double dt, Input input) {
    Objects.requireNonNull(input);
    return choices.update(input, this);
  }

  /**
   * Retourne les choix du menu, pour l'affichage.
   *
   * @return choix du menu
   */
  public Choices choices() {
    return choices;
  }
}
