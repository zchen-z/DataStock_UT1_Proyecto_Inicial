package es.iesisidra.datastock.ficheros;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.ValidadorProducto;

/**
 * RF-FIC-06 · Guardar y recuperar el inventario mediante serialización Java.
 * Escribe la colección completa en un fichero binario .dat y la lee después.
 */
public class SerializadorInventario {

    /**
     * Solo se permiten las clases que DataStock guarda en el .dat (la lista, sus arrays internos,
     * Producto y BigDecimal con su BigInteger).
     * Cualquier otra clase se rechaza: así no se deserializan ficheros de procedencia desconocida.
     */
    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "maxdepth=10;java.util.ArrayList;java.lang.Object;es.iesisidra.datastock.modelo.Producto;"
                    + "java.math.BigDecimal;java.math.BigInteger;java.lang.Number;!*");

    private final ValidadorProducto validador = new ValidadorProducto();

    /**
     * Serializa la colección completa de productos en un fichero .dat.
     *
     * @param productos productos a guardar
     * @param ruta      fichero de destino
     * @throws IOException si no se puede escribir el fichero
     */
    public void guardar(List<Producto> productos, Path ruta) throws IOException {
        try (OutputStream salida = Files.newOutputStream(ruta);
             ObjectOutputStream oos = new ObjectOutputStream(salida)) {
            oos.writeObject(new ArrayList<>(productos)); // se guarda una copia como ArrayList
        }
    }

    /**
     * Lee un fichero .dat generado por DataStock y devuelve sus productos.
     * No modifica el inventario: quien llama decide si sustituye la colección.
     *
     * @param ruta fichero .dat
     * @return lista de productos recuperados
     * @throws IOException si el fichero no se puede leer, está dañado o no es de DataStock
     */
    public List<Producto> recuperar(Path ruta) throws IOException {
        Object leido;
        try (InputStream entrada = Files.newInputStream(ruta);
             ObjectInputStream ois = new ObjectInputStream(entrada)) {
            ois.setObjectInputFilter(FILTRO);
            leido = ois.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("el fichero no es un inventario de DataStock");
        }
        return comprobarContenido(leido);
    }

    /**
     * Comprueba que el objeto leído es una lista de productos válidos.
     *
     * @param leido objeto deserializado
     * @return la lista de productos
     * @throws IOException si el contenido no es el esperado
     */
    private List<Producto> comprobarContenido(Object leido) throws IOException {
        if (!(leido instanceof List<?> lista)) {
            throw new IOException("el fichero no contiene una lista de productos");
        }
        List<Producto> productos = new ArrayList<>();
        for (Object elemento : lista) {
            if (!(elemento instanceof Producto p)) {
                throw new IOException("el fichero contiene objetos que no son productos");
            }
            if (!validador.validar(p).isEmpty()) {
                throw new IOException("el fichero contiene productos con datos no válidos (" + p.getCodigo() + ")");
            }
            productos.add(p);
        }
        return productos;
    }
}
