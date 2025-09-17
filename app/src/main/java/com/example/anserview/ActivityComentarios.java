package com.example.anserview;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ActivityComentarios extends AppCompatActivity {

    private int idLugar;
    private String nombreLugar;
    private String autorCorreo;
    private MyDatabaseHelper dbHelper;

    private TextView tvNombreLugar, tvNoComentarios;
    private RecyclerView rvComentarios;
    private ComentarioAdapter adapter;
    private List<Comentario> listaComentarios;

    private EditText etComentario;
    private RatingBar ratingBar;
    private Button btnEnviarComentario, btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comentarios);

        // Obtener datos del Intent
        Intent intent = getIntent();
        if (intent != null) {
            idLugar = intent.getIntExtra("ID_LUGAR", -1);
            nombreLugar = intent.getStringExtra("NOMBRE_LUGAR");
            // Corrección: Obtener el correo del Intent
            autorCorreo = intent.getStringExtra("USER_EMAIL");
        }

        // Referenciar elementos de la interfaz
        tvNombreLugar = findViewById(R.id.tv_nombre_lugar_comentario);
        tvNoComentarios = findViewById(R.id.tv_no_comentarios);
        rvComentarios = findViewById(R.id.rv_comentarios);
        etComentario = findViewById(R.id.et_escribir_comentario);
        ratingBar = findViewById(R.id.rating_bar_add);
        btnEnviarComentario = findViewById(R.id.btn_enviar_comentario);
        btnVolver = findViewById(R.id.btn_atras_lugares);

        dbHelper = new MyDatabaseHelper(this);
        listaComentarios = new ArrayList<>();
        adapter = new ComentarioAdapter(this, listaComentarios);
        rvComentarios.setLayoutManager(new LinearLayoutManager(this));
        rvComentarios.setAdapter(adapter);

        tvNombreLugar.setText(nombreLugar);
        cargarComentarios();

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityComentarios.this, ActivityLugares.class);
                startActivity(intent);
                finish();
            }
        });

        btnEnviarComentario.setOnClickListener(v -> {
            String comentario = etComentario.getText().toString().trim();
            float calificacion = ratingBar.getRating();

            if (comentario.isEmpty() || calificacion == 0) {
                Toast.makeText(this, "Por favor, escribe un comentario y da una calificación", Toast.LENGTH_SHORT).show();
                return;
            }

            // Insertar comentario en la base de datos
            long id = dbHelper.insertarComentario(idLugar, autorCorreo, comentario, calificacion);
            if (id > 0) {
                Toast.makeText(this, "Comentario enviado", Toast.LENGTH_SHORT).show();
                etComentario.setText("");
                ratingBar.setRating(0);
                cargarComentarios(); // Recargar la lista
            } else {
                Toast.makeText(this, "Error al enviar comentario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarComentarios() {
        listaComentarios.clear();
        Cursor cursor = dbHelper.getComentariosForLugar(idLugar);

        if (cursor != null && cursor.moveToFirst()) {
            tvNoComentarios.setVisibility(View.GONE);
            do {
                String autor = cursor.getString(cursor.getColumnIndexOrThrow("autor_correo"));
                String texto = cursor.getString(cursor.getColumnIndexOrThrow("comentario"));
                float calificacion = cursor.getFloat(cursor.getColumnIndexOrThrow("calificacion"));
                listaComentarios.add(new Comentario(autor, texto, calificacion));
            } while (cursor.moveToNext());
        } else {
            tvNoComentarios.setVisibility(View.VISIBLE);
        }

        if (cursor != null) {
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}
