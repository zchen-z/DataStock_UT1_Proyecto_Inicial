package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.modelo.Producto;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado común del inventario durante la ejecución de UT1.
 * <p>
 * Esta clase solo proporciona la colección compartida. No implementa
 * importación, validación, búsqueda, exportación ni persistencia: esos son
 * ejercicios que deberá desarrollar el alumnado.
 * </p>
 */
public class Inventario {
    private final List<Producto> productos = new ArrayList<>();

    /**
     * Obtiene la colección que comparten las opciones del menú.
     * <p>
     * ATENCIÓN: la lista es modificable. El alumnado deberá aplicar las
     * reglas del enunciado antes de añadir o sustituir elementos.
     * </p>
     */
    public List<Producto> getProductos() {
        return productos;
    }

    /**
     * Busca un producto por su código, sin distinguir mayúsculas
     * y eliminando espacios al principio y al final.
     * @param codigo código a buscar
     * @return el producto encontrado, o null si no existe
     */
    public Producto buscarPorCodigo(String codigo) {
        Producto encontrado = null;
        if (codigo != null) {
            String buscado = codigo.trim();
            for (Producto p : productos) {
                if (encontrado == null && p.getCodigo().equalsIgnoreCase(buscado)) {
                    encontrado = p;
                }
            }
        }
        return encontrado;
    }

    /**
     * Indica si ya hay un producto con ese código en el inventario.
     * @param codigo código a comprobar
     * @return true si el código ya existe
     */
    public boolean existeCodigo(String codigo) {
        return buscarPorCodigo(codigo) != null;
    }

    /**
     * Añade un producto solo si su código no existe todavía.
     * @param producto producto ya validado
     * @return true si se ha añadido, false si era un duplicado
     */
    public boolean agregar(Producto producto) {
        boolean anadido = false;
        if (!existeCodigo(producto.getCodigo())) {
            productos.add(producto);
            anadido = true;
        }
        return anadido;
    }

    /**
     * Devuelve una copia de los productos ordenada por código.
     * La colección original no se modifica.
     * @return lista nueva ordenada por código (sin distinguir mayúsculas)
     */
    public List<Producto> listarOrdenadosPorCodigo() {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort((a, b) -> a.getCodigo().compareToIgnoreCase(b.getCodigo()));
        return copia;
    }

    /**
     * Busca los productos cuyo nombre contiene el texto indicado,
     * sin distinguir mayúsculas. El resultado se devuelve ordenado por código.
     * @param texto fragmento del nombre a buscar
     * @return lista de coincidencias; vacía si no hay ninguna o el texto está vacío
     */
    public List<Producto> buscarPorNombre(String texto) {
        List<Producto> coincidencias = new ArrayList<>();
        if (texto != null && !texto.isBlank()) {
            String buscado = texto.trim().toLowerCase();
            for (Producto p : listarOrdenadosPorCodigo()) {
                if (p.getNombre().toLowerCase().contains(buscado)) {
                    coincidencias.add(p);
                }
            }
        }
        return coincidencias;
    }
}
