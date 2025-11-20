package com.example.breathe_tracking;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

// Imports de Bluetooth
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

// Imports de Ubicación
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

// Imports de Utilidades
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

// Imports de Firebase
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase para gestionar el servicio principal en segundo plano (Foreground Service).
 * Esta clase es el motor de la aplicación y tiene múltiples responsabilidades:
 * 1.  **Escaneo Bluetooth (BLE):** Inicializa y mantiene un escaneo constante para el dispositivo beacon ("rocio").
 * 2.  **Decodificación de Trama:** Parsea la trama de datos personalizada del beacon para extraer mediciones (O3, Temperatura, CO2, Batería).
 * 3.  **Seguimiento de Ubicación:** Obtiene la ubicación GPS actual del teléfono usando FusedLocationProviderClient.
 * 4.  **Publicación de Datos (LiveData):** Actualiza el TrackingDataHolder (el "tablón de anuncios") con los nuevos datos de ubicación y mediciones para que la SesionSensorActivity pueda mostrarlos en tiempo real.
 * 5.  **Lógica de Alertas:** Compara las mediciones recibidas con umbrales predefinidos (ej. CO2 > 1200) y envía notificaciones de alta prioridad al usuario si se superan.
 * 6.  **Vigilante de Conexión (Watchdog):** Mantiene un temporizador que se reinicia con cada beacon recibido. Si pasan 3 minutos sin datos, asume que el sensor está desconectado y notifica al usuario.
 * 7.  **Subida de datos en tiempo real a Firebase
 */

public class SensorTrackingService extends Service {

    // --- Constantes de Configuración ---
    private static final String ETIQUETA_LOG = "SensorService";
    private static final String CHANNEL_ID = "SensorTrackingChannel";
    private static final String ALERT_CHANNEL_ID = "AlertChannel";

    private static final int NOTIFICATION_ID = 1;

    // IDs únicos para cada tipo de alerta
    private static final int CO2_ALERT_ID = 101;
    private static final int OZONE_ALERT_ID = 102;
    private static final int TEMP_ALERT_ID = 103;
    private static final int BATTERY_ALERT_ID = 104;
    private static final int CONNECTION_ALERT_ID = 105;

    // --- Módulos Principales ---
    private FusedLocationProviderClient fusedLocationClient; // Cliente para servicios de ubicación de Google
    private LocationCallback locationCallback; // "Oyente" para cuando llega una nueva ubicación
    private BluetoothLeScanner elEscanner; // Objeto de Android para escanear Bluetooth LE
    private ScanCallback callbackDelEscaneo; // "Oyente" para cuando se detecta un beacon
    private TrackingDataHolder dataHolder; // Nuestro "tablón de anuncios" (Singleton) para comunicar con la UI

    // Vigilante de Conexión
    private Handler watchdogHandler = new Handler(Looper.getMainLooper()); // Handler para programar tareas
    private Runnable watchdogRunnable; // La tarea (Runnable) que se ejecutará si se pierde la conexión
    private static final long WATCHDOG_DELAY_MS = 3 * 60 * 1000; // 3 minutos

    // Memoria para los últimos valores que hemos recibido para evitar actualizaciones innecesarias si no ha cambiado el valor
    private float lastUpdatedTemp = -999.0f;
    private float lastUpdatedOzono = -999.0f;
    private int lastUpdatedCo2 = -999;

    // Conexión y referncias de firebase
    private FirebaseFirestore db;
    private String sensorCode;
    private DocumentReference sensorDocRef;
    private static final String SENSOR_DOCUMENT_ID = "12345";


    // --- onCreate --------------------------------------------------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();
        // Configuración de los servicios de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dataHolder = TrackingDataHolder.getInstance();
        // Configuración de los canales de notificaciones
        createNotificationChannels();

        // Incializamos la base de datos de firebase
        db = FirebaseFirestore.getInstance();


        //Mostrar nueva ubicacion si cambia
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    // Convierte las coordenadas (Lat/Lon) en una dirección (Calle, Ciudad)
                    if (location != null) getAddressFromLocation(location);
                }
            }
        };

        //Tareas que ejecuta el observador cuando se desactiva o se pierde la conexión
        watchdogRunnable = () -> {
            Log.e(ETIQUETA_LOG, "¡No se han recibido datos del sensor en 3 minutos!");
            // Mostrar que el sensor se ha desconectado
            dataHolder.estadoData.postValue("Desconectado");
            //Guardamos la hora de desconexion para mostrar la alerta
            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            String message = currentTime + " - El sensor no está funcionando correctamente";
            //Muestra la alerta en el tablón de incidencias (hora y mensaje)
            dataHolder.incidenciaData.postValue(message);
            //Envia notificacion sobre la alerta
            sendAlertNotification("Alerta de Conexión", "El sensor no está funcionando correctamente", CONNECTION_ALERT_ID);

            // Logica de firestor para actualizacion del estado del sensor
            // Creamos un Mapa con solo los campos que queremos modificar: estado y timestamp.
            Map<String, Object> desconexionData = new HashMap<>();
            desconexionData.put("estado", "Desconectado");
            desconexionData.put("ultima_conexion", FieldValue.serverTimestamp());

            // Usamos SET con MERGE: Esto garantiza que los campos de medición no se borren
            sensorDocRef.set(desconexionData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(ETIQUETA_LOG, "Estado de Desconexión subido a Firestore."))
                    .addOnFailureListener(e -> Log.e(ETIQUETA_LOG, "Fallo al subir estado de desconexión: " + e.getMessage()));


        };
    }
    // --- Fin onCreate ----------------------------------------------------------------------------------

    // --- onStarCommand ---------------------------------------------------------------------------------
    //Se llama cada vez que la Activity (SesionSensorActivity) llama a startForegroundService().
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // se llama a la notificacion que nos muestra que el servicio esta en segundo plano
        // Crea la intención para que al tocar la notificación se abra sesionSensor
        Intent notificationIntent = new Intent(this, SesionSensorActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Breathe Traking")
                .setContentText("Monitorizando el sensor en tiempo real.")
                .setSmallIcon(R.drawable.logo_app)
                .setContentIntent(pendingIntent) 
                .build();

        startForeground(NOTIFICATION_ID, notification);
        startLocationUpdates();
        inicializarYComenzarEscaneoBeacon();
        // Inicializamos al vigilante de conexión
        resetWatchdog();

        // Asignar el código de sensor fijo
        sensorCode = SENSOR_DOCUMENT_ID;
        // Inicializar la referencia de Firestore
        sensorDocRef = db.collection("sensores").document(sensorCode);

        return START_STICKY;
    }
    // --- Fin onStarCommand -----------------------------------------------------------------------------


    //--- onDestory ------------------------------------------------------------------------------------
    // Se llama cuando la Activity (SesionSensorActivity) se destruye
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        detenerEscaneoBeacon();
        watchdogHandler.removeCallbacks(watchdogRunnable);
    }
    // --- Fin onDestroy -------------------------------------------------------------------------------

    /**
     * Metodos para recibir, filtrar y decodificar el  Beacon
     */
    // --- Escaneo beacon ------------------------------------------------------------------------------
    private void inicializarYComenzarEscaneoBeacon() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null || !bta.isEnabled()) {
            stopSelf();
            return;
        }
        this.elEscanner = bta.getBluetoothLeScanner();
        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                if (resultado != null) mostrarInformacionDispositivoBTLE(resultado);
            }
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            this.elEscanner.startScan(null, settings, this.callbackDelEscaneo);
        }
    }

    // --- fin escaneo beacon -------------------------------------------------------------------------

    // --- detener escaner de beacon ------------------------------------------------------------------
    private void detenerEscaneoBeacon() {
        if (elEscanner != null && callbackDelEscaneo != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            this.elEscanner.stopScan(this.callbackDelEscaneo);
        }
    }

    // --- fin detener escaner de beacon --------------------------------------------------------------


    // --- mostrar la informacion del beacon -----------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        // Comprobamos que el dispositivo es el correcto
        BluetoothDevice device = resultado.getDevice();
        if (device == null || device.getName() == null || !device.getName().equals("rocio")) return;

        // Reiniciamos el watchdog
        resetWatchdog();

        // Parseamos la trama
        ScanRecord scanRecord = resultado.getScanRecord();
        if (scanRecord == null) return;

        // Extraemos los datos del beacon
        byte[] payload = scanRecord.getManufacturerSpecificData(0x004C);
        if (payload == null || payload.length != 9 || payload[0] != (byte) 0xAA) return;

        // Parseamos los datos del beacon
        float o3_ppm = ((payload[2] & 0xFF) << 8 | (payload[1] & 0xFF)) / 1000.0f;
        float temperatura_c = ((payload[4] & 0xFF) << 8 | (payload[3] & 0xFF)) / 10.0f;
        int co2_ppm = (payload[6] & 0xFF) << 8 | (payload[5] & 0xFF);
        int bat_porc = (payload[8] & 0xFF) << 8 | (payload[7] & 0xFF);

        // Comprobar si los valores de medición han cambiado.
        if (temperatura_c == lastUpdatedTemp && o3_ppm == lastUpdatedOzono && co2_ppm == lastUpdatedCo2) {
            dataHolder.bateriaData.postValue(bat_porc); // Actualizamos la batería siempre
            return; // Si los datos principales no cambian, salimos.
        }

        // Si los datos SÍ han cambiado, actualizamos la memoria y procedemos.
        Log.i(ETIQUETA_LOG, "¡Nuevos datos de sensor detectados! Actualizando UI y hora.");
        lastUpdatedTemp = temperatura_c;
        lastUpdatedOzono = o3_ppm;
        lastUpdatedCo2 = co2_ppm;

        // Comprobamos las alertas
        checkAlerts(co2_ppm, o3_ppm, temperatura_c, bat_porc);

        // Actualizamos la UI con los nuevos datos
        // Obtenemos la hora actual para saber la ultima actualizacion de datos
        String receptionTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        dataHolder.timeData.postValue("Última conex. " + receptionTime);
        dataHolder.ozonoData.postValue(o3_ppm);
        dataHolder.temperaturaData.postValue(temperatura_c);
        dataHolder.co2Data.postValue(co2_ppm);
        dataHolder.bateriaData.postValue(bat_porc);

        // Lógica de subida a firestore
        // Obtenemos los valores de contexto (que se actualizaron en los otros hilos/callbacks)
        String ubicacion = dataHolder.locationData.getValue();
        String estado = dataHolder.estadoData.getValue();

        // Llamamos a la función de subida. Pasamos los 6 argumentos.
        if (ubicacion != null && estado != null) {
            subirDatosAFirebase(o3_ppm, temperatura_c, co2_ppm, bat_porc, ubicacion, estado);
        }
        // Fin lógica

    }
    //--- fin mostrar la informacion del beacon -------------------------------------------------------


    //--- Alertas sobre medidas -----------------------------------------------------------------------
    private void checkAlerts(int co2, float ozono, float temperatura, int bateria) {
        List<String> currentAlertMessages = new ArrayList<>();
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        // SI las medidas entran en un rango determinado se mostrara una alerta en el tablón de alertas y en notificaciones
        if (co2 >= 1200) {
            String message = currentTime + " - Nivel de CO2 elevado: " + co2 + " ppm";
            currentAlertMessages.add(message);
            sendAlertNotification("Alerta de CO2", "Nivel de CO2 elevado: " + co2 + " ppm", CO2_ALERT_ID);
        } else {
            cancelAlertNotification(CO2_ALERT_ID);
        }

        if (ozono >= 0.9) {
            String message = currentTime + " - Nivel de Ozono elevado: " + String.format(Locale.getDefault(), "%.3f ppm", ozono);
            currentAlertMessages.add(message);
            sendAlertNotification("Alerta de Ozono", "Nivel de Ozono elevado: " + String.format(Locale.getDefault(), "%.3f ppm", ozono), OZONE_ALERT_ID);
        } else {
            cancelAlertNotification(OZONE_ALERT_ID);
        }

        if (temperatura > 35) {
            String message = currentTime + " - Temperatura elevada: " + String.format(Locale.getDefault(), "%.1f ºC", temperatura);
            currentAlertMessages.add(message);
            sendAlertNotification("Alerta de Temperatura", "Temperatura elevada: " + String.format(Locale.getDefault(), "%.1f ºC", temperatura), TEMP_ALERT_ID);
        } else {
            cancelAlertNotification(TEMP_ALERT_ID);
        }

        // Si la bateria es baja solo se muestra la notificacion, no la alerta
        if (bateria <= 15) {
            sendAlertNotification("Alerta de Batería", "Nivel de batería bajo: " + bateria + "%", BATTERY_ALERT_ID);
        } else {
            cancelAlertNotification(BATTERY_ALERT_ID);
        }

        // Si no hay alertas ni incidencias mostramos: Sin alertas/incidencias
        if (currentAlertMessages.isEmpty()) {
            dataHolder.alertData.postValue("Sin alertas");
        } else {
            StringJoiner joiner = new StringJoiner("\n\n");
            for (String msg : currentAlertMessages) {
                joiner.add(msg);
            }
            dataHolder.alertData.postValue(joiner.toString());
        }

        if ("Conectado".equals(dataHolder.estadoData.getValue())) {
            dataHolder.incidenciaData.postValue("Sin incidencias");
        }
    }
    // --- fin alertas sobre medidas ---------------------------------------------------------------------------------


    // --- Notificaciones -------------------------------------------------------------------------------
    // Crea una notificacion de alerta
    private void sendAlertNotification(String title, String message, int notificationId) {
        Intent notificationIntent = new Intent(this, SesionSensorActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.logo_app)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build();
        getSystemService(NotificationManager.class).notify(notificationId, n);
    }

    // Elimina una notificacion si ya no existe
    private void cancelAlertNotification(int notificationId) {
        getSystemService(NotificationManager.class).cancel(notificationId);
    }

    // Crea los canales de notificaciones
    private void createNotificationChannels() {
        NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Sensor Tracking", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);

        NotificationChannel alertChannel = new NotificationChannel(ALERT_CHANNEL_ID, "Alertas de Sensor", NotificationManager.IMPORTANCE_HIGH);
        getSystemService(NotificationManager.class).createNotificationChannel(alertChannel);
    }
    // --- fin notificaciones ---------------------------------------------------------------------------

    //--- metodos localizacion --------------------------------------------------------------------------
    // Obtiene la ubicación actual del teléfono
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    // Convierte las coordenadas (Lat/Lon) en una dirección (Calle, Ciudad)
    private void getAddressFromLocation(android.location.Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String street = addresses.get(0).getThoroughfare();
                String city = addresses.get(0).getLocality();
                dataHolder.locationData.postValue((street != null ? street : "") + ", " + (city != null ? city : ""));
            }
        } catch (IOException e) {
            Log.e(ETIQUETA_LOG, "Error Geocoder", e);
        }
    }
    // --- fin localizacion ----------------------------------------------------------------------------


    // --- Vigilante de Conexión -----------------------------------------------------------------------
    // cuenta 3 minutos para saber si el sensor está desconectado, si recibe datos lo reinicia para volver a contar los 3 minutos
    // SI no recibe los datos en 3 minutos se muestra una alerta y se reinicia el watchdog
    private void resetWatchdog() {
        watchdogHandler.removeCallbacks(watchdogRunnable);
        watchdogHandler.postDelayed(watchdogRunnable, WATCHDOG_DELAY_MS);

        if (!"Conectado".equals(dataHolder.estadoData.getValue())) {
            Log.i(ETIQUETA_LOG, "¡Reconexión con el sensor detectada!");
            cancelAlertNotification(CONNECTION_ALERT_ID);
            dataHolder.incidenciaData.postValue("Sin incidencias");
        }
        dataHolder.estadoData.postValue("Conectado");
    }
    // --- fin vigilante de conexión -------------------------------------------------------------------
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // --- Inicio subirDatosFirebase --------------------------------------------------------------------------------------------------
    public void subirDatosAFirebase(Float o3_ppm, Float temp_c, Integer co2_ppm, Integer bat_porc, String ubicacion, String estado) {

        // Verficar que esten todos los datos necesarios para subir a la bbdd
        if (o3_ppm == null || temp_c == null || co2_ppm == null || bat_porc == null || ubicacion == null || estado == null) {
            Log.w("Firestore", "Intento de subida fallido: LiveData incompleto.");
            return;
        }

        LecturaSensor nuevaLectura = new LecturaSensor(o3_ppm, temp_c, co2_ppm, bat_porc, ubicacion, estado);

        sensorDocRef.collection("mediciones").add(nuevaLectura) // Se sube el objeto completo
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Historial guardado.");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al escribir en el historial", e);
                });


        // Subida a Campos Directos (Última Lectura)

        Map<String, Object> camposDirectos = new HashMap<>();

        // Datos de la Lectura
        camposDirectos.put("ozono", o3_ppm);
        camposDirectos.put("temperatura", temp_c);
        camposDirectos.put("co2", co2_ppm);
        camposDirectos.put("bateria", bat_porc);

        // Metadatos (Campos añadidos)
        camposDirectos.put("ubicacion", ubicacion);
        camposDirectos.put("estado", estado);
        camposDirectos.put("ultima_conexion", FieldValue.serverTimestamp());

        // Usamos SET con MERGE
        sensorDocRef.set(camposDirectos, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Campos directos del sensor actualizados."))
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al actualizar campos directos: " + e.getMessage());
                });
    }

    // --- Fin subirDatosFirebase --------------------------------------------------------------------------------------------------


}
