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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * CLASE MAIN ACTIVITY - LOG IN
 * - Logica fake log in (30/10/25 - Sandra)
 * - Lector QR (16/11/25 - Rocio)
 * - Acceso biometrico (17/11/25 - Rocio)
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


    //----- Lanzador de la camara -----------------------------------------------------------------------------------------
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    //----- Fin Lanzador de la camara -------------------------------------------------------------------------------------


    // --- Metodo Principal (onCreate) ----------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vinculamos con la vista de login
        setContentView(R.layout.login);

        // Buscamos los elementos de la vista  y los inicializamos en variables
        EditText sensorCodeEditText = findViewById(R.id.editText_codigo);
        Button loginButton = findViewById(R.id.button_entrar);
        TextView qrCodeTextView = findViewById(R.id.textView_codigoQR);

        // Llamada a acceso biométrico
        mostrarAutenticacionBiometrica();

        //Inicializamos el manejador de resultados del QR
        qrScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Manejador del resultado de la actividad QR
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());

                    if (scanResult != null && scanResult.getContents() != null) {
                        String codigoQR = scanResult.getContents();

                        //PEGAR EL CÓDIGO EN EL CAMPO
                        sensorCodeEditText.setText(codigoQR);

                        Toast.makeText(this, "Código de sesión pegado", Toast.LENGTH_SHORT).show();

                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        // Definimos el comportamiento del boton de login
        loginButton.setOnClickListener(v -> {
            String sensorCode = sensorCodeEditText.getText().toString();

            // Comprobamos que el campo no esté vacío y que el codigo sea correcto (12345)
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
                finish(); // Cerramos la actividad de login si es correcto
            } else {
                // Si el código es incorrecto, mostramos un mensaje de error
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage("El código de vinculación es incorrecto.")
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });

        //Texto que abre la camara QR
        qrCodeTextView.setOnClickListener(v -> {
            //Comprobamos si hay permiso para abrir la camara. Si no lo tenemos, lo pedimos!!
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }
    // --- Fin Metodo Principal (onCreate) ------------------------------------------------------------------------------------------


    // --- Intent para abrir la camara ------------------------------------------------------------------------------------
    private void openCamera() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Escanea el Código de Sesión");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);

        Intent scanIntent = integrator.createScanIntent();

        //Iniciar el escaneo usando el lanzador que configuramos para manejar el resultado
        qrScannerLauncher.launch(scanIntent);
    }
    // --- Fin Intent para abrir la camara ------------------------------------------------------------------------------


    // --- Metodo para acceso biométrico --------------------------------------------------------------------------
    private void mostrarAutenticacionBiometrica() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso Biométrico")
                .setSubtitle("Usa tu huella dactilar o rostro para iniciar sesión")
                .setNegativeButtonText("Usar código de sensor") // Opción para volver al login normal
                .setConfirmationRequired(true)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                MainActivity.this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // Acceso concedido
                        iniciarSesionExitosa("BIOMETRIC_CODE");
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Error
                        Toast.makeText(MainActivity.this, "Error de autenticación: " + errString, Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private void iniciarSesionExitosa(String code) {
        Intent intent = new Intent(MainActivity.this, SesionSensorActivity.class);
        intent.putExtra("SENSOR_CODE", code);
        startActivity(intent);
        finish();
    }

    // --- Fin Metodo para acceso biométrico ----------------------------------------------------------------------

}
