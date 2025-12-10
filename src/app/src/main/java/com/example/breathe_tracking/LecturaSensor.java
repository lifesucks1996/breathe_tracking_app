/**
 * @file LecturaSensor.java
 * @brief Modelo de datos (POJO) para almacenar una lectura individual del sensor en la colección de historial de Firebase Firestore.
 * @package com.example.breathe_tracking
 */
package com.example.breathe_tracking;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * @class LecturaSensor
 * @brief Clase modelo para subir las lecturas de los sensores a Firebase Firestore.
 *
 * Copyrigth © 2025
 *
 * Esta clase define la estructura de datos que se guarda como documento dentro de la subcolección
 * "mediciones" de cada sensor en Firestore, utilizada para mantener un historial de datos.
 *
 * @note El campo 'fecha' está anotado con @ServerTimestamp para asegurar que el servidor de Firebase
 * registre la hora exacta de la subida, garantizando precisión en el historial.
 */
public class LecturaSensor {
    /** @brief Nivel de Ozono (O3) medido, en partes por millón (ppm). */
    public float O3;
    /** @brief Temperatura medida, en grados Celsius. */
    public float temperatura;
    /** @brief Nivel de Dióxido de Carbono (CO2) medido, en partes por millón (ppm). */
    public int co2;
    /** @brief Porcentaje de batería del sensor. */
    public int bateria;
    /** @brief Ubicación geográfica del dispositivo móvil en el momento de la lectura (dirección legible). */
    public String ubicacion;
    /** @brief Estado de conexión del sensor ("Conectado" o "Desconectado") en el momento de la lectura. */
    public String estado;

    /**
     * @brief Timestamp del servidor que registra la hora exacta en la que la lectura fue subida a Firestore.
     * @see com.google.firebase.firestore.ServerTimestamp
     */
    @ServerTimestamp
    public Date fecha;

    /**
     * @brief Constructor vacío requerido por Firebase para la deserialización automática.
     */
    public LecturaSensor() {}

    /**
     * @brief Constructor con todos los campos de datos y contexto.
     * (O3:float, temperatura:float, co2:int, bateria:int, ubicacion:String, estado:String) -> LecturaSensor() -> ()
     * @param O3 Nivel de ozono.
     * @param temperatura Nivel de temperatura.
     * @param co2 Nivel de dióxido de carbono.
     * @param bateria Porcentaje de batería.
     * @param ubicacion Dirección actual del dispositivo.
     * @param estado Estado de conexión del sensor.
     */
    public LecturaSensor(float O3, float temperatura, int co2, int bateria, String ubicacion, String estado) {
        this.O3 = O3;
        this.temperatura = temperatura;
        this.co2 = co2;
        this.bateria = bateria;
        this.ubicacion = ubicacion;
        this.estado = estado;
        // El campo 'fecha' se inicializa automáticamente por Firestore al subir el objeto.
    }
}
