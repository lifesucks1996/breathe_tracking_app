package com.example.breathe_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class EnvioIncidenciasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.envio_incidencias);

        // Encontrar las vistas
        EditText tituloEditText = findViewById(R.id.editText_tituloIncidencia);
        EditText mensajeEditText = findViewById(R.id.editText_mensaje);
        Button acceptButton = findViewById(R.id.button_aceptar);
        Button cancelButton = findViewById(R.id.button_cancelar);

        // --- Lógica para rellenar los datos automáticamente ---
        Intent intent = getIntent();
        String sensorName = intent.getStringExtra("SENSOR_NAME");
        String ubicacion = intent.getStringExtra("UBICACION");
        String ultimaConexion = intent.getStringExtra("ULTIMA_CONEXION");

        if (ultimaConexion != null) {
            ultimaConexion = ultimaConexion.replace("Última conex. ", "");
        }

        String titulo = String.format("AVISO:'%s' Desconectado", sensorName);
        String mensaje = String.format(Locale.getDefault(),
                "El '%s' de la zona '%s' ha dejado de funcionar. La última lectura se recibió a las %s. Por favor, compruebe la conexión o si existe algún problema con el sensor.",
                sensorName, ubicacion, ultimaConexion);

        tituloEditText.setText(titulo);
        mensajeEditText.setText(mensaje);

        // --- Lógica de los botones ---
        cancelButton.setOnClickListener(v -> {
            finish();
        });

        acceptButton.setOnClickListener(v -> {
            String tituloIncidencia = tituloEditText.getText().toString();
            String mensajeIncidencia = mensajeEditText.getText().toString();

            // --- FUTURA IMPLEMENTACIÓN DE FIREBASE ---
            // Aquí es donde añadirías el código para enviar los datos a Firebase.
            // Ejemplo:
            // FirebaseFirestore db = FirebaseFirestore.getInstance();
            // Map<String, Object> incidencia = new HashMap<>();
            // incidencia.put("titulo", tituloIncidencia);
            // incidencia.put("mensaje", mensajeIncidencia);
            // incidencia.put("timestamp", new Date());
            // db.collection("incidencias").add(incidencia);
            // ------------------------------------------

            // Mostrar pop-up de confirmación
            new AlertDialog.Builder(this)
                    .setTitle("Enviado")
                    .setMessage("Mensaje enviado correctamente al administrador")
                    .setPositiveButton("Aceptar", (dialog, which) -> {
                        // Al pulsar aceptar en el pop-up, se cierra la pantalla de incidencias.
                        finish();
                    })
                    .show();
        });
    }
}
