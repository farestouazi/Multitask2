package multitask2.controller;

import java.util.List;
import java.util.Objects;

import com.github.forax.zen.KeyboardEvent.Key;

/**
 * Liste de choix dont un seul est sélectionné. Haut et bas déplacent la
 * sélection, Espace la valide.
 */
public final class Choices {

  /** Choix proposés, dans l'ordre d'affichage. */
  private final List<Choice> choices;
  /** Indice du choix actuellement sélectionné. */
  private int selected;

  /**
   * Crée une liste de choix dont le premier est sélectionné.
   *
   * @param choices choix proposés, au moins un
   * @throws NullPointerException     si {@code choices} est {@code null}
   * @throws IllegalArgumentException si {@code choices} est vide
   */
  public Choices(List<Choice> choices) {
    Objects.requireNonNull(choices);
    if (choices.isEmpty()) {
      throw new IllegalArgumentException("At least one choice is required.");
    }
    this.choices = List.copyOf(choices);
  }

  /**
   * Déplace la sélection selon les touches pressées et renvoie l'écran suivant
   * si le choix sélectionné est validé.
   *
   * @param input   état des entrées clavier
   * @param current écran actuel, renvoyé tant que rien n'est validé
   * @return l'écran du choix validé, ou {@code current}
   * @throws NullPointerException si un des arguments est {@code null}
   */
  public Screen update(Input input, Screen current) {
    Objects.requireNonNull(input);
    Objects.requireNonNull(current);
    var move = (input.isKeyJustPressed(Key.DOWN) ? 1 : 0) - (input.isKeyJustPressed(Key.UP) ? 1 : 0);
    selected = Math.floorMod(selected + move, choices.size());
    if (input.isKeyJustPressed(Key.SPACE)) {
      return choices.get(selected).next().get();
    }
    return current;
  }

  /**
   * Retourne les textes des choix, dans l'ordre d'affichage.
   *
   * @return liste non modifiable des textes
   */
  public List<String> labels() {
    return choices.stream().map(Choice::label).toList();
  }

  /**
   * Retourne l'indice du choix sélectionné.
   *
   * @return indice entre {@code 0} et le nombre de choix moins un
   */
  public int selected() {
    return selected;
  }
}
