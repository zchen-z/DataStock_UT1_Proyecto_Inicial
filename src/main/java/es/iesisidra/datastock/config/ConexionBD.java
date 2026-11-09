package es.iesisidra.datastock.config;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones con MySQL usando los datos de db.properties (excluido de Git).
 */
public class ConexionBD {

    private static final Path FICHERO = Path.of("db.properties"); // en la raíz del proyecto

    /**
     * Abre una conexión nueva con MySQL. Quien la pide debe cerrarla (try-with-resources).
     *
     * @return conexión abierta
     * @throws SQLException si falta db.properties o MySQL rechaza la conexión
     */
    public static Connection obtenerConexion() throws SQLException {
        Properties datos = new Properties();
        try (InputStream entrada = Files.newInputStream(FICHERO)) {
            datos.load(entrada); // lee las líneas clave=valor
        } catch (IOException e) {
            throw new SQLException("no se encuentra el fichero db.properties");
        }
        return DriverManager.getConnection(datos.getProperty("db.url"),
                datos.getProperty("db.user"), datos.getProperty("db.password"));
    }
}