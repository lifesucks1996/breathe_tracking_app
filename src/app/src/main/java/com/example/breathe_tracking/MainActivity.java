package com.example.breathe_tracking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * CLASE MAIN ACTIVITY - LOG IN
 * - Logica fake log in
 * - Fake lector QR
 */


public class MainActivity extends AppCompatActivity {

    // --- Definición del Lanzador de Permisos ----------------------------------------------------------------------------
    // Permisos para abrir la camara (si acepta llama al metodo para abrir la camara, si no se muestra un toast)
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            });
    // --- Fin Lanzador de Permisos ---------------------------------------------------------------------------------------



    // --- Método Principal (onCreate) ----------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vinculamos con la vista de login
        setContentView(R.layout.login);

        // Buscamos los elementos de la vista  y los inicializamos en variables
        EditText sensorCodeEditText = findViewById(R.id.editText_codigo);
        Button loginButton = findViewById(R.id.button_entrar);
        TextView qrCodeTextView = findViewById(R.id.textView_codigoQR);

        // Definimos el comportamiento del boton de login
        loginButton.setOnClickListener(v -> {
            String sensorCode = sensorCodeEditText.getText().toString();

            // Comprobamos que el campo no esté vacío y que el código sea correcto (12345)
            if (sensorCode.isEmpty()) {
                // Si el campo está vacío, mostramos un mensaje de error
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error, campo vacío")
                        .setMessage("Añade el código del sensor para vincular.")
                        .setPositiveButton("Aceptar", null)
                        .show();
            } else if ("12345".equals(sensorCode)) {
                // Si el código es correcto, pasamos a la siguiente actividad
                Intent intent = new Intent(MainActivity.this, SesionSensorActivity.class);
                intent.putExtra("SENSOR_CODE", sensorCode);
                startActivity(intent);
                finish(); // <--  cerramos la actividad de login para que no pueda volver a ella
            } else {
                // Si el código es incorrecto, mostramos un mensaje de error
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage("El código de vinculación es incorrecto.")
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });

        // Comportamiento del botón de QR que por el momento es abrir la camara para una simulación
        qrCodeTextView.setOnClickListener(v -> {
            // Comprobamos si hay permiso para abrir la camara si no lo tenemos, lo pedimos
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }
    // --- Fin Método Principal (onCreate) ------------------------------------------------------------------------------------------

    // --- Intent para abrir la camara ------------------------------------------------------------------------------------
    private void openCamera() {
        // Esto abre la app de la cámara. Para escanear QR, necesitarás una librería.
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivity(intent);
    }

    // --- Fin Intent para abrir la camara ------------------------------------------------------------------------------
}
