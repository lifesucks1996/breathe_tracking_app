package com.example.breathe_tracking;

import androidx.lifecycle.MutableLiveData;

/**
 * Singleton "Tablón de Anuncios" para comunicar el Servicio (background) 
 * con la Actividad (UI) de forma segura.
 */
public class TrackingDataHolder {
    private static final TrackingDataHolder instance = new TrackingDataHolder();

    // LiveData para la ubicación (del GPS del móvil)
    public final MutableLiveData<String> locationData = new MutableLiveData<>();

    // LiveData para la hora de conexión (del Beacon)
    public final MutableLiveData<String> timeData = new MutableLiveData<>();

    // --- ¡NUEVOS LiveData para los datos del Beacon! ---
    public final MutableLiveData<Float> temperaturaData = new MutableLiveData<>();
    public final MutableLiveData<Float> ozonoData = new MutableLiveData<>();
    public final MutableLiveData<Integer> co2Data = new MutableLiveData<>();
    public final MutableLiveData<Integer> bateriaData = new MutableLiveData<>();

    private TrackingDataHolder() {}

    public static TrackingDataHolder getInstance() {
        return instance;
    }
}
