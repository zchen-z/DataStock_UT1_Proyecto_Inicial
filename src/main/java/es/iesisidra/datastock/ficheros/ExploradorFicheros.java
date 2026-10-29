package es.iesisidra.datastock.ficheros;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExploradorFicheros {
    /**
     * Comprueba la ruta introducida por el usuario, muestra su información y 
     * si es un fichero o un directorio, hace el método correspondiente.
     * Los errores se informa con un mensaje; nunca se cierra el programa porque si.
     *
     * @param textoRuta ruta escrita por el usuario (relativa o absoluta)
     */
    public void explorar(String textoRuta){
        if(textoRuta == null || textoRuta.isBlank()){
            System.out.println("Error. La ruta no puede estar vacía");
        }else{
            try {
                Path ruta = Path.of(textoRuta.trim()); // Convierte el texto en un objeto Path, para que NIO pueda representar una ruta

                if(!Files.exists(ruta)){
                    System.out.println("Error: La ruta no existe: " + ruta.toAbsolutePath());
                }else if(Files.isRegularFile(ruta)){
                    mostrarFichero(ruta);
                }else if(Files.isDirectory(ruta)){
                    mostrarDirectorio(ruta);
                }else{
                    System.out.println("Error: La ruta no es ni fichero ni directorio");
                }
            } catch (InvalidPathException e) {
                System.out.println("ERROR: La ruta no es válida: " + e.getMessage());
            }
        }
    }

/**
 * Mostrar la informacion de un fichero: nombre, ruta absoluta y tamaño en bytes.
 * @param fichero ruta de un fichero que si existe
 */
private void mostrarFichero(Path fichero) {
    try {
        System.out.println("Tipo: FICHERO");
        System.out.println("Nombre: " + fichero.getFileName());
        System.out.println("Ruta absoluta: " + fichero.toAbsolutePath());
        System.out.println("Tamaño: " + Files.size(fichero) + " bytes"); //Este puede fallar por ejemplo si no tienes permisos o el fichero se borra justo en ese momento, y lanza IOException que es obligatoiro capturar con tryCatch.
    } catch (IOException e) {
        System.out.println("ERROR: No se pudo leer el fichero: " + e.getMessage());
    }
}

/**
 * Lee el contenido de un directorio sin entrar en subcarpetas
 * y lo muestra por pantalla. Informa si está vacío o si no hay permisos.
 *
 * @param directorio ruta de un directorio si existe
 */
private void mostrarDirectorio(Path directorio) {
    if (!Files.isReadable(directorio)) {
        System.out.println("ERROR: No tienes permisos para leer: " + directorio.toAbsolutePath());
    } else {
        List<Path> hijos = new ArrayList<>();

        try (DirectoryStream<Path> contenido = Files.newDirectoryStream(directorio)) {
            for (Path hijo : contenido) {
                hijos.add(hijo);
            }

            System.out.println("Directorio: " + directorio.toAbsolutePath());
            if (hijos.isEmpty()) {
                System.out.println("El directorio está vacío.");
            }
            listarHijos(hijos);

        } catch (AccessDeniedException e) {
            System.out.println("ERROR: Acceso denegado: " + directorio.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo leer el directorio: " + e.getMessage());
        }
    }
}

/**
 * Imprime cada elemento indicando si es fichero o directorio con su tamaño si es fichero y el total.
 *
 * @param hijos elementos del directorio
 * @throws IOException si no se puede leer el tamaño de algún fichero
 */
private void listarHijos(List<Path> hijos) throws IOException {
    int ficheros = 0;
    int directorios = 0;

    for (Path hijo : hijos) {
        if (Files.isDirectory(hijo)) {
            directorios++;
            System.out.printf("DIRECTORIO:  %s%n", hijo.getFileName());
        } else {
            ficheros++;
            System.out.printf("FICHERO:  %-30s %d bytes%n", hijo.getFileName(), Files.size(hijo));
        }
    }
    System.out.println("Total ficheros: " + ficheros + " | Directorios: " + directorios);
}


}
