/**
 * @file Utilidades.java
 * @brief Clase que proporciona métodos estáticos para la manipulación y conversión entre diferentes tipos de datos,
 * especialmente para la conversión entre cadenas, arrays de bytes, enteros, longs y UUIDs.
 * @package com.example.breathe_tracking
 */
package com.example.breathe_tracking;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------
/**
 * @class Utilidades
 * @brief Colección de métodos utilitarios estáticos para conversiones de tipos de bajo nivel.
 */
public class Utilidades {

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte una cadena de texto en un array de bytes.
     * @param texto La cadena de entrada.
     * @return El array de bytes que representa la cadena, usando la codificación por defecto del sistema.
     */
    public static byte[] stringToBytes ( String texto ) {
        return texto.getBytes();
        // byte[] b = string.getBytes(StandardCharsets.UTF_8); // Ja
    } // ()

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte una cadena de 16 caracteres en un objeto UUID.
     * @note La cadena se divide en dos partes de 8 caracteres (Más Significativo y Menos Significativo).
     * @param uuid La cadena de 16 caracteres a convertir.
     * @return El objeto UUID resultante.
     * @throws Error Si la cadena no tiene exactamente 16 caracteres.
     */
    public static UUID stringToUUID( String uuid ) {
        if ( uuid.length() != 16 ) {
            throw new Error( "stringUUID: string no tiene 16 caracteres ");
        }
        byte[] comoBytes = uuid.getBytes();

        String masSignificativo = uuid.substring(0, 8);
        String menosSignificativo = uuid.substring(8, 16);
        UUID res = new UUID( Utilidades.bytesToLong( masSignificativo.getBytes() ), Utilidades.bytesToLong( menosSignificativo.getBytes() ) );

        return res;
    } // ()

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte un objeto UUID en una cadena de caracteres.
     * @param uuid El objeto UUID de entrada.
     * @return La cadena de caracteres resultante de concatenar los bytes del UUID.
     */
    public static String uuidToString ( UUID uuid ) {
        return bytesToString( dosLongToBytes( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ) );
    } // ()

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte un objeto UUID en una cadena hexadecimal (formato con ':').
     * @param uuid El objeto UUID de entrada.
     * @return La cadena hexadecimal que representa el UUID.
     */
    public static String uuidToHexString ( UUID uuid ) {
        return bytesToHexString( dosLongToBytes( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ) );
    } // ()

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte un array de bytes en una cadena de texto.
     * @param bytes El array de bytes de entrada.
     * @return La cadena de texto resultante, tratando cada byte como un carácter.
     */
    public static String bytesToString( byte[] bytes ) {
        if (bytes == null ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append( (char) b );
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Combina dos valores 'long' (más significativos y menos significativos) en un único array de 16 bytes.
     * @param masSignificativos El valor long que representa la parte superior (8 bytes).
     * @param menosSignificativos El valor long que representa la parte inferior (8 bytes).
     * @return Un array de bytes de 16 posiciones.
     */
    public static byte[] dosLongToBytes( long masSignificativos, long menosSignificativos ) {
        ByteBuffer buffer = ByteBuffer.allocate( 2 * Long.BYTES );
        buffer.putLong( masSignificativos );
        buffer.putLong( menosSignificativos );
        return buffer.array();
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte un array de bytes en un valor entero (int) usando BigInteger.
     * @note Este método utiliza BigInteger para manejar la conversión, lo cual no es ideal para
     * lecturas de bajo nivel byte a byte o manejo de endianness específico.
     * @param bytes El array de bytes de entrada (máximo 4 bytes).
     * @return El valor int resultante.
     */
    public static int bytesToInt( byte[] bytes ) {
        return new BigInteger(bytes).intValue();
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte un array de bytes en un valor long usando BigInteger.
     * @note Similar a \ref bytesToInt, utiliza BigInteger para la conversión.
     * @param bytes El array de bytes de entrada (máximo 8 bytes).
     * @return El valor long resultante.
     */
    public static long bytesToLong( byte[] bytes ) {
        return new BigInteger(bytes).longValue();
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte un array de bytes en un valor entero (int) con lógica de desplazamiento de bits y manejo de signo (complemento a 2).
     * @note Este método implementa una conversión byte a byte con desplazamiento (Big Endian)
     * y una lógica de manejo de signo que parece ser una adaptación manual para enteros con signo.
     * @param bytes El array de bytes de entrada (máximo 4 bytes).
     * @return El valor int resultante.
     * @throws Error Si el array de bytes supera los 4 bytes.
     */
    public static int bytesToIntOK( byte[] bytes ) {
        if (bytes == null ) {
            return 0;
        }

        if ( bytes.length > 4 ) {
            throw new Error( "demasiados bytes para pasar a int ");
        }
        int res = 0;



        for( byte b : bytes ) {
            res =  (res << 8) // * 16
                    + (b & 0xFF); // para quedarse con 1 byte (2 cuartetos) de lo que haya en b
        } // for

        if ( (bytes[ 0 ] & 0x8) != 0 ) {
            // si tiene signo negativo (un 1 a la izquierda del primer byte
            res = -(~(byte)res)-1; // complemento a 2 (~) de res pero como byte, -1
        }
        return res;
    } // ()

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * @brief Convierte un array de bytes en una cadena hexadecimal, con los bytes separados por dos puntos (':').
     * @param bytes El array de bytes de entrada.
     * @return La cadena hexadecimal formateada (ej: "aa:bb:cc:dd:").
     */
    public static String bytesToHexString( byte[] bytes ) {

        if (bytes == null ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
            sb.append(':');
        }
        return sb.toString();
    } // ()

}