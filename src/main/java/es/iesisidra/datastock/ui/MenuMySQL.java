package es.iesisidra.datastock.ui;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.servicio.Inventario;
import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;
import es.iesisidra.datastock.util.LectorConsola;


/**
 * Opción 7 del menú principal: funcionalidades de MySQL (UT2).
 */
public class MenuMySQL {

    private final Inventario inventario; // se usará en la opción 7.8
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MenuCategorias menuCategorias = new MenuCategorias();
    private final MenuProveedores menuProveedores = new MenuProveedores();
    private final MenuProductos menuProductos = new MenuProductos();
    private final MenuConsultasBD menuConsultasBD = new MenuConsultasBD();

    /** @param inventario inventario en memoria de la UT1 */
    public MenuMySQL(Inventario inventario) {
        this.inventario = inventario;
    }

    /** Muestra el menú MySQL hasta que el usuario vuelve al principal. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n========== GESTIÓN MYSQL - UT2 ==========");
            System.out.println("1. Comprobar conexión y mostrar información de la BD");
            System.out.println("2. Gestionar categorías");
            System.out.println("3. Gestionar proveedores");
            System.out.println("4. Gestionar productos (CRUD)");
            System.out.println("5. Consultar y buscar productos");
            System.out.println("6. Registrar entrada o salida de stock");
            System.out.println("7. Consultar historial de movimientos");
            System.out.println("8. Importar productos de memoria a MySQL");
            System.out.println("9. Demostración de transacción con commit/rollback");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 9);
            switch (opcion) {
                case 1 -> comprobarConexion();
                case 2 -> menuCategorias.iniciar();
                case 3 -> menuProveedores.iniciar();
                case 4 -> menuProductos.iniciar();
                case 5 -> menuConsultasBD.iniciar();
                default -> aviso("Opción pendiente de implementar.");
            }
        } while (opcion != 0);
    }

    /** Opción 7.1 · RF-JDBC-01: conecta y muestra datos de la base de datos. */
    private void comprobarConexion() {
        try (Connection con = ConexionBD.obtenerConexion()) {
            DatabaseMetaData meta = con.getMetaData(); // información del servidor
            ok("Conexión establecida correctamente.");
            System.out.println("Base de datos: " + con.getCatalog());
            System.out.println("Gestor: " + meta.getDatabaseProductName());
            System.out.println("Versión: " + meta.getDatabaseProductVersion());
            System.out.println("Productos almacenados: " + productoDAO.contar());
        } catch (SQLException e) {
            error("No se pudo conectar con MySQL: " + e.getMessage());
        }
        LectorConsola.pausar();
    }

    
}
