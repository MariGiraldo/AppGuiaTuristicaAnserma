package com.example.anserview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActivityMapa extends AppCompatActivity {

    private MapView mapView;
    private Button btn_lugares;
    private Marker markerPersonaje;
    private static final int Codigo_COARSE = 500;
    private TextView tv_distancia;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    private final String QUERY_HOTELES = "[out:json]; area[\"name\"=\"Anserma\"][\"boundary\"=\"administrative\"]->.searchArea;"
            + "("
            + "  node[\"tourism\"=\"hotel\"](area.searchArea);"
            + "  node[\"tourism\"=\"guest_house\"](area.searchArea);"
            + "  node[\"tourism\"=\"motel\"](area.searchArea);"
            + "  node[\"tourism\"=\"camp_site\"](area.searchArea);"
            + "  way[\"tourism\"=\"hotel\"](area.searchArea);"
            + "  way[\"tourism\"=\"guest_house\"](area.searchArea);"
            + "  way[\"tourism\"=\"motel\"](area.searchArea);"
            + "  way[\"tourism\"=\"camp_site\"](area.searchArea);"
            + "  relation[\"tourism\"=\"hotel\"](area.searchArea);"
            + "  relation[\"tourism\"=\"guest_house\"](area.searchArea);"
            + "  relation[\"tourism\"=\"motel\"](area.searchArea);"
            + "  relation[\"tourism\"=\"camp_site\"](area.searchArea);"
            + ");"
            + "out body; >; out skel qt;";

    private final String QUERY_COMIDA = "[out:json]; area[\"name\"=\"Anserma\"][\"boundary\"=\"administrative\"]->.searchArea;"
            + "("
            + "  node[\"amenity\"=\"restaurant\"](area.searchArea);"
            + "  node[\"amenity\"=\"fast_food\"](area.searchArea);"
            + "  node[\"amenity\"=\"cafe\"](area.searchArea);"
            + "  node[\"amenity\"=\"bar\"](area.searchArea);"
            + "  way[\"amenity\"=\"restaurant\"](area.searchArea);"
            + "  way[\"amenity\"=\"fast_food\"](area.searchArea);"
            + "  way[\"amenity\"=\"cafe\"](area.searchArea);"
            + "  way[\"amenity\"=\"bar\"](area.searchArea);"
            + "  relation[\"amenity\"=\"restaurant\"](area.searchArea);"
            + "  relation[\"amenity\"=\"fast_food\"](area.searchArea);"
            + "  relation[\"amenity\"=\"cafe\"](area.searchArea);"
            + "  relation[\"amenity\"=\"bar\"](area.searchArea);"
            + ");"
            + "out body; >; out skel qt;";

    private final String QUERY_RECREACION = "[out:json]; area[\"name\"=\"Anserma\"][\"boundary\"=\"administrative\"]->.searchArea;"
            + "("
            + "  node[\"leisure\"=\"park\"](area.searchArea);"
            + "  node[\"leisure\"=\"pitch\"](area.searchArea);"
            + "  node[\"leisure\"=\"swimming_pool\"](area.searchArea);"
            + "  way[\"leisure\"=\"park\"](area.searchArea);"
            + "  way[\"leisure\"=\"pitch\"](area.searchArea);"
            + "  way[\"leisure\"=\"swimming_pool\"](area.searchArea);"
            + "  relation[\"leisure\"=\"park\"](area.searchArea);"
            + "  relation[\"leisure\"=\"pitch\"](area.searchArea);"
            + "  relation[\"leisure\"=\"swimming_pool\"](area.searchArea);"
            + ");"
            + "out body; >; out skel qt;";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        setContentView(R.layout.activity_mapa);

        mapView = findViewById(R.id.mapView);
        btn_lugares = findViewById(R.id.btn_favoritos);
        tv_distancia = findViewById(R.id.tv_distancia);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);
        mapView.getController().setCenter(new GeoPoint(5.2353, -75.7919)); // Centro de Anserma

        ejecutarConsultaOverpass(QUERY_HOTELES, "Hotel");
        ejecutarConsultaOverpass(QUERY_COMIDA, "Comida");
        ejecutarConsultaOverpass(QUERY_RECREACION, "Recreación");

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location ubicacion : locationResult.getLocations()) {
                    actualizarUbicacion(ubicacion);
                }
            }
        };

        PedirPermisosDeUbicacion();
    }

    private void PedirPermisosDeUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        double lat = ubicacion.getLatitude();
        double lon = ubicacion.getLongitude();
        GeoPoint punto = new GeoPoint(lat, lon);

        if (markerPersonaje == null) {
            markerPersonaje = new Marker(mapView);
            markerPersonaje.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            markerPersonaje.setTitle("Estoy aquí");
            mapView.getOverlays().add(markerPersonaje);
        }

        markerPersonaje.setPosition(punto);
        mapView.getController().setCenter(punto);
        mapView.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Codigo_COARSE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarActualizacionesUbicacion();
        }
    }

    private void ejecutarConsultaOverpass(String consulta, String tipoLugar) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://overpass-api.de/api/interpreter?data=" + Uri.encode(consulta);

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray elements = jsonObject.getAsJsonArray("elements");

                    runOnUiThread(() -> {
                        for (JsonElement element : elements) {
                            JsonObject obj = element.getAsJsonObject();

                            double lat, lon;
                            if (obj.has("lat") && obj.has("lon")) {
                                lat = obj.get("lat").getAsDouble();
                                lon = obj.get("lon").getAsDouble();
                            } else if (obj.has("center")) {
                                JsonObject center = obj.getAsJsonObject("center");
                                lat = center.get("lat").getAsDouble();
                                lon = center.get("lon").getAsDouble();
                            } else {
                                continue;
                            }

                            // Extraer nombre real o fallback
                            String nombre = tipoLugar;
                            if (obj.has("tags")) {
                                JsonObject tags = obj.getAsJsonObject("tags");
                                if (tags.has("name")) {
                                    nombre = tags.get("name").getAsString();
                                } else if (tags.has("brand")) {
                                    nombre = tags.get("brand").getAsString();
                                } else if (tags.has("operator")) {
                                    nombre = tags.get("operator").getAsString();
                                } else if (tags.has("description")) {
                                    nombre = tags.get("description").getAsString();
                                } else {
                                    // Último recurso: mostrar tipo exacto como categoría
                                    if (tags.has("tourism")) {
                                        nombre = tipoLugar + " (" + tags.get("tourism").getAsString() + ")";
                                    } else if (tags.has("amenity")) {
                                        nombre = tipoLugar + " (" + tags.get("amenity").getAsString() + ")";
                                    } else if (tags.has("leisure")) {
                                        nombre = tipoLugar + " (" + tags.get("leisure").getAsString() + ")";
                                    }
                                }
                            }

                            int icono = R.drawable.ic_marker_default;
                            if (tipoLugar.equals("Hotel")) icono = R.drawable.ic_hotel;
                            else if (tipoLugar.equals("Comida")) icono = R.drawable.ic_food;
                            else if (tipoLugar.equals("Recreación")) icono = R.drawable.ic_recreation;

                            agregarMarcador(lat, lon, nombre, icono);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void agregarMarcador(double lat, double lon, String titulo, int iconoResId) {
        GeoPoint punto = new GeoPoint(lat, lon);
        Marker marker = new Marker(mapView);
        marker.setPosition(punto);
        marker.setTitle(titulo);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(iconoResId, getTheme()));
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }
}
