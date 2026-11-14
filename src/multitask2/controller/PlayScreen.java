package multitask2.controller;

import java.util.Objects;
import java.util.function.Supplier;

/** Écran de jeu : fait avancer une partie jusqu'à la défaite. */
public final class PlayScreen implements Screen {

  /** Fabrique de partie, transmise à l'écran de fin pour pouvoir rejouer. */
  private final Supplier<Party> newParty;
  /** Partie en cours. */
  private final Party party;

  /**
   * Démarre une nouvelle partie.
   *
   * @param newParty fabrique d'une nouvelle partie
   * @throws NullPointerException si {@code newParty} est {@code null}
   */
  public PlayScreen(Supplier<Party> newParty) {
    this.newParty = Objects.requireNonNull(newParty);
    party = newParty.get();
  }

  @Override
  public Screen update(double dt, Input input) {
    Objects.requireNonNull(input);
    if (party.model().update(dt, input)) {
      return this;
    }
    return new GameOverScreen(party.model().time(), newParty);
  }

  /**
   * Retourne la partie en cours, pour l'affichage.
   *
   * @return partie en cours
   */
  public Party party() {
    return party;
  }
}
