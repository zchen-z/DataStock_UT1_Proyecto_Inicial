package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.dao.ProveedorDAO;
import es.iesisidra.datastock.modelo.Categoria;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.modelo.Proveedor;
import es.iesisidra.datastock.servicio.ValidadorProducto;
import es.iesisidra.datastock.util.LectorConsola;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;

/**
 * Opción 7.4 · RF-JDBC-03: alta, consulta, modificación y baja de productos en MySQL.
 */
public class MenuProductos {

    private static final int ERROR_CLAVE_FORANEA = 1451;
    private final ProductoDAO dao = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final ValidadorProducto validador = new ValidadorProducto();

    /** Muestra el menú de productos hasta volver al menú MySQL. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE PRODUCTOS ----------");
            System.out.println("1. Dar de alta un producto");
            System.out.println("2. Consultar producto por código");
            System.out.println("3. Modificar un producto");
            System.out.println("4. Eliminar un producto");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> alta();
                    case 2 -> consultar();
                    case 3 -> modificar();
                    case 4 -> eliminar();
                    case 0 -> { }
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } catch (SQLException e) {
                error("MySQL: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    /** Pide los seis datos, los valida y comprueba código, categoría y proveedor antes de insertar. */
    private void alta() throws SQLException {
        System.out.println("\nALTA DE PRODUCTO");
        String codigo = LectorConsola.leerTexto("Código: ").trim().toUpperCase(); // tec-001 → TEC-001
        String nombre = LectorConsola.leerTexto("Nombre: ").trim();
        String textoPrecio = LectorConsola.leerTexto("Precio: ").trim();
        int stock = LectorConsola.leerEntero("Stock inicial: ");
        int categoria = LectorConsola.leerEntero("ID de categoría: ");
        int proveedor = LectorConsola.leerEntero("ID de proveedor: ");
        String problema = null;
        BigDecimal precio = null;
        try {
            precio = new BigDecimal(textoPrecio.replace(',', '.')); // admite 49,90 y 49.90
        } catch (NumberFormatException e) {
            problema = "el precio debe ser un número (por ejemplo 49.90)";
        }
        Producto p = new Producto(codigo, nombre, precio, stock, categoria, proveedor);
        if (problema == null) {
            problema = comprobar(p);
        }
        if (problema == null && dao.buscarPorCodigo(codigo) != null) {
            problema = "ya existe un producto con el código " + codigo;
        }
        if (problema != null) {
            error(problema);
        } else {
            dao.insertar(p);
            ok("Producto " + codigo + " guardado correctamente.");
        }
    }

    /** Busca un producto por código y muestra su ficha. */
    private void consultar() throws SQLException {
        String codigo = LectorConsola.leerTexto("Código: ").trim().toUpperCase();
        Producto p = dao.buscarPorCodigo(codigo);
        if (p == null) {
            error("No existe ningún producto con el código " + codigo);
        } else {
            mostrarFicha(p);
        }
    }

    /** Cambia nombre, precio, categoría y proveedor. Intro conserva cada valor; código y stock no se cambian. */
    private void modificar() throws SQLException {
        String codigo = LectorConsola.leerTexto("Código: ").trim().toUpperCase();
        Producto p = dao.buscarPorCodigo(codigo);
        if (p == null) {
            error("No existe ningún producto con el código " + codigo);
        } else {
            mostrarFicha(p);
            System.out.println("Pulsa Intro para conservar cada valor. El stock se cambia en Movimientos (7.6).");
            String nombre = LectorConsola.leerTexto("Nuevo nombre: ").trim();
            String precio = LectorConsola.leerTexto("Nuevo precio: ").trim();
            String categoria = LectorConsola.leerTexto("Nuevo ID de categoría: ").trim();
            String proveedor = LectorConsola.leerTexto("Nuevo ID de proveedor: ").trim();
            String problema = null;
            if (!nombre.isEmpty()) {
                p.setNombre(nombre);
            }
            try {
                if (!precio.isEmpty()) {
                    p.setPrecio(new BigDecimal(precio.replace(',', '.')));
                }
                if (!categoria.isEmpty()) {
                    p.setCategoriaId(Integer.parseInt(categoria));
                }
                if (!proveedor.isEmpty()) {
                    p.setProveedorId(Integer.parseInt(proveedor));
                }
            } catch (NumberFormatException e) {
                problema = "precio, categoría y proveedor deben ser números";
            }
            if (problema == null) {
                problema = comprobar(p); // se valida otra vez todo
            }
            if (problema != null) {
                error(problema);
            } else if (!LectorConsola.confirmar("¿Confirmas el cambio?")) {
                aviso("Modificación cancelada.");
            } else if (dao.modificar(p)) {
                ok("Producto " + codigo + " modificado correctamente.");
            } else {
                error("No se ha modificado ninguna fila.");
            }
        }
    }

    /** Muestra el producto, pide confirmación y lo borra si no tiene movimientos. */
    private void eliminar() throws SQLException {
        String codigo = LectorConsola.leerTexto("Código: ").trim().toUpperCase();
        Producto p = dao.buscarPorCodigo(codigo);
        if (p == null) {
            error("No existe ningún producto con el código " + codigo);
        } else {
            mostrarFicha(p);
            if (!LectorConsola.confirmar("¿Confirmas la eliminación?")) {
                aviso("Eliminación cancelada.");
            } else {
                try {
                    if (dao.eliminar(codigo)) {
                        ok("Producto " + codigo + " eliminado correctamente.");
                    } else {
                        error("No se ha eliminado ninguna fila.");
                    }
                } catch (SQLException e) {
                    if (e.getErrorCode() == ERROR_CLAVE_FORANEA) {
                        error("No se puede eliminar: el producto tiene movimientos de stock. Se conserva el historial.");
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Valida los datos (reglas de la UT1 + 2 decimales) y que existan la categoría y el proveedor.
     *
     * @return el motivo del rechazo, o null si el producto se puede guardar
     */
    private String comprobar(Producto p) throws SQLException {
        List<String> errores = validador.validar(p);
        String problema = errores.isEmpty() ? null : String.join("; ", errores);
        if (problema == null && categoriaDAO.buscarPorId(p.getCategoriaId()) == null) {
            problema = "no existe la categoría con ID " + p.getCategoriaId();
        }
        if (problema == null && proveedorDAO.buscarPorId(p.getProveedorId()) == null) {
            problema = "no existe el proveedor con ID " + p.getProveedorId();
        }
        return problema;
    }

    /** Imprime todos los datos del producto con los nombres de categoría y proveedor. */
    private void mostrarFicha(Producto p) throws SQLException {
        Categoria c = categoriaDAO.buscarPorId(p.getCategoriaId());
        Proveedor pr = proveedorDAO.buscarPorId(p.getProveedorId());
        System.out.println("Código: " + p.getCodigo());
        System.out.println("Nombre: " + p.getNombre());
        System.out.println("Precio: " + p.getPrecio() + " EUR");
        System.out.println("Stock: " + p.getStock());
        System.out.println("Categoría: " + p.getCategoriaId() + " - " + c.getNombre());
        System.out.println("Proveedor: " + pr.getId() + " - " + pr.getNombre());
    }
}
