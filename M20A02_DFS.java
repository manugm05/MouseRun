package mouserun.mouse;

import com.sun.tools.javac.util.Pair;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import mouserun.game.Mouse;
import mouserun.game.Grid;
import mouserun.game.Cheese;

/**
 * Clase que contiene el esqueleto del raton para las prácticas de Inteligencia
 * Artificial del curso 2019-20.
 *
 * @author Alejandro Arroyo Loaisa y Manuel Germán Morales
 */
public class M20A02_DFS extends Mouse {

    public static int NO_PASO = -1;
    /**
     * Variable para almacenar la ultima celda visitada
     */
    private Grid lastGrid;
    private boolean hePisadoBomba;
    //Para facilitar a MOVE saber si tiene que explorar o averiguar el camino al queso
    private boolean quesoEstaEnCasillaConocida; 
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

    /**
     * Constructor.
     */
    public M20A02_DFS() {
        super("Antonio");
        celdasVisitadas = new HashMap<>();
        pilaMovimientos = new Stack<>();
        caminoHaciaQueso = new Stack<>();
        lastGrid = null;
        hePisadoBomba = false;
        quesoEstaEnCasillaConocida = false;
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
                //System.out.println("Sé dónde está el queso, voy a por él\n");
                //Si sé dónde está, meto la casilla a la que me acabo de mover, para que no pete el método
                Pair<Integer, Integer> key = new Pair(currentGrid.getX(), currentGrid.getY());
                Vector<Integer> muros = compruebaMurosAlrededor(currentGrid);
                Pair<Grid, Vector<Integer>> casilla = new Pair(currentGrid, muros);
                
                celdasVisitadas.put(key, casilla);

                //Realizo recorrido DFS para obtener el camino hacia el queso
                recorridoDFS(currentGrid, new Grid(cheese.getX(), cheese.getY()));
                //System.out.println("Camino calculado. Necesito hacer " + caminoHaciaQueso.size() + " movimientos \n");
                
                //Y pongo nuevoQueso a false para que esto no lo vuelva a calcular
                nuevoQueso = false;
            } else {
                //System.out.println("Hay queso, pero no sé donde está :(\n");
                //Si no sé dónde está, continua explorando normalmente
            }

        }

        //Si el caminoHaciaQueso tiene elementos es PORQUE SÉ DONDE ESTÁ EL QUESO Y PUEDO
        //MOVERME HACIA ÉL. Por lo tanto, voy hacia allá
        if (caminoHaciaQueso.size() > 0) {

            int movimiento = caminoHaciaQueso.peek();
            caminoHaciaQueso.pop();
            //System.out.println("ESTOY YENDO HACIA EL QUESO. Hacia: " + movimiento);
            //JUSTO ESTE IF DE AQUI HACE QUE SE PARE ANTES DEL ULTIMO MOVIMIENTO PARA COMPROBAR------------------------------------------------
            //lo que está justo debajo comentado era para saber el camino sin hacerlo, se puede quitar.
//            if(caminoHaciaQueso.size() == 0){
//                exit(0);
//            }
//            while(caminoHaciaQueso.size()   >   0){
//                int movimiento = caminoHaciaQueso.peek();
//                caminoHaciaQueso.remove();
//                System.out.println("ESTOY YENDO HACIA EL QUESO. Hacia: " + movimiento); 
//            }
//            
//            if(caminoHaciaQueso.size() == 0){
//                exit(0);
//            }
            pilaMovimientos.push(new Pair(currentGrid, direccionComplementaria(movimiento)) );
            if( !visitada(currentGrid)){
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
                //System.out.println("Añadida celda (" + currentGrid.getX() + ", " + currentGrid.getY() + "). Visitadas: " + celdasVisitadas.size() + " celdas.\n");
                if (hePisadoBomba) {
                    //System.out.println("He añadido una celda en la que he aparecido por una explosion");
                }
            }

            Vector<Integer> muros = celdasVisitadas.get(key).snd;

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
                            //System.out.println("He explotado pero puedo explorar\n");
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

            //System.out.println("He explorado todo lo que hay alrededor de la casilla\n");
            Pair<Grid, Integer> ultimo;
            //4A - Si he explorad, me situo [PUEDE FALLAR]
            if (!pilaMovimientos.isEmpty()) {
                ultimo = pilaMovimientos.peek();
                if (hePisadoBomba) {
                    while (ultimo.fst != currentGrid && !pilaMovimientos.empty()) {
                        pilaMovimientos.pop();
                        ultimo = pilaMovimientos.peek();
                    }
                    //System.out.println("He conseguido un checkpoint\n");
                    hePisadoBomba = false;
                }
                //System.out.println("Tamaño de la pila: " + pilaMovimientos.size() + "\n");
                //4B - Si NO he podido moverme porque ya he visitado todas las casillas a mi alrededor (incluido callejón sin salida),
                //     miro cuál ha sido mi último movimiento y lo deshago.

                //System.out.println("Volviendo atras...\n");
                pilaMovimientos.pop();
                return ultimo.snd;
            }
            //TODO: En el caso que mi pila esté vacía, debería explorar el mapa de manera aleatoria para ver si hay casillas
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
        //System.out.println("Calculando si queso en celda visitada");
        Pair<Integer, Integer> casillaQueso = new Pair(cheese.getX(), cheese.getY());
        if (celdasVisitadas.containsKey(casillaQueso)) {

            //System.out.println("ESTA EN CELDA VISITADA.Calculado");
            return true;
        } else {

            //System.out.println("NO ESTÁ EN CELDA VISITADA. Calculado");
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
        
        //System.out.println("HA SALIDO UN QUESO NUEVO\n");
        //Pongo a true nuevoQueso para que el ratón sepa que tiene que INTENTAR calcular el camino. Si no puede, es porque
        //está en celda no visitada
        nuevoQueso = true;
        //Vacio el camino hacia el queso para recalcularlo
        while (!caminoHaciaQueso.isEmpty()) {
            caminoHaciaQueso.pop();
        }
    }

    /**
     * @brief Método que se llama cuando el raton pisa una bomba
     */
    @Override
    public void respawned() {
        //System.out.println("He pisado una bomba!\n");
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

    public void recorridoDFS(Grid dondeEstoy, Grid dondeEstaQueso) {

        HashMap<Pair<Integer, Integer>, Grid> celdasVisitadasDuranteDFS = new HashMap<>();
        
        //Creo los PAIR que indican donde esta el RATÓN y donde está el QUESO
        Pair<Integer, Integer> pairDondeEstoy = new Pair(dondeEstoy.getX(), dondeEstoy.getY());
        Pair<Integer, Integer> pairDondeEstaQueso = new Pair(dondeEstaQueso.getX(), dondeEstaQueso.getY());
        
        //Comienzo la búsqueda DFS
        //System.out.println("\n1 comienzo recorridoDFS");
        recorridoDFS_Recursivo(pairDondeEstoy, pairDondeEstaQueso, -1, celdasVisitadasDuranteDFS);
        //System.out.println("\n2 terminado recorridoDFS");
        
    }

    private boolean recorridoDFS_Recursivo(Pair<Integer, Integer> dondeEstoy, Pair<Integer, Integer> dondeEstaQueso, int porDondeHeVenidoJustoAhora, HashMap<Pair<Integer, Integer>, Grid> celdasVisitadasDuranteDFS) {

        boolean estaCasillaLaHeVisitado = celdasVisitadas.containsKey(dondeEstoy);
        boolean visitadaDuranteBusqueda = celdasVisitadasDuranteDFS.containsKey(dondeEstoy);
        
        int porDondeHeVenidoLocal = porDondeHeVenidoJustoAhora;
        boolean quesoEncontrado = false;
        //Este if es para que al hacer la recursividad y se mueva hacia una casilla, no vuelva de nuevo a la que vino
        //Si he visitado la casilla donde estoy, puedo seguir el camino por ahí, si no, corto directamente ese camino
        if (estaCasillaLaHeVisitado) {
            if(!visitadaDuranteBusqueda){
                    celdasVisitadasDuranteDFS.put(dondeEstoy, new Grid(dondeEstoy.fst,dondeEstoy.snd));
            }
            else{
                return false;
            }
            //System.out.println("..");

            //Averiguo los caminos que tengo alrededor 
            Vector<Integer> muros = celdasVisitadas.get(dondeEstoy).snd;

            //System.out.println("..");
            //System.out.println("Ratón: (" + dondeEstoy.fst +", "+ dondeEstoy.snd +").   Queso: (" + dondeEstaQueso.fst + ", " + dondeEstaQueso.snd + ").");
            
            //Si la casilla donde estoy es la misma que la del queso, lo he encontrado.
            //Pongo a TRUE quesoEncontrado para que ayude al salir de la recursividad y devuelvo true tambien
            if (dondeEstoy.fst == dondeEstaQueso.fst && dondeEstoy.snd == dondeEstaQueso.snd) {

                //System.out.println("QUESO ENCONTRADO");
                //exit(0);
                return true;
            }
            int cuantasCaminosHay = 0;
            //Si he llegado hasta aquí es que tengo que buscar el camino todavía desde donde estoy
            for (int i = 0; i < 4; i++) {
                //System.out.println("Muro " + (i+1) + ". Contenido:" + muros.get(i) + ". He llegado desde: " + porDondeHeVenidoJustoAhora);
                //Llevo la cuenta de los caminos disponibles, para saber si es un callejón sin salida
                if( muros.get(i) != -1){
                    cuantasCaminosHay++;
                }
                //Compruebo para cada muro si se puede ir y también si no es por donde he llegado
                if (muros.get(i) == Mouse.UP && muros.get(i) != porDondeHeVenidoJustoAhora ) {

                    //System.out.println("Camino hacia el queso ARRIBA");
                    
                    porDondeHeVenidoJustoAhora = Mouse.DOWN;
                    
                    
                    //Hago el recorrido recursivo buscando el queso por este camino. Devuelve true o false si se puede o no
                    quesoEncontrado = recorridoDFS_Recursivo(new Pair(dondeEstoy.fst, dondeEstoy.snd + 1), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteDFS);
                    if(quesoEncontrado){
                        //Guardo el movimiento que he hecho en la cola
                        caminoHaciaQueso.add(Mouse.UP);
                        return true;
                    }
                }if (muros.get(i) == Mouse.DOWN && muros.get(i) != porDondeHeVenidoJustoAhora) {

                    //System.out.println("Camino hacia el queso ABAJO");
                    
                    porDondeHeVenidoJustoAhora =  Mouse.UP;
                    
                    //Hago el recorrido recursivo buscando el queso por este camino. Devuelve true o false si se puede o no
                    quesoEncontrado =  recorridoDFS_Recursivo(new Pair(dondeEstoy.fst, dondeEstoy.snd - 1), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteDFS);
                    if(quesoEncontrado){
                        //Guardo el movimiento que he hecho en la cola
                        caminoHaciaQueso.add(Mouse.DOWN);
                        return true;
                    }       
                }if (muros.get(i) == Mouse.LEFT && muros.get(i) != porDondeHeVenidoJustoAhora) {

                    //System.out.println("Camino hacia el queso IZQUIERDA");
                    
                    porDondeHeVenidoJustoAhora =  Mouse.RIGHT;
                    
                    //Hago el recorrido recursivo buscando el queso por este camino. Devuelve true o false si se puede o no
                    quesoEncontrado = recorridoDFS_Recursivo(new Pair(dondeEstoy.fst - 1, dondeEstoy.snd), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteDFS);
                    if(quesoEncontrado){
                        //Guardo el movimiento que he hecho en la cola
                        caminoHaciaQueso.add(Mouse.LEFT);
                        return true;
                    }
                }if (muros.get(i) == Mouse.RIGHT && muros.get(i) != porDondeHeVenidoJustoAhora) {

                    //System.out.println("Camino hacia el queso DERECHA");
                    
                    porDondeHeVenidoJustoAhora =  Mouse.LEFT;
                    
                    //Hago el recorrido recursivo buscando el queso por este camino. Devuelve true o false si se puede o no
                    quesoEncontrado = recorridoDFS_Recursivo(new Pair(dondeEstoy.fst + 1, dondeEstoy.snd), dondeEstaQueso, porDondeHeVenidoJustoAhora, celdasVisitadasDuranteDFS);
                    if(quesoEncontrado){
                        //Guardo el movimiento que he hecho en la cola
                        caminoHaciaQueso.add(Mouse.RIGHT);
                        return true;
                    }
                }
                porDondeHeVenidoJustoAhora = porDondeHeVenidoLocal;
                //Si el camino es un callejón sin salida, corto este camino. Es decir, si hay solo un camino disponible o es la última iteración
                if( cuantasCaminosHay == 1 && i == 3){
                    //System.out.println("Me vuelvo una casilla atrás. Hacia "+ porDondeHeVenidoJustoAhora);
                    porDondeHeVenidoJustoAhora = direccionComplementaria(porDondeHeVenidoJustoAhora);
                    return false;
                }
                //System.out.println("No puedo hacia ahí.");

            }
        }else{
            //No he visitado esta casilla por lo que debo volver a la anterior. Salgo de esta iteracion de la recursividad
            //System.out.println("No la he visitado.");
            return false;
        }
        //Este return NUNCA se hace pero el programa me da error si no hago un return
        return quesoEncontrado;
    }
}
