package com.example.anserview;

// Clase modelo para representar un Lugar con los datos de la base de datos
public class Lugar {
    private int id;
    private String nombre;
    private String descripcion;
    private byte[] imagen;
    private String usuarioCorreo; // Nuevo campo para el correo del creador
    private String tipoLugar; // Nuevo campo para el tipo de lugar

    public Lugar(int id, String nombre, String descripcion, byte[] imagen, String usuarioCorreo, String tipoLugar) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.usuarioCorreo = usuarioCorreo;
        this.tipoLugar = tipoLugar;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public byte[] getImagen() {
        return imagen;
    }

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }

    public String getTipoLugar() {
        return tipoLugar;
    }
}
