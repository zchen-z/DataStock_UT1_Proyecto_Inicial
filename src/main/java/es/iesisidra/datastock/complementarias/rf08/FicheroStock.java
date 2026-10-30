package es.iesisidra.datastock.complementarias.rf08;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import es.iesisidra.datastock.modelo.Producto;

/**
 * RF-FIC-08 · Lógica del fichero binario de stock con RandomAccessFile.
 * <p>Cada registro ocupa 36 bytes: código (12 char = 24 bytes) + precio
 * (double = 8 bytes) + stock (int = 4 bytes). Esta clase no lee del teclado
 * ni imprime nada: eso lo hace {@link MenuAccesoAleatorio}.</p>
 * <p>El fichero es una copia independiente: modificarlo no cambia los
 * productos que hay en memoria.</p>
 */
public class FicheroStock {

    /** Caracteres reservados para el código (cada char ocupa 2 bytes). */
    public static final int CARACTERES_CODIGO = 12;
    /** Tamaño de un registro: 24 + 8 + 4 = 36 bytes. */
    public static final int TAM_REGISTRO = CARACTERES_CODIGO * 2 + 8 + 4;
    /** Posición del stock dentro del registro: 24 + 8 = 32 bytes. */
    public static final int DESPLAZAMIENTO_STOCK = CARACTERES_CODIGO * 2 + 8;

    /**
     * Genera el fichero con un registro por producto, en el orden recibido.
     * Si el fichero ya existía, se vacía antes de escribir.
     *
     * @param productos productos a escribir (ya ordenados por código)
     * @param fichero   ruta del fichero binario
     * @return tamaño final del fichero en bytes
     * @throws IOException si no se puede escribir
     */
    public long generar(List<Producto> productos, Path fichero) throws IOException {
        long tamano;
        try (RandomAccessFile raf = new RandomAccessFile(fichero.toFile(), "rw")) {
            raf.setLength(0); // trunca el fichero antes de escribir
            for (Producto p : productos) {
                raf.writeChars(codigoFijo(p.getCodigo()));      // 12 char = 24 bytes
                raf.writeDouble(p.getPrecio().doubleValue());  // 8 bytes
                raf.writeInt(p.getStock());                    // 4 bytes
            }
            tamano = raf.length();
        }
        return tamano;
    }

    /**
     * Comprueba el fichero y devuelve cuántos registros tiene.
     *
     * @param fichero ruta del fichero binario
     * @return número de registros
     * @throws IOException si no existe, está vacío o su tamaño no es múltiplo de 36
     */
    public long contarRegistros(Path fichero) throws IOException {
        if (!Files.isRegularFile(fichero)) {
            throw new IOException("el fichero no existe; genéralo primero con la opción 1");
        }
        long tamano = Files.size(fichero);
        if (tamano == 0 || tamano % TAM_REGISTRO != 0) {
            throw new IOException("fichero inválido: su tamaño (" + tamano
                    + " bytes) no es múltiplo de " + TAM_REGISTRO);
        }
        return tamano / TAM_REGISTRO;
    }

    /**
     * Lee todos los registros de forma secuencial, desde el principio.
     *
     * @param fichero ruta del fichero binario
     * @return lista con un texto por registro
     * @throws IOException si el fichero no es válido o no se puede leer
     */
    public List<String> leerTodos(Path fichero) throws IOException {
        long registros = contarRegistros(fichero);
        List<String> lista = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(fichero.toFile(), "r")) {
            for (int i = 0; i < registros; i++) {
                lista.add(leerRegistro(raf));
            }
        }
        return lista;
    }

    /**
     * Lee un solo registro saltando directamente a él con seek(posición × 36).
     *
     * @param fichero  ruta del fichero binario
     * @param posicion posición lógica, empezando en 0
     * @return el registro leído, como texto
     * @throws IOException si el fichero no es válido, no se puede leer o la posición está fuera de rango
     */
    public String leer(Path fichero, int posicion) throws IOException {
        comprobarPosicion(fichero, posicion);
        String registro;
        try (RandomAccessFile raf = new RandomAccessFile(fichero.toFile(), "r")) {
            raf.seek((long) posicion * TAM_REGISTRO);
            registro = leerRegistro(raf);
        }
        return registro;
    }

    /**
     * Cambia solo el stock de un registro con seek(posición × 36 + 32),
     * sin reescribir el código ni el precio.
     *
     * @param fichero    ruta del fichero binario
     * @param posicion   posición lógica, empezando en 0
     * @param nuevoStock nuevo valor (mayor o igual que 0)
     * @return el stock que había antes
     * @throws IOException si el fichero no es válido, no se puede escribir, la posición
     *                     está fuera de rango o el stock es negativo
     */
    public int modificarStock(Path fichero, int posicion, int nuevoStock) throws IOException {
        comprobarPosicion(fichero, posicion);
        if (nuevoStock < 0) {
            throw new IOException("el stock no puede ser negativo");
        }
        int anterior;
        try (RandomAccessFile raf = new RandomAccessFile(fichero.toFile(), "rw")) {
            long posStock = (long) posicion * TAM_REGISTRO + DESPLAZAMIENTO_STOCK;
            raf.seek(posStock);
            anterior = raf.readInt();
            raf.seek(posStock); // readInt ha movido el puntero 4 bytes: se vuelve atrás
            raf.writeInt(nuevoStock);
        }
        return anterior;
    }

    /** Lanza IOException si la posición no existe en el fichero. */
    private void comprobarPosicion(Path fichero, int posicion) throws IOException {
        long registros = contarRegistros(fichero);
        if (posicion < 0 || posicion >= registros) {
            throw new IOException("posición fuera de rango (válidas: 0-" + (registros - 1) + ")");
        }
    }

    /**
     * Lee un registro completo desde la posición actual del puntero.
     *
     * @return el registro como texto, por ejemplo "MON-003 | 149.99 EUR | stock 8"
     */
    private String leerRegistro(RandomAccessFile raf) throws IOException {
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < CARACTERES_CODIGO; i++) {
            codigo.append(raf.readChar());
        }
        double precio = raf.readDouble();
        int stock = raf.readInt();
        return codigo.toString().trim() + " | " + precio + " EUR | stock " + stock;
    }

    /** Ajusta el código a exactamente 12 caracteres, rellenando con espacios. */
    private String codigoFijo(String codigo) {
        String recortado = codigo;
        if (codigo.length() > CARACTERES_CODIGO) {
            recortado = codigo.substring(0, CARACTERES_CODIGO);
        }
        return String.format("%-" + CARACTERES_CODIGO + "s", recortado);
    }
}
