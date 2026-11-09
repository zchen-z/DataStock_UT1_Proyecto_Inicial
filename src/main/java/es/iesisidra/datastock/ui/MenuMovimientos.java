package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.MovimientoDAO;
import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.modelo.MovimientoStock;
import es.iesisidra.datastock.modelo.MovimientoStock.TipoMovimiento;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.util.LectorConsola;

import java.sql.SQLException;

import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;

/**
 * Opción 7.6 · RF-JDBC-05: entradas y salidas de stock con el procedimiento almacenado.
 */
public class MenuMovimientos {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();

    /** Muestra el menú de movimientos hasta volver al menú MySQL. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE MOVIMIENTOS ----------");
            System.out.println("1. Registrar una ENTRADA de stock");
            System.out.println("2. Registrar una SALIDA de stock");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            try {
                switch (opcion) {
                    case 1 -> registrar(TipoMovimiento.ENTRADA);
                    case 2 -> registrar(TipoMovimiento.SALIDA);
                    case 0 -> { }
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } catch (SQLException e) {
                error("MySQL: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    /** Pide producto y cantidad, confirma y llama al procedimiento. */
    private void registrar(TipoMovimiento tipo) throws SQLException {
        String codigo = LectorConsola.leerTexto("Código del producto: ").trim().toUpperCase();
        Producto p = productoDAO.buscarPorCodigo(codigo);
        if (p == null) {
            error("No existe ningún producto con el código " + codigo);
        } else {
            System.out.println("Producto: " + p.getNombre() + " | Stock actual: " + p.getStock());
            int cantidad = LectorConsola.leerEntero("Cantidad: ");
            if (cantidad <= 0) {
                error("La cantidad debe ser un entero mayor que 0.");
            } else if (!LectorConsola.confirmar("¿Registrar " + tipo + " de " + cantidad + " unidades?")) {
                aviso("Operación cancelada: no se ha modificado nada.");
            } else {
                try {
                    movimientoDAO.registrar(codigo, tipo, cantidad);
                    MovimientoStock m = movimientoDAO.ultimoDe(codigo);
                    ok("Movimiento registrado: " + m.getTipo() + " de " + m.getCantidad() + " unidades.");
                    System.out.println("Stock anterior: " + m.getStockAnterior() + " -> posterior: " + m.getStockPosterior());
                } catch (SQLException e) {
                    error("No se ha registrado el movimiento: " + e.getMessage() + ". El stock no ha cambiado.");
                }
            }
        }
    }
}
