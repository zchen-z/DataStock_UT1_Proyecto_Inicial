package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.servicio.ValidadorProductoBD;
import es.iesisidra.datastock.util.LectorConsola;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;

/**
 * Opción 7.8 · RF-JDBC-07: copia a MySQL los productos válidos del inventario en memoria.
 * No modifica la memoria ni sincroniza automáticamente.
 */
public class MenuImportacionBD {

    private final Inventario inventario;
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ValidadorProductoBD validadorBD = new ValidadorProductoBD();

    /** @param inventario inventario en memoria de la UT1 */
    public MenuImportacionBD(Inventario inventario) {
        this.inventario = inventario;
    }

    /** Comprueba cada producto, inserta los aceptados en una transacción y muestra el resumen. */
    public void iniciar() {
        System.out.println("\nIMPORTAR PRODUCTOS DE MEMORIA A MYSQL");
        List<Producto> enMemoria = inventario.listarOrdenadosPorCodigo();
        System.out.println("Productos disponibles en memoria: " + enMemoria.size());
        if (enMemoria.isEmpty()) {
            aviso("No hay productos en memoria. Importa antes un JSON o XML (opciones 2 y 3).");
        } else if (!LectorConsola.confirmar("¿Deseas importar estos productos a MySQL?")) {
            aviso("Importación cancelada.");
        } else {
            List<Producto> aceptados = new ArrayList<>();
            int omitidos = 0;
            int rechazados = 0;
            int insertados = 0;
            try {
                for (Producto original : enMemoria) {
                    Producto p = copiaNormalizada(original);
                    System.out.println("\nComprobando " + p.getCodigo() + " - " + p.getNombre() + "...");
                    if (productoDAO.buscarPorCodigo(p.getCodigo()) != null) {
                        aviso("El producto ya existe en MySQL. Se omite.");
                        omitidos++;
                    } else {
                        String problema = validadorBD.comprobar(p);
                        if (problema != null) {
                            error(problema + ". El producto no se puede importar.");
                            rechazados++;
                        } else {
                            ok("Producto válido para importar.");
                            aceptados.add(p);
                        }
                    }
                }
                if (!aceptados.isEmpty()
                        && LectorConsola.confirmar("\nSe van a insertar " + aceptados.size() + " producto(s) en una transacción. ¿Continuar?")) {
                    productoDAO.insertarLote(aceptados);
                    insertados = aceptados.size();
                }
            } catch (SQLException e) {
                error("Error SQL durante la importación: " + e.getMessage());
                error("Se ha hecho rollback: no se ha confirmado ninguna inserción del lote.");
            }
            System.out.println("\n----- RESULTADO DE LA IMPORTACIÓN -----");
            System.out.println("Productos analizados:            " + enMemoria.size());
            System.out.println("Productos insertados:            " + insertados);
            System.out.println("Omitidos por código duplicado:   " + omitidos);
            System.out.println("Rechazados por datos incorrectos: " + rechazados);
            ok("Importación finalizada.");
        }
        LectorConsola.pausar();
    }

    /**
     * Copia del producto con el código en mayúsculas. Se usa una copia
     * para no modificar el inventario en memoria.
     */
    private Producto copiaNormalizada(Producto o) {
        return new Producto(o.getCodigo().trim().toUpperCase(), o.getNombre().trim(), o.getPrecio(),
                o.getStock(), o.getCategoriaId(), o.getProveedorId());
    }
}
