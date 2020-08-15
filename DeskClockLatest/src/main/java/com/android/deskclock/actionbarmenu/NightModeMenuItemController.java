/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.deskclock.actionbarmenu;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.ScreensaverActivity;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;

import static android.view.Menu.NONE;

/**
 * {@link MenuItemController} for controlling night mode display.
 */
public final class NightModeMenuItemController implements MenuItemController {

    private static final int NIGHT_MODE_MENU_RES_ID = R.id.menu_item_night_mode;
    private static final int REQUEST_WRITE_SETTINGS_PERMISSION = 5;
    private static final String TAG = "LK";


    private final Context mContext;
    private boolean canStartScreensaver;

    public NightModeMenuItemController(Context context) {
        mContext = context;
    }

    @Override
    public int getId() {
        return NIGHT_MODE_MENU_RES_ID;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
        menu.add(NONE, NIGHT_MODE_MENU_RES_ID, NONE, R.string.menu_item_night_mode)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public void onPrepareOptionsItem(MenuItem item) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DataModel.getDataModel().isScreensaverAlwaysOn()) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(mContext)) {
                requestWriteSettingsPermission();
            } else {
                // Permission granted OR is lower API level
                canStartScreensaver = true;
                int originalTimeout = 0;
                try {
                    originalTimeout = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
                } catch (Settings.SettingNotFoundException e) {
                    Log.e(TAG, e.toString());
                }

                Log.d(TAG, "current autolock timeout=" + originalTimeout);

                DeskClock.lastSaveAutoLockTimeout = originalTimeout;
                Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 2_147_483_647); // = 24.8 DAYS
            }
        } else {
            canStartScreensaver = true;
        }

        if (canStartScreensaver) {

            mContext.startActivity(new Intent(mContext, ScreensaverActivity.class)
                                           .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                           .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
        }
        return true;
    }


    @TargetApi(23)
    public void requestWriteSettingsPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + mContext.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ((Activity) mContext).startActivityForResult(intent, REQUEST_WRITE_SETTINGS_PERMISSION);
    }
}
