package com.example.breathe_tracking;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.List;

public class IncidenciasActivity extends AppCompatActivity {

    private TextView ultimasAlertasTextView;
    private TextView incidenciasEnviadasTextView;
    private ImageView backArrow;
    private TrackingDataHolder dataHolder;

    /** @brief Lista que almacena las últimas 4 alertas de mediciones para mostrar en la UI. */
    private final List<String> ultimasCuatroAlertas = new ArrayList<>();
    /** @brief Constante que define el número máximo de alertas a mostrar en la UI. */
    private static final int MAX_ALERTS_IN_UI = 4;

    // --- IDs de Notificaciones y Canal ---
    /** @brief ID para el canal de notificaciones de alertas. */
    private static final String CHANNEL_ID = "alert_channel";
    /** @brief ID de notificación para alertas de CO2. */
    private static final int CO2_NOTIFICATION_ID = 1001;
    /** @brief ID de notificación para alertas de Ozono. */
    private static final int OZONE_NOTIFICATION_ID = 1002;
    /** @brief ID de notificación para alertas de Temperatura. */
    private static final int TEMP_NOTIFICATION_ID = 1003;
    /** @brief ID por defecto para otras alertas. */
    private static final int DEFAULT_NOTIFICATION_ID = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incidencias);

        ultimasAlertasTextView = findViewById(R.id.textView_ultimasAlertas);
        incidenciasEnviadasTextView = findViewById(R.id.textView_incidenciasEnviadas);
        backArrow = findViewById(R.id.img_back_arrow);

        dataHolder = TrackingDataHolder.getInstance();

        // Botón para volver a la pantalla anterior
        backArrow.setOnClickListener(v -> finish());

        createNotificationChannel();
        setupObservers();
    }

    /**
     * @brief Configura los observadores para los datos de alertas e incidencias.
     *        - UI: Mantiene una lista de las últimas 4 alertas.
     *        - Notificaciones: Lanza una notificación por CADA TIPO de medida, reemplazándola si ya existe una de ese tipo.
     */
    private void setupObservers() {
        dataHolder.alertData.observe(this, newAlerts -> {
            if (newAlerts != null && !newAlerts.isEmpty()) {
                boolean listChanged = false;
                for (String alert : newAlerts) {
                    if (!ultimasCuatroAlertas.contains(alert)) {
                        // Envía una notificación específica para el tipo de alerta
                        sendNotification("Nueva Alerta de Medición", alert);

                        // --- Lógica para la UI ---
                        ultimasCuatroAlertas.add(0, alert);
                        listChanged = true;
                        if (ultimasCuatroAlertas.size() > MAX_ALERTS_IN_UI) {
                            ultimasCuatroAlertas.remove(MAX_ALERTS_IN_UI);
                        }
                    }
                }
                if (listChanged) {
                    actualizarTextoAlertas();
                }
            }
        });

        dataHolder.incidenciaData.observe(this, incidencia -> {
            if (incidencia != null) {
                incidenciasEnviadasTextView.setText(incidencia);
            }
        });

        actualizarTextoAlertas();
    }

    /**
     * @brief Actualiza el TextView de alertas con el contenido de la lista.
     */
    private void actualizarTextoAlertas() {
        if (ultimasCuatroAlertas.isEmpty()) {
            ultimasAlertasTextView.setText("No hay alertas");
        } else {
            String textoAlertas = TextUtils.join("\n\n", ultimasCuatroAlertas);
            ultimasAlertasTextView.setText(textoAlertas);
        }
    }

    /**
     * @brief Crea el canal de notificaciones necesario para Android 8.0 (API 26) y superior.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alertas de Medición";
            String description = "Canal para notificaciones de alertas de mediciones del sensor.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    /**
     * @brief Obtiene un ID de notificación único basado en el tipo de alerta.
     * @param alertMessage El mensaje de la alerta.
     * @return Un ID entero para la notificación.
     */
    private int getNotificationIdForAlert(String alertMessage) {
        if (alertMessage.contains("CO2")) {
            return CO2_NOTIFICATION_ID;
        } else if (alertMessage.contains("Ozono")) {
            return OZONE_NOTIFICATION_ID;
        } else if (alertMessage.contains("Temperatura")) {
            return TEMP_NOTIFICATION_ID;
        } else {
            return DEFAULT_NOTIFICATION_ID;
        }
    }

    /**
     * @brief Crea y muestra una notificación, usando un ID específico para cada tipo de medida.
     * @param title El título de la notificación.
     * @param message El cuerpo del mensaje de la notificación.
     */
    private void sendNotification(String title, String message) {
        Intent intent = new Intent(this, IncidenciasActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return; // El permiso se pide en la actividad principal
        }

        // Usa un ID específico para cada tipo de alerta para que se reemplacen por tipo
        int notificationId = getNotificationIdForAlert(message);
        notificationManager.notify(notificationId, builder.build());
    }
}
