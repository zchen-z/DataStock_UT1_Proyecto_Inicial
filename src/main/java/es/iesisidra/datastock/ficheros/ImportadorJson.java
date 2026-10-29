package es.iesisidra.datastock.ficheros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.servicio.ResultadoImportacion;
import es.iesisidra.datastock.servicio.ValidadorProducto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import es.iesisidra.datastock.modelo.Producto;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.gson.JsonObject;
import java.math.BigDecimal;

/**
 * RF-FIC-02 · Importar un catálogo JSON con Gson.
 * Lee el fichero, valida cada producto y añade al inventario solo los válidos.
 * Si el documento no se puede interpretar, no se añade ningún producto.
 */
public class ImportadorJson {

    private final ValidadorProducto validador = new ValidadorProducto();

    /**
     * Importa los productos de un fichero JSON al inventario.
     * 
     * @param textoRuta  ruta del fichero escrita por el usuario
     * @param inventario colección común donde se añaden los productos válidos
     * @return resultado con contadores e incidencias
     */
    public ResultadoImportacion importar(String textoRuta, Inventario inventario) {
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
                        procesarProductos(lista, inventario, resultado);
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
     * @param texto     contenido del fichero
     * @param resultado donde se anota el error si la estructura no es correcta
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
 * Recorre el array, valida cada producto y añade al inventario los válidos.
 * @param lista      array productos del JSON
 * @param inventario colección común
 * @param resultado  donde se guardan importados y rechazos
 */
private void procesarProductos(JsonArray lista, Inventario inventario, ResultadoImportacion resultado) {
    resultado.setEncontrados(lista.size());
    Set<String> codigosDelFichero = new HashSet<>(); // códigos ya importados de este fichero

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
                } else if (inventario.existeCodigo(producto.getCodigo())) {
                    errores.add("código duplicado (ya existe en el inventario)");
                } else {
                    inventario.agregar(producto);
                    codigosDelFichero.add(clave);
                    resultado.anotarImportado();
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
 * @param obj     objeto JSON de un producto
 * @param errores lista donde se añaden los problemas de lectura
 * @return producto con los valores que se han podido leer
 */
private Producto construirProducto(JsonObject obj, List<String> errores) {
    Producto p = new Producto();
    String codigo = leerTexto(obj, "codigo", errores);
    String nombre = leerTexto(obj, "nombre", errores);
    p.setCodigo(codigo == null ? null : codigo.trim()); // se guardan sin espacios extremos
    p.setNombre(nombre == null ? null : nombre.trim());
    p.setPrecio(leerDecimal(obj, "precio", errores));
    p.setStock(leerEntero(obj, "stock", errores));
    p.setCategoriaId(leerEntero(obj, "categoriaId", errores));
    p.setProveedorId(leerEntero(obj, "proveedorId", errores));
    return p;
}

//Metodos para leer campos

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
