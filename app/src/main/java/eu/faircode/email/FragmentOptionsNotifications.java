package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2019 by Marcel Bokhorst (M66B)
*/

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.Group;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import static android.app.Activity.RESULT_OK;

public class FragmentOptionsNotifications extends FragmentBase implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SwitchCompat swBadge;
    private SwitchCompat swUnseenIgnored;
    private SwitchCompat swNotifySummary;
    private SwitchCompat swNotifyRemove;
    private SwitchCompat swNotifyPreview;
    private CheckBox cbNotifyActionTrash;
    private CheckBox cbNotifyActionJunk;
    private CheckBox cbNotifyActionArchive;
    private CheckBox cbNotifyActionReply;
    private CheckBox cbNotifyActionReplyDirect;
    private CheckBox cbNotifyActionFlag;
    private CheckBox cbNotifyActionSeen;
    private CheckBox cbNotifyActionSnooze;
    private TextView tvNotifyActionsPro;
    private SwitchCompat swBiometricsNotify;
    private Button btnManage;
    private TextView tvManageHint;
    private ImageButton ibManage;
    private SwitchCompat swLight;
    private Button btnSound;
    private SwitchCompat swAlertOnce;
    private TextView tvNoGrouping;
    private TextView tvNoChannels;

    private Group grpNotification;

    private final static String[] RESET_OPTIONS = new String[]{
            "badge", "unseen_ignored",
            "notify_summary", "notify_remove", "notify_preview",
            "notify_trash", "notify_junk", "notify_archive", "notify_reply", "notify_reply_direct",
            "notify_flag", "notify_seen", "notify_snooze",
            "biometrics_notify",
            "light", "sound", "alert_once"
    };

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setSubtitle(R.string.title_setup);
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_options_notifications, container, false);

        // Get controls

        swBadge = view.findViewById(R.id.swBadge);
        swUnseenIgnored = view.findViewById(R.id.swUnseenIgnored);
        swNotifySummary = view.findViewById(R.id.swNotifySummary);
        swNotifyRemove = view.findViewById(R.id.swNotifyRemove);
        swNotifyPreview = view.findViewById(R.id.swNotifyPreview);
        cbNotifyActionTrash = view.findViewById(R.id.cbNotifyActionTrash);
        cbNotifyActionJunk = view.findViewById(R.id.cbNotifyActionJunk);
        cbNotifyActionArchive = view.findViewById(R.id.cbNotifyActionArchive);
        cbNotifyActionReply = view.findViewById(R.id.cbNotifyActionReply);
        cbNotifyActionReplyDirect = view.findViewById(R.id.cbNotifyActionReplyDirect);
        cbNotifyActionFlag = view.findViewById(R.id.cbNotifyActionFlag);
        cbNotifyActionSeen = view.findViewById(R.id.cbNotifyActionSeen);
        cbNotifyActionSnooze = view.findViewById(R.id.cbNotifyActionSnooze);
        tvNotifyActionsPro = view.findViewById(R.id.tvNotifyActionsPro);
        swBiometricsNotify = view.findViewById(R.id.swBiometricsNotify);
        btnManage = view.findViewById(R.id.btnManage);
        tvManageHint = view.findViewById(R.id.tvManageHint);
        ibManage = view.findViewById(R.id.ibManage);
        swLight = view.findViewById(R.id.swLight);
        btnSound = view.findViewById(R.id.btnSound);
        swAlertOnce = view.findViewById(R.id.swAlertOnce);
        tvNoGrouping = view.findViewById(R.id.tvNoGrouping);
        tvNoChannels = view.findViewById(R.id.tvNoChannels);

        grpNotification = view.findViewById(R.id.grpNotification);

        setOptions();

        // Wire controls

        PackageManager pm = getContext().getPackageManager();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        swBadge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("badge", checked).apply();
                ServiceSynchronize.reload(getContext(), "badge");
            }
        });

        swUnseenIgnored.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("unseen_ignored", checked).apply();
                ServiceSynchronize.reload(getContext(), "unseen_ignored");
            }
        });

        swNotifySummary.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("notify_summary", checked).apply();
                enableOptions();
            }
        });

        swNotifyRemove.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("notify_remove", checked).apply();
            }
        });

        swNotifyPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("notify_preview", checked).apply();
            }
        });

        cbNotifyActionTrash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_trash", checked).apply();
            }
        });

        cbNotifyActionJunk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_junk", checked).apply();
            }
        });

        cbNotifyActionArchive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_archive", checked).apply();
            }
        });

        cbNotifyActionReply.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_reply", checked).apply();
            }
        });

        cbNotifyActionReplyDirect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_reply_direct", checked).apply();
            }
        });

        cbNotifyActionFlag.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_flag", checked).apply();
            }
        });

        cbNotifyActionSeen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_seen", checked).apply();
            }
        });

        cbNotifyActionSnooze.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                prefs.edit().putBoolean("notify_snooze", checked).apply();
            }
        });

        Helper.linkPro(tvNotifyActionsPro);

        swBiometricsNotify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("biometrics_notify", checked).apply();
            }
        });

        final Intent manage = getIntentNotifications(getContext());
        btnManage.setVisibility(manage.resolveActivity(pm) == null ? View.GONE : View.VISIBLE);
        btnManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(manage);
            }
        });

        final Intent channel = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, "notification");

        tvManageHint.setVisibility(channel.resolveActivity(pm) == null ? View.GONE : View.VISIBLE);

        ibManage.setVisibility(channel.resolveActivity(pm) == null ? View.GONE : View.VISIBLE);
        ibManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(channel);
            }
        });

        swLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("light", checked).apply();
            }
        });

        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sound = prefs.getString("sound", null);
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.title_advanced_sound));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sound == null ? null : Uri.parse(sound));
                startActivityForResult(Helper.getChooser(getContext(), intent), ActivitySetup.REQUEST_SOUND);
            }
        });

        swAlertOnce.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("alert_once", !checked).apply();
            }
        });

        swAlertOnce.setVisibility(Log.isXiaomi() || BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        grpNotification.setVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.O || BuildConfig.DEBUG ? View.VISIBLE : View.GONE);

        // https://developer.android.com/training/notify-user/group
        tvNoGrouping.setVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? View.VISIBLE : View.GONE);

        // https://developer.android.com/training/notify-user/channels
        tvNoChannels.setVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
            setOptions();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_default:
                onMenuDefault();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onMenuDefault() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        for (String option : RESET_OPTIONS)
            editor.remove(option);
        editor.apply();
        ToastEx.makeText(getContext(), R.string.title_setup_done, Toast.LENGTH_LONG).show();
    }

    private void setOptions() {
        boolean pro = ActivityBilling.isPro(getContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        swBadge.setChecked(prefs.getBoolean("badge", true));
        swUnseenIgnored.setChecked(prefs.getBoolean("unseen_ignored", false));
        swNotifySummary.setChecked(prefs.getBoolean("notify_summary", false));
        swNotifyPreview.setChecked(prefs.getBoolean("notify_preview", true));

        cbNotifyActionTrash.setChecked(prefs.getBoolean("notify_trash", true) || !pro);
        cbNotifyActionJunk.setChecked(prefs.getBoolean("notify_junk", false) && pro);
        cbNotifyActionArchive.setChecked(prefs.getBoolean("notify_archive", true) || !pro);
        cbNotifyActionReply.setChecked(prefs.getBoolean("notify_reply", false) && pro);
        cbNotifyActionReplyDirect.setChecked(prefs.getBoolean("notify_reply_direct", false) && pro);
        cbNotifyActionFlag.setChecked(prefs.getBoolean("notify_flag", false) && pro);
        cbNotifyActionSeen.setChecked(prefs.getBoolean("notify_seen", true) || !pro);
        cbNotifyActionSnooze.setChecked(prefs.getBoolean("notify_snooze", false) || !pro);

        swNotifyRemove.setChecked(prefs.getBoolean("notify_remove", true));
        swBiometricsNotify.setChecked(prefs.getBoolean("biometrics_notify", false));

        swLight.setChecked(prefs.getBoolean("light", false));
        swAlertOnce.setChecked(!prefs.getBoolean("alert_once", true));

        enableOptions();
    }

    private void enableOptions() {
        boolean pro = ActivityBilling.isPro(getContext());
        boolean checked = swNotifySummary.isChecked();

        swNotifyPreview.setEnabled(!checked);
        cbNotifyActionTrash.setEnabled(pro && !checked);
        cbNotifyActionJunk.setEnabled(pro && !checked);
        cbNotifyActionArchive.setEnabled(pro && !checked);
        cbNotifyActionReply.setEnabled(pro && !checked);
        cbNotifyActionReplyDirect.setEnabled(pro && !checked);
        cbNotifyActionFlag.setEnabled(pro && !checked);
        cbNotifyActionSeen.setEnabled(pro && !checked);
        cbNotifyActionSnooze.setEnabled(pro && !checked);
        swBiometricsNotify.setEnabled(!checked);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            switch (requestCode) {
                case ActivitySetup.REQUEST_SOUND:
                    if (resultCode == RESULT_OK && data != null)
                        onSelectSound((Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI));
                    break;
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    private void onSelectSound(Uri uri) {
        Log.i("Selected ringtone=" + uri);
        if (uri != null && "file".equals(uri.getScheme()))
            uri = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (uri == null)
            prefs.edit().remove("sound").apply();
        else
            prefs.edit().putString("sound", uri.toString()).apply();
    }

    private static Intent getIntentNotifications(Context context) {
        return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra("app_package", context.getPackageName())
                .putExtra("app_uid", context.getApplicationInfo().uid)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
    }

}
