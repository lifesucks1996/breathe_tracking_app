/**
 * @file SesionSensorActivity.java
 * @brief Actividad principal de monitorización en tiempo real.
 * @package com.example.breathe_tracking
 * @copyright Copyright © 2025
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
 * @brief Dashboard principal que visualiza los datos del sensor y gestiona el estado de la conexión.
 * @extends AppCompatActivity
 *
 * @details
 * Esta actividad actúa como la vista principal (View) en la arquitectura de la aplicación.
 * Se suscribe a los cambios de datos emitidos por @ref TrackingDataHolder (ViewModel/Repository)
 * para actualizar la interfaz de usuario en tiempo real sin bloquear el hilo principal.
 *
 *
 *
 * **Funcionalidades Clave:**
 * 1. **Monitorización en Tiempo Real:** Visualización de O3, CO2, Temperatura y Batería.
 * 2. **Feedback Visual Semántico:**
 * - Barras de progreso con código de colores (Verde/Naranja/Rojo) según umbrales de peligro.
 * 3. **Gestión de Ciclo de Vida del Servicio:** Inicia el @ref SensorTrackingService garantizando los permisos necesarios.
 * 4. **Gestión de Incidencias:**
 * - Bloqueo de UI mediante Overlay cuando hay desconexión.
 * - Escucha activa en Firestore para desbloquear la UI automáticamente si la incidencia se resuelve remotamente.
 *
 * @author Sandra (UI, Lógica de visualización y alertas - 11/11/2025)
 * @author Rocio (Conexión con base de datos y lógica de servicio - 19/11/2025)
 * @see SensorTrackingService
 * @see TrackingDataHolder
 */
public class SesionSensorActivity extends AppCompatActivity {

    // --- Elementos de la UI ---
    /** @brief Muestra la dirección o coordenadas actuales del dispositivo. */
    private TextView ubicacionTextView;
    /** @brief Muestra la hora del último paquete de datos recibido. */
    private TextView ultimaConexionTextView;
    /** @brief Muestra el porcentaje de batería del sensor. */
    private TextView bateriaTextView;
    /** @brief Muestra el valor numérico de Ozono. */
    private TextView ozonoTextView;
    /** @brief Muestra el valor numérico de Temperatura. */
    private TextView temperaturaTextView;
    /** @brief Muestra el valor numérico de CO2. */
    private TextView co2TextView;
    /** @brief Muestra mensajes de texto si se superan los límites de seguridad. */
    private TextView alertaTextView;
    /** @brief Muestra el estado de incidencias o desconexión. */
    private TextView incidenciaTextView;
    /** @brief Muestra "Conectado" (Verde) o "Desconectado" (Rojo). */
    private TextView estadoTextView;
    /** @brief Botón para navegar a la actividad de reporte. */
    private TextView reportarIncidenciaTextView;
    /** @brief Muestra el ID del sensor vinculado. */
    private TextView nombreSensorTextView;
    /** @brief Enlace a la actividad de gráficas históricas. */
    private TextView verGraficasTextView;
    /** @brief Icono dinámico para la intensidad de señal (RSSI). */
    private ImageView imgSignal;

    // --- Barras de Progreso ---
    /** @brief Indicador visual para CO2. */
    private ProgressBar co2ProgressBar;
    /** @brief Indicador visual para Ozono. */
    private ProgressBar ozonoProgressBar;
    /** @brief Indicador visual para Temperatura. */
    private ProgressBar temperaturaProgressBar;
    /**@brief Enlace a la actividad de manual de usuario. */
    private TextView manualUsuarioTextView;
    /**@ brief Enlace a la actividad de notificaciones. */
    private ImageView notificacionesButton;
    /** @brief Overlay para reportar incidencias. */
    private ConstraintLayout layoutOverlayDesconexion;
    /** @brief Botón para reportar incidencias. */
    private Button btnReportarOverlay;

    // --- Lógica de Datos y backend ---
    /** @brief Instancia Singleton que contiene los LiveData observables. */
    private TrackingDataHolder dataHolder;
    /** @brief Identificador único del sensor recibido por Intent. */
    private String sensorId;
    /** @brief Instancia de Firestore para operaciones con la base de datos. */
    private FirebaseFirestore db;
    /** @brief Listener para la resolución de incidencias. */
    private ListenerRegistration incidenciaListener;



    /**
     * @brief Gestiona el retorno de la actividad de reporte de incidencias.
     * Si el reporte es exitoso, actualiza el botón del overlay para evitar duplicados
     * y activa la escucha de resolución en Firestore.
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
     * @brief Gestiona la solicitud de permisos en tiempo de ejecución.
     * Verifica permisos críticos (Ubicación, Bluetooth y Notificaciones) antes de arrancar el servicio.
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

    /**
     * @brief Método de inicialización del ciclo de vida.
     * Configura la inyección de dependencias, inicializa vistas y observadores.
     * @param savedInstanceState Estado guardado de la aplicación.
     */
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
     * @brief Vincula los objetos Java con los elementos del XML.
     * Aplica estilos programáticos (como el subrayado de enlaces).
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
        manualUsuarioTextView = findViewById(R.id.textView_manualUsuario);

        // Añadir subrayado a los textos
        verGraficasTextView.setPaintFlags(verGraficasTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        manualUsuarioTextView.setPaintFlags(manualUsuarioTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    /**
     * @brief Configura los Listeners para la interacción del usuario.
     * Define la navegación a otras actividades (Gráficas, Incidencias, Manual, Login).
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

        manualUsuarioTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SesionSensorActivity.this, ManualUsuarioActivity.class);
            startActivity(intent);
        });
    }

    /**
     * @brief Configura el patrón Observer sobre los LiveData del DataHolder.
     *
     * @details
     * Define la lógica de presentación:
     * - **Colores:** Asigna drawables (Verde/Naranja/Rojo) a las barras de progreso.
     * - **Visibilidad:** Muestra/Oculta el overlay de desconexión según el estado.
     * - **Iconografía:** Cambia el icono de señal según el RSSI.
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
     * @brief Inicia una escucha activa en Firestore.
     * Si la incidencia activa (vinculada al ID del sensor) marca `resuelta: true`,
     * la aplicación desbloquea automáticamente la interfaz del usuario.
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
     * @brief Verifica permisos e inicia el servicio.
     *
     * @note Gestiona explícitamente `POST_NOTIFICATIONS` para Android 13+ (API 33).
     * Si faltan permisos, los solicita en bloque. Si se tienen, arranca el servicio.
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
     * @brief Arranca el @ref SensorTrackingService como Foreground Service.
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        if (sensorId != null) {
            serviceIntent.putExtra("SENSOR_ID_KEY", sensorId);
        }
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    /**
     * @brief Detiene el servicio de rastreo al cerrar sesión.
     */
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        stopService(serviceIntent);
    }

    /**
     * @brief Limpieza de recursos al destruir la actividad.
     * Elimina el listener de Firestore para evitar fugas de memoria.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (incidenciaListener != null) {
            incidenciaListener.remove();
        }
    }
}
