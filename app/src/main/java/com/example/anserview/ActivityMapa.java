package com.example.anserview;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    //    private static final int REQUEST_IMAGE_CAPTURE = 1001;
//
//    private String pendingName;
//    private String pendingDescription;
//    private double pendingLat, pendingLon;
//    private Bitmap capturedBitmap;
    private ActivityResultLauncher<Void> cameraLauncher;
    private Bitmap capturedImage = null;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private Button fab_add_place, fab_view_places, fab_my_location;

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

        fab_add_place = findViewById(R.id.fab_add_place);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                result -> {
                    if (result != null) {
                        capturedImage = result;
                        Toast.makeText(this, "Foto capturada", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        loadSavedPlaces();


        // Inicializa el lanzador de permisos
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // El permiso fue concedido. Lanza la cámara.
                        Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show();
                        cameraLauncher.launch(null); // Llama al lanzador de la cámara
                    } else {
                        // El permiso fue denegado. Informa al usuario.
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                });


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


        fab_add_place = findViewById(R.id.fab_add_place);
        fab_add_place.setOnClickListener(v -> showAddPlaceDialog());

    }
    private void showAddPlaceDialog() {
        // Layout dinámico dentro del diálogo
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_place, null);

        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        Button btnTakePhoto = dialogView.findViewById(R.id.btn_take_photo);

        btnTakePhoto.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // El permiso ya está concedido, simplemente lanza la cámara
                cameraLauncher.launch(null);
            } else {
                // Solicita el permiso
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }

        });

        new AlertDialog.Builder(this)
                .setTitle("Agregar lugar")
                .setView(dialogView)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    if (name.isEmpty()) name = "Lugar sin nombre";
                    if (description.isEmpty()) description = "Sin descripción";

                    if (myLocationMarker != null) {
                        GeoPoint point = myLocationMarker.getPosition();
                        addCustomMarker(point, name, description, capturedImage);

                        // Guardar en BD
                        MyDatabaseHelper dbHelper = new MyDatabaseHelper(this, "AppDB", null, 1);
                        byte[] fotoBytes = null;
                        if (capturedImage != null) {
                            fotoBytes = bitmapToBytes(capturedImage);
                        }
                        ContentValues registro = new ContentValues();
                        registro.put("nombre", name);
                        registro.put("descripcion", description);
                        registro.put("lat", point.getLatitude());
                        registro.put("lon", point.getLongitude());
                        registro.put("imagen", fotoBytes);

                        long resultado = dbHelper.insert("Lugares", null, registro);
                        dbHelper.close();

                        if (resultado != -1) {
                            Toast.makeText(this, "Lugar guardado en la base de datos", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error al guardar lugar", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No se detectó ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                    capturedImage = null; // Reset
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private byte[] bitmapToBytes(Bitmap bitmap) {
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void addCustomMarker(GeoPoint point, String name, String description, Bitmap photo) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(name);



        MyDatabaseHelper adminBD = new MyDatabaseHelper(this,"administradorBD", null, 1);
        SQLiteDatabase baseDeDatos = adminBD.getWritableDatabase();
        String nombre = name;
        String descripcion = description;
        Location ubicacion = new Location("manual");
        ubicacion.setLatitude(point.getLatitude());
        ubicacion.setLongitude(point.getLongitude());

        bitmapToBytes(photo);


        marker.setOnMarkerClickListener((m, map) -> {
            if (m.isInfoWindowShown()) {
                // Si ya está abierto → lo cierra
                m.closeInfoWindow();
            } else {
                // Si está cerrado → lo abre
                InfoWindow infoWindow = new InfoWindow(R.layout.infowindow_place_detail, mapView) {
                    @Override
                    public void onOpen(Object item) {
                        View v = mView;
                        TextView tvTitle = v.findViewById(R.id.iw_title);
                        TextView tvDesc = v.findViewById(R.id.iw_description);
                        ImageView ivImage = v.findViewById(R.id.iw_image);

                        tvTitle.setText(name);
                        tvDesc.setText(description);

                        if (photo != null) {
                            ivImage.setVisibility(View.VISIBLE);
                            ivImage.setImageBitmap(photo);
                        } else {
                            ivImage.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onClose() {}
                };

                m.setInfoWindow(infoWindow);
                m.showInfoWindow();
            }
            return true;
        });


        mapView.getOverlays().add(marker);
        mapView.invalidate();
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

        // Añadir el marcador ahora (sin descripción todavía)
        mapView.getOverlays().add(marker);
        mapView.invalidate();

        // Llamar a Wikipedia para obtener la descripción y/o imagen
        new Thread(() -> {
            String description = type.toString();
            String imageUrl = null;

            try {
                String wikiUrl = "https://es.wikipedia.org/w/api.php?action=query&prop=extracts|pageimages&exintro=1&explaintext=1&pithumbsize=200&format=json&generator=geosearch&ggsradius=1000&ggslimit=1&ggscoord="
                        + lat + "|" + lon;

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


            runOnUiThread(() -> {
                marker.setOnMarkerClickListener((m, map) -> {
                    if (m.isInfoWindowShown()) {
                        m.closeInfoWindow();
                    } else {
                        showPlaceInfo(m, lat, lon);
                    }
                    return true;
                });
            });
        }).start();
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

    private void loadSavedPlaces() {
        MyDatabaseHelper dbHelper = new MyDatabaseHelper(this, "AppDB", null, 1);
        Cursor cursor = dbHelper.getAllLugares();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("lon"));
                byte[] fotoBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("imagen"));

                Bitmap foto = null;
                if (fotoBytes != null) {
                    foto = android.graphics.BitmapFactory.decodeByteArray(fotoBytes, 0, fotoBytes.length);
                }

                GeoPoint point = new GeoPoint(lat, lon);
                addCustomMarker(point, nombre, descripcion, foto);
            }
            cursor.close();
        }
        dbHelper.close();
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