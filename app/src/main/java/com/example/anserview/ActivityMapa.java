package com.example.anserview;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.LinearLayout;

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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private ActivityResultLauncher<Void> cameraLauncher;
    private Bitmap capturedImage = null;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private ExtendedFloatingActionButton fab_add_place;
    private ExtendedFloatingActionButton fab_view_places;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 500;
    private static final String TAG = "ActivityMapa";
    private String usuarioCorreo; // Variable para almacenar el correo del usuario

    private enum PlaceType {
        HOTELS, FOOD, RECREATION, OTHER
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

        // Obtener el correo del usuario desde el Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("USER_EMAIL")) {
            usuarioCorreo = intent.getStringExtra("USER_EMAIL");
        } else {
            // Manejar el caso si el correo no se pasa, por ejemplo, con un valor predeterminado
            // o volviendo a la actividad de inicio de sesión.
            usuarioCorreo = "desconocido@anserview.com";
        }

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

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        cameraLauncher.launch(null);
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupUI() {
        mapView = findViewById(R.id.mapView);
        fabMyLocation = findViewById(R.id.fab_my_location);
        fab_add_place = findViewById(R.id.fab_add_place);
        fab_view_places = findViewById(R.id.fab_view_places);

        fabMyLocation.setOnClickListener(v -> {
            if (myLocationMarker != null) {
                mapView.getController().animateTo(myLocationMarker.getPosition());
            } else {
                Toast.makeText(this, "Esperando ubicación...", Toast.LENGTH_SHORT).show();
            }
        });

        fab_add_place.setOnClickListener(v -> showAddPlaceDialog());

        // Asegurar que el correo se pasa a la siguiente actividad
        fab_view_places.setOnClickListener(v -> {
            Intent intent = new Intent(ActivityMapa.this, ActivityLugares.class);
            intent.putExtra("USER_EMAIL", usuarioCorreo);
            startActivity(intent);
        });
    }

    private void showAddPlaceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_place, null);

        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        Button btnTakePhoto = dialogView.findViewById(R.id.btn_take_photo);

        // Nuevo: Referencia a los radio botones individuales
        RadioButton radioFood = dialogView.findViewById(R.id.radio_food);
        RadioButton radioHotel = dialogView.findViewById(R.id.radio_hotel);
        RadioButton radioRecreation = dialogView.findViewById(R.id.radio_recreation);
        RadioButton radioOther = dialogView.findViewById(R.id.radio_other);

        // Variable para almacenar el tipo de lugar seleccionado
        final String[] selectedPlaceType = {"OTHER"};

        // Configurar los listeners para los radio botones
        View.OnClickListener radioClickListener = v -> {
            radioFood.setChecked(v.getId() == R.id.radio_food);
            radioHotel.setChecked(v.getId() == R.id.radio_hotel);
            radioRecreation.setChecked(v.getId() == R.id.radio_recreation);
            radioOther.setChecked(v.getId() == R.id.radio_other);

            if (v.getId() == R.id.radio_food) {
                selectedPlaceType[0] = "FOOD";
            } else if (v.getId() == R.id.radio_hotel) {
                selectedPlaceType[0] = "HOTELS";
            } else if (v.getId() == R.id.radio_recreation) {
                selectedPlaceType[0] = "RECREATION";
            } else if (v.getId() == R.id.radio_other) {
                selectedPlaceType[0] = "OTHER";
            }
        };

        radioFood.setOnClickListener(radioClickListener);
        radioHotel.setOnClickListener(radioClickListener);
        radioRecreation.setOnClickListener(radioClickListener);
        radioOther.setOnClickListener(radioClickListener);
        // Por defecto, seleccionar "Otro"
        radioOther.setChecked(true);

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null);
            } else {
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

                        PlaceType placeType = PlaceType.valueOf(selectedPlaceType[0]);

                        addCustomMarker(point, name, description, capturedImage, placeType);

                        MyDatabaseHelper dbHelper = new MyDatabaseHelper(this);
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
                        registro.put("tipo_lugar", placeType.name());

                        // Nuevo: Aquí se agrega el correo del usuario
                        registro.put("usuario_correo", usuarioCorreo);

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
                    capturedImage = null;
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void addCustomMarker(GeoPoint point, String name, String description, Bitmap photo, PlaceType type) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(name);

        int iconResId = getIconForPlaceType(type);
        marker.setIcon(getResources().getDrawable(iconResId, getTheme()));

        marker.setOnMarkerClickListener((m, map) -> {
            if (m.isInfoWindowShown()) {
                m.closeInfoWindow();
            } else if (myLocationMarker != null) {
                GeoPoint userPoint = myLocationMarker.getPosition();
                Location userLocation = new Location("");
                userLocation.setLatitude(userPoint.getLatitude());
                userLocation.setLongitude(userPoint.getLongitude());

                Location markerLocation = new Location("");
                markerLocation.setLatitude(point.getLatitude());
                markerLocation.setLongitude(point.getLongitude());

                double distancia = userLocation.distanceTo(markerLocation);
                showCustomInfoWindow(m, name, description, null, photo, distancia);
                drawRoute(userPoint, point);
            }
            return true;
        });

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void setupMap() {
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);
        mapView.getController().setCenter(new GeoPoint(5.2353, -75.7919));
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
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
            case OTHER:
                overpassTags = "node[\"name\"](area.searchArea);";
                break;
            default:
                return;
        }
        String query = String.format(overpassQueryTemplate, overpassTags);
        executeOverpassQuery(query, type);
    }

    private void executeOverpassQuery(String query, PlaceType type) {
        String url = "https://overpass-api.de/api/interpreter?data=" + Uri.encode(query);
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
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error Overpass", e);
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
        } else return;

        String name = getPlaceName(obj);
        int iconResId = getIconForPlaceType(type);
        GeoPoint point = new GeoPoint(lat, lon);

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(name);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(iconResId, getTheme()));

        mapView.getOverlays().add(marker);

        marker.setOnMarkerClickListener((m, map) -> {
            if (m.isInfoWindowShown()) {
                m.closeInfoWindow();
            } else if (myLocationMarker != null) {
                GeoPoint userPoint = myLocationMarker.getPosition();
                Location userLocation = new Location("");
                userLocation.setLatitude(userPoint.getLatitude());
                userLocation.setLongitude(userPoint.getLongitude());

                Location markerLocation = new Location("");
                markerLocation.setLatitude(lat);
                markerLocation.setLongitude(lon);

                new Thread(() -> {
                    String description = "Sin descripción disponible";
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
                        Log.e(TAG, "Error Wikipedia", e);
                    }
                    String finalDescription = description;
                    String finalImageUrl = imageUrl;
                    runOnUiThread(() -> {
                        double distancia = userLocation.distanceTo(markerLocation);
                        showCustomInfoWindow(m, name, finalDescription, finalImageUrl, null, distancia);
                        drawRoute(userPoint, new GeoPoint(lat, lon));
                    });
                }).start();
            }
            return true;
        });
    }

    private void showCustomInfoWindow(Marker marker, String title, String description, String imageUrl, Bitmap photo, double distancia) {
        InfoWindow.closeAllInfoWindowsOn(mapView);
        InfoWindow infoWindow = new InfoWindow(R.layout.infowindow_place_detail, mapView) {
            @Override
            public void onOpen(Object item) {
                View v = mView;
                TextView tvTitle = v.findViewById(R.id.iw_title);
                TextView tvDesc = v.findViewById(R.id.iw_description);
                ImageView ivImage = v.findViewById(R.id.iw_image);
                TextView tvDistancia = v.findViewById(R.id.tv_distancia);

                tvTitle.setText(title);
                tvDesc.setText(description);
                tvDistancia.setText("Está a: " + String.format(Locale.getDefault(), "%.2f", distancia) + " metros");

                if (photo != null) {
                    ivImage.setVisibility(View.VISIBLE);
                    ivImage.setImageBitmap(photo);
                } else if (imageUrl != null) {
                    ivImage.setVisibility(View.VISIBLE);
                    Picasso.get().load(imageUrl).into(ivImage);
                } else {
                    ivImage.setVisibility(View.GONE);
                }
            }
            @Override public void onClose() {}
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
            case HOTELS: return R.drawable.ic_hotel;
            case FOOD: return R.drawable.ic_food;
            case RECREATION: return R.drawable.ic_recreation;
            case OTHER: return R.drawable.ic_marker_default;
            default: return R.drawable.ic_marker_default;
        }
    }

    private void loadSavedPlaces() {
        MyDatabaseHelper dbHelper = new MyDatabaseHelper(this);
        Cursor cursor = dbHelper.getAllLugares();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("lon"));
                byte[] fotoBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("imagen"));
                String tipoLugarString = cursor.getString(cursor.getColumnIndexOrThrow("tipo_lugar"));

                Bitmap foto = null;
                if (fotoBytes != null) {
                    foto = android.graphics.BitmapFactory.decodeByteArray(fotoBytes, 0, fotoBytes.length);
                }

                PlaceType tipoLugar;
                try {
                    tipoLugar = PlaceType.valueOf(tipoLugarString);
                } catch (IllegalArgumentException e) {
                    tipoLugar = PlaceType.OTHER;
                }

                addCustomMarker(new GeoPoint(lat, lon), nombre, descripcion, foto, tipoLugar);
            }
            cursor.close();
        }
        dbHelper.close();
    }

    private void drawRoute(GeoPoint startPoint, GeoPoint endPoint) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + startPoint.getLongitude() + "," + startPoint.getLatitude() + ";"
                + endPoint.getLongitude() + "," + endPoint.getLatitude() + "?overview=full&geometries=geojson";

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray routes = jsonObject.getAsJsonArray("routes");

                    if (routes.size() > 0) {
                        JsonObject route = routes.get(0).getAsJsonObject();
                        JsonObject geometry = route.getAsJsonObject("geometry");
                        JsonArray coordinates = geometry.getAsJsonArray("coordinates");

                        List<GeoPoint> routePoints = new ArrayList<>();
                        for (JsonElement coordinate : coordinates) {
                            JsonArray coords = coordinate.getAsJsonArray();
                            double lon = coords.get(0).getAsDouble();
                            double lat = coords.get(1).getAsDouble();
                            routePoints.add(new GeoPoint(lat, lon));
                        }

                        runOnUiThread(() -> {
                            Polyline routeOverlay = new Polyline();
                            routeOverlay.setPoints(routePoints);
                            routeOverlay.setColor(0xFFFF0000);
                            routeOverlay.setWidth(5f);

                            for (int i = 0; i < mapView.getOverlays().size(); i++) {
                                if (mapView.getOverlays().get(i) instanceof Polyline) {
                                    mapView.getOverlays().remove(i);
                                }
                            }

                            mapView.getOverlays().add(routeOverlay);
                            mapView.invalidate();
                        });
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error al trazar la ruta", e);
            }
        }).start();
    }
}
