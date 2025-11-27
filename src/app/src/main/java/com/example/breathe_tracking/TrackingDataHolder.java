/**
 * @file TrackingDataHolder.java
 * @brief Implementa un Singleton para actuar como repositorio central de datos en tiempo real mediante LiveData.
 * @package com.example.breathe_tracking
 */
package com.example.breathe_tracking;

import androidx.lifecycle.MutableLiveData;

/**
 * @class TrackingDataHolder
 * @brief "Tablón de Anuncios" de la aplicación implementado como patrón Singleton.
 *
 * Su función principal es servir de puente seguro y reactivo para la comunicación de datos
 * entre el servicio de fondo (\ref SensorTrackingService) y la interfaz de usuario (\ref SesionSensorActivity).
 *
 * Utiliza \ref MutableLiveData para que la Activity pueda observar automáticamente los cambios
 * en los datos del sensor y actualizar la UI de manera eficiente y segura (thread-safe).
 */
public class TrackingDataHolder {
    /** @brief Instancia única y estática de la clase (Singleton). */
    private static final TrackingDataHolder instance = new TrackingDataHolder();

    // LiveData para datos principales
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

    // LiveData para el estado de conexión
    /** @brief Contiene el estado de conexión del sensor ("Conectado" o "Desconectado"). */
    public final MutableLiveData<String> estadoData = new MutableLiveData<>();

    // LiveData para Alertas e Incidencias
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
     * @return La instancia Singleton de TrackingDataHolder.
     */
    public static TrackingDataHolder getInstance() {
        return instance;
    }
}
