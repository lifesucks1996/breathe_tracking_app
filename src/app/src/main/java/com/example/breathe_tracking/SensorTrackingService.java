package com.example.breathe_tracking;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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

public class SensorTrackingService extends Service {

    private static final String ETIQUETA_LOG = "SensorService";
    private static final String CHANNEL_ID = "SensorTrackingChannel";
    private static final String ALERT_CHANNEL_ID = "AlertChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int CO2_ALERT_ID = 101;
    private static final int OZONE_ALERT_ID = 102;
    private static final int TEMP_ALERT_ID = 103;
    private static final int BATTERY_ALERT_ID = 104;
    private static final int CONNECTION_ALERT_ID = 105;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo;
    private TrackingDataHolder dataHolder;

    // Lógica de Vigilancia (Watchdog)
    private Handler watchdogHandler = new Handler(Looper.getMainLooper());
    private Runnable watchdogRunnable;
    private static final long WATCHDOG_DELAY_MS = 3 * 60 * 1000; // 3 minutos

    // Memoria para los últimos valores que SÍ causaron una actualización
    private float lastUpdatedTemp = -999.0f;
    private float lastUpdatedOzono = -999.0f;
    private int lastUpdatedCo2 = -999;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dataHolder = TrackingDataHolder.getInstance();
        createNotificationChannels();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) getAddressFromLocation(location);
                }
            }
        };

        watchdogRunnable = () -> {
            Log.e(ETIQUETA_LOG, "¡No se han recibido datos del sensor en 3 minutos!");
            dataHolder.estadoData.postValue("Desconectado");
            dataHolder.alertData.postValue("El sensor esta desconectado o no esta funcionando correctamente");
            dataHolder.alertTimeData.postValue(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            sendAlertNotification("Alerta de Conexión", "El sensor no está funcionando correctamente", CONNECTION_ALERT_ID);
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Breathe Traking").setContentText("Monitorizando el sensor en tiempo real.").setSmallIcon(R.drawable.logo_app).build();
        startForeground(NOTIFICATION_ID, notification);
        startLocationUpdates();
        inicializarYComenzarEscaneoBeacon();
        // Iniciamos el vigilante aquí para que empiece la cuenta atrás desde el principio
        resetWatchdog(); 
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        detenerEscaneoBeacon();
        watchdogHandler.removeCallbacks(watchdogRunnable);
    }

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

    private void detenerEscaneoBeacon() {
        if (elEscanner != null && callbackDelEscaneo != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            this.elEscanner.stopScan(this.callbackDelEscaneo);
        }
    }

    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        BluetoothDevice device = resultado.getDevice();
        if (device == null || device.getName() == null || !device.getName().equals("rocio")) return;
        
        resetWatchdog();

        ScanRecord scanRecord = resultado.getScanRecord();
        if (scanRecord == null) return;

        byte[] payload = scanRecord.getManufacturerSpecificData(0x004C);
        if (payload == null || payload.length != 9 || payload[0] != (byte) 0xAA) return;

        float o3_ppm = ((payload[2] & 0xFF) << 8 | (payload[1] & 0xFF)) / 1000.0f;
        float temperatura_c = ((payload[4] & 0xFF) << 8 | (payload[3] & 0xFF)) / 10.0f;
        int co2_ppm = (payload[6] & 0xFF) << 8 | (payload[5] & 0xFF);
        int bat_porc = (payload[8] & 0xFF) << 8 | (payload[7] & 0xFF);

        // ¡LÓGICA CLAVE! Comprobar si los valores de medición han cambiado.
        if (temperatura_c == lastUpdatedTemp && o3_ppm == lastUpdatedOzono && co2_ppm == lastUpdatedCo2) {
            dataHolder.bateriaData.postValue(bat_porc); // Actualizamos la batería siempre
            return; // Si los datos principales no cambian, salimos.
        }

        // Si los datos SÍ han cambiado, actualizamos la memoria y procedemos.
        Log.i(ETIQUETA_LOG, "¡Nuevos datos de sensor detectados! Actualizando UI y hora.");
        lastUpdatedTemp = temperatura_c;
        lastUpdatedOzono = o3_ppm;
        lastUpdatedCo2 = co2_ppm;
        
        checkAlerts(co2_ppm, o3_ppm, temperatura_c, bat_porc);

        String receptionTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        dataHolder.timeData.postValue("Última conex. " + receptionTime);
        dataHolder.ozonoData.postValue(o3_ppm);
        dataHolder.temperaturaData.postValue(temperatura_c);
        dataHolder.co2Data.postValue(co2_ppm);
        dataHolder.bateriaData.postValue(bat_porc);
    }
    
    private void checkAlerts(int co2, float ozono, float temperatura, int bateria) {
        List<String> currentAlertMessages = new ArrayList<>();

        if (co2 >= 1200) {
            currentAlertMessages.add("Nivel de CO2 elevado: " + co2 + " ppm");
            sendAlertNotification("Alerta de CO2", "Nivel de CO2 elevado: " + co2 + " ppm", CO2_ALERT_ID);
        } else {
            cancelAlertNotification(CO2_ALERT_ID);
        }

        if (ozono >= 0.9) {
            currentAlertMessages.add("Nivel de Ozono elevado: " + String.format(Locale.getDefault(), "%.3f ppm", ozono));
            sendAlertNotification("Alerta de Ozono", "Nivel de Ozono elevado: " + String.format(Locale.getDefault(), "%.3f ppm", ozono), OZONE_ALERT_ID);
        } else {
            cancelAlertNotification(OZONE_ALERT_ID);
        }

        if (temperatura > 35) {
            currentAlertMessages.add("Temperatura elevada: " + String.format(Locale.getDefault(), "%.1f ºC", temperatura));
            sendAlertNotification("Alerta de Temperatura", "Temperatura elevada: " + String.format(Locale.getDefault(), "%.1f ºC", temperatura), TEMP_ALERT_ID);
        } else {
            cancelAlertNotification(TEMP_ALERT_ID);
        }

        if (bateria <= 15) {
            sendAlertNotification("Alerta de Batería", "Nivel de batería bajo: " + bateria + "%", BATTERY_ALERT_ID);
        } else {
            cancelAlertNotification(BATTERY_ALERT_ID);
        }
        
        if (dataHolder.estadoData.getValue() == null || "Conectado".equals(dataHolder.estadoData.getValue())) {
             if (currentAlertMessages.isEmpty()) {
                dataHolder.alertData.postValue("Sin alertas");
                dataHolder.alertTimeData.postValue("");
            } else {
                StringJoiner joiner = new StringJoiner("\n"); 
                for (String msg : currentAlertMessages) {
                    joiner.add(msg);
                }
                dataHolder.alertData.postValue(joiner.toString());
                dataHolder.alertTimeData.postValue(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            }
        }
    }
    
    private void sendAlertNotification(String title, String message, int notificationId) {
        Notification n = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID).setContentTitle(title).setContentText(message).setSmallIcon(R.drawable.logo_app).setPriority(NotificationCompat.PRIORITY_HIGH).build();
        getSystemService(NotificationManager.class).notify(notificationId, n);
    }

    private void cancelAlertNotification(int notificationId) {
        getSystemService(NotificationManager.class).cancel(notificationId);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

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

    private void createNotificationChannels() {
        NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Sensor Tracking", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        
        NotificationChannel alertChannel = new NotificationChannel(ALERT_CHANNEL_ID, "Alertas de Sensor", NotificationManager.IMPORTANCE_HIGH);
        getSystemService(NotificationManager.class).createNotificationChannel(alertChannel);
    }

    private void resetWatchdog() {
        watchdogHandler.removeCallbacks(watchdogRunnable);
        watchdogHandler.postDelayed(watchdogRunnable, WATCHDOG_DELAY_MS);

        if (!"Conectado".equals(dataHolder.estadoData.getValue())) {
            Log.i(ETIQUETA_LOG, "¡Reconexión con el sensor detectada!");
            cancelAlertNotification(CONNECTION_ALERT_ID);
            dataHolder.alertData.postValue("Sin alertas");
            dataHolder.alertTimeData.postValue("");
        }
        dataHolder.estadoData.postValue("Conectado");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
