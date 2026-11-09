package es.iesisidra.datastock.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import es.iesisidra.datastock.ficheros.ExploradorFicheros;
import es.iesisidra.datastock.ficheros.ExportadorProductos;
import es.iesisidra.datastock.ficheros.ImportadorJson;
import es.iesisidra.datastock.ficheros.ImportadorXml;
import es.iesisidra.datastock.ficheros.SerializadorInventario;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.servicio.ResultadoImportacion;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;

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
    private final ImportadorXml importadorXml = new ImportadorXml();
    private final ExportadorProductos exportador = new ExportadorProductos();
    private final SerializadorInventario serializador = new SerializadorInventario();
    private final MenuMySQL menuMySQL;

    /** @param inventario colección común de productos de esta ejecución. */
    public MenuPrincipal(Inventario inventario) {
        this.inventario = inventario;
        this.menuMySQL = new MenuMySQL(inventario);
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
            System.out.println("7. Gestionar base de datos MySQL");
            System.out.println("0. Salir");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 7);
            switch (opcion) {
                case 1 -> explorarRuta();
                case 2 -> importarJson();
                case 3 -> importarXml();
                case 4 -> menuConsultas();
                case 5 -> menuExportaciones();
                case 6 -> menuSerializacion();
                case 7 -> menuMySQL.iniciar();
                case 0 -> System.out.println(ColoresConsola.pintar("Hasta pronto.", ColoresConsola.VERDE));
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /**
     * Opción 1. RF-FIC-01.Pide la ruta al usuario y hace la exploración en
     * ExploradorFicheros.
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
     * Opción 3 · RF-FIC-03: pide la ruta, delega en ImportadorXml y muestra el
     * resumen.
     */
    private void importarXml() {
        System.out.println("\n===== IMPORTAR XML =====");
        String ruta = LectorConsola.leerTexto("Ruta: ");
        System.out.println("Leyendo documento XML...");
        ResultadoImportacion resultado = importadorXml.importar(ruta, inventario);
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
            System.out.println(
                    "Importados: " + resultado.getImportados() + " | Rechazados: " + resultado.getRechazados());
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
                case 1 -> listarProductos();
                case 2 -> buscarPorCodigo();
                case 3 -> buscarPorNombre();
                case 0 -> {
                }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /** Opción 4.1 · RF-FIC-04: lista todos los productos ordenados por código. */
    private void listarProductos() {
        List<Producto> lista = inventario.listarOrdenadosPorCodigo();
        if (lista.isEmpty()) {
            System.out.println("El inventario está vacío. Importa primero un catálogo JSON o XML.");
        } else {
            mostrarTabla(lista);
        }
        LectorConsola.pausar();
    }

    /** Opción 4.2 · RF-FIC-04: busca un producto por su código exacto. */
    private void buscarPorCodigo() {
        if (inventario.getProductos().isEmpty()) {
            System.out.println("El inventario está vacío. Importa primero un catálogo JSON o XML.");
        } else {
            String codigo = LectorConsola.leerTexto("Código: ");
            if (codigo.isBlank()) {
                System.out.println("El código no puede estar vacío.");
            } else {
                Producto p = inventario.buscarPorCodigo(codigo);
                if (p == null) {
                    System.out.println("No existe ningún producto con el código " + codigo.trim() + ".");
                } else {
                    System.out.println("Código: " + p.getCodigo());
                    System.out.println("Nombre: " + p.getNombre());
                    System.out.println("Precio: " + p.getPrecio() + " EUR | Stock: " + p.getStock());
                    System.out.println("Categoría: " + p.getCategoriaId() + " | Proveedor: " + p.getProveedorId());
                }
            }
        }
        LectorConsola.pausar();
    }

    /** Opción 4.3 · RF-FIC-04: busca productos cuyo nombre contenga un texto. */
    private void buscarPorNombre() {
        if (inventario.getProductos().isEmpty()) {
            System.out.println("El inventario está vacío. Importa primero un catálogo JSON o XML.");
        } else {
            String texto = LectorConsola.leerTexto("Texto a buscar: ");
            if (texto.isBlank()) {
                System.out.println("El texto de búsqueda no puede estar vacío.");
            } else {
                List<Producto> encontrados = inventario.buscarPorNombre(texto);
                if (encontrados.isEmpty()) {
                    System.out.println("No hay productos cuyo nombre contenga \"" + texto.trim() + "\".");
                } else {
                    mostrarTabla(encontrados);
                }
            }
        }
        LectorConsola.pausar();
    }

    /** Muestra una lista de productos en forma de tabla. */
    private void mostrarTabla(List<Producto> lista) {
        System.out.printf("%-12s %-25s %12s %6s %5s %5s%n", "CÓDIGO", "NOMBRE", "PRECIO", "STOCK", "CAT", "PROV");
        for (Producto p : lista) {
            System.out.printf("%-12s %-25s %8s EUR %6d %5d %5d%n", p.getCodigo(), p.getNombre(),
                    p.getPrecio(), p.getStock(), p.getCategoriaId(), p.getProveedorId());
        }
        System.out.println("Total: " + lista.size() + " producto(s)");
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
                case 1 -> exportar("JSON");
                case 2 -> exportar("XML");
                case 0 -> {
                }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /**
     * Opciones 5.1 y 5.2 · RF-FIC-05: comprueba el inventario y la ruta, pide
     * confirmación si el fichero existe y delega en ExportadorProductos.
     *
     * @param formato "JSON" o "XML"
     */
    private void exportar(String formato) {
        System.out.println("\n===== EXPORTAR " + formato + " =====");
        if (inventario.getProductos().isEmpty()) {
            System.out.println("El inventario está vacío: no hay productos para exportar.");
        } else {
            String texto = LectorConsola.leerTexto("Ruta: ");
            String error = exportador.comprobarDestino(texto);
            if (error != null) {
                System.out.println("ERROR: " + error);
            } else {
                Path ruta = Path.of(texto.trim()).toAbsolutePath();
                if (Files.exists(ruta) && !LectorConsola.confirmar("El fichero ya existe. ¿Sobrescribir?")) {
                    System.out.println("Exportación cancelada: el fichero original no se ha modificado.");
                } else {
                    List<Producto> productos = inventario.listarOrdenadosPorCodigo();
                    try {
                        if (formato.equals("JSON")) {
                            exportador.exportarJson(productos, ruta);
                        } else {
                            exportador.exportarXml(productos, ruta);
                        }
                        System.out.println("Fichero generado correctamente.");
                        System.out.println("Ruta: " + ruta);
                        System.out.println("Formato: " + formato);
                        System.out.println("Productos exportados: " + productos.size());
                    } catch (IOException e) {
                        System.out.println("ERROR al escribir el fichero: " + e.getMessage());
                    }
                }
            }
        }
        LectorConsola.pausar();
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
                case 1 -> guardarInventario();
                case 2 -> recuperarInventario();
                case 0 -> {
                }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /**
     * Opción 6.1 · RF-FIC-06: guarda el inventario completo en un fichero .dat
     * mediante serialización. Si el inventario está vacío no se crea ningún fichero.
     */
    private void guardarInventario() {
        System.out.println("\n===== GUARDAR INVENTARIO =====");
        if (inventario.getProductos().isEmpty()) {
            System.out.println("El inventario está vacío: no se crea ningún fichero.");
        } else {
            String texto = LectorConsola.leerTexto("Ruta: ");
            String error = exportador.comprobarDestino(texto);
            if (error != null) {
                System.out.println("ERROR: " + error);
            } else {
                Path ruta = Path.of(texto.trim()).toAbsolutePath();
                if (Files.exists(ruta) && !LectorConsola.confirmar("El fichero ya existe. ¿Sobrescribir?")) {
                    System.out.println("Guardado cancelado: el fichero original no se ha modificado.");
                } else {
                    try {
                        serializador.guardar(inventario.getProductos(), ruta);
                        System.out.println("Inventario guardado correctamente en " + ruta);
                        System.out.println("Productos guardados: " + inventario.getProductos().size());
                    } catch (IOException e) {
                        System.out.println("ERROR al escribir el fichero: " + e.getMessage());
                    }
                }
            }
        }
        LectorConsola.pausar();
    }

    /**
     * Opción 6.2 · RF-FIC-06: recupera un fichero .dat y sustituye el inventario,
     * pidiendo confirmación si ya hay productos. Si la lectura falla, la memoria no cambia.
     */
    private void recuperarInventario() {
        System.out.println("\n===== RECUPERAR INVENTARIO =====");
        String texto = LectorConsola.leerTexto("Ruta: ");
        Path ruta = comprobarFicheroLectura(texto);
        if (ruta != null) {
            try {
                List<Producto> recuperados = serializador.recuperar(ruta);
                boolean sustituir = true;
                if (!inventario.getProductos().isEmpty()) {
                    System.out.println("Actualmente hay " + inventario.getProductos().size() + " productos en memoria.");
                    System.out.println("La recuperación sustituirá el inventario actual.");
                    sustituir = LectorConsola.confirmar("¿Continuar?");
                }
                if (sustituir) {
                    inventario.sustituirTodos(recuperados);
                    System.out.println("Inventario recuperado correctamente.");
                    System.out.println("Productos disponibles: " + inventario.getProductos().size());
                } else {
                    System.out.println("Recuperación cancelada: se conserva el inventario actual.");
                }
            } catch (IOException e) {
                System.out.println("ERROR: el fichero está dañado o no es compatible (" + e.getMessage() + ").");
                System.out.println("El inventario en memoria no se ha modificado.");
            }
        }
        LectorConsola.pausar();
    }

    /**
     * Comprueba que la ruta indicada es un fichero existente que se puede leer.
     *
     * @param texto ruta escrita por el usuario
     * @return la ruta, o null si no es válida (ya se ha mostrado el motivo)
     */
    private Path comprobarFicheroLectura(String texto) {
        Path ruta = null;
        if (texto == null || texto.isBlank()) {
            System.out.println("ERROR: la ruta no puede estar vacía");
        } else {
            try {
                Path candidata = Path.of(texto.trim()).toAbsolutePath();
                if (!Files.exists(candidata)) {
                    System.out.println("ERROR: el fichero no existe: " + candidata);
                } else if (!Files.isRegularFile(candidata)) {
                    System.out.println("ERROR: la ruta no es un fichero");
                } else if (!Files.isReadable(candidata)) {
                    System.out.println("ERROR: no tienes permisos para leer el fichero");
                } else {
                    ruta = candidata;
                }
            } catch (java.nio.file.InvalidPathException e) {
                System.out.println("ERROR: la ruta no es válida");
            }
        }
        return ruta;
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
