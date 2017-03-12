package com.example.android.sunshine.utilities;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.example.android.sunshine.utilities.NotificationUtils.INDEX_MAX_TEMP;
import static com.example.android.sunshine.utilities.NotificationUtils.INDEX_MIN_TEMP;
import static com.example.android.sunshine.utilities.NotificationUtils.INDEX_WEATHER_ID;
import static com.example.android.sunshine.utilities.NotificationUtils.WEATHER_NOTIFICATION_PROJECTION;

/**
 * Created by rodrigocastro on 11/03/17.
 */

public class WearableUtils {
    private static final String TAG = "WearableUtils";

    private static final String WEATHER_ICON_PATH = "/weather_icon";
    private static final String WEATHER_MIN_PATH = "/weather_min";
    private static final String WEATHER_MAX_PATH = "/weather_max";
    private static final String WEATHER_ICON_KEY = "weather_icon";
    private static final String WEATHER_MAX_KEY = "weather_max";
    private static final String WEATHER_MIN_KEY = "weather_min";

    public static void notify(Context context, GoogleApiClient mGoogleApiClient) {

        Uri todaysWeatherUri = WeatherContract.WeatherEntry
                .buildWeatherUriWithDate(SunshineDateUtils.normalizeDate(System.currentTimeMillis()));

        Cursor todayWeatherCursor = context.getContentResolver().query(
                todaysWeatherUri,
                WEATHER_NOTIFICATION_PROJECTION,
                null,
                null,
                null);

        if (todayWeatherCursor.moveToFirst()) {

            int weatherId = todayWeatherCursor.getInt(INDEX_WEATHER_ID);
            Double high = todayWeatherCursor.getDouble(INDEX_MAX_TEMP);
            Double low = todayWeatherCursor.getDouble(INDEX_MIN_TEMP);

            Resources resources = context.getResources();

            int smallArtResourceId = SunshineWeatherUtils
                    .getSmallArtResourceIdForWeatherCondition(weatherId);
            Bitmap smallIcon = BitmapFactory.decodeResource(resources, smallArtResourceId);


            sendDataItem(mGoogleApiClient, WEATHER_ICON_PATH, WEATHER_ICON_KEY, "", toAsset(smallIcon));
            sendDataItem(mGoogleApiClient, WEATHER_MAX_PATH, WEATHER_MAX_KEY, SunshineWeatherUtils.formatTemperature(context, high), null);
            sendDataItem(mGoogleApiClient, WEATHER_MIN_PATH, WEATHER_MIN_KEY, SunshineWeatherUtils.formatTemperature(context, low), null);
        }

    }

    private static void sendDataItem(GoogleApiClient mGoogleApiClient, String path, String key, String value, Asset asset) {

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);

        Log.d(TAG, String.valueOf("asset is null?" + asset != null));
        if (asset != null) {
            putDataMapRequest.getDataMap().putAsset(key, asset);
        } else {
            putDataMapRequest.getDataMap().putString(key, value);
        }


        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();

        Log.d(TAG, "Generating DataItem: " + request);
        if (!mGoogleApiClient.isConnected()) {
            return;
        }
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                    + dataItemResult.getStatus().getStatusCode());
                        }
                    }
                });
    }

    public static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
