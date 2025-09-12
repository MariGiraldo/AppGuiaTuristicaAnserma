package com.example.anserview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public MyDatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }
    public Cursor getAllLugares() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Lugares", null);
    }


    @Override
    public void onCreate(SQLiteDatabase BaseDeDatos) {
        BaseDeDatos.execSQL("CREATE TABLE Registros(correo TEXT PRIMARY KEY, password TEXT)");

        BaseDeDatos.execSQL("CREATE TABLE Lugares(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, " +
                "descripcion TEXT, " +
                "lat DOUBLE, " +
                "lon DOUBLE, " +
                "imagen BLOB)");

        // Usuario de prueba
        BaseDeDatos.execSQL("INSERT INTO Registros(correo, password) VALUES('mari@correo.com', '1234')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Registros");
        db.execSQL("DROP TABLE IF EXISTS Lugares");
        onCreate(db);
    }

    // Método de inserción REAL
    public long insert(String tabla, String nullColumnHack, ContentValues valores) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.insert(tabla, nullColumnHack, valores);
    }

}
