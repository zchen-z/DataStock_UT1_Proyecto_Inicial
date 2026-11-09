package es.iesisidra.datastock.modelo;

/**
 * DTO para las consultas: un producto con los nombres de su categoría y su proveedor (JOIN).
 */
public class ProductoDetalle {

    private final Producto producto;
    private final String categoria;
    private final String proveedor;

    /** Crea el detalle a partir de una fila del ResultSet. */
    public ProductoDetalle(Producto producto, String categoria, String proveedor) {
        this.producto = producto;
        this.categoria = categoria;
        this.proveedor = proveedor;
    }

    /** @return el producto */
    public Producto getProducto() { return producto; }
    /** @return nombre de la categoría */
    public String getCategoria() { return categoria; }
    /** @return nombre del proveedor */
    public String getProveedor() { return proveedor; }
}
