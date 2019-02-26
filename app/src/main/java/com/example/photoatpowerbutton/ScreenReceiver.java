package com.example.photoatpowerbutton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenReceiver extends BroadcastReceiver {

    private MainActivity activity;

    public ScreenReceiver(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_SCREEN_OFF:
                activity.lockedScreen();
                break;
            case Intent.ACTION_SCREEN_ON:
                activity.turningOnScreen();
        }
    }
}
