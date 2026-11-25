package multitask2.controller;

import java.util.Objects;

/** Écran final : il demande l'arrêt de l'application. */
public record QuitScreen() implements Screen {

  @Override
  public Screen update(double dt, Input input) {
    Objects.requireNonNull(input);
    return this;
  }

  @Override
  public boolean finished() {
    return true;
  }
}
