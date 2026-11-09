package es.iesisidra.datastock.ui;

import java.sql.SQLException;
import java.util.List;

import es.iesisidra.datastock.dao.ProveedorDAO;
import es.iesisidra.datastock.modelo.Proveedor;
import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;
import es.iesisidra.datastock.util.LectorConsola;

/**
 * Opción 7.3 · RF-JDBC-02: gestión de proveedores en MySQL.
 */
public class MenuProveedores {

    private static final int ERROR_CLAVE_FORANEA = 1451;
    private static final String PATRON_EMAIL = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+"; // algo@algo.algo
    private final ProveedorDAO dao = new ProveedorDAO();

    /** Muestra el menú de proveedores hasta volver al menú MySQL. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE PROVEEDORES ----------");
            System.out.println("1. Dar de alta un proveedor");
            System.out.println("2. Listar proveedores");
            System.out.println("3. Modificar un proveedor");
            System.out.println("4. Eliminar un proveedor");
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
                error("MySQL: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    /** Pide ID, nombre y email opcional, comprueba duplicados e inserta. */
    private void alta() throws SQLException {
        System.out.println("\nALTA DE PROVEEDOR");
        int id = LectorConsola.leerEntero("ID de proveedor: ");
        String nombre = LectorConsola.leerTexto("Nombre: ").trim();
        String email = LectorConsola.leerTexto("Email (opcional, Intro para dejarlo vacío): ").trim();
        if (email.isEmpty()) {
            email = null; // sin email → NULL
        }
        String problema = validar(nombre, email);
        if (id <= 0) {
            problema = "el ID debe ser un entero positivo";
        }
        if (problema == null && dao.buscarPorId(id) != null) {
            problema = "ya existe un proveedor con el ID " + id;
        }
        if (problema == null && email != null && dao.existeEmail(email, 0)) {
            problema = "ya existe un proveedor con el email " + email;
        }
        if (problema != null) {
            error(problema);
        } else {
            dao.insertar(new Proveedor(id, nombre, email));
            ok("Proveedor registrado correctamente.");
        }
    }

    /** Muestra los proveedores ordenados por ID; «—» si no tienen email. */
    private void listar() throws SQLException {
        List<Proveedor> lista = dao.listar();
        if (lista.isEmpty()) {
            aviso("No hay proveedores registrados.");
        } else {
            System.out.printf("%-5s %-30s %s%n", "ID", "NOMBRE", "EMAIL");
            for (Proveedor p : lista) {
                System.out.printf("%-5d %-30s %s%n", p.getId(), p.getNombre(), mostrarEmail(p.getEmail()));
            }
        }
    }

    /** Cambia nombre y/o email. Intro conserva el valor; «-» deja el email vacío. */
    private void modificar() throws SQLException {
        int id = LectorConsola.leerEntero("ID del proveedor: ");
        Proveedor p = dao.buscarPorId(id);
        if (p == null) {
            error("No existe ningún proveedor con el ID " + id);
        } else {
            System.out.println("Nombre actual: " + p.getNombre());
            System.out.println("Email actual: " + mostrarEmail(p.getEmail()));
            String nombre = LectorConsola.leerTexto("Nuevo nombre (Intro = conservar): ").trim();
            if (nombre.isEmpty()) {
                nombre = p.getNombre();
            }
            String email = LectorConsola.leerTexto("Nuevo email (Intro = conservar, - = sin email): ").trim();
            if (email.isEmpty()) {
                email = p.getEmail();
            } else if (email.equals("-")) {
                email = null;
            }
            String problema = validar(nombre, email);
            if (problema == null && email != null && dao.existeEmail(email, id)) {
                problema = "ya existe otro proveedor con el email " + email;
            }
            if (problema != null) {
                error(problema);
            } else if (!LectorConsola.confirmar("¿Confirmas el cambio?")) {
                aviso("Modificación cancelada.");
            } else if (dao.modificar(new Proveedor(id, nombre, email))) {
                ok("Proveedor modificado correctamente.");
            } else {
                error("No se ha modificado ninguna fila.");
            }
        }
    }

    /** Muestra el proveedor, pide confirmación y lo borra si no tiene productos. */
    private void eliminar() throws SQLException {
        int id = LectorConsola.leerEntero("ID del proveedor: ");
        Proveedor p = dao.buscarPorId(id);
        if (p == null) {
            error("No existe ningún proveedor con el ID " + id);
        } else {
            System.out.println("Proveedor: " + p.getId() + " - " + p.getNombre() + " - " + mostrarEmail(p.getEmail()));
            if (!LectorConsola.confirmar("¿Confirmas la eliminación?")) {
                aviso("Eliminación cancelada.");
            } else {
                try {
                    if (dao.eliminar(id)) {
                        ok("Proveedor eliminado correctamente.");
                    } else {
                        error("No se ha eliminado ninguna fila.");
                    }
                } catch (SQLException e) {
                    if (e.getErrorCode() == ERROR_CLAVE_FORANEA) {
                        error("No se puede eliminar: el proveedor tiene productos asociados.");
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * @return el motivo si nombre o email no son válidos, o null si son correctos
     */
    private String validar(String nombre, String email) {
        String problema = null;
        if (nombre.isEmpty()) {
            problema = "el nombre es obligatorio";
        } else if (nombre.length() > 100) {
            problema = "el nombre no puede superar 100 caracteres";
        } else if (email != null && email.length() > 120) {
            problema = "el email no puede superar 120 caracteres";
        } else if (email != null && !email.matches(PATRON_EMAIL)) {
            problema = "el email no tiene un formato válido";
        }
        return problema;
    }

        /** @return el email, o "—" si no tiene */
    private String mostrarEmail(String email) {
        String texto = "—";
        if (email != null) {
            texto = email;
        }
        return texto;
    }
}
