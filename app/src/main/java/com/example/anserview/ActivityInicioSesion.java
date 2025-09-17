package com.example.anserview;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityInicioSesion extends AppCompatActivity {

    private EditText et_email2, et_cont2;
    private Button btn_inicio;
    private TextView tv_registrarse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_sesion);

        // Referencias
        et_email2 = findViewById(R.id.et_email);
        et_cont2 = findViewById(R.id.et_cont);
        btn_inicio = findViewById(R.id.btn_registrarse);
        tv_registrarse = findViewById(R.id.tv_inicioSesion);

        btn_inicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarSesion();
            }
        });

        // Configurar el listener de clic para el TextView
        tv_registrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Crear un Intent para navegar a ActivityRegistrarse
                Intent intent = new Intent(ActivityInicioSesion.this, ActivityRegistrarse.class);
                startActivity(intent);
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

        MyDatabaseHelper adminBD = new MyDatabaseHelper(this);
        SQLiteDatabase baseDeDatos = adminBD.getReadableDatabase();

        Cursor cursor = baseDeDatos.rawQuery(
                "SELECT password FROM Registros WHERE correo = ?", new String[]{correo}
        );

        if (cursor.moveToFirst()) {
            String passwordBD = cursor.getString(0);

            if (cont.equals(passwordBD)) {
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                // Aquí se ha agregado la corrección: se pasa el correo en el Intent
                Intent intent = new Intent(ActivityInicioSesion.this, ActivityMapa.class);
                intent.putExtra("USER_EMAIL", correo);
                startActivity(intent);

                finish(); // Finaliza la actividad de inicio de sesión
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
