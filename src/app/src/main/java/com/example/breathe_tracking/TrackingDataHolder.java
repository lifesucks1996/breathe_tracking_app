package com.example.breathe_tracking;

import androidx.lifecycle.MutableLiveData;

public class TrackingDataHolder {
    private static final TrackingDataHolder instance = new TrackingDataHolder();

    // LiveData para datos principales
    public final MutableLiveData<String> locationData = new MutableLiveData<>();
    public final MutableLiveData<String> timeData = new MutableLiveData<>();
    public final MutableLiveData<Float> temperaturaData = new MutableLiveData<>();
    public final MutableLiveData<Float> ozonoData = new MutableLiveData<>();
    public final MutableLiveData<Integer> co2Data = new MutableLiveData<>();
    public final MutableLiveData<Integer> bateriaData = new MutableLiveData<>();

    // LiveData para el estado de conexión
    public final MutableLiveData<String> estadoData = new MutableLiveData<>();

    // LiveData para Alertas e Incidencias
    public final MutableLiveData<String> alertData = new MutableLiveData<>();
    public final MutableLiveData<String> incidenciaData = new MutableLiveData<>();

    private TrackingDataHolder() {}

    public static TrackingDataHolder getInstance() {
        return instance;
    }
}
