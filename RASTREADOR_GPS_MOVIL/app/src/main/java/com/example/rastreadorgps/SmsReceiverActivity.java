package com.example.rastreadorgps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiverActivity extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String GPS_TRACKER_NUMBER = "+593XXXXXXXXX"; // ¡Reemplaza con el número exacto de tu GPS Coban!

    @Override
    public void onReceive(Context context, Intent intent) {
        // Verifica que la acción sea la de SMS recibido
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus == null) {
                    Log.e(TAG, "No PDU found in SMS bundle.");
                    return;
                }

                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    String senderNum = smsMessage.getDisplayOriginatingAddress();
                    String messageBody = smsMessage.getMessageBody();

                    Log.d(TAG, "SMS Recibido de: " + senderNum + ", Mensaje: " + messageBody);

                    // Filtra mensajes solo del número de tu GPS Coban
                    if (senderNum != null && senderNum.equals(GPS_TRACKER_NUMBER)) {
                        Log.d(TAG, "Mensaje del GPS Coban detectado. Procesando...");
                        parseGpsMessage(context, messageBody);
                    } else {
                        Log.d(TAG, "SMS no es del GPS Coban o número no coincide.");
                    }
                }
            } else {
                Log.e(TAG, "SMS bundle is null.");
            }
        }
    }

    /**
     * Parsea el cuerpo del mensaje SMS para extraer coordenadas o URL de mapa.
     * Envía un Intent a MainActivity con los datos.
     * @param context El contexto de la aplicación.
     * @param messageBody El cuerpo del mensaje SMS.
     */
    private void parseGpsMessage(Context context, String messageBody) {
        double latitude = 0.0;
        double longitude = 0.0;
        boolean coordinatesFound = false;

        // Intenta parsear el formato "Lat:XX.XXXX,Lon:YY.YYYY,Speed:ZZkm/h,Time:..."
        if (messageBody.contains("Lat:") && messageBody.contains("Lon:")) {
            try {
                String[] parts = messageBody.split(",");
                for (String part : parts) {
                    if (part.startsWith("Lat:")) {
                        latitude = Double.parseDouble(part.substring(4).trim());
                    } else if (part.startsWith("Lon:")) {
                        longitude = Double.parseDouble(part.substring(4).trim());
                    }
                }
                coordinatesFound = true;
                Log.d(TAG, "Coordenadas extraídas (formato Lat/Lon): Lat " + latitude + ", Lon " + longitude);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error al parsear coordenadas del formato Lat/Lon: " + e.getMessage());
            }
        }

        // Si no se encontraron coordenadas, intenta parsear una URL de Google Maps
        if (!coordinatesFound && messageBody.contains("maps.google.com")) {
            // Patrón para extraer latitud y longitud de una URL de Google Maps
            Pattern pattern = Pattern.compile("q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(messageBody);
            if (matcher.find()) {
                try {
                    latitude = Double.parseDouble(matcher.group(1));
                    longitude = Double.parseDouble(matcher.group(2));
                    coordinatesFound = true;
                    Log.d(TAG, "Coordenadas extraídas (formato Google Maps URL): Lat " + latitude + ", Lon " + longitude);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error al parsear coordenadas de la URL de Google Maps: " + e.getMessage());
                }
            }
        }

        // Envía los datos a MainActivity
        Intent mapIntent = new Intent(context, MainActivity.class);
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Necesario para iniciar una Activity desde un BroadcastReceiver

        if (coordinatesFound) {
            mapIntent.putExtra("latitude", latitude);
            mapIntent.putExtra("longitude", longitude);
            mapIntent.putExtra("message_type", "coordinates"); // Indica que se enviaron coordenadas
        } else {
            // Si no se pudieron extraer coordenadas, envía el mensaje completo para depuración o visualización
            mapIntent.putExtra("raw_sms_message", messageBody);
            mapIntent.putExtra("message_type", "raw_sms"); // Indica que se envió el SMS crudo
            Log.w(TAG, "No se pudieron extraer coordenadas. Enviando mensaje SMS completo a MainActivity.");
        }

        context.startActivity(mapIntent);
    }
}