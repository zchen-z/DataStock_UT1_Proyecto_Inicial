package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.modelo.Categoria;
import es.iesisidra.datastock.util.LectorConsola;

import java.sql.SQLException;
import java.util.List;

import static es.iesisidra.datastock.util.ColoresConsola.aviso;
import static es.iesisidra.datastock.util.ColoresConsola.error;
import static es.iesisidra.datastock.util.ColoresConsola.ok;

/**
 * Opción 7.9 · RF-JDBC-08: demostración de commit y rollback con categorías de prueba.
 * Usa IDs altos (9001-9003) que no afectan a datos reales.
 */
public class MenuTransaccion {

    private static final int ID_COMMIT_1 = 9001;
    private static final int ID_COMMIT_2 = 9002;
    private static final int ID_ROLLBACK = 9003;
    private final CategoriaDAO dao = new CategoriaDAO();

    /** Explica la demostración, pide confirmación y ejecuta los dos escenarios. */
    public void iniciar() {
        System.out.println("\nDEMOSTRACIÓN DE TRANSACCIONES JDBC");
        System.out.println("Se usarán categorías de prueba con IDs 9001, 9002 y 9003, sin productos asociados.");
        System.out.println("A) commit: se insertan 9001 y 9002 en una transacción y se confirman.");
        System.out.println("B) rollback: se inserta 9003 y después se repite el 9001 (clave duplicada); se deshace todo.");
        try {
            if (dao.buscarPorId(ID_COMMIT_1) != null || dao.buscarPorId(ID_COMMIT_2) != null
                    || dao.buscarPorId(ID_ROLLBACK) != null) {
                aviso("Ya existen datos de una demostración anterior. Bórralos antes con:");
                System.out.println("DELETE FROM categoria WHERE id IN (9001, 9002, 9003);");
            } else if (!LectorConsola.confirmar("¿Comenzar la demostración?")) {
                aviso("Demostración cancelada.");
            } else {
                escenarioCommit();
                escenarioRollback();
                System.out.println("\nDatos de prueba que quedan: categorías 9001 y 9002.");
                System.out.println("Para borrarlos: DELETE FROM categoria WHERE id IN (9001, 9002);");
            }
        } catch (SQLException e) {
            error("MySQL: " + e.getMessage());
        }
        LectorConsola.pausar();
    }

    /** Escenario A: dos inserciones correctas y commit. */
    private void escenarioCommit() throws SQLException {
        System.out.println("\n--- Escenario A: commit ---");
        dao.insertarLote(List.of(new Categoria(ID_COMMIT_1, "Demo commit 1"),
                new Categoria(ID_COMMIT_2, "Demo commit 2")));
        if (dao.buscarPorId(ID_COMMIT_1) != null && dao.buscarPorId(ID_COMMIT_2) != null) {
            ok("Commit correcto: las categorías 9001 y 9002 existen en MySQL.");
        } else {
            error("Las categorías no aparecen después del commit.");
        }
    }

    /** Escenario B: la segunda inserción repite una clave, se hace rollback y la primera no queda. */
    private void escenarioRollback() throws SQLException {
        System.out.println("\n--- Escenario B: rollback ---");
        try {
            dao.insertarLote(List.of(new Categoria(ID_ROLLBACK, "Demo rollback"),
                    new Categoria(ID_COMMIT_1, "Demo duplicada")));
            error("No se ha producido el error esperado.");
        } catch (SQLException e) {
            aviso("Error esperado: " + e.getMessage());
            if (dao.buscarPorId(ID_ROLLBACK) == null) {
                ok("Rollback correcto: la categoría 9003 no se ha guardado.");
            } else {
                error("La categoría 9003 se ha guardado: el rollback no ha funcionado.");
            }
        }
    }
}
