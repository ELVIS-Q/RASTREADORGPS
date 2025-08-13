package com.example.rastreadorgps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100; // Unificado para todos los permisos
    private static final String GPS_NUMBER = "+59399 319 2876"; // ¡Reemplaza con el número exacto de tu GPS Coban!
    // ID de la aplicación para la estructura de la base de datos (ajusta si es necesario)
    private static final String APP_ID = "rastreadorgps_app";

    private TextView locationTextView;
    private Button requestDeviceLocationButton;
    private MaterialButton sendCobanSmsButton;
    private MaterialButton btnVerHistorial;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(); // Declarado como final
    private FirebaseAuth mAuth;
    private MaterialButton logoutButton;

    // Variables para almacenar la última ubicación conocida
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Verificar si el usuario está logueado, si no, redirigir a LoginActivity
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Finaliza MainActivity para que no pueda volver con el botón atrás
            return; // Importante para detener la ejecución si no hay usuario
        }

        // 1. Inicializar vistas
        locationTextView = findViewById(R.id.locationTextView);
        requestDeviceLocationButton = findViewById(R.id.requestLocationButton);
        sendCobanSmsButton = findViewById(R.id.sendCobanSmsButton);
        btnVerHistorial = findViewById(R.id.btnVerHistorial);
        logoutButton = findViewById(R.id.logoutButton);

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtener el nombre de usuario
        String userName = getIntent().getStringExtra("user");
        if (userName != null && !userName.isEmpty()) {
            setTitle(getString(R.string.user_title_format, userName)); // Usar string resource si lo tienes
        } else {
            setTitle(getString(R.string.app_name)); // Título por defecto
        }

        // 2. Solicitar permisos en tiempo de ejecución (SMS y Ubicación)
        checkAllPermissions();

        // 3. Configurar el botón para solicitar ubicación del PROPIO DISPOSITIVO
        requestDeviceLocationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MainActivity.this, location -> {
                            if (location != null) {
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();

                                // Usar recurso de string con placeholders
                                String locText = getString(R.string.device_location_format, currentLatitude, currentLongitude);
                                locationTextView.setText(locText);
                                Toast.makeText(MainActivity.this, getString(R.string.location_obtained_device), Toast.LENGTH_SHORT).show(); // Nuevo string

                                // Guardar en Realtime Database
                                guardarUbicacionEnRealtimeDatabase(currentLatitude, currentLongitude, "device_gps");

                                updateMapLocation(currentLatitude, currentLongitude);
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.location_not_obtained_device), Toast.LENGTH_LONG).show();
                                Log.w(TAG, "getLastLocation returned null.");
                            }
                        })
                        .addOnFailureListener(MainActivity.this, e -> {
                            Toast.makeText(MainActivity.this, getString(R.string.error_getting_device_location, e.getMessage()), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error getting last location", e);
                        });
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.location_permission_not_granted), Toast.LENGTH_SHORT).show();
                checkAllPermissions(); // Re-solicitar permisos
            }
        });

        // 4. Configurar el botón para ENVIAR SMS al GPS Coban
        if (sendCobanSmsButton != null) {
            sendCobanSmsButton.setOnClickListener(v -> {
                sendSms(GPS_NUMBER, "URL#"); // Comando común para pedir URL de ubicación al Coban
            });
        }

        // 5. Configurar el botón para ver el historial
        btnVerHistorial.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistorialActivity.class);
            startActivity(intent);
        });

        // 6. Inicializar Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 7. Manejar el Intent inicial si la actividad se lanzó con datos de SMS
        handleIntent(getIntent());
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Cierra la sesión del usuario en Firebase
            Toast.makeText(MainActivity.this, "Sesión cerrada.", Toast.LENGTH_SHORT).show();
            // Redirige a la LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpia el historial de actividades
            startActivity(intent);
            finish(); // Finaliza MainActivity para que no se pueda volver con el botón atrás
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Actualiza el Intent de la actividad
        handleIntent(intent);
    }

    // Método para procesar los datos de ubicación recibidos del SmsReceiver
    private void handleIntent(Intent intent) {
        if (intent != null) {
            String messageType = intent.getStringExtra("message_type");

            if ("coordinates".equals(messageType)) {
                double latitude = intent.getDoubleExtra("latitude", 0.0);
                double longitude = intent.getDoubleExtra("longitude", 0.0);

                // Usar recurso de string con placeholders
                String locationText = getString(R.string.coban_location_format, latitude, longitude);
                locationTextView.setText(locationText);
                Toast.makeText(this, getString(R.string.coban_location_received), Toast.LENGTH_SHORT).show();

                // Guardar en Realtime Database
                guardarUbicacionEnRealtimeDatabase(latitude, longitude, "gps_coban");

                if (googleMap != null) {
                    updateMapLocation(latitude, longitude);
                }

            } else if ("raw_sms".equals(messageType)) {
                String rawMessage = intent.getStringExtra("raw_sms_message");
                // Usar recurso de string con placeholders
                locationTextView.setText(getString(R.string.raw_coban_message_format, rawMessage));
                Toast.makeText(this, getString(R.string.coban_raw_message_received, rawMessage), Toast.LENGTH_LONG).show();
                Log.w(TAG, "Received raw SMS from Coban: " + rawMessage);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        if (currentLatitude != 0.0 || currentLongitude != 0.0) {
            updateMapLocation(currentLatitude, currentLongitude);
        } else {
            LatLng defaultLocation = new LatLng(-0.1807, -78.4678); // Quito, Ecuador
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        }
    }

    private void updateMapLocation(double latitude, double longitude) {
        LatLng newLocation = new LatLng(latitude, longitude);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(newLocation).title("Ubicación del Vehículo"));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 15));
    }

    /**
     * Guarda la ubicación (latitud, longitud, timestamp y fuente) en Firebase Realtime Database.
     * La ubicación se guarda bajo el ID del usuario actual.
     * Path: /artifacts/{appId}/users/{userId}/locations
     * @param latitude Latitud de la ubicación.
     * @param longitude Longitud de la ubicación.
     * @param source Fuente de la ubicación (ej. "gps_coban", "device_gps").
     */
    private void guardarUbicacionEnRealtimeDatabase(double latitude, double longitude, String source) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, getString(R.string.user_not_authenticated_save_location));
            Toast.makeText(this, getString(R.string.must_login_to_save_locations), Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid(); // ID del usuario autenticado

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("timestamp", System.currentTimeMillis()); // Marca de tiempo en milisegundos
        locationData.put("formattedTimestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        locationData.put("source", source);

        // La estructura será: /artifacts/{APP_ID}/users/{userId}/locations/{pushId}
        mDatabase.child("artifacts").child(APP_ID).child("users").child(userId).child("locations")
                .push() // Genera una clave única para cada ubicación
                .setValue(locationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, getString(R.string.location_saved_rtdb));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, getString(R.string.error_saving_location_rtdb, e.getMessage()), e);
                    Toast.makeText(MainActivity.this, getString(R.string.error_saving_location_rtdb, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void checkAllPermissions() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show();
                if (googleMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, getString(R.string.some_permissions_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendSms(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(getApplicationContext(), getString(R.string.command_sms_sent_coban), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_to_send_sms_coban, e.getMessage()), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error sending SMS", e);
            }
        } else {
            Toast.makeText(this, getString(R.string.sms_permission_not_granted), Toast.LENGTH_SHORT).show();
            checkAllPermissions();
        }
    }
}
