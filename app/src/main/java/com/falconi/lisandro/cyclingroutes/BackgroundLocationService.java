package com.falconi.lisandro.cyclingroutes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;

/**
 * Created by LisandroF on 06/09/2014.
 */
public class BackgroundLocationService extends Service implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = BackgroundLocationService.class.getSimpleName();
    private LocationManager locationManager;
    private NotificationManager mNM;
    final static int myID = 1234;
    public static final int MIN_TIME = 5000;
    public static final int MIN_DISTANCE = 70;
    private static final long DURATION_TO_FIX_LOST_MS = 10000;
    public static final String PROVIDER = LocationManager.GPS_PROVIDER;


    @Override
    public void onCreate() {
        super.onCreate();
//        locationManager.requestLocationUpdates(PROVIDER, MIN_TIME, MIN_DISTANCE, this);
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Mi ruta en bici";

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());


        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(321, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //The intent to launch when the user clicks the expanded notification
        Intent intent1 = new Intent(this, MainActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent1, 0);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Mi ruta en bici MDP")
                        .setContentText("Trazando...");

        mBuilder.setContentIntent(pendIntent);

        //This constructor is deprecated. Use Notification.Builder instead
        Notification notice = new Notification(R.drawable.ic_launcher, "Mi ruta en bici MDP", System.currentTimeMillis());

        //This method is deprecated. Use Notification.Builder instead.
        notice.setLatestEventInfo(this, "Title text", "Content text", pendIntent);

        notice.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(myID, mBuilder.build());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        stopForeground(true);
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "location changed services");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
