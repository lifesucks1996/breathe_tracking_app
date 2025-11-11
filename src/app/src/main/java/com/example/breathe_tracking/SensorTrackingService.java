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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// (Opcional) Import para Firebase
// import com.google.firebase.firestore.FirebaseFirestore;

public class SensorTrackingService extends Service {

    private static final String ETIQUETA_LOG = "SensorService";
    private static final String CHANNEL_ID = "SensorTrackingChannel";
    private static final int NOTIFICATION_ID = 1;

    // Módulos de Tareas
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo;

    // Módulos de Datos
    private TrackingDataHolder dataHolder;
    // private FirebaseFirestore db; // Descomentar cuando integres Firebase

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dataHolder = TrackingDataHolder.getInstance();
        // db = FirebaseFirestore.getInstance(); // Descomentar cuando integres Firebase
        createNotificationChannel();

        // Callback para la ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) getAddressFromLocation(location);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Breathe Traking")
                .setContentText("Monitorizando el sensor en tiempo real.")
                .setSmallIcon(R.drawable.logo_app)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        startLocationUpdates();
        inicializarYComenzarEscaneoBeacon();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        detenerEscaneoBeacon();
    }

    // --- LÓGICA DE BEACON (TU CÓDIGO INTEGRADO) ---
    private void inicializarYComenzarEscaneoBeacon() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null || !bta.isEnabled()) {
            Log.e(ETIQUETA_LOG, "Bluetooth no está disponible o activado.");
            stopSelf();
            return;
        }

        this.elEscanner = bta.getBluetoothLeScanner();
        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                if (resultado == null) return;
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(ETIQUETA_LOG, "Fallo en el escaneo de BT: " + errorCode);
            }
        };

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            Log.i(ETIQUETA_LOG, ">>> Escaneo constante de Beacon iniciado <<<");
            this.elEscanner.startScan(null, settings, this.callbackDelEscaneo);
        }
    }

    private void detenerEscaneoBeacon() {
        if (elEscanner != null && callbackDelEscaneo != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            Log.i(ETIQUETA_LOG, ">>> Escaneo de Beacon detenido <<<");
            this.elEscanner.stopScan(this.callbackDelEscaneo);
        }
    }

    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        BluetoothDevice device = resultado.getDevice();
        String nombreDispositivo = device.getName();

        if (nombreDispositivo == null || !nombreDispositivo.equals("rocio")) return;

        ScanRecord scanRecord = resultado.getScanRecord();
        if (scanRecord == null) return;

        SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
        final int MANUFACTURER_ID_ESPERADO = 0x004C;
        byte[] payload = manufacturerData.get(MANUFACTURER_ID_ESPERADO);

        if (payload == null || payload.length != 9 || payload[0] != (byte) 0xAA) return;

        // Decodificación
        float o3_ppm = ((payload[2] & 0xFF) << 8 | (payload[1] & 0xFF)) / 1000.0f;
        float temperatura_c = ((payload[4] & 0xFF) << 8 | (payload[3] & 0xFF)) / 10.0f;
        int co2_ppm = (payload[6] & 0xFF) << 8 | (payload[5] & 0xFF);
        int bat_porc = (payload[8] & 0xFF) << 8 | (payload[7] & 0xFF);

        // Actualización de UI (vía LiveData)
        String receptionTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        dataHolder.timeData.postValue("Última conex. " + receptionTime);
        dataHolder.ozonoData.postValue(o3_ppm);
        dataHolder.temperaturaData.postValue(temperatura_c);
        dataHolder.co2Data.postValue(co2_ppm);
        dataHolder.bateriaData.postValue(bat_porc);

        // (Opcional) Subida a Firebase
        // subirDatosAFirebase(o3_ppm, temperatura_c, co2_ppm, bat_porc, resultado.getRssi());

        Log.d(ETIQUETA_LOG, "Datos 'rocio' -> O3: " + o3_ppm + ", Temp: " + temperatura_c + ", CO2: " + co2_ppm + ", Bat: " + bat_porc);
    }

    /*
    private void subirDatosAFirebase(float o3, float temp, int co2, int bat, int rssi) {
        Map<String, Object> data = new HashMap<>();
        data.put("o3_ppm", o3);
        data.put("temperatura_c", temp);
        data.put("co2_ppm", co2);
        data.put("bateria_porc", bat);
        data.put("rssi", rssi);
        data.put("timestamp", new Date());

        // Reemplaza "ID_DEL_SENSOR" con el ID real que pasaste al servicio
        // db.collection("sensores").document("ID_DEL_SENSOR").collection("mediciones").add(data)
        //    .addOnSuccessListener(docRef -> Log.d(ETIQUETA_LOG, "Datos subidos a Firebase"))
        //    .addOnFailureListener(e -> Log.e(ETIQUETA_LOG, "Error al subir a Firebase", e));
    }
    */

    // --- LÓGICA DE UBICACIÓN ---
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 segundos
        locationRequest.setFastestInterval(5000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void getAddressFromLocation(android.location.Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
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

    // --- UTILIDADES ---
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sensor Tracking", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
