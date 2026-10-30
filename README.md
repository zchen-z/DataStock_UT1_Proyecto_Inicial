# DataStock – UT1 · Ficheros y formatos de intercambio de datos

**Módulo:** Acceso a Datos · 2.º DAM · IES Isidra de Guzmán · Curso 2026–2027  
**Lenguaje:** Java 21 · **Construcción:** Maven · **Interfaz:** consola

Aplicación de consola para gestionar el inventario de una pequeña empresa. En esta primera fase (UT1) lee catálogos JSON y XML, valida los productos, los guarda en una colección en memoria, permite consultarlos y exportarlos, y guarda y recupera el inventario con serialización. Incluye dos actividades independientes: consultas XPath y acceso aleatorio a un fichero binario. No se conecta a MySQL.

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

**Actividades independientes** (no están en el menú principal): en VS Code, abrir su `App.java` y pulsar **Run**; o desde terminal:

```bash
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.rf07.App
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.rf08.App
```

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
| 6.1 / 6.2 | Guardar / recuperar inventario (.dat, serialización) | RF-FIC-06 | Implementado |
| Independiente | Consultas XPath (`complementarias.rf07.App`) | RF-FIC-07 | Implementado |
| Independiente | Acceso aleatorio con `RandomAccessFile` (`complementarias.rf08.App`) | RF-FIC-08 | Implementado |

## 4. Estructura del proyecto

```text
DataStock_UT1_Proyecto_Inicial/
├── pom.xml
├── README.md
├── DataStock_UT1_Registro_Pruebas.odt    # Registro de pruebas ejecutadas
├── evidencias/                           # Capturas de cada prueba (CP-xxx.png)
├── datos/
│   ├── ejemplos/                         # Catálogos facilitados por la profesora
│   ├── pruebas/
│   │   ├── explorar/                     # RF-FIC-01
│   │   ├── json/                         # RF-FIC-02
│   │   ├── xml/                          # RF-FIC-03
│   │   ├── serializacion/                # RF-FIC-06
│   │   ├── xpath/                        # RF-FIC-07
│   │   └── acceso_aleatorio/             # RF-FIC-08
│   └── salidas/
├── material_referencia/                  # Modelo relacional y script SQL (UT2)
└── src/main/java/es/iesisidra/datastock/
    ├── App.java                          # Punto de entrada
    ├── ficheros/
    │   ├── ExploradorFicheros.java       # RF-FIC-01 (NIO.2)
    │   ├── ImportadorJson.java           # RF-FIC-02 (Gson)
    │   ├── ImportadorXml.java            # RF-FIC-03 (DOM, protegido frente a XXE)
    │   ├── ExportadorProductos.java      # RF-FIC-05 (Gson y DOM, UTF-8, ordenado por código)
    │   └── SerializadorInventario.java   # RF-FIC-06 (ObjectOutputStream / ObjectInputStream)
    ├── servicio/
    │   ├── Inventario.java               # Colección común: alta sin duplicados, búsquedas y sustitución
    │   ├── ValidadorProducto.java        # Reglas comunes de validación (JSON y XML)
    │   └── ResultadoImportacion.java     # Contadores e incidencias de una importación
    ├── ui/
    │   └── MenuPrincipal.java            # Menús: solo piden datos y delegan
    ├── modelo/                           # Producto, Categoria, Proveedor, MovimientoStock
    ├── util/                             # LectorConsola, ColoresConsola
    └── complementarias/                  # Actividades independientes del menú
        ├── rf07/                         # RF-FIC-07 · XPath
        │   ├── App.java                  # Punto de entrada
        │   ├── MenuXPath.java            # Pide datos y muestra resultados
        │   └── ConsultasXPath.java       # Las cuatro consultas con XPath
        └── rf08/                         # RF-FIC-08 · RandomAccessFile
            ├── App.java                  # Punto de entrada
            ├── MenuAccesoAleatorio.java  # Pide datos y muestra resultados
            └── FicheroStock.java         # Registros de 36 bytes con seek()
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

## 5.2. Serialización (RF-FIC-06)

- **Guardar** escribe la colección completa en un `.dat` con `ObjectOutputStream`. Si el inventario está vacío no se crea ningún fichero; si el fichero existe, se pide confirmación.
- **Recuperar** lee el `.dat` con `ObjectInputStream`, comprueba que es una lista de `Producto` válidos y **sustituye** el inventario, con confirmación si ya hay productos en memoria.
- Un filtro de deserialización (`ObjectInputFilter`) solo admite las clases que guarda DataStock, para no deserializar ficheros de procedencia desconocida.
- Si el fichero está dañado o no es compatible, el inventario en memoria no cambia.

## 5.3. Actividades independientes

- **RF-FIC-07 · XPath:** abre un catálogo XML (rechaza DTD) y permite buscar por código, filtrar por categoría, filtrar por precio estrictamente superior y contar productos con `count()`. Los textos del usuario no se concatenan en las expresiones XPath: se seleccionan los nodos y sus valores se comparan en Java.
- **RF-FIC-08 · Acceso aleatorio:** carga un catálogo JSON o XML y genera `stock.bin` con registros de 36 bytes (código 12 caracteres = 24 bytes, precio `double` = 8 bytes, stock `int` = 4 bytes). Consulta un registro con `seek(posición × 36)` y modifica solo el stock con `seek(posición × 36 + 32)`. `stock.bin` es una copia independiente: sus cambios no afectan a los productos en memoria.

**Decisión propia:** el patrón del código admite también minúsculas (`[A-Za-z0-9-]+`), porque el enunciado indica que los códigos se comparan sin distinguir mayúsculas.

## 6. Pruebas

Las pruebas ejecutadas están documentadas en `DataStock_UT1_Registro_Pruebas.odt`, con sus capturas en `evidencias/`:

| Requisito | Pruebas | Estado |
|---|---|---|
| RF-FIC-01 | CP-FIC-01 a 03 | Superadas |
| RF-FIC-02 | CP-JSON-01 a 04 | Superadas |
| RF-FIC-03 | CP-XML-01 a 04 | Superadas |
| RF-FIC-04 | CP-CON-01 a 04 | Superadas (CP-CON-04 repetida importando JSON y XML) |
| RF-FIC-05 | CP-EXP-01 a 05 | Superadas |
| RF-FIC-06 | CP-SER-01 a 04 | Superadas |
| RF-FIC-07 | CP-XPATH-01 a 05 | Superadas |
| RF-FIC-08 | CP-RAF-01 a 05 | Superadas |

## 7. Entrega

La versión entregada en plazo de la UT1 se identifica con la etiqueta Git **`v1.1-UT1`** 
