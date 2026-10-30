package es.iesisidra.datastock.complementarias.rf08;

import es.iesisidra.datastock.servicio.Inventario;

/**
 * RF-FIC-08 · Punto de entrada de la actividad de acceso aleatorio.
 * <p>Actividad independiente del menú principal de DataStock. Igual que
 * la App principal, solo crea los objetos necesarios y arranca el menú.</p>
 */
public final class App {

    private App() { }

    /** Crea un inventario propio de la actividad y arranca su menú. */
    public static void main(String[] args) {
        Inventario inventario = new Inventario();
        MenuAccesoAleatorio menu = new MenuAccesoAleatorio(inventario);
        menu.iniciar();
    }
}
