package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.modelo.Producto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de importar un fichero JSON o XML.
 * Guarda los contadores, las incidencias y (UT3) los productos válidos leídos,
 * para que el menú decida qué hacer con ellos y muestre el resumen final.
 */
public class ResultadoImportacion {

    private int encontrados;
    private int importados;
    private int rechazados;
    private final List<String> incidencias = new ArrayList<>();
    private String errorDocumento; // null = el documento se ha podido leer
    private final List<Producto> validos = new ArrayList<>(); // UT3: productos que devuelve LectorProductosJson

    public int getEncontrados() {
        return encontrados;
    }

    /**
     * Anota cuántos productos contiene el fichero.
     *
     * @param total número de registros encontrados
     */
    public void setEncontrados(int total) {
        this.encontrados = total;
    }

    public int getImportados() {
        return importados;
    }

    public void setImportados(int importados) {
        this.importados = importados;
    }

    public int getRechazados() {
        return rechazados;
    }

    public void setRechazados(int rechazados) {
        this.rechazados = rechazados;
    }

    public List<String> getIncidencias() {
        return incidencias;
    }

    public String getErrorDocumento() {
        return errorDocumento;
    }

    public void setErrorDocumento(String errorDocumento) {
        this.errorDocumento = errorDocumento;
    }

    /** Suma un producto importado correctamente. */
    public void anotarImportado() {
        importados++;
    }

    /**
     * Suma un producto rechazado y guarda el motivo.
     * 
     * @param referencia código del producto o "Producto N" si no tiene código
     * @param motivo     explicación del rechazo
     */
    public void anotarRechazo(String referencia, String motivo) {
        rechazados++;
        incidencias.add(referencia + ": " + motivo);
    }

    /**
     * Marca el documento entero como no válido (mal formado, raíz incorrecta...).
     * En ese caso no se importa ningún producto.
     *
     * @param motivo explicación del error
     */
    public void rechazarDocumento(String motivo) {
        this.errorDocumento = motivo;
    }

    /**
     * UT3 · Guarda un producto que ha pasado todas las validaciones.
     *
     * @param producto producto válido leído del fichero
     */
    public void anotarValido(Producto producto) {
        validos.add(producto);
    }

    /** @return productos válidos leídos por el componente (vacía si el documento no es válido) */
    public List<Producto> getValidos() {
        return validos;
    }

    /** @return si el documento se ha podido leer y procesar */
    public boolean documentoValido() {
        return errorDocumento == null;
    }

}