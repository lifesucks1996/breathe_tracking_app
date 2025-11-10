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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SensorTrackingService extends Service {

    public static final String ACTION_LOCATION_UPDATE = "com.example.breathe_tracking.LOCATION_UPDATE";
    public static final String EXTRA_LOCATION_ADDRESS = "extra_location_address";
    public static final String ACTION_TIME_UPDATE = "com.example.breathe_tracking.TIME_UPDATE";
    public static final String EXTRA_TIME = "extra_time";

    private static final String CHANNEL_ID = "SensorTrackingChannel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Handler timeUpdateHandler;
    private Runnable timeUpdateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

        // Runnable para la hora
        timeUpdateHandler = new Handler(Looper.getMainLooper());
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                broadcastTimeUpdate("Última conex. " + currentTime);
                timeUpdateHandler.postDelayed(this, 60000); // Cada 60 segundos
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Breathe Traking")
                .setContentText("Monitorizando el sensor en tiempo real.")
                .setSmallIcon(R.drawable.logo_app) // Asegúrate de tener este icono
                .build();

        startForeground(NOTIFICATION_ID, notification);
        startLocationUpdates();
        timeUpdateHandler.post(timeUpdateRunnable);

        return START_STICKY; // El servicio se reiniciará si el sistema lo mata
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 segundos
        locationRequest.setFastestInterval(5000); // 5 segundos

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permiso gestionado en la actividad
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void getAddressFromLocation(android.location.Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String street = address.getThoroughfare();
                String city = address.getLocality();
                String fullAddress = (street != null ? street : "") + ", " + (city != null ? city : "");
                broadcastLocationUpdate(fullAddress);
            }
        } catch (IOException e) {
            // Ignorar errores de geocoding en segundo plano
        }
    }

    private void broadcastLocationUpdate(String address) {
        Intent intent = new Intent(ACTION_LOCATION_UPDATE);
        intent.putExtra(EXTRA_LOCATION_ADDRESS, address);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastTimeUpdate(String time) {
        Intent intent = new Intent(ACTION_TIME_UPDATE);
        intent.putExtra(EXTRA_TIME, time);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Sensor Tracking Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
