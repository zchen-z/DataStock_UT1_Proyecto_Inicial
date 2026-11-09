package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import es.iesisidra.datastock.modelo.Producto;
import java.sql.PreparedStatement;

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

        /**
     * Inserta un producto nuevo.
     *
     * @param p producto ya validado
     * @throws SQLException si MySQL rechaza la inserción
     */
    public void insertar(Producto p) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO producto (codigo, nombre, precio, stock, categoria_id, proveedor_id) "
                     + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setBigDecimal(3, p.getPrecio()); // dinero exacto
            ps.setInt(4, p.getStock());
            ps.setInt(5, p.getCategoriaId());
            ps.setInt(6, p.getProveedorId());
            ps.executeUpdate();
        }
    }

    /**
     * @param codigo código ya normalizado (mayúsculas)
     * @return el producto, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Producto buscarPorCodigo(String codigo) throws SQLException {
        Producto p = null;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT codigo, nombre, precio, stock, categoria_id, proveedor_id FROM producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p = new Producto(rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio"),
                            rs.getInt("stock"), rs.getInt("categoria_id"), rs.getInt("proveedor_id"));
                }
            }
        }
        return p;
    }

    /**
     * Cambia nombre, precio, categoría y proveedor. El stock NO se toca:
     * solo cambia con los movimientos (opción 7.6).
     *
     * @return true si se ha modificado una fila
     * @throws SQLException si MySQL rechaza el cambio
     */
    public boolean modificar(Producto p) throws SQLException {
        boolean modificado;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE producto SET nombre = ?, precio = ?, categoria_id = ?, proveedor_id = ? WHERE codigo = ?")) {
            ps.setString(1, p.getNombre());
            ps.setBigDecimal(2, p.getPrecio());
            ps.setInt(3, p.getCategoriaId());
            ps.setInt(4, p.getProveedorId());
            ps.setString(5, p.getCodigo());
            modificado = ps.executeUpdate() == 1;
        }
        return modificado;
    }

    /**
     * @return true si se ha borrado una fila
     * @throws SQLException si MySQL lo impide (tiene movimientos)
     */
    public boolean eliminar(String codigo) throws SQLException {
        boolean eliminado;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            eliminado = ps.executeUpdate() == 1;
        }
        return eliminado;
    }
}
