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

import java.util.ArrayList;
import java.util.List;

public class InformacionActivity extends AppCompatActivity {

    private LineChart chart;
    private Spinner spinner;
    private ImageView imgCarita;
    private TextView txtExplicacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.informacion);

        chart = findViewById(R.id.chart_gases);
        spinner = findViewById(R.id.spinner_gases);
        imgCarita = findViewById(R.id.img_carita);
        txtExplicacion = findViewById(R.id.txt_explicacion);

        // 1. Configurar Spinner
        String[] gases = {"CO2", "O3", "NO2", "SO2", "CO"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, gases);
        spinner.setAdapter(adapter);

        // 2. Listener del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String gasSeleccionado = gases[position];
                cargarDatosGas(gasSeleccionado);
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

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        // Aquí podrías formatear el eje X para que muestre horas (HH:mm)
    }

    private void cargarDatosGas(String tipoGas) {
        // --- AQUÍ CONECTARÍAS CON TU BASE DE DATOS (ROOM / FIREBASE) ---
        // Para el ejemplo, generaré datos falsos aleatorios

        List<Entry> entries = new ArrayList<>();
        // Simulamos 24 horas de datos (o medias por hora)
        for (int i = 0; i < 24; i++) {
            float valor = (float) (Math.random() * 1.2); // Valor aleatorio entre 0.0 y 1.2
            entries.add(new Entry(i, valor));
        }

        // Definir los umbrales según el gas (Ejemplo)
        float limiteBueno = 0.5f;
        float limiteRegular = 0.8f;

        // Si es CO2, los valores son mucho más altos (ej. 400-1000 ppm)
        if(tipoGas.equals("CO2")) {
            limiteBueno = 600f;
            limiteRegular = 1000f;
            // Ajustamos los datos falsos para que tengan sentido con CO2
            entries.clear();
            for (int i = 0; i < 24; i++) entries.add(new Entry(i, (float)(400 + Math.random() * 800)));
        }

        // 3. Dibujar las Líneas de Límite (Las líneas discontinuas del boceto)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // Limpiar anteriores

        // Línea "Regular" (Empieza lo malo)
        LimitLine llMalo = new LimitLine(limiteRegular, "Malo (Máx Perm.)");
        llMalo.setLineColor(Color.RED);
        llMalo.setLineWidth(2f);
        llMalo.enableDashedLine(10f, 10f, 0f); // Línea discontinua
        llMalo.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);

        // Línea "Bueno"
        LimitLine llBueno = new LimitLine(limiteBueno, "Límite Recomendado");
        llBueno.setLineColor(Color.GREEN);
        llBueno.setLineWidth(2f);
        llBueno.enableDashedLine(10f, 10f, 0f);
        llBueno.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);

        leftAxis.addLimitLine(llMalo);
        leftAxis.addLimitLine(llBueno);
        leftAxis.setAxisMinimum(0f); // Empezar en 0

        // 4. Crear el Dataset
        LineDataSet dataSet = new LineDataSet(entries, "Nivel de " + tipoGas);
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary)); // Tu color azul
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false); // No mostrar números en cada punto para limpieza

        // Relleno bajo la curva (Opcional, queda bonito)
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.animateX(1000); // Animación suave

        // 5. Calcular la Carita y el Mensaje
        evaluarExposicion(entries, limiteRegular);
    }

    private void evaluarExposicion(List<Entry> datos, float limiteMalo) {
        int contadorPicosMalos = 0;

        for (Entry e : datos) {
            if (e.getY() > limiteMalo) {
                contadorPicosMalos++;
            }
        }

        // Lógica simple: Si más del 20% de las mediciones son malas -> Carita Triste
        // Como tenemos 24 puntos (horas), digamos que si hay más de 3 horas malas.
        if (contadorPicosMalos > 3) {
            imgCarita.setImageResource(R.drawable.ic_face_sad); // Necesitas este icono
            imgCarita.setColorFilter(Color.RED);
            txtExplicacion.setText("¡Cuidado! Has pasado por zonas de alta exposición frecuentemente hoy. Intenta evitar estas rutas mañana.");
        } else if (contadorPicosMalos > 0) {
            imgCarita.setImageResource(R.drawable.ic_face_neutral); // Necesitas este icono
            imgCarita.setColorFilter(Color.parseColor("#FFA500")); // Naranja
            txtExplicacion.setText("Atención: Has tenido algunos picos de exposición puntuales, pero el promedio es aceptable.");
        } else {
            imgCarita.setImageResource(R.drawable.ic_face_happy); // Necesitas este icono
            imgCarita.setColorFilter(Color.GREEN);
            txtExplicacion.setText("¡Excelente! Tu exposición a gases hoy ha sido mínima. Sigue así.");
        }
    }
}