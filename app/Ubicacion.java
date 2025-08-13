public class Ubicacion {
    public double lat;
    public double lng;
    public String timestamp;

    public Ubicacion() {} // Constructor vacío requerido por Firebase

    public Ubicacion(double lat, double lng, String timestamp) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }
}
