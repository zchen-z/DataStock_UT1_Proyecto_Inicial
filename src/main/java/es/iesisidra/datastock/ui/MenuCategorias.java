package es.iesisidra.datastock.ui;

import java.sql.SQLException;
import java.util.List;

import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.modelo.Categoria;
import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;
import es.iesisidra.datastock.util.LectorConsola;

/**
 * Opción 7.2 · RF-JDBC-02: gestión de categorías en MySQL.
 */
public class MenuCategorias {

    private static final int ERROR_CLAVE_FORANEA = 1451; // código MySQL: la fila está referenciada
    private final CategoriaDAO dao = new CategoriaDAO();

    /** Muestra el menú de categorías hasta volver al menú MySQL. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE CATEGORÍAS ----------");
            System.out.println("1. Dar de alta una categoría");
            System.out.println("2. Listar categorías");
            System.out.println("3. Modificar el nombre de una categoría");
            System.out.println("4. Eliminar una categoría");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> alta();
                    case 2 -> listar();
                    case 3 -> modificar();
                    case 4 -> eliminar();
                    case 0 -> { }
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } catch (SQLException e) {
                error("MySQL: " + e.getMessage()); // cualquier fallo SQL vuelve al menú
            }
        } while (opcion != 0);
    }

    /** Pide ID y nombre, comprueba duplicados e inserta. */
    private void alta() throws SQLException {
        System.out.println("\nALTA DE CATEGORÍA");
        int id = LectorConsola.leerEntero("ID de categoría: ");
        String nombre = LectorConsola.leerTexto("Nombre: ").trim();
        String problema = validarNombre(nombre);
        if (id <= 0) {
            problema = "el ID debe ser un entero positivo";
        }
        if (problema == null && dao.buscarPorId(id) != null) {
            problema = "ya existe una categoría con el ID " + id;
        }
        if (problema == null && dao.existeNombre(nombre, 0)) {
            problema = "ya existe una categoría llamada " + nombre;
        }
        if (problema != null) {
            error(problema);
        } else {
            dao.insertar(new Categoria(id, nombre));
            ok("Categoría registrada correctamente.");
        }
    }

    /** Muestra las categorías ordenadas por ID. */
    private void listar() throws SQLException {
        List<Categoria> lista = dao.listar();
        if (lista.isEmpty()) {
            aviso("No hay categorías registradas.");
        } else {
            System.out.printf("%-5s %s%n", "ID", "NOMBRE");
            for (Categoria c : lista) {
                System.out.printf("%-5d %s%n", c.getId(), c.getNombre());
            }
        }
    }

    /** Cambia el nombre de una categoría existente, con confirmación. */
    private void modificar() throws SQLException {
        int id = LectorConsola.leerEntero("ID de la categoría: ");
        Categoria c = dao.buscarPorId(id);
        if (c == null) {
            error("No existe ninguna categoría con el ID " + id);
        } else {
            System.out.println("Nombre actual: " + c.getNombre());
            String nombre = LectorConsola.leerTexto("Nuevo nombre: ").trim();
            String problema = validarNombre(nombre);
            if (problema == null && dao.existeNombre(nombre, id)) {
                problema = "ya existe otra categoría llamada " + nombre;
            }
            if (problema != null) {
                error(problema);
            } else if (!LectorConsola.confirmar("¿Confirmas el cambio?")) {
                aviso("Modificación cancelada.");
            } else if (dao.modificarNombre(id, nombre)) {
                ok("Categoría modificada correctamente.");
            } else {
                error("No se ha modificado ninguna fila.");
            }
        }
    }

    /** Muestra la categoría, pide confirmación y la borra si no tiene productos. */
    private void eliminar() throws SQLException {
        int id = LectorConsola.leerEntero("ID de la categoría: ");
        Categoria c = dao.buscarPorId(id);
        if (c == null) {
            error("No existe ninguna categoría con el ID " + id);
        } else {
            System.out.println("Categoría: " + c.getId() + " - " + c.getNombre());
            if (!LectorConsola.confirmar("¿Confirmas la eliminación?")) {
                aviso("Eliminación cancelada.");
            } else {
                try {
                    if (dao.eliminar(id)) {
                        ok("Categoría eliminada correctamente.");
                    } else {
                        error("No se ha eliminado ninguna fila.");
                    }
                } catch (SQLException e) {
                    if (e.getErrorCode() == ERROR_CLAVE_FORANEA) {
                        error("No se puede eliminar: la categoría tiene productos asociados.");
                    } else {
                        throw e; // otro error: lo muestra iniciar()
                    }
                }
            }
        }
    }

    /**
     * @return el motivo si el nombre no es válido, o null si es correcto
     */
    private String validarNombre(String nombre) {
        String problema = null;
        if (nombre.isEmpty()) {
            problema = "el nombre es obligatorio";
        } else if (nombre.length() > 60) {
            problema = "el nombre no puede superar 60 caracteres";
        }
        return problema;
    }
}
