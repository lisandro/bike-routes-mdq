package com.falconi.lisandro.cyclingroutes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cartodb.CartoDBClientIF;
import com.cartodb.CartoDBException;
import com.cartodb.impl.ApiKeyCartoDBClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends ActionBarActivity implements LocationListener, GpsStatus.Listener {

    public static final int MIN_TIME = 5000;
    public static final int MIN_DISTANCE = 70;
    private static final long DURATION_TO_FIX_LOST_MS = 10000;
    public static final String PROVIDER = LocationManager.GPS_PROVIDER;
    public static final String START_TRACK_MSG = "Empezo a trackear";
    public static final String STOP_TRACK_MSG = "Termino de trackear";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button playButton;
    private Button stopButton;
    private LocationManager locationManager;
    private static final String TAG = MainActivity.class.getSimpleName();
    private List<LatLng> route = new ArrayList<LatLng>();
    private Polyline lineRoute;
    private PolylineOptions lineOptions;
    private String provider;
    private GpsStatus mStatus;
    private long locationTime;
    private boolean tracking = false;
    private TextView status;
    private ArrayList<Double> registerSpeed = new ArrayList<Double>();
    private Intent i;


    @Override
    protected void onResume() {
        super.onResume();
        setCurrentLocationOnMap();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView) findViewById(R.id.TRACK_SPEED);
        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        // Showing status
        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        } else { // Google Play Services are available
            setUpMap();
            initLocationServices();
            final LocationListener locationListener = this;
            initPlayStopButtons(locationListener);
            i = new Intent(this, BackgroundLocationService.class);
        }

    }

    private void setUpMap() {
        // Getting reference to the SupportMapFragment of activity_main.xml
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // Getting GoogleMap object from the fragment
        mMap = fm.getMap();
        // Enabling MyLocation Layer of Google Map
        mMap.setMyLocationEnabled(true);
        // Disabling Zoom controls of Google Map
        mMap.getUiSettings().setZoomControlsEnabled(false);
        //mMap.setTrafficEnabled(true);
    }

    private void initLocationServices() {
        // Getting LocationManager object from System Service LOCATION_SERVICE
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);

        //Checking for GPS
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // Getting the name of the best provider
        provider = locationManager.getBestProvider(criteria, true);

        setCurrentLocationOnMap();
    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Tu GPS parece estar deshabilitado, quieres habilitarlo?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void setCurrentLocationOnMap() {
        // Getting Current Location
        final Location location = locationManager.getLastKnownLocation(PROVIDER);
        if (location != null) {
            onLocationChanged(location);
        }
    }

    private void initPlayStopButtons(final LocationListener locationListener) {
        playButton = (Button) findViewById(R.id.buttonStart);
        playButton.setVisibility(View.VISIBLE);
        stopButton = (Button) findViewById(R.id.buttonStop);
        stopButton.setVisibility(View.GONE);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                } else {
                    Location location = locationManager.getLastKnownLocation(PROVIDER);
                    if (location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() - 2 * 60 * 1000) {
                        playButton.setClickable(false);

                        //Setting visibility of buttons
                        playButton.setVisibility(View.GONE);
                        stopButton.setVisibility(View.VISIBLE);
                        if (lineRoute != null) {
                            lineRoute.remove();
                        }
                        tracking = true;
                        //Creating route PolyLine
                        lineOptions = initPolyLineOptions();
                        //Add PolyLine to map
                        lineRoute = mMap.addPolyline(lineOptions);
                        //Requesting for Location Updates
                        locationManager.requestLocationUpdates(PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
                        Log.d(TAG, START_TRACK_MSG);
                        //Initializing new Array of Locations
                        route = new ArrayList<LatLng>();
                        registerSpeed = new ArrayList<Double>();
                        //First location
                        setCurrentLocationOnMap();
                        Toast.makeText(getApplicationContext(), "Vamos!", Toast.LENGTH_LONG).show();
                        startService(i);
                    } else {
                        Toast.makeText(getApplicationContext(), "Esperando por ubicacion, intenta nuevamente!", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopButton.setVisibility(View.GONE);
                playButton.setVisibility(View.VISIBLE);
                playButton.setClickable(true);
                locationManager.removeUpdates(locationListener);
                Log.d(TAG, STOP_TRACK_MSG);
                Log.d(TAG, "Posiciones detectadas:" + Integer.toString(route.size()));
                Double sumSpeeds = 0d;
                Double avgSpeed;
                if (!registerSpeed.isEmpty()) {
                    for (Double mark : registerSpeed) {
                        sumSpeeds += mark;
                    }
                    avgSpeed = sumSpeeds / registerSpeed.size();
                } else {
                    avgSpeed = sumSpeeds;
                }
                DecimalFormat df = new DecimalFormat("#0.00");
                Log.i("info", "Velocidad promedio: " + df.format(avgSpeed));


                if (route.size() > 0) {
                    if (avgSpeed < 30d) {
                        new SaveRouteTask().execute(route);
                        tracking = false;
                        Toast.makeText(getApplicationContext(), "Velocidad promedio: " + df.format(avgSpeed), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Al parecer la ruta no ha sido en bicicleta: " + df.format(avgSpeed), Toast.LENGTH_LONG).show();

                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Ruta demasiado corta", Toast.LENGTH_LONG).show();
                }
                stopService(i);
            }
        });
    }

    private PolylineOptions initPolyLineOptions() {
        return new PolylineOptions().width(15).color(Color.BLUE);
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i("info", "Nueva location detectada");

        locationTime = SystemClock.elapsedRealtime();

        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        DecimalFormat df = new DecimalFormat("#0.00");

        double speedKmh = location.getSpeed() * 3.6;

        if (speedKmh > 5d) {
            registerSpeed.add(speedKmh);
        }

        String speedValue = df.format(speedKmh);

        status.setText(speedValue + " Km/h");

        // Creating a LatLng object for the current location
        LatLng latLng = new LatLng(latitude, longitude);

        if (tracking) {
            route.add(latLng);
            lineRoute.setPoints(route);
        }

        // Showing the current location in Google Map
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(getBaseContext(), "GPS encendido ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(getBaseContext(), "GPS apagado ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        TextView status = (TextView) findViewById(R.id.textViewGPS_STATUS);
        mStatus = locationManager.getGpsStatus(mStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                clickablePlayButton(true);
                status.setText("GPS Listo");
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                boolean isGPSFix = (SystemClock.elapsedRealtime() - locationTime) < 3000;

                if (!isGPSFix) {
                    status.setText("GPS Listo");
                    clickablePlayButton(true);
                } else {
                    status.setText("Buscando satelites");
                    clickablePlayButton(false);
                }
                break;

            case GpsStatus.GPS_EVENT_STARTED:
                status.setText("Iniciando GPS");
                clickablePlayButton(false);
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                status.setText("GPS detenido");
                clickablePlayButton(false);
                break;
            default:
                Log.w(TAG, "unknown GpsStatus event type. " + event);
        }
    }

    private void clickablePlayButton(boolean flag) {
        if (!tracking) {
            playButton.setClickable(flag);
        }
    }

    /**
     *
     */
    private class SaveRouteTask extends AsyncTask<List<LatLng>, Void, Boolean> {
        public static final String CARTODB_API_KEY = "3a218292a8048717de62b12a184ea640fa97d7c5";
        public static final String CARTODB_ACCOUNT = "lismdq";
        private List<LatLng>[] rutaSalvar;

        @Override
        protected Boolean doInBackground(List<LatLng>... params) {
            CartoDBClientIF cartoDBCLient = null;
            rutaSalvar = params;
            try {
                cartoDBCLient = new ApiKeyCartoDBClient(CARTODB_ACCOUNT, CARTODB_API_KEY);
            } catch (CartoDBException e) {
                e.printStackTrace();
                return false;
            }

            if (cartoDBCLient != null) {
                try {
                    String linePoints = "";
                    for (LatLng position : params[0]) {
                        double lat = position.latitude;
                        double longi = position.longitude;
                        linePoints = linePoints + longi + " " + lat + ",";
                        //    System.out.println(cartoDBCLient.executeQuery("INSERT INTO mdp_routes (the_geom,name) " +
                        //    "VALUES (ST_SetSRID(ST_Point("+longi+","+lat+"),4326),'"+android.os.Build.MODEL+"')"));
                    }
                    if (linePoints.length() > 0 && linePoints.charAt(linePoints.length() - 1) == ',') {
                        linePoints = linePoints.substring(0, linePoints.length() - 1);
                    }
                    System.out.println(cartoDBCLient.executeQuery("INSERT INTO mdp_routes_copy (the_geom) " +
                            "VALUES (ST_GeomFromText('MULTILINESTRING((" + linePoints + "))',4326))"));
                    return true;
                } catch (CartoDBException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(getApplicationContext(), "Excelente! Ya guardamos tu ruta", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Algo ha salido mal, no pudimos guardar la ruta", Toast.LENGTH_LONG).show();
                doInBackground(rutaSalvar);
            }
        }
    }

}
