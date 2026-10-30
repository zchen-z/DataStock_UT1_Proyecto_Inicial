package es.iesisidra.datastock.complementarias.rf07;

import es.iesisidra.datastock.util.LectorConsola;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * RF-FIC-07 · Menú de la actividad de consultas XPath.
 * <p>Igual que {@code MenuPrincipal}: pide datos al usuario, delega en
 * {@link ConsultasXPath} y muestra los resultados.</p>
 */
public class MenuXPath {

    private final ConsultasXPath consultas = new ConsultasXPath();

    /** Pide el fichero XML, lo abre y muestra el menú de consultas hasta salir. */
    public void iniciar() {
        System.out.println("===== CONSULTAS XPATH =====");
        if (abrirFichero()) {
            int opcion;
            do {
                System.out.println("\n1. Por código | 2. Por categoría | 3. Precio superior | 4. Contar | 0. Salir");
                opcion = LectorConsola.leerOpcion("Opción: ", 0, 4);
                try {
                    switch (opcion) {
                        case 1 -> buscarPorCodigo();
                        case 2 -> filtrarPorCategoria();
                        case 3 -> filtrarPorPrecio();
                        case 4 -> contar();
                        case 0 -> System.out.println("Fin de las consultas XPath.");
                        default -> throw new IllegalStateException("Opción inesperada");
                    }
                } catch (XPathExpressionException e) {
                    System.out.println("ERROR al evaluar la expresión XPath: " + e.getMessage());
                }
            } while (opcion != 0);
        }
    }

    /**
     * Pide la ruta del XML y lo abre.
     *
     * @return true si el documento se ha abierto correctamente
     */
    private boolean abrirFichero() {
        boolean abierto = false;
        String texto = LectorConsola.leerTexto("Fichero: ");
        if (texto.isBlank()) {
            System.out.println("ERROR: la ruta no puede estar vacía.");
        } else {
            try {
                consultas.abrir(Path.of(texto.trim()));
                abierto = true;
            } catch (InvalidPathException e) {
                System.out.println("ERROR: la ruta no es válida.");
            } catch (IOException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
        return abierto;
    }

    /** Opción 1: busca por código. */
    private void buscarPorCodigo() throws XPathExpressionException {
        String codigo = LectorConsola.leerTexto("Código: ");
        if (codigo.isBlank()) {
            System.out.println("El código no puede estar vacío.");
        } else {
            mostrar(consultas.buscarPorCodigo(codigo));
        }
    }

    /** Opción 2: filtra por categoría (entero positivo). */
    private void filtrarPorCategoria() throws XPathExpressionException {
        int categoria = LectorConsola.leerEntero("Categoría: ");
        if (categoria <= 0) {
            System.out.println("La categoría debe ser un entero positivo.");
        } else {
            mostrar(consultas.filtrarPorCategoria(categoria));
        }
    }

    /** Opción 3: filtra por precio estrictamente superior al indicado. */
    private void filtrarPorPrecio() throws XPathExpressionException {
        String texto = LectorConsola.leerTexto("Precio mínimo: ").trim();
        BigDecimal minimo = null;
        try {
            minimo = new BigDecimal(texto);
        } catch (NumberFormatException e) {
            System.out.println("El precio debe ser un número (por ejemplo 100 o 19.95).");
        }
        if (minimo != null) {
            mostrar(consultas.filtrarPorPrecioSuperior(minimo));
        }
    }

    /** Opción 4: cuenta los productos del documento. */
    private void contar() throws XPathExpressionException {
        System.out.println("Número de productos: " + consultas.contar());
    }

    /** Muestra las líneas encontradas y el total, o un aviso si no hay coincidencias. */
    private void mostrar(List<String> lineas) {
        if (lineas.isEmpty()) {
            System.out.println("No hay coincidencias.");
        } else {
            for (String linea : lineas) {
                System.out.println(linea);
            }
            System.out.println("Productos encontrados: " + lineas.size());
        }
    }
}