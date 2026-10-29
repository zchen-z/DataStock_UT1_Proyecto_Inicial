package es.iesisidra.datastock.ficheros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import es.iesisidra.datastock.modelo.Producto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * RF-FIC-05 · Exportar el inventario a JSON (Gson) y a XML (DOM).
 * Recibe los productos ya ordenados por código y los escribe en UTF-8.
 */
public class ExportadorProductos {

    /**
     * Comprueba que la ruta de destino se puede usar para escribir.
     *
     * @param textoRuta ruta escrita por el usuario
     * @return mensaje de error, o null si la ruta es correcta
     */
    public String comprobarDestino(String textoRuta) {
        String error = null;
        if (textoRuta == null || textoRuta.isBlank()) {
            error = "la ruta no puede estar vacía";
        } else {
            try {
                Path ruta = Path.of(textoRuta.trim()).toAbsolutePath();
                Path directorio = ruta.getParent();
                if (Files.isDirectory(ruta)) {
                    error = "la ruta es un directorio; indica también el nombre del fichero";
                } else if (directorio == null || !Files.isDirectory(directorio)) {
                    error = "el directorio de destino no existe";
                } else if (!Files.isWritable(directorio)) {
                    error = "no tienes permisos para escribir en el directorio de destino";
                }
            } catch (InvalidPathException e) {
                error = "la ruta no es válida";
            }
        }
        return error;
    }

    /**
     * Escribe los productos en un fichero JSON con raíz "productos": [...].
     *
     * @param productos productos a exportar (ya ordenados)
     * @param ruta      fichero de destino
     * @throws IOException si no se puede escribir el fichero
     */
    public void exportarJson(List<Producto> productos, Path ruta) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject raiz = new JsonObject();
        raiz.add("productos", gson.toJsonTree(productos)); // forma A: de objetos Java a JSON

        try (Writer escritor = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            gson.toJson(raiz, escritor);
        }
    }

    /**
     * Escribe los productos en un fichero XML con estructura catalogo/producto/campos.
     *
     * @param productos productos a exportar (ya ordenados)
     * @param ruta      fichero de destino
     * @throws IOException si no se puede escribir el fichero o generar el XML
     */
    public void exportarXml(List<Producto> productos, Path ruta) throws IOException {
        try {
            Document documento = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element catalogo = documento.createElement("catalogo");
            documento.appendChild(catalogo);

            for (Producto p : productos) {
                Element producto = documento.createElement("producto");
                agregarCampo(documento, producto, "codigo", p.getCodigo());
                agregarCampo(documento, producto, "nombre", p.getNombre());
                agregarCampo(documento, producto, "precio", p.getPrecio().toPlainString());
                agregarCampo(documento, producto, "stock", String.valueOf(p.getStock()));
                agregarCampo(documento, producto, "categoriaId", String.valueOf(p.getCategoriaId()));
                agregarCampo(documento, producto, "proveedorId", String.valueOf(p.getProveedorId()));
                catalogo.appendChild(producto);
            }

            Transformer transformador = TransformerFactory.newInstance().newTransformer();
            transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformador.setOutputProperty(OutputKeys.INDENT, "yes"); // con sangría
            transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            try (OutputStream salida = Files.newOutputStream(ruta)) {
                transformador.transform(new DOMSource(documento), new StreamResult(salida));
            }
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException("no se pudo generar el XML: " + e.getMessage(), e);
        }
    }

    /** Crea una etiqueta hija con su texto, por ejemplo <codigo>TEC-001</codigo>. */
    private void agregarCampo(Document documento, Element padre, String etiqueta, String valor) {
        Element campo = documento.createElement(etiqueta);
        campo.setTextContent(valor);
        padre.appendChild(campo);
    }
}
