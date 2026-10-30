package es.iesisidra.datastock.complementarias.rf08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

import es.iesisidra.datastock.ficheros.ImportadorJson;
import es.iesisidra.datastock.ficheros.ImportadorXml;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.servicio.ResultadoImportacion;
import es.iesisidra.datastock.util.LectorConsola;

/**
 * RF-FIC-08 · Menú de la actividad de acceso aleatorio.
 * <p>Igual que {@code MenuPrincipal}: pide datos al usuario, delega en
 * {@link FicheroStock} y muestra los resultados.</p>
 */
public class MenuAccesoAleatorio {

    private final Inventario inventario;
    private final FicheroStock ficheroStock = new FicheroStock();
    private final ImportadorJson importadorJson = new ImportadorJson();
    private final ImportadorXml importadorXml = new ImportadorXml();
    private Path fichero;

    /** @param inventario colección donde se cargan los productos de esta actividad */
    public MenuAccesoAleatorio(Inventario inventario) {
        this.inventario = inventario;
    }

    /** Carga un catálogo, pide la ruta del fichero binario y muestra el menú hasta salir. */
    public void iniciar() {
        System.out.println("===== ACCESO ALEATORIO =====");
        cargarCatalogo();
        fichero = pedirFichero();
        if (fichero != null) {
            int opcion;
            do {
                System.out.println("\n1. Generar stock.bin | 2. Mostrar todos | 3. Consultar posición"
                        + " | 4. Modificar stock | 0. Salir");
                opcion = LectorConsola.leerOpcion("Opción: ", 0, 4);
                switch (opcion) {
                    case 1 -> generar();
                    case 2 -> mostrarTodos();
                    case 3 -> consultar();
                    case 4 -> modificarStock();
                    case 0 -> System.out.println("Fin de la actividad de acceso aleatorio.");
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } while (opcion != 0);
        }
    }

    /** Importa un catálogo JSON o XML (según la extensión) con los importadores de RF-02 y RF-03. */
    private void cargarCatalogo() {
        String ruta = LectorConsola.leerTexto("Catálogo JSON o XML a cargar (Intro para ninguno): ").trim();
        if (!ruta.isEmpty()) {
            ResultadoImportacion resultado;
            if (ruta.toLowerCase().endsWith(".xml")) {
                resultado = importadorXml.importar(ruta, inventario);
            } else {
                resultado = importadorJson.importar(ruta, inventario);
            }
            if (resultado.documentoValido()) {
                System.out.println("Productos cargados en memoria: " + inventario.getProductos().size());
            } else {
                System.out.println("ERROR: " + resultado.getErrorDocumento());
            }
        }
    }

    /**
     * Pide la ruta del fichero binario con el que se trabajará.
     *
     * @return la ruta, o null si no es válida (ya se ha mostrado el motivo)
     */
    private Path pedirFichero() {
        Path ruta = null;
        String texto = LectorConsola.leerTexto("Fichero binario (por ejemplo datos/salidas/stock.bin): ");
        if (texto.isBlank()) {
            System.out.println("ERROR: la ruta no puede estar vacía.");
        } else {
            try {
                ruta = Path.of(texto.trim());
            } catch (InvalidPathException e) {
                System.out.println("ERROR: la ruta no es válida.");
            }
        }
        return ruta;
    }

    /** Opción 1: genera el fichero con los productos en memoria, ordenados por código. */
    private void generar() {
        List<Producto> productos = inventario.listarOrdenadosPorCodigo();
        if (productos.isEmpty()) {
            System.out.println("No hay productos en memoria: no se genera el fichero.");
        } else if (Files.exists(fichero) && !LectorConsola.confirmar("El fichero ya existe. ¿Sobrescribir?")) {
            System.out.println("Generación cancelada: el fichero no se ha modificado.");
        } else {
            try {
                long tamano = ficheroStock.generar(productos, fichero);
                System.out.println("Fichero generado: " + fichero.toAbsolutePath());
                System.out.println("Registros: " + productos.size() + " | Tamaño: " + tamano + " bytes");
            } catch (IOException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    /** Opción 2: muestra todos los registros (lectura secuencial). */
    private void mostrarTodos() {
        try {
            List<String> registros = ficheroStock.leerTodos(fichero);
            System.out.println("Registros: " + registros.size() + " | Tamaño: " + Files.size(fichero) + " bytes");
            for (int i = 0; i < registros.size(); i++) {
                System.out.println(i + ". " + registros.get(i));
            }
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /** Opción 3: consulta un registro por su posición (acceso directo con seek). */
    private void consultar() {
        int posicion = LectorConsola.leerEntero("Posición (empezando en 0): ");
        try {
            System.out.println(ficheroStock.leer(fichero, posicion));
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /** Opción 4: modifica solo el stock de un registro. */
    private void modificarStock() {
        int posicion = LectorConsola.leerEntero("Posición (empezando en 0): ");
        try {
            System.out.println(ficheroStock.leer(fichero, posicion));
            int nuevo = LectorConsola.leerEntero("Nuevo stock: ");
            int anterior = ficheroStock.modificarStock(fichero, posicion, nuevo);
            System.out.println("Actualización correcta: stock " + anterior + " -> " + nuevo);
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
