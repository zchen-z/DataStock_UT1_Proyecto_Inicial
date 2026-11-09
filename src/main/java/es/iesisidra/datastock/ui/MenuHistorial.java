package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.MovimientoDAO;
import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.modelo.MovimientoStock;
import es.iesisidra.datastock.util.LectorConsola;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;

/**
 * Opción 7.7 · RF-JDBC-06: consulta del historial de movimientos (solo lectura).
 */
public class MenuHistorial {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    /** Muestra el menú de historial hasta volver al menú MySQL. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE HISTORIAL ----------");
            System.out.println("1. Consultar todos los movimientos");
            System.out.println("2. Consultar movimientos de un producto");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            try {
                switch (opcion) {
                    case 1 -> todos();
                    case 2 -> deProducto();
                    case 0 -> { }
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } catch (SQLException e) {
                error("MySQL: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    /** Historial completo. */
    private void todos() throws SQLException {
        List<MovimientoStock> lista = movimientoDAO.listarTodos();
        if (lista.isEmpty()) {
            aviso("No hay movimientos registrados.");
        } else {
            mostrarTabla(lista);
        }
    }

    /** Historial de un producto: distingue «no existe» de «existe pero sin movimientos». */
    private void deProducto() throws SQLException {
        String codigo = LectorConsola.leerTexto("Código del producto: ").trim().toUpperCase();
        if (productoDAO.buscarPorCodigo(codigo) == null) {
            error("No existe ningún producto con el código " + codigo);
        } else {
            List<MovimientoStock> lista = movimientoDAO.listarDeProducto(codigo);
            if (lista.isEmpty()) {
                aviso("Sin movimientos registrados para " + codigo + ".");
            } else {
                mostrarTabla(lista);
            }
        }
    }

    /** Imprime los movimientos en columnas con encabezados. */
    private void mostrarTabla(List<MovimientoStock> lista) {
        System.out.printf("%-5s %-19s %-12s %-8s %8s %8s %9s%n",
                "ID", "FECHA", "PRODUCTO", "TIPO", "CANTIDAD", "ANTERIOR", "POSTERIOR");
        for (MovimientoStock m : lista) {
            System.out.printf("%-5d %-19s %-12s %-8s %8d %8d %9d%n", m.getId(), m.getFecha().format(FORMATO_FECHA),
                    m.getProductoCodigo(), m.getTipo(), m.getCantidad(), m.getStockAnterior(), m.getStockPosterior());
        }
        System.out.println("Total: " + lista.size() + " movimiento(s)");
    }
}
