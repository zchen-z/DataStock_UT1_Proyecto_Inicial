package es.iesisidra.datastock.ficheros;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.ResultadoImportacion;
import es.iesisidra.datastock.servicio.ValidadorProducto;

/**
 * UT3 · Componente reutilizable que lee productos de un fichero JSON con Gson.
 * <p>Recibe la ruta del fichero y devuelve un {@link ResultadoImportacion} con los
 * productos válidos y el motivo de cada rechazo. No imprime nada, no lee del
 * teclado, no modifica el inventario y no accede a MySQL: quien lo usa decide
 * qué hacer con los productos.</p>
 * <p>Formato esperado: un objeto raíz con el array {@code "productos"}. Las reglas
 * de validación son las de la UT1 ({@link ValidadorProducto}).</p>
 */
public class LectorProductosJson {

    private final ValidadorProducto validador = new ValidadorProducto();

    /**
     * Lee el fichero JSON y valida cada producto.
     *
     * @param textoRuta ruta del fichero JSON
     * @return resultado con los productos válidos ({@code getValidos()}) y las incidencias.
     *         Si el fichero no existe, no se puede leer o el JSON está mal formado,
     *         {@code documentoValido()} es false y {@code getErrorDocumento()} indica el motivo.
     *         Este método no lanza excepciones: todos los errores se devuelven en el resultado.
     */
    public ResultadoImportacion leer(String textoRuta) {
        ResultadoImportacion resultado = new ResultadoImportacion();
        if (textoRuta == null || textoRuta.isBlank()) {
            resultado.rechazarDocumento("la ruta no puede estar vacía");
        } else {
            try {
                Path ruta = Path.of(textoRuta.trim());
                if (!Files.exists(ruta)) {
                    resultado.rechazarDocumento("el fichero no existe: " + ruta.toAbsolutePath());
                } else if (!Files.isRegularFile(ruta)) {
                    resultado.rechazarDocumento("la ruta no es un fichero");
                } else if (!Files.isReadable(ruta)) {
                    resultado.rechazarDocumento("no tienes permisos para leer el fichero");
                } else {
                    String texto = Files.readString(ruta, StandardCharsets.UTF_8);
                    JsonArray lista = obtenerArrayProductos(texto, resultado);
                    if (lista != null) {
                        procesarProductos(lista, resultado);
                    }
                }
            } catch (InvalidPathException e) {
                resultado.rechazarDocumento("la ruta no es válida");
            } catch (IOException e) {
                resultado.rechazarDocumento("no se pudo leer el fichero: " + e.getMessage());
            }
        }
        return resultado;
    }

    /**
     * Analiza el texto JSON y comprueba su estructura general:
     * raíz de tipo objeto con un array "productos".
     *
     * @return el array de productos, o null si el documento no es válido
     */
    private JsonArray obtenerArrayProductos(String texto, ResultadoImportacion resultado) {
        JsonArray lista = null;
        try {
            JsonElement raiz = JsonParser.parseString(texto);
            if (!raiz.isJsonObject()) {
                resultado.rechazarDocumento("la raíz del JSON debe ser un objeto");
            } else if (!raiz.getAsJsonObject().has("productos") || !raiz.getAsJsonObject().get("productos").isJsonArray()) {
                resultado.rechazarDocumento("falta el array \"productos\"");
            } else {
                lista = raiz.getAsJsonObject().getAsJsonArray("productos");
            }
        } catch (JsonParseException e) {
            resultado.rechazarDocumento("el JSON está mal formado");
        }
        return lista;
    }

    /**
     * Recorre el array, valida cada producto y guarda los válidos en el resultado.
     * Los códigos repetidos dentro del mismo fichero se rechazan.
     */
    private void procesarProductos(JsonArray lista, ResultadoImportacion resultado) {
        resultado.setEncontrados(lista.size());
        Set<String> codigosDelFichero = new HashSet<>(); // códigos ya aceptados de este fichero

        for (int i = 0; i < lista.size(); i++) {
            String referencia = "Producto " + (i + 1);
            List<String> errores = new ArrayList<>();
            JsonElement elemento = lista.get(i);

            if (!elemento.isJsonObject()) {
                errores.add("el registro no es un objeto JSON");
            } else {
                Producto producto = construirProducto(elemento.getAsJsonObject(), errores);
                if (producto.getCodigo() != null && !producto.getCodigo().isBlank()) {
                    referencia = producto.getCodigo();
                }
                if (errores.isEmpty()) {
                    errores.addAll(validador.validar(producto));
                }
                if (errores.isEmpty()) {
                    String clave = producto.getCodigo().toUpperCase();
                    if (codigosDelFichero.contains(clave)) {
                        errores.add("código duplicado en el mismo fichero");
                    } else {
                        codigosDelFichero.add(clave);
                        resultado.anotarValido(producto); // no se añade al inventario aquí
                    }
                }
            }
            if (!errores.isEmpty()) {
                resultado.anotarRechazo(referencia, String.join("; ", errores));
            }
        }
    }

    /**
     * Lee los seis campos de un objeto JSON y construye un Producto.
     * Los campos que faltan o tienen un tipo incorrecto se anotan en errores.
     */
    private Producto construirProducto(JsonObject obj, List<String> errores) {
        Producto p = new Producto();
        String codigo = leerTexto(obj, "codigo", errores);
        String nombre = leerTexto(obj, "nombre", errores);
        if (codigo != null) {
            codigo = codigo.trim(); // se guardan sin espacios extremos
        }
        if (nombre != null) {
            nombre = nombre.trim();
        }
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecio(leerDecimal(obj, "precio", errores));
        p.setStock(leerEntero(obj, "stock", errores));
        p.setCategoriaId(leerEntero(obj, "categoriaId", errores));
        p.setProveedorId(leerEntero(obj, "proveedorId", errores));
        return p;
    }

    /** Lee un campo de texto; guarda un error si falta o no es un valor simple. */
    private String leerTexto(JsonObject obj, String campo, List<String> errores) {
        String valor = null;
        if (!obj.has(campo) || obj.get(campo).isJsonNull()) {
            errores.add("falta el campo " + campo);
        } else if (!obj.get(campo).isJsonPrimitive()) {
            errores.add("el campo " + campo + " no es un texto");
        } else {
            valor = obj.get(campo).getAsString();
        }
        return valor;
    }

    /** Lee un campo decimal (precio) y guarda un error si falta o no es un número. */
    private BigDecimal leerDecimal(JsonObject obj, String campo, List<String> errores) {
        BigDecimal valor = null;
        if (!obj.has(campo) || obj.get(campo).isJsonNull()) {
            errores.add("falta el campo " + campo);
        } else {
            try {
                valor = obj.get(campo).getAsBigDecimal();
            } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException e) {
                errores.add("el campo " + campo + " no es un número válido");
            }
        }
        return valor;
    }

    /** Lee un campo entero y guarda un error si falta, no es un número o tiene decimales. */
    private int leerEntero(JsonObject obj, String campo, List<String> errores) {
        int valor = 0;
        BigDecimal numero = leerDecimal(obj, campo, errores);
        if (numero != null) {
            try {
                valor = numero.intValueExact(); // falla si tiene decimales (12.5)
            } catch (ArithmeticException e) {
                errores.add("el campo " + campo + " debe ser un número entero");
            }
        }
        return valor;
    }
}
