/**
 * @file IncidenciasActivity.java
 * @brief Actividad para la visualización del historial de notificaciones y gestión de incidencias.
 * @package com.example.breathe_tracking
 * @copyright Copyright © 2025
 */

package com.example.breathe_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * @class IncidenciasActivity
 * @brief Centro de notificaciones de la aplicación.
 * @extends AppCompatActivity
 *
 * @details
 * Esta actividad cumple dos funciones principales:
 * 1. **Historial de Alertas:** Muestra una lista filtrada de las últimas mediciones peligrosas detectadas por el sensor.
 * 2. **Gestión de Incidencias:** Muestra el estado de los reportes enviados y permite la navegación para reportar nuevas incidencias manualmente.
 *
 * Utiliza un algoritmo de filtrado mediante `LinkedHashSet` para garantizar que el usuario vea siempre
 * las alertas más recientes y únicas, evitando la saturación de información en la pantalla.
 *
 * @author Sandra (Creación clase y dataholder - 21/12)
 * @author Rocio (Filtrado de incidencias segun el sensor_id, si esta por resolver y por orden cronologico - 28/12)
 * @author Rocio (Cuando la incidencia esté resuleta se borra del historial - 28/12)
 * @author Rocio (Cuando se resuelve la incidencia se envia un correo al adminsitrador - 28/12)
 */

public class IncidenciasActivity extends AppCompatActivity {

    private TextView ultimasAlertasTextView;
    private TextView incidenciasEnviadasTextView;
    private ImageView backArrow;
    private Button reportarIncidenciaButton;
    private TrackingDataHolder dataHolder;
    private String sensorId;
    private String ubicacion;

    private ListenerRegistration firestoreListener;

    /** @brief Lista que almacena las últimas 6 alertas de mediciones para mostrar en la UI. */
    private final List<String> ultimasSeisAlertas = new ArrayList<>();
    /** @brief Constante que define el número máximo de alertas a mostrar en la UI. */
    private static final int MAX_ALERTS_IN_UI = 6;

    /**
     * @brief Inicialización de la actividad.
     * Recupera el contexto (Sensor y Ubicación) del Intent y configura la navegación.
     * (savedInstanceState:Bundle) -> onCreate() -> ()
     * @param savedInstanceState Estado guardado.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incidencias);

        Intent intent = getIntent();
        if (intent != null) {
            sensorId = intent.getStringExtra("SENSOR_NAME");
            ubicacion = intent.getStringExtra("UBICACION"); // Recibimos la ubicación
        }

        ultimasAlertasTextView = findViewById(R.id.textView_ultimasAlertas);
        incidenciasEnviadasTextView = findViewById(R.id.textView_incidenciasEnviadas);
        backArrow = findViewById(R.id.img_back_arrow);
        reportarIncidenciaButton = findViewById(R.id.button_incidenciasManuales);

        dataHolder = TrackingDataHolder.getInstance();

        backArrow.setOnClickListener(v -> finish());

        reportarIncidenciaButton.setOnClickListener(v -> {
            Intent newintent = new Intent(IncidenciasActivity.this, EnvioIncidenciasActivity.class);
            newintent.putExtra("SENSOR_ID", sensorId);
            newintent.putExtra("UBICACION", ubicacion);
            startActivity(newintent);
        });

        setupObserversLocales();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Iniciamos la escucha cuando la pantalla se hace visible
        setupFirestoreRealtimeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detenemos la escucha cuando la pantalla ya no se ve para ahorrar datos y batería
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    /**
     * @brief Conecta con Firestore para escuchar cambios en tiempo real.
     * @details Filtra por:
     * 1. sensor_id actual.
     * 2. fecha descendiente.
     * Si una incidencia pasa a 'resuelta=true', desaparece sola de la lista y
     * se envia un correo al administrador.
     */
    private void setupFirestoreRealtimeListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Subimos el límite para asegurarnos de captar las pendientes
        // y las que se acaban de resolver.
        Query query = db.collection("incidencias")
                .whereEqualTo("sensor_id", sensorId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(30); // Aumentamos para no "perder" las pendientes entre las resueltas

        firestoreListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("FirestoreError", e.getMessage());
                incidenciasEnviadasTextView.setText("Error cargando incidencias.");
                return;
            }

            if (snapshots != null) {
                // DETECCIÓN DE CAMBIOS PARA CORREO
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.MODIFIED) {
                        Boolean resueltaAhora = dc.getDocument().getBoolean("resuelta");
                        if (resueltaAhora != null && resueltaAhora) {
                            String titulo = dc.getDocument().getString("titulo");
                            enviarCorreoResolucion(titulo);
                        }
                    }
                }

                // RENDERIZADO DE LA LISTA
                List<String> listaFormateada = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
                int contadorUI = 0;

                for (QueryDocumentSnapshot doc : snapshots) {
                    Boolean isResuelta = doc.getBoolean("resuelta");

                    // Solo añadimos si NO está resuelta
                    if (isResuelta != null && !isResuelta) {
                        // Controlamos el límite de 4 AQUÍ, en la interfaz
                        if (contadorUI < 4) {
                            String titulo = doc.getString("titulo");
                            Timestamp ts = doc.getTimestamp("fecha");
                            String fechaStr = (ts != null) ? sdf.format(ts.toDate()) : "--/--";
                            listaFormateada.add(fechaStr + " - " + titulo);
                            contadorUI++;
                        }
                    }
                }

                if (listaFormateada.isEmpty()) {
                    incidenciasEnviadasTextView.setText("No hay incidencias pendientes.");
                } else {
                    incidenciasEnviadasTextView.setText(TextUtils.join("\n\n", listaFormateada));
                }
            }
        });
    }

    /**
     * @brief Envía el correo avisando que la incidencia se ha cerrado.
     */
    private void enviarCorreoResolucion(String tituloIncidencia) {
        String emailDestino = "sandralovesel@gmail.com";
        String asunto = "Incidencia Resuelta: " + tituloIncidencia;
        String mensaje = "La incidencia con el título '" + tituloIncidencia + "' ha sido marcada como RESUELTA por el equipo técnico.";

        JavaMailAPI mailSender = new JavaMailAPI(this, emailDestino, asunto, mensaje);
        mailSender.execute();

        Toast.makeText(this, "Notificación de resolución enviada", Toast.LENGTH_SHORT).show();
    }

    private void setupObserversLocales() {

        dataHolder.alertData.observe(this, newAlerts -> {
            if (newAlerts != null && !newAlerts.isEmpty()) {
                // Usamos LinkedHashSet para evitar duplicados y mantener orden
                LinkedHashSet<String> uniqueAlerts = new LinkedHashSet<>(newAlerts);
                uniqueAlerts.addAll(ultimasSeisAlertas);

                ultimasSeisAlertas.clear();
                ultimasSeisAlertas.addAll(uniqueAlerts);

                // Limitar a 6 alertas en pantalla
                while (ultimasSeisAlertas.size() > MAX_ALERTS_IN_UI) {
                    ultimasSeisAlertas.remove(ultimasSeisAlertas.size() - 1);
                }

                actualizarTextoAlertas();
            }
        });

        // Inicializar texto si ya había alertas
        actualizarTextoAlertas();

    }


    /**
     * @brief Renderiza la lista de alertas en el TextView.
     * Utiliza `TextUtils.join` para separar cada alerta con un doble salto de línea.
     */
    private void actualizarTextoAlertas() {
        if (ultimasSeisAlertas.isEmpty()) {
            ultimasAlertasTextView.setText("No hay alertas de sensores");
        } else {
            ultimasAlertasTextView.setText(TextUtils.join("\n\n", ultimasSeisAlertas));
        }
    }
}
