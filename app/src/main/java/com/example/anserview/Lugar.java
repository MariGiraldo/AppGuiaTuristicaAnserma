package com.example.anserview;

import java.math.BigDecimal;
import java.math.RoundingMode;
// Clase modelo para representar un Lugar con los datos de la base de datos
public class Lugar {
    private int id;
    private String nombre;
    private String descripcion;
    private byte[] imagen;
    private String usuarioCorreo; // Nuevo campo para el correo del creador
    private String tipoLugar; // Nuevo campo para el tipo de lugar
    private Double promedio;

    public Lugar(int id, String nombre, String descripcion, byte[] imagen, String usuarioCorreo, String tipoLugar, Double promedio) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.usuarioCorreo = usuarioCorreo;
        this.tipoLugar = tipoLugar;
        this.promedio = promedio;
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

    public String getPromedio() {
         return BigDecimal.valueOf(promedio)
                .setScale(2, RoundingMode.HALF_UP)
                .toString();
    }

    public void setPromedio(Double promedio) {
        this.promedio = promedio;
    }


}
