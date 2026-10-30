package es.iesisidra.datastock.complementarias.rf07;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-FIC-07 · Lógica de las consultas XPath sobre un catálogo XML.
 * <p>Abre el documento una vez y permite varias consultas sobre él, sin
 * importar los productos a DataStock. No lee del teclado ni imprime nada:
 * eso lo hace {@link MenuXPath}.</p>
 * <p>Los textos del usuario no se meten dentro de las expresiones XPath:
 * se seleccionan los nodos con XPath y sus valores se comparan en Java.</p>
 */
public class ConsultasXPath {

    private static final String PRODUCTOS = "/catalogo/producto";

    private final XPath xpath = XPathFactory.newInstance().newXPath();
    private Document documento;

    /**
     * Abre el XML con un analizador seguro (sin DTD) y comprueba que la raíz es catalogo.
     *
     * @param ruta fichero XML
     * @throws IOException si no existe, está mal formado, tiene DTD o la raíz no es catalogo
     */
    public void abrir(Path ruta) throws IOException {
        if (!Files.isRegularFile(ruta)) {
            throw new IOException("el fichero no existe o no es un fichero");
        }
        Document leido;
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            // Rechaza cualquier XML con <!DOCTYPE>: sin DTD no hay entidades externas (XXE)
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder analizador = fabrica.newDocumentBuilder();
            analizador.setErrorHandler(new DefaultHandler()); // evita que Java imprima "[Fatal Error]"
            leido = analizador.parse(ruta.toFile());
        } catch (SAXException e) {
            if (e.getMessage() != null && e.getMessage().contains("DOCTYPE")) {
                throw new IOException("el XML contiene una DTD; no se admite por seguridad");
            }
            throw new IOException("el XML está mal formado");
        } catch (ParserConfigurationException e) {
            throw new IOException("no se pudo configurar el analizador XML");
        }
        if (!leido.getDocumentElement().getNodeName().equals("catalogo")) {
            throw new IOException("la raíz del XML debe ser <catalogo>");
        }
        documento = leido;
    }

    /**
     * Consulta 1: productos con el código indicado (sin distinguir mayúsculas).
     *
     * @param codigo código a buscar
     * @return una línea por producto encontrado
     * @throws XPathExpressionException si falla la evaluación XPath
     */
    public List<String> buscarPorCodigo(String codigo) throws XPathExpressionException {
        List<String> encontrados = new ArrayList<>();
        NodeList nodos = (NodeList) xpath.evaluate(PRODUCTOS, documento, XPathConstants.NODESET);
        for (int i = 0; i < nodos.getLength(); i++) {
            Node producto = nodos.item(i);
            if (xpath.evaluate("codigo", producto).trim().equalsIgnoreCase(codigo.trim())) {
                encontrados.add(describir(producto));
            }
        }
        return encontrados;
    }

    /**
     * Consulta 2: productos de una categoría.
     *
     * @param categoria identificador de categoría (entero positivo)
     * @return una línea por producto encontrado
     * @throws XPathExpressionException si falla la evaluación XPath
     */
    public List<String> filtrarPorCategoria(int categoria) throws XPathExpressionException {
        List<String> encontrados = new ArrayList<>();
        NodeList nodos = (NodeList) xpath.evaluate(PRODUCTOS, documento, XPathConstants.NODESET);
        for (int i = 0; i < nodos.getLength(); i++) {
            Node producto = nodos.item(i);
            if (xpath.evaluate("categoriaId", producto).trim().equals(String.valueOf(categoria))) {
                encontrados.add(describir(producto));
            }
        }
        return encontrados;
    }

    /**
     * Consulta 3: productos con precio estrictamente mayor que el indicado.
     * Un producto con precio no numérico no cumple el filtro.
     *
     * @param minimo precio de referencia (excluido)
     * @return una línea por producto encontrado
     * @throws XPathExpressionException si falla la evaluación XPath
     */
    public List<String> filtrarPorPrecioSuperior(BigDecimal minimo) throws XPathExpressionException {
        List<String> encontrados = new ArrayList<>();
        NodeList nodos = (NodeList) xpath.evaluate(PRODUCTOS, documento, XPathConstants.NODESET);
        for (int i = 0; i < nodos.getLength(); i++) {
            Node producto = nodos.item(i);
            try {
                BigDecimal precio = new BigDecimal(xpath.evaluate("precio", producto).trim());
                if (precio.compareTo(minimo) > 0) { // > excluye exactamente el valor indicado
                    encontrados.add(describir(producto));
                }
            } catch (NumberFormatException e) {
                // precio no numérico: el producto no cumple el filtro
            }
        }
        return encontrados;
    }

    /**
     * Consulta 4: número de elementos producto, con la función count() de XPath.
     * Cuenta todos, aunque alguno tenga datos incorrectos.
     *
     * @return número de productos
     * @throws XPathExpressionException si falla la evaluación XPath
     */
    public int contar() throws XPathExpressionException {
        Double total = (Double) xpath.evaluate("count(" + PRODUCTOS + ")", documento, XPathConstants.NUMBER);
        return total.intValue();
    }

    /** Devuelve código, nombre y precio de un nodo producto en una línea. */
    private String describir(Node producto) throws XPathExpressionException {
        return String.format("%-10s %-25s %s EUR", xpath.evaluate("codigo", producto).trim(),
                xpath.evaluate("nombre", producto).trim(), xpath.evaluate("precio", producto).trim());
    }
}
