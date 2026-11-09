package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import es.iesisidra.datastock.modelo.Producto;
import java.sql.PreparedStatement;
import es.iesisidra.datastock.modelo.ProductoDetalle;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones JDBC sobre la tabla producto.
 */
public class ProductoDAO {

    /**
     * SELECT común de las consultas: producto + nombres de categoría y proveedor.
     */
    private static final String SELECT_DETALLE = "SELECT p.codigo, p.nombre, p.precio, p.stock, p.categoria_id, p.proveedor_id, "
            + "c.nombre AS categoria, pr.nombre AS proveedor "
            + "FROM producto p "
            + "JOIN categoria c ON p.categoria_id = c.id "
            + "JOIN proveedor pr ON p.proveedor_id = pr.id ";

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
            rs.next(); // COUNT(*) siempre devuelve una fila
            total = rs.getInt(1); // primera columna
        }
        return total;
    }

    /**
     * Inserta un producto con su propia conexión (alta individual, opción 7.4).
     *
     * @throws SQLException si MySQL rechaza la inserción
     */
    public void insertar(Producto p) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            insertar(con, p);
        }
    }

    /**
     * Inserta un producto usando una conexión ya abierta (para transacciones).
     *
     * @throws SQLException si MySQL rechaza la inserción
     */
    public void insertar(Connection con, Producto p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO producto (codigo, nombre, precio, stock, categoria_id, proveedor_id) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setBigDecimal(3, p.getPrecio());
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

    /**
     * @return todos los productos ordenados por código
     * @throws SQLException si falla la consulta
     */
    public List<ProductoDetalle> listarDetalle() throws SQLException {
        List<ProductoDetalle> lista;
        try (Connection con = ConexionBD.obtenerConexion();
                PreparedStatement ps = con.prepareStatement(SELECT_DETALLE + "ORDER BY p.codigo")) {
            lista = leerDetalles(ps);
        }
        return lista;
    }

    /**
     * @param codigo código ya normalizado
     * @return la ficha del producto, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public ProductoDetalle buscarDetalle(String codigo) throws SQLException {
        ProductoDetalle detalle = null;
        try (Connection con = ConexionBD.obtenerConexion();
                PreparedStatement ps = con.prepareStatement(SELECT_DETALLE + "WHERE p.codigo = ?")) {
            ps.setString(1, codigo);
            List<ProductoDetalle> lista = leerDetalles(ps);
            if (!lista.isEmpty()) {
                detalle = lista.get(0); // el código es único: como mucho hay uno
            }
        }
        return detalle;
    }

    /**
     * Búsqueda parcial: productos cuyo nombre contiene el texto.
     *
     * @return coincidencias ordenadas por código
     * @throws SQLException si falla la consulta
     */
    public List<ProductoDetalle> buscarPorNombre(String texto) throws SQLException {
        List<ProductoDetalle> lista;
        try (Connection con = ConexionBD.obtenerConexion();
                PreparedStatement ps = con
                        .prepareStatement(SELECT_DETALLE + "WHERE p.nombre LIKE ? ORDER BY p.codigo")) {
            ps.setString(1, "%" + texto + "%"); // % = cualquier texto antes y después
            lista = leerDetalles(ps);
        }
        return lista;
    }

    /**
     * @return productos con stock estrictamente menor que el umbral, por stock y
     *         código
     * @throws SQLException si falla la consulta
     */
    public List<ProductoDetalle> stockInferior(int umbral) throws SQLException {
        List<ProductoDetalle> lista;
        try (Connection con = ConexionBD.obtenerConexion();
                PreparedStatement ps = con
                        .prepareStatement(SELECT_DETALLE + "WHERE p.stock < ? ORDER BY p.stock, p.codigo")) {
            ps.setInt(1, umbral);
            lista = leerDetalles(ps);
        }
        return lista;
    }

    /** Ejecuta la consulta y convierte cada fila en un ProductoDetalle. */
    private List<ProductoDetalle> leerDetalles(PreparedStatement ps) throws SQLException {
        List<ProductoDetalle> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = new Producto(rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio"),
                        rs.getInt("stock"), rs.getInt("categoria_id"), rs.getInt("proveedor_id"));
                lista.add(new ProductoDetalle(p, rs.getString("categoria"), rs.getString("proveedor")));
            }
        }
        return lista;
    }

        /**
     * Inserta varios productos en una única transacción: o se guardan todos o ninguno.
     *
     * @param productos productos ya validados
     * @throws SQLException si falla alguna inserción (ya se ha hecho rollback)
     */
    public void insertarLote(List<Producto> productos) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);        // empieza la transacción
            try {
                for (Producto p : productos) {
                    insertar(con, p);
                }
                con.commit();                // todo bien: se confirman todas
            } catch (SQLException e) {
                con.rollback();              // algo falló: se deshacen todas
                throw e;                     // el menú informa del error
            } finally {
                con.setAutoCommit(true);     // se deja como estaba
            }
        }
    }
}
