# DataStock – Acceso a Datos (UT1, UT2 y UT3)

**2.º DAM · IES Isidra de Guzmán · 2026–2027** · Java 21 · Maven · consola

Aplicación de consola para gestionar un inventario. La **UT1** trabaja con ficheros y un inventario **en memoria** (opciones 1–6). La **UT2** añade la opción **7**, que trabaja con **MySQL** mediante **JDBC**. La **UT3** separa dos funciones en **componentes reutilizables**.

## 1. Requisitos

- JDK 21 y Maven (o VS Code con Extension Pack for Java).
- MySQL **8.0.16 o posterior** (probado con 8.0.46).
- Dependencias en `pom.xml`: Gson 2.11.0, **MySQL Connector/J 9.1.0** y **sqlite-jdbc 3.46.1.3**.

## 2. Preparar la base de datos

Ejecutar `material_referencia/DataStock_BD_MySQL.sql` (en Workbench con ⚡, o `mysql -u root -p < material_referencia/DataStock_BD_MySQL.sql`). Crea la base de datos `datastock`, las tablas `categoria`, `proveedor`, `producto` y `movimiento_stock`, y el procedimiento `registrar_movimiento`. Las restricciones del script no se han modificado.

## 3. Configurar la conexión

Copiar `db.properties.example` como **`db.properties`** en la raíz del proyecto y poner los datos del MySQL local:

```properties
db.url=jdbc:mysql://localhost:3306/datastock
db.user=root
db.password=TU_CONTRASEÑA
```

`db.properties` está en `.gitignore`, así que la contraseña **no se sube** a GitHub.

## 4. Ejecutar

Abrir en VS Code la carpeta que contiene `pom.xml` y ejecutar `es.iesisidra.datastock.App`. Desde terminal:

```bash
mvn clean compile
mvn exec:java
```

Actividades independientes:

```bash
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.rf07.App       # XPath
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.rf08.App       # RandomAccessFile
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.DemoSQLite     # SQLite
```

## 5. Menús

| Opción | Función | Requisito |
|---|---|---|
| 1 | Explorar ficheros y directorios | RF-FIC-01 |
| 2 / 3 | Importar productos desde JSON / XML (la 2 usa `LectorProductosJson`, UT3) | RF-FIC-02 / 03 |
| 4 | Consultar productos en memoria | RF-FIC-04 |
| 5 | Exportar a JSON / XML | RF-FIC-05 |
| 6 | Guardar / recuperar inventario (.dat) | RF-FIC-06 |
| **7** | **Gestionar base de datos MySQL** | **RF-JDBC** |
| 7.1 | Comprobar conexión e información de la BD | RF-JDBC-01 |
| 7.2 / 7.3 | Categorías / proveedores | RF-JDBC-02 |
| 7.4 | Productos (CRUD) | RF-JDBC-03 |
| 7.5 | Consultas (JOIN, LIKE, umbral de stock); la 7.5.2 usa `ProductoDAO.buscarDetalle`, UT3 | RF-JDBC-04 |
| 7.6 | Entrada o salida de stock (procedimiento almacenado) | RF-JDBC-05 |
| 7.7 | Historial de movimientos | RF-JDBC-06 |
| 7.8 | Copiar productos de memoria a MySQL (transacción) | RF-JDBC-07 |
| 7.9 | Demostración de commit y rollback | RF-JDBC-08 |

## 6. Memoria y MySQL

Son **dos almacenes distintos**. Las opciones 1–6 usan el inventario en memoria, que se pierde al cerrar salvo que se guarde con la opción 6. La opción 7 lee y escribe directamente en MySQL, y sus datos se conservan entre ejecuciones. **No hay sincronización automática:** solo la opción 7.8 copia a MySQL los productos válidos de la memoria, sin modificar la memoria.

La demostración 7.9 usa las categorías de prueba 9001 y 9002. Para borrarlas: `DELETE FROM categoria WHERE id IN (9001, 9002);`

## 7. Estructura

```text
src/main/java/es/iesisidra/datastock/
├── App.java
├── config/          ConexionBD (lee db.properties)
├── dao/             CategoriaDAO, ProveedorDAO, ProductoDAO, MovimientoDAO (todo el SQL)
├── ficheros/        UT1: importar, exportar, serializar · UT3: LectorProductosJson
├── servicio/        Inventario, ValidadorProducto, ValidadorProductoBD
├── ui/              MenuPrincipal, MenuMySQL y un menú por cada opción de la 7
├── modelo/          Producto, Categoria, Proveedor, MovimientoStock, ProductoDetalle
├── util/            LectorConsola, ColoresConsola
└── complementarias/ rf07 (XPath), rf08 (RandomAccessFile), DemoSQLite
```

## 8. Comparativa MySQL y SQLite

| | MySQL | SQLite |
|---|---|---|
| ¿Necesita servidor? | Sí, un servicio aparte | No, está embebido en la aplicación |
| ¿Dónde se guardan los datos? | En el servidor | En un fichero (`datos/sqlite/productos_demo.db`) |
| Conexión JDBC | `jdbc:mysql://localhost:3306/datastock` + usuario y contraseña | `jdbc:sqlite:datos/sqlite/productos_demo.db`, sin credenciales |
| Dependencia | `mysql-connector-j` 9.1.0 | `sqlite-jdbc` 3.46.1.3 |
| Ventajas | Multiusuario, acceso por red, permisos, procedimientos almacenados | Sin instalación, ligero, un solo fichero portable |
| Inconvenientes | Hay que instalarlo y mantenerlo | Escritura concurrente limitada, sin usuarios ni permisos |
| Lo elegiría para | Aplicaciones multiusuario o web (como DataStock) | Aplicaciones locales, de escritorio o móviles, y prototipos |

**Pool de conexiones:** mantiene varias conexiones abiertas y las reutiliza, en lugar de abrir una nueva para cada operación. Es útil cuando hay muchas peticiones a la vez, como en un servidor web. DataStock es de consola y tiene un solo usuario, así que le bastan **conexiones cortas**, que cada operación abre y cierra con try-with-resources.

## 9. UT3: componentes reutilizables

| Componente | Entrada | Salida | Usado en |
|---|---|---|---|
| `ficheros/LectorProductosJson` → `leer(ruta)` | Ruta del JSON | `ResultadoImportacion` con los productos válidos y los errores | Opción **2** (importar JSON) |
| `dao/ProductoDAO` → `buscarDetalle(codigo)` | Código | La ficha del producto, o `null` si no existe | Opción **7.5.2** (buscar por código) |

Ninguno de los dos componentes escribe en la consola ni lee del teclado: **el menú** muestra `[OK]` en verde, `[ERROR]` en rojo o `[AVISO]` en amarillo. Para la consulta JDBC se ha reutilizado el DAO de la UT2 en lugar de crear una clase nueva, porque el enunciado pide no duplicar clases. El antiguo `ImportadorJson` se ha sustituido por `LectorProductosJson`, que ya no añade nada al inventario: lo hace el menú.

**¿Qué se gana y qué cuesta separar la lectura del JSON del menú?** Se gana que el lector se puede reutilizar (lo usan la opción 2 y la actividad RF-08) y probar sin la consola, y que cada clase tiene una sola tarea. Cuesta una clase más y que el menú tenga que recorrer el resultado para añadir los productos y mostrar el resumen.

**¿Por qué separar la consulta JDBC de la presentación?** Así el mismo método sirve para la consola, para una interfaz gráfica o para una prueba. Además, el SQL queda en un solo sitio (el DAO) y, si cambia la base de datos, no hay que tocar los menús.

**Herramientas:** **VS Code** con Extension Pack for Java para editar, ejecutar y depurar. **Maven** gestiona las dependencias (Gson y los conectores) y compila el proyecto (`mvn clean compile`). **Javadoc** documenta las clases y los métodos públicos de los componentes (propósito, parámetros, resultado y errores). No se ha usado JUnit, porque era opcional.

**Opciones modificadas:** la **2** (importar productos desde JSON) y la **7.5.2** (buscar producto por código en MySQL).

## 10. Pruebas y entrega

Las pruebas están en `DataStock_UT1_Registro_Pruebas.odt`, con las capturas en `evidencias/`. Las etiquetas de entrega son `v1.1-UT1` (UT1), `UT2-v1.0` (UT2) y **`UT3-v1.0`** (UT3).
