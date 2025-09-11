package com.example.anserview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
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

import java.io.File;

public class ActivityMapa extends AppCompatActivity {

    private MapView mapView;
    private Button btn_lugares;
    private Marker markerPersonaje;
    private static final int Codigo_COARSE = 500;
    private TextView tv_distancia;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración de OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        setContentView(R.layout.activity_mapa);

        // Inicializar componentes
        mapView = findViewById(R.id.mapView);
        btn_lugares = findViewById(R.id.btn_favoritos);
        tv_distancia = findViewById(R.id.tv_distancia);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurar mapa
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);

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

        // Pedir permisos automáticamente
        PedirPermisosDeUbicacion();
    }

    private void PedirPermisosDeUbicacion() {
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
        mapView.getController().setCenter(punto);

        if (markerPersonaje == null) {
            markerPersonaje = new Marker(mapView);
            markerPersonaje.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            markerPersonaje.setTitle("Estoy aquí");
            mapView.getOverlays().add(markerPersonaje);
        }

        markerPersonaje.setPosition(punto);
        mapView.invalidate();
    }

    // Para manejar el permiso si el usuario acepta manualmente
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Codigo_COARSE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarActualizacionesUbicacion();
        }
    }
}
