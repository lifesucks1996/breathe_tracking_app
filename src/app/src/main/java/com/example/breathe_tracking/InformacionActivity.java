package com.example.breathe_tracking;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @class Información Activity
 * @brief Actividad sobre información resumen sobre la exposicion diaria a los gases
 *
 *
 * Copyrigth © 2025
 * Fecha, autor, aportacion:
 * - Sandra Moll Cots (27-11-2025) Funcionamiento básico graficas, spiner con gases y evaluacion del aire
 * - Rocio Piquer (27-11-2025) Conexion base de datos para mostrar datos en grafica
 *
 */
public class InformacionActivity extends AppCompatActivity {

    private LineChart chart;
    private Spinner spinner;
    private ImageView imgCarita;
    private ImageView imgBackArrow; // Flecha para volver
    private TextView txtExplicacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.informacion);

        chart = findViewById(R.id.chart_gases);
        spinner = findViewById(R.id.spinner_gases);
        imgCarita = findViewById(R.id.img_carita);
        imgBackArrow = findViewById(R.id.img_back_arrow);
        txtExplicacion = findViewById(R.id.txt_explicacion);

        // Configurar botón de volver
        imgBackArrow.setOnClickListener(v -> finish());

        // 1. Configurar Spinner con los nombres de los gases
        // El orden debe coincidir con la lógica del switch en cargarDatosGas
        String[] gases = {
                "Ozono (O3)",
                "Monóxido de Carbono (CO)",
                "Dióxido de Nitrógeno (NO2)",
                "Dióxido de Azufre (SO2)",
                "Dióxido de Carbono (CO2)"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, gases);
        spinner.setAdapter(adapter);

        // 2. Listener del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Pasamos la posición o el nombre para identificar el gas
                cargarDatosGas(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Configuración base del gráfico
        configurarEstiloGrafico();
    }

    private void configurarEstiloGrafico() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        
        // Fondo gris muy claro para resaltar mejor el gradiente si es necesario, o transparente
        // chart.setBackgroundColor(Color.WHITE);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        
        chart.getAxisRight().setEnabled(false); // Desactivar eje derecho
    }

    private void cargarDatosGas(int position) {
        // Definición de variables según el gas seleccionado
        float limiteSeguro = 0f;   // Límite superior de "Bueno"
        float limitePeligro = 0f;  // Límite inferior de "Peligroso"
        String unidad = "";
        String nombreGas = "";
        float valorMinSimulado = 0f;
        float valorMaxSimulado = 0f;

        // Configuración de rangos según lo solicitado
        switch (position) {
            case 0: // Ozono (O3)
                nombreGas = "Ozono (O3)";
                unidad = "ppm";
                // Actualizado: 0-0.6 Seguro, 0.6-0.9 Riesgo, >0.9 Peligroso
                limiteSeguro = 0.6f;
                limitePeligro = 0.9f;
                valorMinSimulado = 0.3f;
                valorMaxSimulado = 1.1f; // Ajustado para ver picos peligrosos
                break;

            case 1: // Monóxido de Carbono (CO)
                nombreGas = "Monóxido de Carbono (CO)";
                unidad = "mg/m³";
                limiteSeguro = 10f;
                limitePeligro = 30f;
                valorMinSimulado = 5f;
                valorMaxSimulado = 40f;
                break;

            case 2: // Dióxido de Nitrógeno (NO2)
                nombreGas = "Dióxido de Nitrógeno (NO2)";
                unidad = "µg/m³";
                limiteSeguro = 200f;
                limitePeligro = 400f;
                valorMinSimulado = 100f;
                valorMaxSimulado = 500f;
                break;

            case 3: // Dióxido de Azufre (SO2)
                nombreGas = "Dióxido de Azufre (SO2)";
                unidad = "µg/m³";
                limiteSeguro = 350f;
                limitePeligro = 500f;
                valorMinSimulado = 200f;
                valorMaxSimulado = 600f;
                break;

            case 4: // Dióxido de Carbono (CO2)
                nombreGas = "Dióxido de Carbono (CO2)";
                unidad = "ppm";
                // Actualizado: 0-800 Seguro, 800-1200 Riesgo, >1200 Peligroso
                limiteSeguro = 800f;
                limitePeligro = 1200f;
                valorMinSimulado = 600f;
                valorMaxSimulado = 1500f; // Ajustado para ver picos peligrosos
                break;
        }

        // Generación de datos simulados
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            // Generar valor aleatorio dentro de un rango razonable para el ejemplo
            float rango = valorMaxSimulado - valorMinSimulado;
            float valor = (float) (valorMinSimulado + Math.random() * rango);
            entries.add(new Entry(i, valor));
        }

        // Configurar Eje Y (Izquierdo)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // Limpiar líneas anteriores
        leftAxis.setAxisMinimum(0f);    // Siempre empezar en 0

        // Línea de Límite Seguro (Verde)
        LimitLine llSeguro = new LimitLine(limiteSeguro, "Seguro (" + limiteSeguro + " " + unidad + ")");
        llSeguro.setLineColor(Color.GREEN);
        llSeguro.setLineWidth(2f);
        llSeguro.enableDashedLine(10f, 10f, 0f);
        llSeguro.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llSeguro.setTextSize(10f);

        // Línea de Límite Peligroso (Rojo)
        LimitLine llPeligro = new LimitLine(limitePeligro, "Peligroso (" + limitePeligro + " " + unidad + ")");
        llPeligro.setLineColor(Color.RED);
        llPeligro.setLineWidth(2f);
        llPeligro.enableDashedLine(10f, 10f, 0f);
        llPeligro.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        llPeligro.setTextSize(10f);

        leftAxis.addLimitLine(llSeguro);
        leftAxis.addLimitLine(llPeligro);

        // Crear Dataset
        LineDataSet dataSet = new LineDataSet(entries, "Nivel de " + nombreGas);
        
        // Configuración de COLOR del trazo
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        
        // 1. LÍNEA MÁS FINA
        dataSet.setLineWidth(1f); 
        
        // Configuración de los puntos (círculos)
        dataSet.setCircleRadius(2f); // Círculos también un poco más pequeños
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false); // Sin números sobre los puntos

        // 2. RELLENO CON GRADIENTE SEMÁFORO
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.gradiente_semaforo));
        
        // Quitamos transparencia por código para usar la del XML
        dataSet.setFillAlpha(255); 

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.animateX(1000);
        
        // Forzar repintado para actualizar ejes y limites
        chart.invalidate(); 

        // Evaluar exposición con los nuevos límites
        evaluarExposicion(entries, limiteSeguro, limitePeligro);
    }

    private void evaluarExposicion(List<Entry> datos, float limiteSeguro, float limitePeligro) {
        int contadorPeligroso = 0;
        int contadorRiesgo = 0;

        for (Entry e : datos) {
            float val = e.getY();
            if (val > limitePeligro) {
                contadorPeligroso++;
            } else if (val > limiteSeguro) {
                contadorRiesgo++;
            }
        }

        // Lógica de evaluación
        if (contadorPeligroso > 0) {
            // Si hay al menos 1 medición en rango peligroso
            imgCarita.setImageResource(R.drawable.ic_face_sad);
            txtExplicacion.setText("¡Alerta! Se han detectado niveles PELIGROSOS. Evita la zona.");
        } else if (contadorRiesgo > 3) {
            // Si hay varias mediciones en riesgo
            imgCarita.setImageResource(R.drawable.ic_face_neutral);
            txtExplicacion.setText("Precaución: Niveles de riesgo detectados. La calidad del aire no es óptima.");
        } else {
            // Todo mayormente seguro
            imgCarita.setImageResource(R.drawable.ic_face_happy);
            txtExplicacion.setText("¡Excelente! Calidad del aire segura.");
        }
    }
}