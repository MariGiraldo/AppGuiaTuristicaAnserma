package com.example.anserview;

public class Comentario {
    private String autorCorreo;
    private String textoComentario;
    private float calificacion;

    public Comentario(String autorCorreo, String textoComentario, float calificacion) {
        this.autorCorreo = autorCorreo;
        this.textoComentario = textoComentario;
        this.calificacion = calificacion;
    }

    public String getAutorCorreo() {
        return autorCorreo;
    }

    public String getTextoComentario() {
        return textoComentario;
    }

    public float getCalificacion() {
        return calificacion;
    }
}
