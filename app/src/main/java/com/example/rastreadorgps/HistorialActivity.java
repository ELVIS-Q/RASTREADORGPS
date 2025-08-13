package com.example.rastreadorgps;

import android.content.Intent; // Necesario para redirigir si no hay usuario
import android.os.Bundle;
import android.util.Log; // Para mensajes de log
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Importar FirebaseUser
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private static final String TAG = "HistorialActivity";
    private static final String APP_ID = "rastreadorgps_app"; // Debe coincidir con MainActivity

    private RecyclerView recyclerView;
    private UbicacionAdapter adapter;
    private List<Ubicacion> listaUbicaciones = new ArrayList<>(); // Renombrado para claridad
    private DatabaseReference locationsRef; // Renombrado para claridad
    private FirebaseAuth mAuth; // Instancia de FirebaseAuth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // **IMPORTANTE: Verificar si el usuario está autenticado**
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado. Redirigiendo a LoginActivity.");
            Toast.makeText(this, "Debe iniciar sesión para ver el historial.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Finaliza esta actividad para que no se quede en el stack
            return; // Detener la ejecución de onCreate si no hay usuario
        }

        // Inicializar RecyclerView
        recyclerView = findViewById(R.id.recyclerHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UbicacionAdapter(listaUbicaciones); // Usar el nombre de lista actualizado
        recyclerView.setAdapter(adapter);

        // Obtener la referencia a la base de datos para las ubicaciones del usuario actual
        String userId = currentUser.getUid(); // Obtener el UID del usuario logueado
        Log.d(TAG, "Cargando historial para el usuario: " + userId);

        locationsRef = FirebaseDatabase.getInstance()
                .getReference("artifacts")
                .child(APP_ID)
                .child("users")
                .child(userId)
                .child("locations");

        // Añadir un ValueEventListener para escuchar cambios en los datos
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaUbicaciones.clear(); // Limpiar la lista antes de añadir nuevos datos
                if (snapshot.exists()) {
                    for (DataSnapshot dato : snapshot.getChildren()) {
                        try {
                            Ubicacion ubicacion = dato.getValue(Ubicacion.class);
                            if (ubicacion != null) {
                                listaUbicaciones.add(0, ubicacion); // Añadir al principio para mostrar las más recientes primero
                            } else {
                                Log.w(TAG, "No se pudo parsear Ubicacion de: " + dato.getKey());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al parsear Ubicacion de DataSnapshot: " + dato.getKey(), e);
                        }
                    }
                    Log.d(TAG, "Historial cargado. Total de ubicaciones: " + listaUbicaciones.size());
                    adapter.notifyDataSetChanged(); // Notificar al adaptador que los datos han cambiado
                } else {
                    Log.d(TAG, "No hay ubicaciones para el usuario: " + userId);
                    Toast.makeText(HistorialActivity.this, "No hay ubicaciones en el historial.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar historial desde Firebase: " + error.getMessage(), error.toException());
                Toast.makeText(HistorialActivity.this, "Error al cargar historial: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        MaterialButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    // Opcional: Si quieres que la barra de acción muestre el botón de retroceso
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Cierra esta actividad y vuelve a la anterior
        return true;
    }
}
