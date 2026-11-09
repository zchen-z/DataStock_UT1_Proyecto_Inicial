package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.MovimientoStock;
import es.iesisidra.datastock.modelo.MovimientoStock.TipoMovimiento;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones JDBC sobre movimiento_stock y el procedimiento registrar_movimiento.
 */
public class MovimientoDAO {

    /**
     * Llama al procedimiento almacenado, que actualiza el stock y guarda el movimiento
     * en una sola transacción. Si falla (stock insuficiente...), MySQL deshace todo.
     *
     * @throws SQLException con el motivo que devuelve el procedimiento
     */
    public void registrar(String codigo, TipoMovimiento tipo, int cantidad) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             CallableStatement cs = con.prepareCall("{CALL registrar_movimiento(?, ?, ?)}")) {
            cs.setString(1, codigo);
            cs.setString(2, tipo.name()); // "ENTRADA" o "SALIDA"
            cs.setInt(3, cantidad);
            cs.execute();
        }
    }

    /**
     * @return el último movimiento de un producto, o null si no tiene
     * @throws SQLException si falla la consulta
     */
    public MovimientoStock ultimoDe(String codigo) throws SQLException {
        MovimientoStock m = null;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM movimiento_stock WHERE producto_codigo = ? ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m = crearMovimiento(rs);
                }
            }
        }
        return m;
    }

        /**
     * @return todos los movimientos, ordenados por fecha e ID
     * @throws SQLException si falla la consulta
     */
    public List<MovimientoStock> listarTodos() throws SQLException {
        List<MovimientoStock> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM movimiento_stock ORDER BY fecha, id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(crearMovimiento(rs));
            }
        }
        return lista;
    }

    /**
     * @param codigo código del producto (ya normalizado)
     * @return sus movimientos ordenados por fecha e ID; vacía si no tiene
     * @throws SQLException si falla la consulta
     */
    public List<MovimientoStock> listarDeProducto(String codigo) throws SQLException {
        List<MovimientoStock> lista = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM movimiento_stock WHERE producto_codigo = ? ORDER BY fecha, id")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(crearMovimiento(rs));
                }
            }
        }
        return lista;
    }

    /** Convierte la fila actual del ResultSet en un MovimientoStock. */
    private MovimientoStock crearMovimiento(ResultSet rs) throws SQLException {
        return new MovimientoStock(rs.getLong("id"), rs.getString("producto_codigo"),
                TipoMovimiento.valueOf(rs.getString("tipo")), rs.getInt("cantidad"),
                rs.getObject("fecha", LocalDateTime.class), rs.getInt("stock_anterior"),
                rs.getInt("stock_posterior"));
    }
}
