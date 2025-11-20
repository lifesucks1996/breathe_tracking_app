package com.example.breathe_tracking;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Clase modelo para subir las lecturas de los sensores a Firebase Firestore.
 * El campo 'fecha' es un Timestamp del servidor para el historial.
 */
public class LecturaSensor {
    public float O3;
    public float temperatura;
    public int co2;
    public int bateria;

    public String ubicacion;
    public String estado;

    @ServerTimestamp
    public Date fecha;

    public LecturaSensor() {}

    public LecturaSensor(float O3, float temperatura, int co2, int bateria, String ubicacion, String estado) {
        this.O3 = O3;
        this.temperatura = temperatura;
        this.co2 = co2;
        this.bateria = bateria;
        this.ubicacion = ubicacion;
        this.estado = estado;
    }
}
