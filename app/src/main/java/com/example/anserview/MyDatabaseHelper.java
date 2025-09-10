package com.example.anserview;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public MyDatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase BaseDeDatos) {
        BaseDeDatos.execSQL("CREATE TABLE Registros(correo TEXT PRIMARY KEY, password TEXT)");

        // ✅ Insertar usuario de prueba (dato quemado)
        BaseDeDatos.execSQL("INSERT INTO Registros(correo, password) VALUES('mari@correo.com', '1234')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Aquí puedes manejar actualizaciones de esquema si lo necesitas
    }
}
