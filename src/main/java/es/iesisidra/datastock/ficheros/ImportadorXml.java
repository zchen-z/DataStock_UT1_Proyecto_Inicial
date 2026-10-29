package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.servicio.ResultadoImportacion;
import es.iesisidra.datastock.servicio.ValidadorProducto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import es.iesisidra.datastock.modelo.Producto;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

/**
 * RF-FIC-03 · Importar un catálogo XML mediante DOM.
 * Funciona igual que ImportadorJson: mismas validaciones, mismo resultado.
 * El analizador se configura para rechazar DTD y entidades externas (XXE).
 */
public class ImportadorXml {

    private final ValidadorProducto validador = new ValidadorProducto();

    /**
     * Importa los productos de un fichero XML al inventario.
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
                    resultado.rechazarDocumento("la ruta no es un fichero (¿es un directorio?)");
                } else if (!Files.isReadable(ruta)) {
                    resultado.rechazarDocumento("no tienes permisos para leer el fichero");
                } else {
                    Document documento = crearAnalizadorSeguro().parse(ruta.toFile());
                    Element raiz = documento.getDocumentElement();
                    if (!raiz.getNodeName().equals("catalogo")) {
                        resultado.rechazarDocumento(
                                "la raíz del XML debe ser <catalogo> y es <" + raiz.getNodeName() + ">");
                    } else {
                        procesarProductos(raiz.getElementsByTagName("producto"), inventario, resultado);
                    }
                }
            } catch (InvalidPathException e) {
                resultado.rechazarDocumento("la ruta no es válida");
            } catch (SAXException e) {
                if (e.getMessage() != null && e.getMessage().contains("DOCTYPE")) {
                    resultado.rechazarDocumento("el XML contiene una DTD; no se admite por seguridad");
                } else {
                    resultado.rechazarDocumento("el XML está mal formado");
                }
            } catch (IOException e) {
                resultado.rechazarDocumento("no se pudo leer el fichero: " + e.getMessage());
            } catch (ParserConfigurationException e) {
                resultado.rechazarDocumento("no se pudo configurar el analizador XML");
            }
        }
        return resultado;
    }

    /**
     * Crea un DocumentBuilder protegido frente a XXE:
     * rechaza cualquier XML que contenga una DTD (<!DOCTYPE>).
     * @return analizador DOM seguro
     * @throws ParserConfigurationException si el parser no admite la opción
     *                                      indicada
     */
    private DocumentBuilder crearAnalizadorSeguro() throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance(); 
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // Rechaza cualquier XML con <!DOCTYPE>: sin DTD no hay entidades externas (XXE)

        DocumentBuilder analizador = fabrica.newDocumentBuilder();
        analizador.setErrorHandler(new DefaultHandler()); // evita que Java imprima "[Fatal Error]"
        return analizador;
    }

    /**
 * Recorre los elementos producto, valida cada uno y añade al inventario los válidos.
 *
 * @param nodos      lista de elementos producto
 * @param inventario colección común
 * @param resultado  donde se anotan importados y rechazos
 */
private void procesarProductos(NodeList nodos, Inventario inventario, ResultadoImportacion resultado) {
    resultado.setEncontrados(nodos.getLength());
    Set<String> codigosDelFichero = new HashSet<>(); // códigos ya importados de este fichero

    for (int i = 0; i < nodos.getLength(); i++) {
        String referencia = "Producto " + (i + 1);
        List<String> errores = new ArrayList<>();
        Element elemento = (Element) nodos.item(i);

        Producto producto = construirProducto(elemento, errores);
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
        if (!errores.isEmpty()) {
            resultado.anotarRechazo(referencia, String.join("; ", errores));
        }
    }
}

/**
 * Lee los seis campos de un elemento producto y construye un Producto(mismo proceso que en el de json).
 * Los campos que faltan o tienen un valor incorrecto se anotan en errores.
 * @param elemento elemento producto del XML
 * @param errores  lista donde se añaden los problemas de lectura
 * @return producto con los valores que se han podido leer
 */
private Producto construirProducto(Element elemento, List<String> errores) {
    Producto p = new Producto();
    p.setCodigo(leerTexto(elemento, "codigo", errores));
    p.setNombre(leerTexto(elemento, "nombre", errores));
    p.setPrecio(leerDecimal(elemento, "precio", errores));
    p.setStock(leerEntero(elemento, "stock", errores));
    p.setCategoriaId(leerEntero(elemento, "categoriaId", errores));
    p.setProveedorId(leerEntero(elemento, "proveedorId", errores));
    return p;
}

/** Devuelve el texto (sin espacios extremos) de una etiqueta hija y anota un error si falta. */
private String leerTexto(Element elemento, String etiqueta, List<String> errores) {
    String valor = null;
    NodeList lista = elemento.getElementsByTagName(etiqueta);
    if (lista.getLength() == 0) {
        errores.add("falta el campo " + etiqueta);
    } else {
        valor = lista.item(0).getTextContent().trim();
    }
    return valor;
}

/** Convierte el texto de una etiqueta a BigDecimal y anota un error si falta o no es un número. */
private BigDecimal leerDecimal(Element elemento, String etiqueta, List<String> errores) {
    BigDecimal valor = null;
    String texto = leerTexto(elemento, etiqueta, errores);
    if (texto != null) {
        try {
            valor = new BigDecimal(texto);
        } catch (NumberFormatException e) {
            errores.add("el campo " + etiqueta + " no es un número válido");
        }
    }
    return valor;
}

/** Convierte el texto de una etiqueta a int y anota un error si falta, no es un número o tiene decimales. */
private int leerEntero(Element elemento, String etiqueta, List<String> errores) {
    int valor = 0;
    BigDecimal numero = leerDecimal(elemento, etiqueta, errores);
    if (numero != null) {
        try {
            valor = numero.intValueExact(); // falla si tiene decimales (12.5)
        } catch (ArithmeticException e) {
            errores.add("el campo " + etiqueta + " debe ser un número entero");
        }
    }
    return valor;
}
}
