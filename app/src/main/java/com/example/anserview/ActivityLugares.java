package com.example.anserview;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ActivityLugares extends AppCompatActivity {
    private RecyclerView recyclerView;
    private LugarAdapter adapter;
    private List<Lugar> listaLugares;
    private Button btnBackToMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lugares);


        recyclerView = findViewById(R.id.rv_lugares);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnBackToMap = findViewById(R.id.btn_back_to_map);

        btnBackToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityLugares.this, ActivityMapa.class);
                startActivity(intent);
                finish();
            }
        });

        listaLugares = new ArrayList<>();
        cargarLugaresDesdeBD();

        adapter = new LugarAdapter(this, listaLugares);
        recyclerView.setAdapter(adapter);
    }

    private void cargarLugaresDesdeBD() {
        MyDatabaseHelper dbHelper = new MyDatabaseHelper(this);
        Cursor cursor = dbHelper.getAllLugaresWithUser();



        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                byte[] imagen = cursor.getBlob(cursor.getColumnIndexOrThrow("imagen"));
                // Nuevo: obtener el correo del usuario
                String usuarioCorreo = cursor.getString(cursor.getColumnIndexOrThrow("usuario_correo"));
                // Nuevo: obtener el tipo de lugar
                String tipoLugar = cursor.getString(cursor.getColumnIndexOrThrow("tipo_lugar"));

                Double promedio = dbHelper.getPromedioCalificacion(id);

                String promedioString = promedio.toString();

                // Nuevo: pasar el tipo de lugar y el correo al constructor de la clase Lugar
                listaLugares.add(new Lugar(id, nombre, descripcion, imagen, usuarioCorreo, tipoLugar, promedio));
            }
            cursor.close();
        }
        dbHelper.close();
    }
}
