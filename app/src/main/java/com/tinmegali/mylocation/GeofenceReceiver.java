package com.tinmegali.mylocation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import android.util.Log;
/**
 * Created by akankanh on 3/8/18.
 */

public class GeofenceReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            handleError(intent);
        } else {
            handleEnterExit(intent, geofencingEvent);
        }
    }
    private void handleError(Intent intent) {
    }

    private void handleEnterExit(Intent intent, GeofencingEvent geofencingEvent) {
        int transition = geofencingEvent.getGeofenceTransition();
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER){
            Log.v("geofence","entered");
        }else if(transition == Geofence.GEOFENCE_TRANSITION_EXIT){
            Log.v("geofence","exited");
        }
    }
}
