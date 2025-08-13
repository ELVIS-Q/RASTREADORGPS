package com.example.rastreadorgps;
public class Ubicacion {
    private double latitude;
    private double longitude;
    private long timestamp; // Usamos long para System.currentTimeMillis()
    private String formattedTimestamp; // Para la fecha y hora legible
    private String source; // "gps_coban" o "device_gps"

    // Constructor vacío requerido por Firebase Realtime Database
    public Ubicacion() {
        // Default constructor required for calls to DataSnapshot.getValue(Ubicacion.class)
    }

    // Constructor con todos los campos
    public Ubicacion(double latitude, double longitude, long timestamp, String formattedTimestamp, String source) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.formattedTimestamp = formattedTimestamp;
        this.source = source;
    }

    // Getters para acceder a los datos
    // Asegúrate de que los nombres de los getters sigan la convención de JavaBeans
    // (getCampo) para que Firebase pueda deserializar correctamente.
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return formattedTimestamp;
    }

    public String getSource() {
        return source;
    }

    // Setters (opcionales si solo vas a leer, pero útiles para Firebase deserialización)
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setFormattedTimestamp(String formattedTimestamp) {
        this.formattedTimestamp = formattedTimestamp;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
