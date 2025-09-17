package com.example.anserview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AnserView.db";
    // Versión actualizada para agregar la columna tipo_lugar
    private static final int DATABASE_VERSION = 3;

    public MyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear la tabla para los registros de usuarios
        String createTableRegistros = "CREATE TABLE Registros ("
                + "correo TEXT PRIMARY KEY, "
                + "password TEXT)";
        db.execSQL(createTableRegistros);

        // Crear la tabla para los lugares guardados, incluyendo 'tipo_lugar'
        String createTableLugares = "CREATE TABLE Lugares ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT, "
                + "descripcion TEXT, "
                + "lat REAL, "
                + "lon REAL, "
                + "imagen BLOB, "
                + "usuario_correo TEXT, "
                + "tipo_lugar TEXT)";
        db.execSQL(createTableLugares);

        // Nueva tabla para comentarios y calificaciones
        String createTableComentarios = "CREATE TABLE Comentarios ("
                + "id_comentario INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "id_lugar INTEGER, "
                + "autor_correo TEXT, "
                + "comentario TEXT, "
                + "calificacion REAL, " // Valor numérico para la calificación
                + "FOREIGN KEY(id_lugar) REFERENCES Lugares(id))";
        db.execSQL(createTableComentarios);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Lógica de actualización de la base de datos
        if (oldVersion < 2) {
            // Se agrega la tabla Comentarios si la versión anterior era < 2
            db.execSQL("CREATE TABLE Comentarios ("
                    + "id_comentario INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "id_lugar INTEGER, "
                    + "autor_correo TEXT, "
                    + "comentario TEXT, "
                    + "calificacion REAL, "
                    + "FOREIGN KEY(id_lugar) REFERENCES Lugares(id))");
        }
        if (oldVersion < 3) {
            // Se agrega la columna 'tipo_lugar' a la tabla Lugares si la versión anterior era < 3
            db.execSQL("ALTER TABLE Lugares ADD COLUMN tipo_lugar TEXT DEFAULT 'OTHER'");
        }
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.insert(table, nullColumnHack, values);
    }

    public Cursor getAllLugares() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Lugares", null);
    }

    public Cursor getAllLugaresWithUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, nombre, descripcion, lat, lon, imagen, usuario_correo, tipo_lugar FROM Lugares", null);
    }

    // Nuevo método para insertar un comentario
    public long insertarComentario(int idLugar, String autorCorreo, String comentario, float calificacion) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_lugar", idLugar);
        values.put("autor_correo", autorCorreo);
        values.put("comentario", comentario);
        values.put("calificacion", calificacion);
        return db.insert("Comentarios", null, values);
    }

    // Nuevo método para obtener los comentarios de un lugar
    public Cursor getComentariosForLugar(int idLugar) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Comentarios WHERE id_lugar = ?", new String[]{String.valueOf(idLugar)});
    }
}
