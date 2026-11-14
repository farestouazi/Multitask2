package multitask2.view;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Répartition d'une zone entre les mini-jeux actifs.
 *
 * <p>
 * Un seul mini-jeu occupe toute la zone. Chaque mini-jeu qui arrive ensuite
 * partage en deux la cellule du précédent, en alternant les coupes : la
 * première est verticale (gauche et droite), la deuxième horizontale (haut et
 * bas), la troisième verticale, et ainsi de suite. Le mini-jeu précédent garde
 * la première moitié (gauche ou haut), le nouveau prend la seconde (droite ou
 * bas).
 * </p>
 */
public final class Tiling {

  /**
   * Constructeur privé afin d'empêcher l'instanciation de cette classe
   * utilitaire.
   */
  private Tiling() {
    super();
  }

  /**
   * Calcule la cellule de chacun des mini-jeux, dans l'ordre de leur arrivée.
   * Les cellules sont disjointes et leur réunion est exactement la zone.
   *
   * @param count nombre de mini-jeux actifs
   * @param area  zone à répartir
   * @return liste non modifiable de {@code count} cellules, la cellule d'indice
   *         {@code i} étant celle du mini-jeu d'indice {@code i}
   * @throws IllegalArgumentException si {@code count} est négatif
   * @throws NullPointerException     si {@code area} est {@code null}
   */
  public static List<Rectangle> split(int count, Rectangle area) {
    if (count < 0) {
      throw new IllegalArgumentException("The count cannot be negative.");
    }
    Objects.requireNonNull(area);
    var cells = new ArrayList<Rectangle>();
    if (count > 0) {
      cells.add(new Rectangle(area));
    }
    for (var k = 1; k < count; k++) {
      var parts = cut(cells.get(k - 1), k % 2 == 1);
      cells.set(k - 1, parts[0]);
      cells.add(parts[1]);
    }
    return List.copyOf(cells);
  }

  /**
   * Coupe une cellule en deux moitiés.
   *
   * @param cell     cellule à couper
   * @param vertical {@code true} pour une coupe verticale (gauche et droite),
   *                 {@code false} pour une coupe horizontale (haut et bas)
   * @return la première moitié (gauche ou haut) puis la seconde (droite ou bas)
   */
  private static Rectangle[] cut(Rectangle cell, boolean vertical) {
    if (vertical) {
      var half = cell.width / 2;
      return new Rectangle[] { new Rectangle(cell.x, cell.y, half, cell.height),
          new Rectangle(cell.x + half, cell.y, cell.width - half, cell.height) };
    }
    var half = cell.height / 2;
    return new Rectangle[] { new Rectangle(cell.x, cell.y, cell.width, half),
        new Rectangle(cell.x, cell.y + half, cell.width, cell.height - half) };
  }
}
