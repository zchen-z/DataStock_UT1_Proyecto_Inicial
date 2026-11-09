package es.iesisidra.datastock.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Proveedor;

/**
 * Operaciones JDBC sobre la tabla proveedor.
 */
public class ProveedorDAO {

    /**
     * Inserta un proveedor nuevo. Si el email es null, se guarda NULL.
     *
     * @param p proveedor ya validado
     * @throws SQLException si MySQL rechaza la inserción
     */
    public void insertar(Proveedor p) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("INSERT INTO proveedor (id, nombre, email) VALUES (?, ?, ?)")) {
            ps.setInt(1, p.getId());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getEmail()); // null → NULL en MySQL
            ps.executeUpdate();
        }
    }

    /**
     * @return todos los proveedores ordenados por ID
     * @throws SQLException si falla la consulta
     */
    public List<Proveedor> listar() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nombre, email FROM proveedor ORDER BY id")) {
            while (rs.next()) {
                lista.add(new Proveedor(rs.getInt("id"), rs.getString("nombre"), rs.getString("email")));
            }
        }
        return lista;
    }

    /**
     * @return el proveedor, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Proveedor buscarPorId(int id) throws SQLException {
        Proveedor p = null;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id, nombre, email FROM proveedor WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p = new Proveedor(rs.getInt("id"), rs.getString("nombre"), rs.getString("email"));
                }
            }
        }
        return p;
    }

    /**
     * Comprueba si otro proveedor ya usa ese email.
     *
     * @param idExcluido ID que no se cuenta (el propio al modificar; 0 en el alta)
     * @return true si el email ya está en uso
     * @throws SQLException si falla la consulta
     */
    public boolean existeEmail(String email, int idExcluido) throws SQLException {
        boolean existe;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM proveedor WHERE email = ? AND id <> ?")) {
            ps.setString(1, email);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                existe = rs.getInt(1) > 0;
            }
        }
        return existe;
    }

    /**
     * Cambia nombre y email de un proveedor.
     *
     * @return true si se ha modificado una fila
     * @throws SQLException si MySQL rechaza el cambio
     */
    public boolean modificar(Proveedor p) throws SQLException {
        boolean modificado;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("UPDATE proveedor SET nombre = ?, email = ? WHERE id = ?")) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getEmail());
            ps.setInt(3, p.getId());
            modificado = ps.executeUpdate() == 1;
        }
        return modificado;
    }

    /**
     * @return true si se ha borrado una fila
     * @throws SQLException si MySQL lo impide (tiene productos)
     */
    public boolean eliminar(int id) throws SQLException {
        boolean eliminado;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM proveedor WHERE id = ?")) {
            ps.setInt(1, id);
            eliminado = ps.executeUpdate() == 1;
        }
        return eliminado;
    }
}
