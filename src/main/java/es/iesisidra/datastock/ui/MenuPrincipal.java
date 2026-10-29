package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;

import es.iesisidra.datastock.ficheros.ExploradorFicheros;

import es.iesisidra.datastock.ficheros.ImportadorJson;
import es.iesisidra.datastock.servicio.ResultadoImportacion;

/**
 * Presentación de menús y navegación de la primera fase de DataStock.
 * <p>
 * Los métodos marcados TODO son puntos de partida, NO soluciones de los
 * requisitos. El alumnado deberá delegar el acceso a ficheros, la validación
 * y el procesamiento en métodos o clases independientes.
 * </p>
 */
public class MenuPrincipal {
    private final Inventario inventario;
    private final ExploradorFicheros explorador = new ExploradorFicheros();
    private final ImportadorJson importadorJson = new ImportadorJson();

    /** @param inventario colección común de productos de esta ejecución. */
    public MenuPrincipal(Inventario inventario) {
        this.inventario = inventario;
    }

    /** Ejecuta el menú principal hasta que el usuario selecciona salir. */
    public void iniciar() {
        int opcion;
        do {
            mostrarCabecera();
            System.out.println("1. Explorar ficheros y directorios");
            System.out.println("2. Importar productos desde JSON");
            System.out.println("3. Importar productos desde XML");
            System.out.println("4. Consultar productos");
            System.out.println("5. Exportar productos");
            System.out.println("6. Guardar y recuperar inventario");
            System.out.println("0. Salir");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 6);
            switch (opcion) {
                case 1 -> explorarRuta();
                case 2 -> importarJson();
                case 3 -> pendiente("RF-FIC-03 · Importar productos desde XML");
                case 4 -> menuConsultas();
                case 5 -> menuExportaciones();
                case 6 -> menuSerializacion();
                case 0 -> System.out.println(ColoresConsola.pintar("Hasta pronto.", ColoresConsola.VERDE));
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /**
     * Opción 1. RF-FIC-01.
     * Pide la ruta al usuario y hace la exploración en ExploradorFicheros.
     */
    private void explorarRuta() {
        System.out.println("\n===== EXPLORAR RUTA =====");
        String ruta = LectorConsola.leerTexto("Ruta: ");
        explorador.explorar(ruta);
        LectorConsola.pausar();
    }

    /**
     * Opción 2 · RF-FIC-02: pide la ruta, delega en ImportadorJson y muestra el
     * resumen.
     */
    private void importarJson() {
        System.out.println("\n===== IMPORTAR JSON =====");
        String ruta = LectorConsola.leerTexto("Ruta: ");
        ResultadoImportacion resultado = importadorJson.importar(ruta, inventario);
        mostrarResumen(resultado);
        LectorConsola.pausar();
    }

    /**
     * Muestra el resumen de una importación (JSON o XML):
     * contadores, incidencias o el error que ha cancelado el documento.
     */
    private void mostrarResumen(ResultadoImportacion resultado) {
        if (!resultado.documentoValido()) {
            System.out.println("ERROR: " + resultado.getErrorDocumento());
            System.out.println("Importación cancelada: no se ha añadido ningún producto.");
        } else {
            System.out.println("Productos encontrados: " + resultado.getEncontrados());
            System.out.println("Importados: " + resultado.getImportados() + " | Rechazados: " + resultado.getRechazados());
            if (resultado.getIncidencias().isEmpty()) {
                System.out.println("Importación finalizada.");
            } else {
                System.out.println("INCIDENCIAS:");
                for (String incidencia : resultado.getIncidencias()) {
                    System.out.println("- " + incidencia);
                }
                System.out.println("Importación finalizada con incidencias.");
            }
        }
    }

    private void mostrarCabecera() {
        System.out.println(
                ColoresConsola.pintar("\n================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("         DATASTOCK - GESTIÓN DE INVENTARIO", ColoresConsola.NEGRITA));
        System.out.println(ColoresConsola.pintar("                 FASE 1 - UT1", ColoresConsola.CIAN));
        System.out.println(
                ColoresConsola.pintar("================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("Productos en memoria: " + inventario.getProductos().size(),
                ColoresConsola.GRIS));
    }

    private void menuConsultas() {
        int opcion;
        do {
            System.out.println("\n---------- CONSULTA DE PRODUCTOS ----------");
            System.out.println("1. Listar todos los productos");
            System.out.println("2. Buscar producto por código");
            System.out.println("3. Buscar productos por nombre");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 3);
            switch (opcion) {
                case 1 -> pendiente("RF-FIC-04 · Listar productos");
                case 2 -> pendiente("RF-FIC-04 · Buscar por código");
                case 3 -> pendiente("RF-FIC-04 · Buscar por nombre");
                case 0 -> {
                }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void menuExportaciones() {
        int opcion;
        do {
            System.out.println("\n---------- EXPORTAR PRODUCTOS ----------");
            System.out.println("1. Exportar productos a JSON");
            System.out.println("2. Exportar productos a XML");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> pendiente("RF-FIC-05 · Exportar JSON");
                case 2 -> pendiente("RF-FIC-05 · Exportar XML");
                case 0 -> {
                }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void menuSerializacion() {
        int opcion;
        do {
            System.out.println("\n---------- GUARDAR Y RECUPERAR INVENTARIO ----------");
            System.out.println("1. Guardar inventario en fichero .dat");
            System.out.println("2. Recuperar inventario desde fichero .dat");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> pendiente("RF-FIC-06 · Guardar inventario");
                case 2 -> pendiente("RF-FIC-06 · Recuperar inventario");
                case 0 -> {
                }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /**
     * Sustituir las llamadas por servicios propios a medida que se implementen los
     * RF.
     */
    private void pendiente(String requisito) {
        System.out.println(ColoresConsola.pintar("[PENDIENTE] " + requisito, ColoresConsola.AMARILLO));
        System.out.println("Esta funcionalidad debe desarrollarla el alumnado.");
        LectorConsola.pausar();
    }
}
