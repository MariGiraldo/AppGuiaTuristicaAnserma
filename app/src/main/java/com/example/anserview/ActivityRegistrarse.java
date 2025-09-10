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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityRegistrarse extends AppCompatActivity {

    private EditText et_email, et_cont;
    private Button btn_registro;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_email = (EditText) findViewById(R.id.et_email);
        et_cont = (EditText) findViewById(R.id.et_cont);
        btn_registro = (Button) findViewById(R.id.btn_registro);

        btn_registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Registro();
            }
        });

    }

    public void Registro() {
        MyDatabaseHelper adminBD = new MyDatabaseHelper
                (this, "administradorBD", null, 1);
        SQLiteDatabase baseDeDatos = adminBD.getWritableDatabase();
        String correo = et_email.toString();
        String cont = et_cont.toString();
        if (!correo.isEmpty() && !cont.isEmpty()) {

            Cursor fila = baseDeDatos.rawQuery(
                    "SELECT correo FROM registros WHERE correo= ?", new String[]{correo}
            );

            if(fila.moveToFirst()){
                Toast.makeText(this, "Este correo ya esta registrado", Toast.LENGTH_SHORT).show();
                fila.close();
                baseDeDatos.close();
                return;
            }

            ContentValues registro = new ContentValues();
            registro.put("correo", correo);
            registro.put("password", cont);

            baseDeDatos.insert("registros", null, registro);
            baseDeDatos.close();
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();

            Intent next = new Intent(ActivityRegistrarse.this, ActivityMapa.class);
            startActivity(next);

        }else{
            Toast.makeText(this, "Ingrese el correo y la contraseña", Toast.LENGTH_SHORT).show();
        }
    }
}