package mouserun.mouse;

import javafx.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;
import mouserun.game.Mouse;
import mouserun.game.Grid;
import mouserun.game.Cheese;

/**
 * Clase que contiene el esqueleto del raton para las prácticas de Inteligencia
 * Artificial del curso 2019-20.
 *
 * @author Alejandro Arroyo Loaisa y Manuel Germán Morales
 */
public class M20A02a extends Mouse {

    public static int NO_PASO = -1;
    /**
     * Variable para almacenar la ultima celda visitada
     */
    private Grid lastGrid;
    private boolean hePisadoBomba;
    /**
     * Tabla hash para almacenar las celdas visitadas por el raton: Clave:
     * Coordenadas Valor: La celda
     */
    private HashMap<Pair<Integer, Integer>, Grid> celdasVisitadas;

    /**
     * Pila para almacenar el camino recorrido. Almacena un objeto con dos
     * valores. Uno es la casilla a la que se movió y otro el movimiento
     * complementario al que se realizó, para poder deshacerlo más fácilmente.
     */
    private Stack<Pair<Grid, Integer>> pilaMovimientos;

    /**
     * Constructor.
     */
    public M20A02a() {
        super("Manolo");
        celdasVisitadas = new HashMap<>();
        pilaMovimientos = new Stack<>();
        lastGrid = null;
        hePisadoBomba = false;
    }

    /**
     * @brief Método principal para el movimiento del raton. Incluye la gestión
     * de cuando un queso aparece o no.
     * @param currentGrid Celda actual
     * @param cheese Queso
     */
    @Override
    public int move(Grid currentGrid, Cheese cheese) {
        Random r = new Random();
        Pair<Integer, Integer> key = new Pair(currentGrid.getX(), currentGrid.getY());
        boolean visitada = visitada(currentGrid);
        //1º - Si la casilla en la que estoy, no ha sido visitada, la añado a la estructura celdasVisitadas.
        if (!visitada) {
            celdasVisitadas.put(key, currentGrid);
//            System.out.println("Añadida celda (" + currentGrid.getX() + ", " + currentGrid.getY() + "). Visitadas: " + celdasVisitadas.size() + " celdas.\n");
            if (hePisadoBomba) {
//                System.out.println("He añandido una celda en la que he aparecido por una explosion");
            }
        }

        //2º - Compruebo qué muros tengo alrededor con compruebaMurosAlrededor(casilla) para saber más fácilmente
        //     hacia dónde puedo moverme.
        int muros[] = compruebaMurosAlrededor(currentGrid);

        //3º - Reviso la estructura muros[] para moverme
        for (int i = 0; i < muros.length; i++) {
            //Compruebo por dónde puedo pasar
            if (muros[i] != NO_PASO && testGrid(muros[i], currentGrid)) {
                int x = currentGrid.getX();
                int y = currentGrid.getY();
                boolean estaEnCasillasVisitadas = false;
                //Después de comprobar si puedo pasar, compruebo SI YA HE PASADO POR AQUÍ ANTES.
                //Pongo estaEnCasillasVisitadas a true si ya he pasado, continúa en false en caso contrario.
                if (i == 0) {
                    estaEnCasillasVisitadas = visitada(x, y + 1);
                } else if (i == 1) {
                    estaEnCasillasVisitadas = visitada(x, y - 1);
                } else if (i == 2) {
                    estaEnCasillasVisitadas = visitada(x - 1, y);
                } else {
                    estaEnCasillasVisitadas = visitada(x + 1, y);
                }
                //Si no he pasado por aquí antes, terminaré mi turno moviéndome, pero antes:
                //  -Aumento el numero de casillas visitadas
                //  -Pongo la casilla actual como ultima casilla, porque será la última casilla visitada del siguiente turno
                //  -Introduzco el movimiento que voy a realizar en la pilaMovimientos
                //  -Me muevo
                if (!estaEnCasillasVisitadas) {
                    if (hePisadoBomba) {
//                        System.out.println("He explotado pero puedo explorar\n");
                        hePisadoBomba = false;
                        pilaMovimientos.removeAllElements();
                    }
                    incExploredGrids();
                    lastGrid = currentGrid;
                    pilaMovimientos.push(new Pair(currentGrid, direccionComplementaria(muros[i])));
                    return muros[i];
                }
            }
        }

//        System.out.println("He explorado todo lo que hay alrededor de la casilla\n");
        Pair<Grid, Integer> ultimo;
        //4A - Si he explorad, me situo [PUEDE FALLAR]
        if (!pilaMovimientos.isEmpty()) {
            ultimo = pilaMovimientos.peek();
            if (hePisadoBomba) {
                while (ultimo.getKey() != currentGrid && !pilaMovimientos.empty()) {
                    pilaMovimientos.pop();
                    ultimo = pilaMovimientos.peek();
                }
//                System.out.println("He conseguido un checkpoint\n");
                hePisadoBomba = false;
            }
//            System.out.println("Tamaño de la pila: " + pilaMovimientos.size() + "\n");
            //4B - Si NO he podido moverme porque ya he visitado todas las casillas a mi alrededor (incluido callejón sin salida),
            //     miro cuál ha sido mi último movimiento y lo deshago.

//            System.out.println("Volviendo atras...\n");
            pilaMovimientos.pop();
            return ultimo.getValue();
        }
        //TODO: En el caso que mi pila esté vacía, debería explorar el mapa de manera aleatoria para ver si hay casillas
        //sin explorar (bombas). Si hay queso, voy a por él.
        return r.nextInt(4)+1;
    }

    /**
     * @brief Devuelve la dirección complementaria a la que se le pasa por
     * cabecera. Ejemplo: direccionComplementaria(ARRIBA) returns ABAJO
     * @param direccion
     * @return La dirección complementaria
     */
    private int direccionComplementaria(int direccion) {
        switch (direccion) {
            case Mouse.UP:
                return Mouse.DOWN;
            case Mouse.DOWN:
                return Mouse.UP;
            case Mouse.LEFT:
                return Mouse.RIGHT;
            case Mouse.RIGHT:
                return Mouse.LEFT;
        }
        return NO_PASO;
    }

    /**
     * @brief Comprueba los muros alrededor de la casilla actual.
     * @param casillaActual
     * @return vector de ints. Los indices del vector representan: 0 - Up. 1 -
     * Down. 2 - Left. 3 - Right.
     */
    private int[] compruebaMurosAlrededor(Grid casillaActual) {
        int[] muros = new int[4];
        for (int i = 0; i < muros.length; i++) {
            muros[i] = NO_PASO;
        }

        if (casillaActual.canGoUp()) {
            muros[0] = Mouse.UP;
        }
        if (casillaActual.canGoDown()) {
            muros[1] = Mouse.DOWN;
        }
        if (casillaActual.canGoLeft()) {
            muros[2] = Mouse.LEFT;
        }
        if (casillaActual.canGoRight()) {
            muros[3] = Mouse.RIGHT;
        }

        return muros;
    }

    /**
     * @brief Método que se llama cuando aparece un nuevo queso
     */
    @Override
    public void newCheese() {

    }

    /**
     * @brief Método que se llama cuando el raton pisa una bomba
     */
    @Override
    public void respawned() {
//        System.out.print("He pisado una bomba!\n");
        hePisadoBomba = true;
    }

    /**
     * @brief Método para evaluar que no nos movamos a la misma celda anterior
     * @param direction Direccion del raton
     * @param currentGrid Celda actual
     * @return True Si las casillas X e Y anterior son distintas a las actuales
     */
    public boolean testGrid(int direction, Grid currentGrid) {
        if (lastGrid == null) {
            return true;
        }

        int x = currentGrid.getX();
        int y = currentGrid.getY();

        switch (direction) {
            case Mouse.UP:
                y += 1;
                break;

            case Mouse.DOWN:
                y -= 1;
                break;

            case Mouse.LEFT:
                x -= 1;
                break;

            case Mouse.RIGHT:
                x += 1;
                break;
        }

        return !(lastGrid.getX() == x && lastGrid.getY() == y);

    }

    /**
     * @brief Método que devuelve si de una casilla dada, está contenida en el
     * mapa de celdasVisitadas
     * @param casilla Casilla que se pasa para saber si ha sido visitada
     * @return True Si esa casilla ya la había visitado
     */
    public boolean visitada(Grid casilla) {
        Pair par = new Pair(casilla.getX(), casilla.getY());
        return celdasVisitadas.containsKey(par);
    }

    public boolean visitada(int x, int y) {
        Pair par = new Pair(x, y);
        return celdasVisitadas.containsKey(par);
    }

    /**
     * @brief Método para calcular si una casilla está en una posición relativa
     * respecto a otra
     * @param actual Celda actual
     * @param anterior Celda anterior
     * @return True Si la posición Y de la actual es mayor que la de la anterior
     */
    public boolean actualArriba(Grid actual, Grid anterior) {
        return actual.getY() > anterior.getY();
    }

    /**
     * @brief Método para calcular si una casilla está en una posición relativa
     * respecto a otra
     * @param actual Celda actual
     * @param anterior Celda anterior
     * @return True Si la posición Y de la actual es menor que la de la anterior
     */
    public boolean actualAbajo(Grid actual, Grid anterior) {
        return actual.getY() < anterior.getY();
    }

    /**
     * @brief Método para calcular si una casilla está en una posición relativa
     * respecto a otra
     * @param actual Celda actual
     * @param anterior Celda anterior
     * @return True Si la posición X de la actual es mayor que la de la anterior
     */
    public boolean actualDerecha(Grid actual, Grid anterior) {
        return actual.getX() > anterior.getX();
    }

    /**
     * @brief Método para calcular si una casilla está en una posición relativa
     * respecto a otra
     * @param actual Celda actual
     * @param anterior Celda anterior
     * @return True Si la posición X de la actual es menor que la de la anterior
     */
    public boolean actualIzquierda(Grid actual, Grid anterior) {
        return actual.getX() < anterior.getX();
    }

}
