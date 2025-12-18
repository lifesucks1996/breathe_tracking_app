/**
 * @file TrackingDataHolder.java
 * @brief Repositorio central de datos en tiempo real implementado como Singleton.
 * @package com.example.breathe_tracking
 * @copyright Copyright © 2025
 */
package com.example.breathe_tracking;

import androidx.lifecycle.MutableLiveData;

/**
 * @class TrackingDataHolder
 * @brief "Tablón de Anuncios" reactivo de la aplicación (Singleton + LiveData).
 *
 * @details
 * Esta clase implementa un almacén de datos centralizado que desacopla el productor de datos
 * (@ref SensorTrackingService) del consumidor (@ref SesionSensorActivity).
 *
 * **Arquitectura:**
 * Actúa como un bus de eventos en tiempo real. Al usar `MutableLiveData`, garantiza que:
 * 1. La UI siempre observe el último dato disponible.
 * 2. No se produzcan fugas de memoria (Memory Leaks) si la actividad se destruye.
 * 3. La actualización de la interfaz sea segura respecto a los hilos (Thread-safe) usando `.postValue()`.
 *
 * @author Sandra (Arquitectura LiveData - 29/10/2025)
 */

public class TrackingDataHolder {
    /** @brief Instancia única y estática de la clase (Singleton). */
    private static final TrackingDataHolder instance = new TrackingDataHolder();

    //----- LiveData para datos principales --------------
    /** @brief Contiene la ubicación actual del dispositivo (ej: "Calle Falsa 123, Ciudad"). */
    public final MutableLiveData<String> locationData = new MutableLiveData<>();
    /** @brief Contiene la hora de la última recepción de datos del sensor (ej: "Última conex. 14:30"). */
    public final MutableLiveData<String> timeData = new MutableLiveData<>();
    /** @brief Contiene la lectura de la temperatura en grados Celsius. */
    public final MutableLiveData<Float> temperaturaData = new MutableLiveData<>();
    /** @brief Contiene la lectura del nivel de Ozono (O3) en ppm. */
    public final MutableLiveData<Float> ozonoData = new MutableLiveData<>();
    /** @brief Contiene la lectura de la concentración de Dióxido de Carbono (CO2) en ppm. */
    public final MutableLiveData<Integer> co2Data = new MutableLiveData<>();
    /** @brief Contiene el porcentaje de batería del sensor. */
    public final MutableLiveData<Integer> bateriaData = new MutableLiveData<>();
    /** @brief Contiene la intensidad de la señal recibida (RSSI) en dBm. */
    public final MutableLiveData<Integer> rssiData = new MutableLiveData<>();

    // ---- LiveData para el estado de conexión ---------------------------
    /** @brief Contiene el estado de conexión del sensor ("Conectado" o "Desconectado"). */
    public final MutableLiveData<String> estadoData = new MutableLiveData<>();

    // ---- LiveData para Alertas e Incidencias ----------------------------
    /** @brief Contiene una cadena con los mensajes de alertas de mediciones (CO2, O3, Temp) que superan umbrales. */
    public final MutableLiveData<String> alertData = new MutableLiveData<>();
    /** @brief Contiene un mensaje sobre incidencias (principalmente la pérdida de conexión del sensor). */
    public final MutableLiveData<String> incidenciaData = new MutableLiveData<>();

    /**
     * @brief Constructor privado para forzar el patrón Singleton.
     */
    private TrackingDataHolder() {}

    /**
     * @brief Proporciona la única instancia accesible de la clase.
     *  () -> getInstance() -> (instance:TrackingDataHolder)
     *
     * @return La instancia Singleton de TrackingDataHolder.
     */
    public static TrackingDataHolder getInstance() {
        return instance;
    }
}