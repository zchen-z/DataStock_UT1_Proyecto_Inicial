package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.modelo.ProductoDetalle;
import es.iesisidra.datastock.util.LectorConsola;

import java.sql.SQLException;
import java.util.List;

import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;

/**
 * Opción 7.5 · RF-JDBC-04: consultas de productos en MySQL (solo lectura).
 */
public class MenuConsultasBD {

    private final ProductoDAO dao = new ProductoDAO();

    /** Muestra el menú de consultas hasta volver al menú MySQL. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE CONSULTAS ----------");
            System.out.println("1. Listar todos los productos");
            System.out.println("2. Buscar producto por código");
            System.out.println("3. Buscar productos por nombre");
            System.out.println("4. Consultar productos con stock inferior a un umbral");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> listar();
                    case 2 -> porCodigo();
                    case 3 -> porNombre();
                    case 4 -> stockInferior();
                    case 0 -> { }
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } catch (SQLException e) {
                error("MySQL: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    /** Listado completo con JOIN, ordenado por código. */
    private void listar() throws SQLException {
        List<ProductoDetalle> lista = dao.listarDetalle();
        if (lista.isEmpty()) {
            aviso("No hay productos almacenados en MySQL.");
        } else {
            mostrarTabla(lista);
        }
    }

    /** Búsqueda exacta por código (normalizado a mayúsculas). */
    private void porCodigo() throws SQLException {
        String codigo = LectorConsola.leerTexto("Código: ").trim().toUpperCase();
        ProductoDetalle d = dao.buscarDetalle(codigo);
        if (d == null) {
            aviso("No se ha encontrado ningún producto con el código " + codigo);
        } else {
            Producto p = d.getProducto();
            System.out.println("Código: " + p.getCodigo());
            System.out.println("Nombre: " + p.getNombre());
            System.out.println("Precio: " + p.getPrecio() + " EUR");
            System.out.println("Stock: " + p.getStock());
            System.out.println("Categoría: " + p.getCategoriaId() + " - " + d.getCategoria());
            System.out.println("Proveedor: " + p.getProveedorId() + " - " + d.getProveedor());
        }
    }

    /** Búsqueda parcial por nombre con LIKE. */
    private void porNombre() throws SQLException {
        String texto = LectorConsola.leerTexto("Texto a buscar: ").trim();
        if (texto.isEmpty()) {
            error("El texto de búsqueda no puede estar vacío.");
        } else {
            List<ProductoDetalle> lista = dao.buscarPorNombre(texto);
            if (lista.isEmpty()) {
                aviso("No hay productos cuyo nombre contenga \"" + texto + "\".");
            } else {
                mostrarTabla(lista);
            }
        }
    }

    /** Productos con stock estrictamente menor que el umbral. */
    private void stockInferior() throws SQLException {
        int umbral = LectorConsola.leerEntero("Umbral de stock: ");
        if (umbral < 0) {
            error("El umbral debe ser mayor o igual que 0.");
        } else {
            List<ProductoDetalle> lista = dao.stockInferior(umbral);
            if (lista.isEmpty()) {
                aviso("No hay productos con stock inferior a " + umbral + ".");
            } else {
                mostrarTabla(lista);
            }
        }
    }

    /** Imprime los productos en columnas con encabezados. */
    private void mostrarTabla(List<ProductoDetalle> lista) {
        System.out.printf("%-12s %-25s %10s %6s  %-15s %s%n", "CÓDIGO", "NOMBRE", "PRECIO", "STOCK", "CATEGORÍA", "PROVEEDOR");
        for (ProductoDetalle d : lista) {
            Producto p = d.getProducto();
            System.out.printf("%-12s %-25s %6s EUR %6d  %-15s %s%n", p.getCodigo(), p.getNombre(), p.getPrecio(),
                    p.getStock(), d.getCategoria(), d.getProveedor());
        }
        System.out.println("Total: " + lista.size() + " producto(s)");
    }
}
