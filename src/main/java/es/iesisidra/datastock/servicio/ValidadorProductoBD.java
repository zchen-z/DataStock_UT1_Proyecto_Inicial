package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.dao.ProveedorDAO;
import es.iesisidra.datastock.modelo.Producto;

import java.sql.SQLException;
import java.util.List;

/**
 * Comprueba si un producto se puede guardar en MySQL: reglas de los datos
 * y que existan su categoría y su proveedor (integridad referencial).
 */
public class ValidadorProductoBD {

    private final ValidadorProducto validador = new ValidadorProducto();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    /**
     * @param p producto con el código ya en mayúsculas
     * @return el motivo del rechazo, o null si se puede guardar
     * @throws SQLException si falla la consulta a MySQL
     */
    public String comprobar(Producto p) throws SQLException {
        List<String> errores = validador.validar(p);
        String problema = null;
        if (!errores.isEmpty()) {
            problema = String.join("; ", errores);
        }
        if (problema == null && categoriaDAO.buscarPorId(p.getCategoriaId()) == null) {
            problema = "no existe la categoría con ID " + p.getCategoriaId();
        }
        if (problema == null && proveedorDAO.buscarPorId(p.getProveedorId()) == null) {
            problema = "no existe el proveedor con ID " + p.getProveedorId();
        }
        return problema;
    }
}
