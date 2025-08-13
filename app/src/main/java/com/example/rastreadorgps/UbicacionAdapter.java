package com.example.rastreadorgps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UbicacionAdapter extends RecyclerView.Adapter<UbicacionAdapter.ViewHolder> {
    private List<Ubicacion> ubicaciones;

    public UbicacionAdapter(List<Ubicacion> ubicaciones) {
        this.ubicaciones = ubicaciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ubicacion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ubicacion ubicacion = ubicaciones.get(position);
        // ¡CORRECCIÓN AQUÍ! Acceder a los datos de la instancia 'ubicacion'
        // Usamos String.format para una mejor presentación y para evitar warnings de concatenación
        String locationInfo = String.format(
                holder.itemView.getContext().getString(R.string.history_location_format),
                ubicacion.getLatitude(),
                ubicacion.getLongitude(),
                ubicacion.getFormattedTimestamp(), // Usar el timestamp formateado
                ubicacion.getSource() // Mostrar la fuente
        );
        holder.info.setText(locationInfo);
    }

    @Override
    public int getItemCount() {
        return ubicaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView info;
        public ViewHolder(View itemView) {
            super(itemView);
            info = itemView.findViewById(R.id.txtUbicacion);
        }
    }
}
