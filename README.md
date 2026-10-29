# DataStock – UT1 · Ficheros y formatos de intercambio de datos

**Módulo:** Acceso a Datos · 2.º DAM · IES Isidra de Guzmán · Curso 2026–2027  
**Lenguaje:** Java 21 · **Construcción:** Maven · **Interfaz:** consola

Aplicación de consola para gestionar el inventario de una pequeña empresa. En esta primera fase (UT1) lee catálogos JSON y XML, valida los productos, los guarda en una colección en memoria y permite consultarlos. No se conecta a MySQL.

## 1. Requisitos previos

- JDK **21** (`java -version`).
- Maven, o VS Code con la extensión **Extension Pack for Java** (incluye Maven).
- Conexión a Internet la primera vez, para descargar Gson.

## 2. Cómo ejecutar

**Desde VS Code:** abrir la carpeta que contiene `pom.xml`, esperar a que se importe el proyecto Maven y ejecutar `es.iesisidra.datastock.App` con **Run**.

**Desde terminal** (en la raíz del proyecto):

```bash
mvn clean compile
mvn exec:java
```

Las rutas de los ficheros se escriben relativas a la raíz del proyecto (por ejemplo `datos/ejemplos/catalogo_valido.json`) o como rutas absolutas.

## 3. Menú y requisitos

| Menú | Operación | Requisito | Estado |
|---|---|---|---|
| 1 | Explorar ficheros y directorios | RF-FIC-01 | Implementado |
| 2 | Importar productos desde JSON (Gson) | RF-FIC-02 | Implementado |
| 3 | Importar productos desde XML (DOM) | RF-FIC-03 | Implementado |
| 4.1 | Listar todos los productos | RF-FIC-04 | Implementado |
| 4.2 | Buscar producto por código | RF-FIC-04 | Implementado |
| 4.3 | Buscar productos por nombre | RF-FIC-04 | Implementado |
| 5.1 / 5.2 | Exportar productos a JSON (Gson) / XML (DOM) | RF-FIC-05 | Implementado |
| 6.1 / 6.2 | Guardar / recuperar inventario (.dat) | RF-FIC-06 | Pendiente |
| Independiente | Consultas XPath (`XPathActividad`) | RF-FIC-07 | Pendiente |
| Independiente | Acceso aleatorio (`AccesoAleatorioActividad`) | RF-FIC-08 | Pendiente |

Las opciones pendientes siguen mostrando el mensaje `[PENDIENTE]` sin cerrar la aplicación.

## 4. Estructura del proyecto

```text
DataStock_UT1_Proyecto_Inicial/
├── pom.xml
├── README.md
├── DataStock_UT1_Registro_Pruebas.docx   # Registro de pruebas ejecutadas
├── evidencias/                           # Capturas de cada prueba (CP-xxx.png)
├── datos/
│   ├── ejemplos/                         # Catálogos facilitados por la profesora
│   ├── pruebas/
│   │   ├── explorar/                     # RF-FIC-01
│   │   ├── json/                         # RF-FIC-02
│   │   └── xml/                          # RF-FIC-03
│   └── salidas/
├── material_referencia/                  # Modelo relacional y script SQL (UT2)
└── src/main/java/es/iesisidra/datastock/
    ├── App.java                          # Punto de entrada
    ├── ficheros/
    │   ├── ExploradorFicheros.java       # RF-FIC-01 (NIO.2)
    │   ├── ImportadorJson.java           # RF-FIC-02 (Gson)
    │   ├── ImportadorXml.java            # RF-FIC-03 (DOM, protegido frente a XXE)
    │   └── ExportadorProductos.java      # RF-FIC-05 (Gson y DOM, UTF-8, ordenado por código)
    ├── servicio/
    │   ├── Inventario.java               # Colección común: alta sin duplicados y búsquedas
    │   ├── ValidadorProducto.java        # Reglas comunes de validación (JSON y XML)
    │   └── ResultadoImportacion.java     # Contadores e incidencias de una importación
    ├── ui/
    │   └── MenuPrincipal.java            # Menús: solo piden datos y delegan
    ├── modelo/                           # Producto, Categoria, Proveedor, MovimientoStock
    ├── util/                             # LectorConsola, ColoresConsola
    └── complementarias/                  # XPathActividad, AccesoAleatorioActividad
```

## 5. Reglas aplicadas en la importación

- **JSON:** raíz de tipo objeto con el array `productos`. **XML:** raíz `catalogo` con elementos `producto`.
- Si el documento está mal formado, tiene una raíz incorrecta o (en XML) contiene una DTD, **se cancela la importación completa** y el inventario no cambia.
- Si el documento es correcto pero algunos productos no lo son, **se importan solo los válidos** y se muestra el motivo de cada rechazo.
- Validaciones de cada producto (las mismas en JSON y XML, en `ValidadorProducto`):
  - `codigo`: obligatorio, máximo 12 caracteres, solo letras, números y guiones.
  - `nombre`: obligatorio, entre 3 y 100 caracteres.
  - `precio`: mayor que 0 (se guarda como `BigDecimal`).
  - `stock`: entero mayor o igual que 0.
  - `categoriaId` y `proveedorId`: enteros positivos.
- Los códigos **no se duplican** (ni en memoria ni dentro del mismo fichero), sin distinguir mayúsculas y sin espacios extremos. Un producto duplicado se rechaza y no sustituye al existente.
- Importar **añade** productos: la colección no se vacía antes.

## 5.1. Exportación

- Se exportan todos los productos en memoria, **ordenados por código** y en **UTF-8**.
- JSON con raíz `{"productos": [...]}`; XML con `catalogo/producto/campos` y sangría.
- Antes de escribir se comprueba que hay productos, que la ruta no está vacía, que el directorio de destino existe y que se puede escribir. Si el fichero ya existe, se pide confirmación.
- No hay conversor directo JSON↔XML: el modelo intermedio es la colección de objetos `Producto`.

**Decisión propia:** el patrón del código admite también minúsculas (`[A-Za-z0-9-]+`), porque el enunciado indica que los códigos se comparan sin distinguir mayúsculas.

## 6. Pruebas

Las pruebas ejecutadas están documentadas en `DataStock_UT1_Registro_Pruebas.docx`, con sus capturas en `evidencias/`:

| Requisito | Pruebas | Estado |
|---|---|---|
| RF-FIC-01 | CP-FIC-01 a 03 | Superadas |
| RF-FIC-02 | CP-JSON-01 a 04 | Superadas |
| RF-FIC-03 | CP-XML-01 a 04 | Superadas |
| RF-FIC-04 | CP-CON-01 a 03 | Superadas |
| RF-FIC-04 | CP-CON-04 | No superada: se ejecutó importando solo el XML; pendiente de repetir importando JSON y XML (ver incidencias del registro) |
| RF-FIC-05 | CP-EXP-01 a 05 | Superadas |
| RF-FIC-06 a 08 | — | No ejecutadas (sin implementar) |

## 7. Entrega

La versión entregada de la UT1 se identifica con la etiqueta Git **`v1-UT1`**.
