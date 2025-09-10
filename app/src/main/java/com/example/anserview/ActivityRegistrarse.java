package com.example.anserview;

import android.annotation.SuppressLint;
import android.content.ContentValues;
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

public class ActivityRegistrarse extends AppCompatActivity {

    private EditText et_email, et_cont;
    private Button btn_registro;
    private TextView tv_inicioSesion;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_email = findViewById(R.id.et_email);
        et_cont = findViewById(R.id.et_cont);
        btn_registro = findViewById(R.id.btn_registrarse);
        tv_inicioSesion = findViewById(R.id.tv_inicioSesion);

        btn_registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Registro();
            }
        });

        tv_inicioSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityRegistrarse.this, ActivityInicioSesion.class);
                startActivity(intent);
                finish(); // opcional: cierra la pantalla de registro
            }
        });
    }

    public void Registro() {
        MyDatabaseHelper adminBD = new MyDatabaseHelper(this, "administradorBD", null, 1);
        SQLiteDatabase baseDeDatos = adminBD.getWritableDatabase();

        String correo = et_email.getText().toString().trim();
        String cont = et_cont.getText().toString().trim();

        if (!correo.isEmpty() && !cont.isEmpty()) {
            Cursor fila = baseDeDatos.rawQuery(
                    "SELECT correo FROM Registros WHERE correo= ?", new String[]{correo}
            );

            if (fila.moveToFirst()) {
                Toast.makeText(this, "Este correo ya está registrado", Toast.LENGTH_SHORT).show();
                fila.close();
                baseDeDatos.close();
                return;
            }

            ContentValues registro = new ContentValues();
            registro.put("correo", correo);
            registro.put("password", cont);

            baseDeDatos.insert("Registros", null, registro);
            baseDeDatos.close();
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();

            Intent next = new Intent(ActivityRegistrarse.this, ActivityMapa.class);
            startActivity(next);
            finish();

        } else {
            Toast.makeText(this, "Ingrese el correo y la contraseña", Toast.LENGTH_SHORT).show();
        }
    }
}
