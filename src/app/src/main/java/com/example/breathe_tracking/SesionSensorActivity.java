/**
 * @file SesionSensorActivity.java
 * @brief Actividad principal que muestra en tiempo real los datos recibidos del sensor y el estado del servicio de rastreo.
 * @package com.example.breathe_tracking
 */
package com.example.breathe_tracking;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @class SesionSensorActivity
 * @brief Clase que representa la actividad de la sesión del sensor.
 *
 * Copyright © 2025
 *
 * Se encarga de:
 * - Mostrar datos en tiempo real (CO2, Ozono, Temp, Batería, Ubicación) obtenidos del \ref SensorTrackingService.
 * - Utilizar \ref TrackingDataHolder (LiveData) para observar los cambios en las mediciones.
 * - Gestionar la solicitud de permisos (Ubicación, Bluetooth, Notificaciones).
 * - Iniciar y detener el \ref SensorTrackingService.
 * - Ofrecer la funcionalidad para reportar incidencias.
 *
 * @extends AppCompatActivity
 */
public class SesionSensorActivity extends AppCompatActivity {

    // --- Vistas de la UI ---
    private TextView ubicacionTextView;
    private TextView ultimaConexionTextView;
    private TextView bateriaTextView;
    private TextView ozonoTextView;
    private TextView temperaturaTextView;
    private TextView co2TextView;
    private TextView estadoTextView;
    private TextView nombreSensorTextView;
    private TextView verGraficasTextView;
    private ImageView imgSignal;
    private ImageView notificacionesButton;
    private ProgressBar co2ProgressBar;
    private ProgressBar ozonoProgressBar;
    private ProgressBar temperaturaProgressBar;
    private ConstraintLayout layoutOverlayDesconexion;
    private Button btnReportarOverlay;

    // --- Lógica de Datos y Backend ---
    private TrackingDataHolder dataHolder;
    private String sensorId;
    private FirebaseFirestore db;
    private ListenerRegistration incidenciaListener;

    /**
     * @brief Lanzador para el resultado de la actividad de reporte de incidencias.
     *        Gestiona la UI cuando una incidencia es reportada con éxito.
     */
    private final ActivityResultLauncher<Intent> reportarIncidenciaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    btnReportarOverlay.setText("Incidencia reportada");
                    btnReportarOverlay.setBackgroundColor(Color.GRAY);
                    btnReportarOverlay.setEnabled(false);
                    Toast.makeText(this, "Incidencia registrada correctamente", Toast.LENGTH_SHORT).show();
                    listenForIncidenciaResolution();
                }
            }
    );

    /**
     * @brief Launcher para la solicitud de múltiples permisos.
     *        Verifica que TODOS los permisos necesarios (Ubicación, Bluetooth y Notificaciones) son concedidos
     *        antes de iniciar el servicio de rastreo para evitar cierres inesperados.
     */
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allPermissionsGranted = permissions.entrySet().stream().allMatch(entry -> entry.getValue());
                if (allPermissionsGranted) {
                    startTrackingService();
                } else {
                    Toast.makeText(this, "Todos los permisos son necesarios para el funcionamiento.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sesion_sensor);

        initializeViews();
        setupListeners();

        dataHolder = TrackingDataHolder.getInstance();
        db = FirebaseFirestore.getInstance();

        // Extraer el ID del sensor del Intent
        Intent currentIntent = getIntent();
        if (currentIntent != null && currentIntent.hasExtra("SENSOR_CODE")) {
            sensorId = currentIntent.getStringExtra("SENSOR_CODE");
            nombreSensorTextView.setText("Sensor " + sensorId);
        }

        setupObservers();
        checkPermissionsAndStartService();
    }

    /**
     * @brief Vincula las variables de la clase con sus vistas correspondientes en el layout XML.
     */
    private void initializeViews() {
        ubicacionTextView = findViewById(R.id.textView_ubicacion);
        ultimaConexionTextView = findViewById(R.id.textView_ultimaConexion);
        bateriaTextView = findViewById(R.id.textView_bateria);
        ozonoTextView = findViewById(R.id.textView_ozono);
        temperaturaTextView = findViewById(R.id.textView_temperatura);
        co2TextView = findViewById(R.id.textView_co2);
        co2ProgressBar = findViewById(R.id.progressBar_co2);
        ozonoProgressBar = findViewById(R.id.progressBar_ozono);
        temperaturaProgressBar = findViewById(R.id.progressBar_temperatura);
        estadoTextView = findViewById(R.id.textView_estado);
        nombreSensorTextView = findViewById(R.id.textView_nombreSensor);
        verGraficasTextView = findViewById(R.id.textView_graficas);
        imgSignal = findViewById(R.id.img_signal);
        notificacionesButton = findViewById(R.id.imageView_notificaciones);
        layoutOverlayDesconexion = findViewById(R.id.layout_overlay_desconexion);
        btnReportarOverlay = findViewById(R.id.btn_reportar_overlay);

        // Añadir subrayado al texto "Saber más..."
        verGraficasTextView.setPaintFlags(verGraficasTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    /**
     * @brief Configura los listeners para los elementos interactivos de la UI.
     */
    private void setupListeners() {
        ImageView cerrarSesionButton = findViewById(R.id.imageView_cerrarSesion);
        cerrarSesionButton.setOnClickListener(v -> {
            stopTrackingService();
            Intent intent = new Intent(SesionSensorActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        notificacionesButton.setOnClickListener(v -> {
            Intent intent = new Intent(SesionSensorActivity.this, IncidenciasActivity.class);
            intent.putExtra("SENSOR_NAME", sensorId);
            intent.putExtra("UBICACION", ubicacionTextView.getText().toString());
            startActivity(intent);
        });

        btnReportarOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(SesionSensorActivity.this, EnvioIncidenciasActivity.class);
            intent.putExtra("SENSOR_NAME", sensorId);
            intent.putExtra("UBICACION", ubicacionTextView.getText().toString());
            intent.putExtra("ULTIMA_CONEXION", ultimaConexionTextView.getText().toString());
            reportarIncidenciaLauncher.launch(intent);
        });

        verGraficasTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SesionSensorActivity.this, InformacionActivity.class);
            startActivity(intent);
        });
    }

    /**
     * @brief Inicializa los observadores de LiveData para actualizar la UI en tiempo real.
     */
    private void setupObservers() {
        dataHolder.locationData.observe(this, address -> {
            if (address != null) ubicacionTextView.setText(address);
        });

        dataHolder.timeData.observe(this, time -> {
            if (time != null) ultimaConexionTextView.setText(time);
        });

        dataHolder.bateriaData.observe(this, bateria -> {
            if (bateria != null) {
                bateriaTextView.setText(String.format(Locale.getDefault(), "%d%%", bateria));
                bateriaTextView.setTextColor(bateria <= 15 ? ContextCompat.getColor(this, R.color.progress_red) : Color.BLACK);
            }
        });

        dataHolder.ozonoData.observe(this, ozono -> {
            if (ozono != null) {
                ozonoTextView.setText(String.format(Locale.getDefault(), "%.3f ppm", ozono));
                ozonoProgressBar.setProgress((int) (ozono * 1000));
                Drawable d = (ozono < 0.6) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_green) : (ozono < 0.9) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                ozonoProgressBar.setProgressDrawable(d);
            }
        });

        dataHolder.temperaturaData.observe(this, temperatura -> {
            if (temperatura != null) {
                temperaturaTextView.setText(String.format(Locale.getDefault(), "%.1f ºC", temperatura));
                temperaturaProgressBar.setProgress(temperatura.intValue());
                Drawable d = (temperatura <= 20) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_blue) : (temperatura <= 28) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                temperaturaProgressBar.setProgressDrawable(d);
            }
        });

        dataHolder.co2Data.observe(this, co2 -> {
            if (co2 != null) {
                co2TextView.setText(String.format(Locale.getDefault(), "%d ppm", co2));
                co2ProgressBar.setProgress(co2);
                Drawable d = (co2 < 800) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_green) : (co2 < 1200) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                co2ProgressBar.setProgressDrawable(d);
            }
        });

        dataHolder.rssiData.observe(this, rssi -> {
            if (rssi != null) {
                if (rssi >= -60) imgSignal.setImageResource(R.drawable.ic_signal_bars_4);
                else if (rssi >= -70) imgSignal.setImageResource(R.drawable.ic_signal_bars_3);
                else if (rssi >= -80) imgSignal.setImageResource(R.drawable.ic_signal_bars_2);
                else if (rssi >= -90) imgSignal.setImageResource(R.drawable.ic_signal_bars_1);
                else imgSignal.setImageResource(R.drawable.ic_signal_bars_0);
            }
        });

        dataHolder.estadoData.observe(this, estado -> {
            if (estado != null) {
                estadoTextView.setText(estado);
                if ("Conectado".equals(estado)) {
                    estadoTextView.setTextColor(ContextCompat.getColor(this, R.color.progress_green));
                    layoutOverlayDesconexion.setVisibility(View.GONE);
                } else {
                    estadoTextView.setTextColor(ContextCompat.getColor(this, R.color.progress_red));
                    layoutOverlayDesconexion.setVisibility(View.VISIBLE);
                    imgSignal.setImageResource(R.drawable.ic_signal_bars_0);
                }
            }
        });
    }

    /**
     * @brief Inicia una escucha en Firestore para la resolución de una incidencia.
     *        Cuando el campo 'resuelta' es true, oculta el overlay y restaura el botón.
     */
    private void listenForIncidenciaResolution() {
        if (sensorId == null || sensorId.isEmpty()) return;
        final DocumentReference incidenciaRef = db.collection("incidencias").document(sensorId);
        incidenciaListener = incidenciaRef.addSnapshotListener(this, (snapshot, e) -> {
            if (e != null) return;
            if (snapshot != null && snapshot.exists()) {
                if (Boolean.TRUE.equals(snapshot.getBoolean("resuelta"))) {
                    layoutOverlayDesconexion.setVisibility(View.GONE);
                    btnReportarOverlay.setText("Reportar Incidencia");
                    btnReportarOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
                    btnReportarOverlay.setEnabled(true);
                    if (incidenciaListener != null) {
                        incidenciaListener.remove();
                    }
                }
            }
        });
    }

    /**
     * @brief Verifica si los permisos necesarios están concedidos e inicia el servicio. Si no, los solicita.
     *        Incluye el permiso de notificaciones para Android 13+.
     */
    private void checkPermissionsAndStartService() {
        List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);

        // A partir de Android 13 (API 33), el permiso de notificaciones es un permiso de ejecución.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.isEmpty()) {
            startTrackingService();
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    /**
     * @brief Inicia el servicio de rastreo en primer plano.
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        if (sensorId != null) {
            serviceIntent.putExtra("SENSOR_ID_KEY", sensorId);
        }
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    /**
     * @brief Detiene el servicio de rastreo.
     */
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (incidenciaListener != null) {
            incidenciaListener.remove();
        }
    }
}
