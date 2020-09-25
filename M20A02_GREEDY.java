package mouserun.mouse;

import javafx.util.Pair;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import mouserun.game.Mouse;
import mouserun.game.Grid;
import mouserun.game.Cheese;

/**
 * Ratón con algoritmo Greedy de búsqueda "Primero el Mejor"
 *
 * @author Alejandro Arroyo Loaisa y Manuel Germán Morales
 */
public class M20A02_GREEDY extends Mouse {

    public static int NO_PASO = -1;

    /**
     * Comparador para poder hacer la PriorityQueue de Pairs. Sin él, no
     * funciona la cola con prioridad.
     */
    final Comparator<Pair<Integer, Integer>> comparador = new Comparator<Pair<Integer, Integer>>() {
        @Override
        public int compare(Pair<Integer, Integer> a, Pair<Integer, Integer> b) {
            return a.getKey().compareTo(b.getKey());
        }
    };

    /**
     * Variable para almacenar la ultima celda visitada
     */
    private Grid lastGrid;
    private boolean hePisadoBomba;
    //Para saber que tiene que intentar calcular el camino al queso
    private boolean nuevoQueso;
    /**
     * Tabla hash para almacenar las celdas visitadas por el raton: Clave:
     * Coordenadas Valor: La celda
     */
    private HashMap<Pair<Integer, Integer>, Pair<Grid, Vector<Integer>>> celdasVisitadas;

    /**
     * Pila para almacenar el camino recorrido. Almacena un objeto con dos
     * valores. Uno es la casilla a la que se movió y otro el movimiento
     * complementario al que se realizó, para poder deshacerlo más fácilmente.
     */
    private Stack<Pair<Grid, Integer>> pilaMovimientos;

    //El camino hacia el queso será una PILA, porque cuando lo calculamos y vamos almacenando
    //los movimientos, los hacemos desde el queso hasta el ratón y por lo tanto, añadimos
    //por el final y quitamos por el final
    private Stack<Integer> caminoHaciaQueso;

    private Stack<Integer> caminoHaciaQuesoGreedy;

    /**
     * Constructor.
     */
    public M20A02_GREEDY() {
        super("Paco");
        celdasVisitadas = new HashMap<>();
        pilaMovimientos = new Stack<>();
        caminoHaciaQueso = new Stack<>();
        lastGrid = null;
        hePisadoBomba = false;
        nuevoQueso = true;
    }

    /**
     * @brief Método principal para el movimiento del raton. Incluye la gestión
     * de cuando un queso aparece o no.
     * @param currentGrid Celda actual
     * @param cheese Queso
     */
    @Override
    public int move(Grid currentGrid, Cheese cheese) {

        //Si ha salido un queso nuevo...
        if (nuevoQueso) {
            //...compruebo si conozco donde está.
            if (quesoEnCeldaVisitada(cheese)) {
                //Si sé dónde está, meto la casilla a la que me acabo de mover.
                Pair<Integer, Integer> key = new Pair(currentGrid.getX(), currentGrid.getY());
                Vector<Integer> muros = compruebaMurosAlrededor(currentGrid);
                Pair<Grid, Vector<Integer>> casilla = new Pair(currentGrid, muros);

                celdasVisitadas.put(key, casilla);

                //Realizo recorrido
                busquedaPrimeroElMejor(currentGrid, cheese);

                //Y pongo nuevoQueso a false para que esto no lo vuelva a calcular
                nuevoQueso = false;
            }
        }

        //Si el caminoHaciaQueso tiene elementos es PORQUE SÉ DONDE ESTÁ EL QUESO Y PUEDO
        //MOVERME HACIA ÉL. Por lo tanto, voy hacia allá
        if (caminoHaciaQueso.size() > 0) {

            int movimiento = caminoHaciaQueso.peek();
            caminoHaciaQueso.pop();
            pilaMovimientos.push(new Pair(currentGrid, direccionComplementaria(movimiento)));
            if (!visitada(currentGrid)) {
                incExploredGrids();
            }
            return movimiento;

        } else {
            //SI caminoHaciaQueso NO TIENE ELEMENTOS, NO SÉ DONDE ESTÁ EL QUESO. Exploro normalmente
            Random r = new Random();
            Pair<Integer, Integer> key = new Pair(currentGrid.getX(), currentGrid.getY());
            boolean visitada = visitada(currentGrid);
            //1º - Si la casilla en la que estoy, no ha sido visitada, la añado a la estructura celdasVisitadas.
            if (!visitada) {
                //2º - Compruebo qué muros tengo alrededor con compruebaMurosAlrededor(casilla) para saber más fácilmente
                //     hacia dónde puedo moverme.
                Vector<Integer> muros = compruebaMurosAlrededor(currentGrid);
                Pair<Grid, Vector<Integer>> casilla = new Pair(currentGrid, muros);
                celdasVisitadas.put(key, casilla);
            }

            Vector<Integer> muros = celdasVisitadas.get(key).getValue();

            //3º - Reviso la estructura muros[] para moverme
            for (int i = 0; i < muros.size(); i++) {
                //Compruebo por dónde puedo pasar
                if (muros.get(i) != NO_PASO && testGrid(muros.get(i), currentGrid)) {
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
                            //He pisado una bomba, pero puedo explorar
                            hePisadoBomba = false;
                            pilaMovimientos.removeAllElements();
                        }
                        incExploredGrids();
                        lastGrid = currentGrid;
                        pilaMovimientos.push(new Pair(currentGrid, direccionComplementaria(muros.get(i))));
                        return muros.get(i);
                    }
                }
            }

            Pair<Grid, Integer> ultimo;
            //4A - Si he explorado, me situo 
            if (!pilaMovimientos.isEmpty()) {
                ultimo = pilaMovimientos.peek();
                //SI he pisado bomba, busco en la pila si ya he pasado por esa casilla, para situarme.
                if (hePisadoBomba) {
                    while (ultimo.getKey() != currentGrid && !pilaMovimientos.empty()) {
                        pilaMovimientos.pop();
                        ultimo = pilaMovimientos.peek();
                    }
                    hePisadoBomba = false;
                }
                //4B - Si NO he podido moverme porque ya he visitado todas las casillas a mi alrededor (incluido callejón sin salida),
                //     miro cuál ha sido mi último movimiento y lo deshago.

                pilaMovimientos.pop();
                return ultimo.getValue();
            }
            //TODO: En el caso que mi pila esté vacía, debería explorar el mapa de manera aleatoria (pero con sentido) para ver si hay casillas
            //sin explorar (bombas). Si hay queso, voy a por él.
            return r.nextInt(4) + 1;
        }
    }

    /**
     * @brief Devuelve true o false si está o no en una casilla visitada
     * @param casilla
     * @param cheese
     * @return true o false
     */
    private boolean quesoEnCeldaVisitada(Cheese cheese) {
        Pair<Integer, Integer> casillaQueso = new Pair(cheese.getX(), cheese.getY());
        if (celdasVisitadas.containsKey(casillaQueso)) {
            return true;
        } else {
            return false;
        }
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
    private Vector<Integer> compruebaMurosAlrededor(Grid casillaActual) {
        Vector<Integer> muros = new Vector();
        for (int i = 0; i < 4; i++) {
            muros.add(NO_PASO);
        }

        if (casillaActual.canGoUp()) {
            muros.set(0, Mouse.UP);
        }
        if (casillaActual.canGoDown()) {
            muros.set(1, Mouse.DOWN);
        }
        if (casillaActual.canGoLeft()) {
            muros.set(2, Mouse.LEFT);
        }
        if (casillaActual.canGoRight()) {
            muros.set(3, Mouse.RIGHT);
        }

        return muros;
    }

    /**
     * @brief Método que se llama cuando aparece un nuevo queso
     */
    @Override
    public void newCheese() {

        //Pongo a true nuevoQueso para que el ratón sepa que tiene que INTENTAR calcular el camino. Si no puede, es porque
        //está en celda no visitada
        nuevoQueso = true;
        //Vacio el camino hacia el queso para recalcularlo
        caminoHaciaQueso.clear();
    }

    /**
     * @brief Método que se llama cuando el raton pisa una bomba
     */
    @Override
    public void respawned() {
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

    /**
     * @brief Cálculo de la distancia de Manhattan
     * @return distancia desde la casilla al queso, entero.
    */
    int distManhattan(Pair<Integer, Integer> queso, int cordX, int cordY) {
        int difx = Math.abs(cordX - queso.getKey());
        int dify = Math.abs(cordY - queso.getValue());
        return (difx + dify);
    }

    public void busquedaPrimeroElMejor(Grid dondeEstoy, Cheese queso) {
        HashMap<Pair<Integer, Integer>, Grid> visitadasGreedy = new HashMap<>();
        HashMap<Pair<Integer, Integer>, Grid> celdasProhibidas = new HashMap<>();

        //Creo los PAIR que indican donde esta el RATÓN y donde está el QUESO
        Pair<Integer, Integer> pairDondeEstoy = new Pair(dondeEstoy.getX(), dondeEstoy.getY());
        Pair<Integer, Integer> pairDondeEstaQueso = new Pair(queso.getX(), queso.getY());

        recorridoGreedy(pairDondeEstoy, pairDondeEstaQueso, NO_PASO, visitadasGreedy, celdasProhibidas);

    }

    private boolean recorridoGreedy(Pair<Integer, Integer> dondeEstoy, Pair<Integer, Integer> dondeEstaQueso, int porDondeHeVenidoJustoAhora, HashMap<Pair<Integer, Integer>, Grid> celdasVisitadasDuranteGreedy, HashMap<Pair<Integer, Integer>, Grid> celdasProhibidas) {
        boolean estaCasillaLaHeVisitado = celdasVisitadas.containsKey(dondeEstoy);
        boolean visitadaDuranteBusqueda = celdasVisitadasDuranteGreedy.containsKey(dondeEstoy);
        boolean estaProhibida = celdasProhibidas.containsKey(dondeEstoy);
        PriorityQueue<Pair<Integer, Integer>> posiblesSiguientes = new PriorityQueue<>(comparador);
        boolean quesoEncontrado = false;

        //¿Tengo Queso?
        if (dondeEstoy.getKey() == dondeEstaQueso.getKey() && dondeEstoy.getValue() == dondeEstaQueso.getValue()) {
            return true;
        }
        
        //¿He explorado la casilla?
        if (!estaCasillaLaHeVisitado) {
            return false;
        }

        //¿He visitado la casilla en la exploracion o está prohibida? SI->Sigo | NO-> Paro
        if (estaProhibida) {
            return false;
        }

        //¿Se ha expandido por otra rama (es decir, ya la he visitado por otro camino)?
        if (!visitadaDuranteBusqueda) {
            celdasVisitadasDuranteGreedy.put(dondeEstoy, new Grid(dondeEstoy.getKey(), dondeEstoy.getValue()));
        } else {
            return false;
        }

        //Obtengo muros
        Vector<Integer> muros = celdasVisitadas.get(dondeEstoy).getValue();
        int cuantosMuros = 0;
        //Veo las distancias:
        for (int i = 0; i < 4; i++) {
            //Cuento cuantos muros tiene, si no es muro, miro distancias según la dirección
            if (muros.get(i) == NO_PASO) {
                cuantosMuros++;
            } else {
                int direccion = muros.get(i);
                if (direccion != porDondeHeVenidoJustoAhora) {
                    int distancia = 0;
                    if (direccion == Mouse.UP) {
                        distancia = distManhattan(dondeEstaQueso, dondeEstoy.getKey(), dondeEstoy.getValue() + 1);
                    }
                    if (direccion == Mouse.DOWN) {
                        distancia = distManhattan(dondeEstaQueso, dondeEstoy.getKey(), dondeEstoy.getValue() - 1);
                    }
                    if (direccion == Mouse.LEFT) {
                        distancia = distManhattan(dondeEstaQueso, dondeEstoy.getKey() - 1, dondeEstoy.getValue());
                    }
                    if (direccion == Mouse.RIGHT) {
                        distancia = distManhattan(dondeEstaQueso, dondeEstoy.getKey() + 1, dondeEstoy.getValue());
                    }
                    posiblesSiguientes.add(new Pair<>(distancia, direccion));
                }

            }

        }
        /*¿Es una casilla que puede ser problematica?  SI -> Descarto | NO -> Empiezo a expandir nodos(casillas) comparando distancias
         * Esta condicion creo que puede ser problemática.
         */
        if (cuantosMuros == 3) {
            porDondeHeVenidoJustoAhora = direccionComplementaria(porDondeHeVenidoJustoAhora);
            celdasProhibidas.put(dondeEstoy, celdasVisitadas.get(dondeEstoy).getKey());
            caminoHaciaQueso.add(porDondeHeVenidoJustoAhora);
            return false;
        }
        /*Mientras que no se me vacie mi pila y no encuentre el queso, busco*/
        while (!posiblesSiguientes.isEmpty()) {
            /*Obtengo de la cola la direccion con menor distancia*/
            int dirPrioritaria = posiblesSiguientes.poll().getValue();
            /*Miro que dirección es*/
            if (dirPrioritaria == Mouse.UP) {
                /*Paso la dir. Complementaria de la direccion por la donde he venido*/
                porDondeHeVenidoJustoAhora = Mouse.DOWN;

                //Hago el recorrido recursivo buscando el queso por este camino. Devuelve true o false si se puede o no
                quesoEncontrado = recorridoGreedy(new Pair(dondeEstoy.getKey(), dondeEstoy.getValue() + 1), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteGreedy, celdasProhibidas);
                if (quesoEncontrado) {
                    //Si encuentro el queso, es decir, no me quedo pillado ni nada, guardo el movimiento
                    caminoHaciaQueso.add(Mouse.UP);
                    return true;
                }
            }
            if (dirPrioritaria == Mouse.DOWN) {

                porDondeHeVenidoJustoAhora = Mouse.UP;

                quesoEncontrado = recorridoGreedy(new Pair(dondeEstoy.getKey(), dondeEstoy.getValue() - 1), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteGreedy, celdasProhibidas);
                if (quesoEncontrado) {
                    caminoHaciaQueso.add(Mouse.DOWN);
                    return true;
                }
            }
            if (dirPrioritaria == Mouse.LEFT) {

                porDondeHeVenidoJustoAhora = Mouse.RIGHT;

                quesoEncontrado = recorridoGreedy(new Pair(dondeEstoy.getKey() - 1, dondeEstoy.getValue()), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteGreedy, celdasProhibidas);
                if (quesoEncontrado) {
                    caminoHaciaQueso.add(Mouse.LEFT);
                    return true;
                }
            }
            if (dirPrioritaria == Mouse.RIGHT) {

                porDondeHeVenidoJustoAhora = Mouse.LEFT;

                quesoEncontrado = recorridoGreedy(new Pair(dondeEstoy.getKey() + 1, dondeEstoy.getValue()), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteGreedy, celdasProhibidas);
                if (quesoEncontrado) {
                    caminoHaciaQueso.add(Mouse.RIGHT);
                    return true;
                }
            }
        }
        /*Si agoto la pila es que no tengo posibles caminos, retorno falso*/
        return false;
    }

}
