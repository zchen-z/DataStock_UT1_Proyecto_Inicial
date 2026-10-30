package es.iesisidra.datastock.complementarias.rf07;

/**
 * RF-FIC-07 · Punto de entrada de la actividad de consultas XPath.
 * <p>Actividad independiente del menú principal de DataStock. Igual que
 * la App principal, solo crea el menú y lo arranca.</p>
 */
public final class App {

    private App() { }

    /** Crea el menú de consultas XPath y lo arranca. */
    public static void main(String[] args) {
        MenuXPath menu = new MenuXPath();
        menu.iniciar();
    }
}
