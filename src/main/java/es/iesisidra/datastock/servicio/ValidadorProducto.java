package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.modelo.Producto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas comunes de validación de productos (enunciado, apartado 6).
 * La usan tanto la importación JSON como la XML, para que ambas
 * apliquen exactamente las mismas comprobaciones.
 */
public class ValidadorProducto {

    /** Patron de letras, numeros y guiones (no distigue mayusculas). */
    private static final String PATRON_CODIGO = "[A-Za-z0-9-]+"; // permiten letras, numeros o guiones; + = uno o mas
    /** Mayor precio que admite DECIMAL(10,2) en MySQL. */
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    /**
     * Comprueba todos los campos de un producto.
     * @param p producto a validar
     * @return devuelve la lista con los motivos de rechazo y vacía si el producto es válido
     */
    public List<String> validar(Producto p) {
        List<String> errores = new ArrayList<>();
        validarCodigo(p.getCodigo(), errores);
        validarNombre(p.getNombre(), errores);
        validarPrecio(p.getPrecio(), errores);

        if (p.getStock() < 0) {
            errores.add("el stock no puede ser negativo");
        }
        if (p.getCategoriaId() <= 0) {
            errores.add("la categoría debe ser un entero positivo");
        }
        if (p.getProveedorId() <= 0) {
            errores.add("el proveedor debe ser un entero positivo");
        }
        return errores;
    }

    /**
     * Patron de codigo: máximo 12 caracteres, solo letras, números y guiones.
     */
    private void validarCodigo(String codigo, List<String> errores) {
        if (codigo == null || codigo.isBlank()) {
            errores.add("el código es obligatorio");
        } else if (codigo.trim().length() > 12) {
            errores.add("el código no puede superar 12 caracteres");
        } else if (!codigo.trim().matches(PATRON_CODIGO)) {
            errores.add("el código solo puede contener letras, números y guiones");
        }
    }

    /**
     * Nombre: obligatorio y entre 3 y 100 caracteres.
     */
    private void validarNombre(String nombre, List<String> errores) {
        if (nombre == null || nombre.isBlank()) {
            errores.add("el nombre es obligatorio");
        } else if (nombre.trim().length() < 3 || nombre.trim().length() > 100) {
            errores.add("el nombre debe tener entre 3 y 100 caracteres");
        }
    }

    /**
     * Precio: obligatorio y mayor que 0.
     */
        private void validarPrecio(BigDecimal precio, List<String> errores) {
        if (precio == null) {
            errores.add("el precio es obligatorio");
        } else if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("el precio debe ser mayor que 0");
        } else if (precio.stripTrailingZeros().scale() > 2) { // 12.345 tiene 3 decimales
            errores.add("el precio no puede tener más de dos decimales");
        } else if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            errores.add("el precio supera el máximo permitido (99999999.99)");
        }
    }
}
