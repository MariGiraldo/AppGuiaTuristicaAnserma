package com.example.anserview;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityInicioSesion extends AppCompatActivity {

    private EditText et_email2, et_cont2;
    private Button btn_inicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_sesion);

        // Referencias
        et_email2 = findViewById(R.id.et_email);
        et_cont2 = findViewById(R.id.et_cont);
        btn_inicio = findViewById(R.id.btn_registrarse);

        btn_inicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarSesion();
            }
        });
    }

    private void iniciarSesion() {
        String correo = et_email2.getText().toString().trim();
        String cont = et_cont2.getText().toString().trim();

        if (correo.isEmpty() || cont.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        MyDatabaseHelper adminBD = new MyDatabaseHelper(this, "administradorBD", null, 1);
        SQLiteDatabase baseDeDatos = adminBD.getReadableDatabase();

        // Consulta a la base de datos
        Cursor cursor = baseDeDatos.rawQuery(
                "SELECT password FROM Registros WHERE correo = ?", new String[]{correo}
        );

        if (cursor.moveToFirst()) {
            String passwordBD = cursor.getString(0);

            if (cont.equals(passwordBD)) {
                // Inicio de sesión exitoso
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(ActivityInicioSesion.this, ActivityMapa.class); // o la que uses
                startActivity(intent);
                finish();

            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Correo no registrado", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        baseDeDatos.close();
    }
}
