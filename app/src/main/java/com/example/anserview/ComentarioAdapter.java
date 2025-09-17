package com.example.anserview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ComentarioAdapter extends RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder> {

    private Context context;
    private List<Comentario> listaComentarios;

    public ComentarioAdapter(Context context, List<Comentario> listaComentarios) {
        this.context = context;
        this.listaComentarios = listaComentarios;
    }

    @NonNull
    @Override
    public ComentarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comentario, parent, false);
        return new ComentarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComentarioViewHolder holder, int position) {
        Comentario comentario = listaComentarios.get(position);
        holder.tvAutor.setText(comentario.getAutorCorreo());
        holder.tvComentario.setText(comentario.getTextoComentario());
        holder.ratingBar.setRating(comentario.getCalificacion());
    }

    @Override
    public int getItemCount() {
        return listaComentarios.size();
    }

    static class ComentarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAutor, tvComentario;
        RatingBar ratingBar;

        public ComentarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAutor = itemView.findViewById(R.id.tv_autor);
            tvComentario = itemView.findViewById(R.id.tv_comentario);
            ratingBar = itemView.findViewById(R.id.rating_bar);
        }
    }
}
