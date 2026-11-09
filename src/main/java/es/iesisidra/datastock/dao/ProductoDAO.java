package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Operaciones JDBC sobre la tabla producto.
 */
public class ProductoDAO {

    /**
     * Cuenta los productos guardados en MySQL.
     *
     * @return número de filas de la tabla producto
     * @throws SQLException si falla la conexión o la consulta
     */
    public int contar() throws SQLException {
        int total;
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM producto")) {
            rs.next();            // COUNT(*) siempre devuelve una fila
            total = rs.getInt(1); // primera columna
        }
        return total;
    }
}
