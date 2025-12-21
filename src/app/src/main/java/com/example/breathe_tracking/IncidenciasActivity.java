package com.example.breathe_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class IncidenciasActivity extends AppCompatActivity {

    private TextView ultimasAlertasTextView;
    private TextView incidenciasEnviadasTextView;
    private ImageView backArrow;
    private Button reportarIncidenciaButton;
    private TrackingDataHolder dataHolder;
    private String sensorId;
    private String ubicacion;


    /** @brief Lista que almacena las últimas 6 alertas de mediciones para mostrar en la UI. */
    private final List<String> ultimasSeisAlertas = new ArrayList<>();
    /** @brief Constante que define el número máximo de alertas a mostrar en la UI. */
    private static final int MAX_ALERTS_IN_UI = 6;

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
            newintent.putExtra("SENSOR_NAME", sensorId);
            newintent.putExtra("UBICACION", ubicacion);
            startActivity(newintent);
        });

        setupObservers();
    }

    /**
     * @brief Configura los observadores de datos para actualizar la UI en tiempo real.
     *        La lógica de alertas utiliza un Set para garantizar que no se muestren alertas duplicadas
     *        y mantiene la lista de la UI con un máximo de 6 alertas únicas.
     */
    private void setupObservers() {
        dataHolder.alertData.observe(this, newAlerts -> {
            if (newAlerts != null && !newAlerts.isEmpty()) {

                // Usamos un LinkedHashSet para preservar el orden y garantizar la unicidad.
                // 1. Se añaden las alertas nuevas que llegan del servicio para darles prioridad.
                LinkedHashSet<String> uniqueAlerts = new LinkedHashSet<>(newAlerts);
                // 2. Luego, se añaden las alertas antiguas que ya teníamos. Las duplicadas serán ignoradas por el Set.
                uniqueAlerts.addAll(ultimasSeisAlertas);

                // 3. Reconstruimos la lista a partir del conjunto único de alertas.
                ultimasSeisAlertas.clear();
                ultimasSeisAlertas.addAll(uniqueAlerts);

                // 4. --- GESTIÓN DEL LÍMITE DE 6 ALERTAS ---
                // Si la lista ahora tiene más de 6 elementos, se eliminan los más antiguos (los del final).
                while (ultimasSeisAlertas.size() > MAX_ALERTS_IN_UI) {
                    ultimasSeisAlertas.remove(ultimasSeisAlertas.size() - 1);
                }

                // 5. Se actualiza la interfaz gráfica con la lista limpia y ordenada.
                actualizarTextoAlertas();
            }
        });

        // Observador para el historial de incidencias enviadas
        dataHolder.incidenciasEnviadasData.observe(this, history -> {
            if (history != null && !history.isEmpty()) {
                incidenciasEnviadasTextView.setText(TextUtils.join("\n\n", history));
            } else {
                incidenciasEnviadasTextView.setText("No hay incidencias enviadas");
            }
        });

        actualizarTextoAlertas();
    }

    /**
     * @brief Actualiza el TextView de alertas con el contenido de la lista.
     */
    private void actualizarTextoAlertas() {
        if (ultimasSeisAlertas.isEmpty()) {
            ultimasAlertasTextView.setText("No hay alertas");
        } else {
            String textoAlertas = TextUtils.join("\n\n", ultimasSeisAlertas);
            ultimasAlertasTextView.setText(textoAlertas);
        }
    }
}
