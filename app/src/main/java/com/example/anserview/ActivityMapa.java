package com.example.anserview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

public class ActivityMapa extends AppCompatActivity {

    private MapView mapView;
    private Marker markerUsuario;
    private static final int Codigo_COARSE = 500;
    private TextView tv_distancia;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        setContentView(R.layout.activity_mapa);

        // Inicializar vistas
        mapView = findViewById(R.id.mapView);
        tv_distancia = findViewById(R.id.tv_distancia);

        // Configuración del mapa
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Inicializar ubicación y red
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);

        // Callback de ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location ubicacion : locationResult.getLocations()) {
                    actualizarUbicacion(ubicacion);
                }
            }
        };

        // Pedir permisos
        pedirPermisosDeUbicacion();

        // Cargar POIs desde Overpass
        cargarPOIsDesdeOverpass();
    }

    private void pedirPermisosDeUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.INTERNET
                    },
                    Codigo_COARSE
            );
        } else {
            iniciarActualizacionesUbicacion();
        }
    }

    private void iniciarActualizacionesUbicacion() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void actualizarUbicacion(Location ubicacion) {
        if (ubicacion == null) return;

        double latitud = ubicacion.getLatitude();
        double longitud = ubicacion.getLongitude();

        GeoPoint punto = new GeoPoint(latitud, longitud);

        if (markerUsuario == null) {
            markerUsuario = new Marker(mapView);
            markerUsuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            markerUsuario.setTitle("Estoy aquí");
            markerUsuario.setIcon(getResources().getDrawable(R.drawable.ic_person_pin, getTheme()));
            mapView.getOverlays().add(markerUsuario);
        }

        markerUsuario.setPosition(punto);

        // Animar la cámara en lugar de setCenter directo
        mapView.getController().animateTo(punto, 16.0, 1500L);
        mapView.invalidate();
    }

    private void cargarPOIsDesdeOverpass() {
        String overpassQuery = "[out:json][timeout:25];"
                + "area[\"name\"=\"Anserma\"][admin_level=8];"
                + "("
                + " node[\"amenity\"=\"restaurant\"](area);"
                + " node[\"tourism\"=\"hotel\"](area);"
                + " node[\"tourism\"=\"attraction\"](area);"
                + ");"
                + "out center;";

        String url = "https://overpass-api.de/api/interpreter?data=" +
                encodeURIComponent(overpassQuery);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        procesarRespuestaPOIs(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ActivityMapa.this,
                                "Error cargando POIs: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("Overpass", "Error: ", error);
                    }
                }
        );

        requestQueue.add(request);
    }

    private void procesarRespuestaPOIs(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONArray elements = json.getJSONArray("elements");
            for (int i = 0; i < elements.length(); i++) {
                JSONObject elem = elements.getJSONObject(i);
                double lat, lon;
                String name = null;

                if (elem.has("lat") && elem.has("lon")) {
                    lat = elem.getDouble("lat");
                    lon = elem.getDouble("lon");
                } else if (elem.has("center")) {
                    JSONObject center = elem.getJSONObject("center");
                    lat = center.getDouble("lat");
                    lon = center.getDouble("lon");
                } else {
                    continue;
                }

                if (elem.has("tags")) {
                    JSONObject tags = elem.getJSONObject("tags");
                    if (tags.has("name")) {
                        name = tags.getString("name");
                    }
                }

                GeoPoint punto = new GeoPoint(lat, lon);
                Marker marker = new Marker(mapView);
                marker.setPosition(punto);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setIcon(getResources().getDrawable(R.drawable.ic_place, getTheme()));
                marker.setTitle(name != null ? name : "POI");

                mapView.getOverlays().add(marker);
            }
            mapView.invalidate();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error procesando POIs", Toast.LENGTH_LONG).show();
        }
    }

    private String encodeURIComponent(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Codigo_COARSE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarActualizacionesUbicacion();
        }
    }
}
