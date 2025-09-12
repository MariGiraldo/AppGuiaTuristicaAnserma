package com.example.anserview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.picasso.Picasso;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActivityMapa extends AppCompatActivity {
    private MapView mapView;
    private Marker myLocationMarker;
    private FloatingActionButton fabMyLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private OkHttpClient httpClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 500;
    private static final String TAG = "ActivityMapa";

    private enum PlaceType {
        HOTELS, FOOD, RECREATION
    }

    private final String overpassQueryTemplate = "[out:json];"
            + "area[\"name\"=\"Anserma\"][\"boundary\"=\"administrative\"]->.searchArea;"
            + "(%s);"
            + "out body; >; out skel qt;";

    private final String queryNodes = "node[%s](area.searchArea);";
    private final String queryWays = "way[%s](area.searchArea);";
    private final String queryRelations = "relation[%s](area.searchArea);";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        httpClient = new OkHttpClient();

        setupUI();
        setupMap();
        setupLocationUpdates();

        loadPlaces(PlaceType.HOTELS);
        loadPlaces(PlaceType.FOOD);
        loadPlaces(PlaceType.RECREATION);
    }

    private void setupUI() {
        mapView = findViewById(R.id.mapView);
        fabMyLocation = findViewById(R.id.fab_my_location);

        fabMyLocation.setOnClickListener(v -> {
            if (myLocationMarker != null) {
                mapView.getController().animateTo(myLocationMarker.getPosition());
            } else {
                Toast.makeText(this, "Esperando ubicación...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMap() {
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);
        mapView.getController().setCenter(new GeoPoint(5.2353, -75.7919)); // Centro de Anserma
    }

    private void setupLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    updateMyLocation(location);
                }
            }
        };
        requestLocationPermissions();
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Permisos de ubicación denegados. La app no podrá mostrar tu posición.", Toast.LENGTH_LONG).show();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(3000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateMyLocation(Location location) {
        if (location == null) return;
        GeoPoint myGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        if (myLocationMarker == null) {
            myLocationMarker = new Marker(mapView);
            myLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_place, getTheme()));
            myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            myLocationMarker.setTitle("Estoy aquí");
            mapView.getOverlays().add(myLocationMarker);
        }

        myLocationMarker.setPosition(myGeoPoint);
        mapView.invalidate();
    }

    private void loadPlaces(PlaceType type) {
        String overpassTags;
        switch (type) {
            case HOTELS:
                overpassTags =
                        String.format(queryNodes, "\"tourism\"=\"hotel\"") +
                                String.format(queryNodes, "\"tourism\"=\"guest_house\"") +
                                String.format(queryWays, "\"tourism\"=\"hotel\"") +
                                String.format(queryWays, "\"tourism\"=\"guest_house\"") +
                                String.format(queryRelations, "\"tourism\"=\"hotel\"") +
                                String.format(queryRelations, "\"tourism\"=\"guest_house\"");
                break;
            case FOOD:
                overpassTags =
                        String.format(queryNodes, "\"amenity\"=\"restaurant\"") +
                                String.format(queryNodes, "\"amenity\"=\"fast_food\"") +
                                String.format(queryNodes, "\"amenity\"=\"cafe\"") +
                                String.format(queryNodes, "\"amenity\"=\"bar\"") +
                                String.format(queryNodes, "\"amenity\"=\"pub\"") +
                                String.format(queryWays, "\"amenity\"=\"restaurant\"") +
                                String.format(queryWays, "\"amenity\"=\"fast_food\"") +
                                String.format(queryWays, "\"amenity\"=\"cafe\"") +
                                String.format(queryWays, "\"amenity\"=\"bar\"") +
                                String.format(queryWays, "\"amenity\"=\"pub\"") +
                                String.format(queryRelations, "\"amenity\"=\"restaurant\"") +
                                String.format(queryRelations, "\"amenity\"=\"fast_food\"") +
                                String.format(queryRelations, "\"amenity\"=\"cafe\"") +
                                String.format(queryRelations, "\"amenity\"=\"bar\"") +
                                String.format(queryRelations, "\"amenity\"=\"pub\"");
                break;
            case RECREATION:
                overpassTags =
                        String.format(queryNodes, "\"leisure\"=\"park\"") +
                                String.format(queryNodes, "\"leisure\"=\"playground\"") +
                                String.format(queryNodes, "\"tourism\"=\"attraction\"") +
                                String.format(queryNodes, "\"leisure\"=\"stadium\"") +
                                String.format(queryWays, "\"leisure\"=\"park\"") +
                                String.format(queryWays, "\"leisure\"=\"playground\"") +
                                String.format(queryWays, "\"tourism\"=\"attraction\"") +
                                String.format(queryWays, "\"leisure\"=\"stadium\"") +
                                String.format(queryRelations, "\"leisure\"=\"park\"") +
                                String.format(queryRelations, "\"leisure\"=\"playground\"") +
                                String.format(queryRelations, "\"tourism\"=\"attraction\"") +
                                String.format(queryRelations, "\"leisure\"=\"stadium\"");
                break;
            default:
                return;
        }

        String query = String.format(overpassQueryTemplate, overpassTags);
        executeOverpassQuery(query, type);
    }

    private void executeOverpassQuery(String query, PlaceType type) {
        String url = "https://overpass-api.de/api/interpreter?data=" + Uri.encode(query);
        Log.d(TAG, "Overpass URL: " + url);

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray elements = jsonObject.getAsJsonArray("elements");

                    runOnUiThread(() -> {
                        for (JsonElement element : elements) {
                            JsonObject obj = element.getAsJsonObject();
                            addMarkerFromElement(obj, type);
                        }
                        mapView.invalidate();
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error ejecutando la consulta Overpass", e);
                runOnUiThread(() -> Toast.makeText(this, "Error de red al cargar lugares.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void addMarkerFromElement(JsonObject obj, PlaceType type) {
        double lat, lon;
        if (obj.has("lat") && obj.has("lon")) {
            lat = obj.get("lat").getAsDouble();
            lon = obj.get("lon").getAsDouble();
        } else if (obj.has("center")) {
            JsonObject center = obj.getAsJsonObject("center");
            lat = center.get("lat").getAsDouble();
            lon = center.get("lon").getAsDouble();
        } else {
            return;
        }

        String name = getPlaceName(obj);
        int iconResId = getIconForPlaceType(type);

        GeoPoint point = new GeoPoint(lat, lon);
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(name);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(iconResId, getTheme()));

        Log.d(TAG, "Marcador añadido: " + name);

        marker.setOnMarkerClickListener((m, map) -> {
            showPlaceInfo(marker, lat, lon);
            return true;
        });

        mapView.getOverlays().add(marker);
    }

    private void showPlaceInfo(Marker marker, double lat, double lon) {
        String wikiUrl = "https://es.wikipedia.org/w/api.php?action=query&prop=extracts|pageimages&exintro=1&explaintext=1&pithumbsize=200&format=json&generator=geosearch&ggsradius=1000&ggslimit=1&ggscoord="
                + lat + "|" + lon;

        new Thread(() -> {
            String description = "Sin descripción disponible";
            String imageUrl = null;

            try {
                Request request = new Request.Builder().url(wikiUrl).build();
                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                    if (root.has("query")) {
                        JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
                        for (String key : pages.keySet()) {
                            JsonObject page = pages.getAsJsonObject(key);
                            if (page.has("extract")) {
                                description = page.get("extract").getAsString();
                            }
                            if (page.has("thumbnail")) {
                                imageUrl = page.getAsJsonObject("thumbnail").get("source").getAsString();
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al consultar Wikipedia", e);
            }

            String finalDescription = description;
            String finalImageUrl = imageUrl;

            runOnUiThread(() -> {
                showInfoWindowWithDetails(marker, marker.getTitle(), finalDescription, finalImageUrl);
            });
        }).start();
    }

    private void showInfoWindowWithDetails(Marker marker, String title, String description, String imageUrl) {
        InfoWindow.closeAllInfoWindowsOn(mapView);

        InfoWindow infoWindow = new InfoWindow(R.layout.infowindow_place_detail, mapView) {
            @Override
            public void onOpen(Object item) {
                View v = mView;
                TextView tvTitle = v.findViewById(R.id.iw_title);
                TextView tvDesc = v.findViewById(R.id.iw_description);
                ImageView ivImage = v.findViewById(R.id.iw_image);

                tvTitle.setText(title);
                tvDesc.setText(description);

                if (imageUrl != null) {
                    ivImage.setVisibility(View.VISIBLE);
                    Picasso.get().load(imageUrl).into(ivImage);
                } else {
                    ivImage.setVisibility(View.GONE);
                }
            }

            @Override
            public void onClose() {}
        };

        marker.setInfoWindow(infoWindow);
        marker.showInfoWindow();
    }

    private String getPlaceName(JsonObject obj) {
        if (obj.has("tags")) {
            JsonObject tags = obj.getAsJsonObject("tags");
            if (tags.has("name")) return tags.get("name").getAsString();
            if (tags.has("brand")) return tags.get("brand").getAsString();
            if (tags.has("operator")) return tags.get("operator").getAsString();
        }
        return "Lugar no especificado";
    }

    private int getIconForPlaceType(PlaceType type) {
        switch (type) {
            case HOTELS:
                return R.drawable.ic_hotel;
            case FOOD:
                return R.drawable.ic_food;
            case RECREATION:
                return R.drawable.ic_recreation;
            default:
                return R.drawable.ic_marker_default;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
