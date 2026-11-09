package es.iesisidra.datastock.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Categoria;

/**
 * Operaciones JDBC sobre la tabla categoria.
 */
public class CategoriaDAO {

    /**
     * Inserta una categoría nueva.
     *
     * @param c categoría ya validada
     * @throws SQLException si MySQL rechaza la inserción
     */
    public void insertar(Categoria c) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("INSERT INTO categoria (id, nombre) VALUES (?, ?)")) {
            ps.setInt(1, c.getId());       // primer ?
            ps.setString(2, c.getNombre()); // segundo ?
            ps.executeUpdate();
        }
    }

    /**
     * @return todas las categorías ordenadas por ID
     * @throws SQLException si falla la consulta
     */
    public List<Categoria> listar() throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nombre FROM categoria ORDER BY id")) {
            while (rs.next()) { // una vuelta por fila
                lista.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        }
        return lista;
    }

    /**
     * @param id identificador a buscar
     * @return la categoría, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Categoria buscarPorId(int id) throws SQLException {
        Categoria c = null;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id, nombre FROM categoria WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    c = new Categoria(rs.getInt("id"), rs.getString("nombre"));
                }
            }
        }
        return c;
    }

    /**
     * Comprueba si otra categoría ya usa ese nombre.
     *
     * @param nombre     nombre a comprobar
     * @param idExcluido ID que no se cuenta (la propia categoría al modificar; 0 en el alta)
     * @return true si el nombre ya está en uso
     * @throws SQLException si falla la consulta
     */
    public boolean existeNombre(String nombre, int idExcluido) throws SQLException {
        boolean existe;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM categoria WHERE nombre = ? AND id <> ?")) {
            ps.setString(1, nombre);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                existe = rs.getInt(1) > 0;
            }
        }
        return existe;
    }

    /**
     * @return true si se ha modificado una fila
     * @throws SQLException si MySQL rechaza el cambio
     */
    public boolean modificarNombre(int id, String nombre) throws SQLException {
        boolean modificada;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("UPDATE categoria SET nombre = ? WHERE id = ?")) {
            ps.setString(1, nombre);
            ps.setInt(2, id);
            modificada = ps.executeUpdate() == 1; // nº de filas afectadas
        }
        return modificada;
    }

    /**
     * @return true si se ha borrado una fila
     * @throws SQLException si MySQL lo impide (por ejemplo, tiene productos)
     */
    public boolean eliminar(int id) throws SQLException {
        boolean eliminada;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM categoria WHERE id = ?")) {
            ps.setInt(1, id);
            eliminada = ps.executeUpdate() == 1;
        }
        return eliminada;
    }
}
