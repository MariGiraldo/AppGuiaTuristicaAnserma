package com.example.anserview;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LugarAdapter extends RecyclerView.Adapter<LugarAdapter.LugarViewHolder> {
    private Context context;
    private List<Lugar> listaLugares;

    public LugarAdapter(Context context, List<Lugar> listaLugares) {
        this.context = context;
        this.listaLugares = listaLugares;
    }

    @NonNull
    @Override
    public LugarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lugar, parent, false);
        return new LugarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LugarViewHolder holder, int position) {
        Lugar lugar = listaLugares.get(position);
        holder.tvNombre.setText(lugar.getNombre());
        holder.tvDescripcion.setText(lugar.getDescripcion());
        holder.tvCreador.setText("Creado por: " + lugar.getUsuarioCorreo());

        byte[] imagenBytes = lugar.getImagen();
        if (imagenBytes != null && imagenBytes.length > 0) {
            holder.ivImagen.setImageBitmap(BitmapFactory.decodeByteArray(imagenBytes, 0, imagenBytes.length));
        } else {
            holder.ivImagen.setImageBitmap(null); // O un placeholder
        }

        // Click Listener para el botón de comentarios
        holder.btnComentarios.setOnClickListener(v -> {
            Intent intent = new Intent(context, ActivityComentarios.class);
            intent.putExtra("ID_LUGAR", lugar.getId());
            intent.putExtra("NOMBRE_LUGAR", lugar.getNombre());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaLugares.size();
    }

    static class LugarViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion, tvCreador;
        ImageView ivImagen;
        Button btnComentarios;

        public LugarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_nombre_lugar);
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion_lugar);
            tvCreador = itemView.findViewById(R.id.tv_creador_lugar);
            // Corregido: La ID debe coincidir con el XML
            ivImagen = itemView.findViewById(R.id.iv_imagen_lugar);
            btnComentarios = itemView.findViewById(R.id.btn_comentarios);
        }
    }
}
