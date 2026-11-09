package es.iesisidra.datastock.complementarias;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;

/**
 * Actividad comparativa (RA2-b): base de datos SQLite embebida en un fichero local.
 * Se ejecuta por separado; no forma parte de los menús de DataStock.
 */
public final class DemoSQLite {

    private static final Path CARPETA = Path.of("datos", "sqlite");
    private static final String URL = "jdbc:sqlite:datos/sqlite/productos_demo.db";

    private DemoSQLite() { }

    /** Abre (o crea) el fichero, crea la tabla, inserta los dos registros y los lista. */
    public static void main(String[] args) {
        try {
            Files.createDirectories(CARPETA); // SQLite crea el fichero, pero no la carpeta
            try (Connection con = DriverManager.getConnection(URL)) {
                ok("Conexión SQLite abierta: " + CARPETA.resolve("productos_demo.db"));
                crearTabla(con);
                insertarDemo(con);
                listar(con);
            }
        } catch (IOException e) {
            error("No se pudo crear la carpeta datos/sqlite: " + e.getMessage());
        } catch (SQLException e) {
            error("SQLite: " + e.getMessage());
        }
    }

    /** Crea la tabla solo si no existe. */
    private static void crearTabla(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS producto_demo ("
                    + "codigo TEXT PRIMARY KEY NOT NULL, nombre TEXT NOT NULL)");
        }
        ok("Tabla producto_demo disponible.");
    }

    /** Inserta los dos registros de ejemplo sin duplicarlos si ya existen. */
    private static void insertarDemo(Connection con) throws SQLException {
        int insertados = 0;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT OR IGNORE INTO producto_demo (codigo, nombre) VALUES (?, ?)")) {
            insertados += insertar(ps, "SQL-001", "Producto demo A");
            insertados += insertar(ps, "SQL-002", "Producto demo B");
        }
        System.out.println("Insertados: " + insertados + " | Ya existentes: " + (2 - insertados));
    }

    /**
     * @return 1 si se ha insertado, 0 si ya existía (OR IGNORE)
     */
    private static int insertar(PreparedStatement ps, String codigo, String nombre) throws SQLException {
        ps.setString(1, codigo);
        ps.setString(2, nombre);
        return ps.executeUpdate();
    }

    /** Muestra los registros ordenados por código. */
    private static void listar(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT codigo, nombre FROM producto_demo ORDER BY codigo")) {
            while (rs.next()) {
                System.out.println(rs.getString("codigo") + " | " + rs.getString("nombre"));
            }
        }
    }
}
