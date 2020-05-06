package com.example.matrice;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.jetbrains.annotations.NotNull;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            ListPreference figureSetPreferences = findPreference(getString(R.string.key_figure_set));
            if(figureSetPreferences != null) {
                figureSetPreferences.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }

            ListPreference transitionTypePreferences = findPreference(getString(R.string.key_transition_type));
            if(transitionTypePreferences != null) {
                transitionTypePreferences.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }

            ListPreference languagePreferences = findPreference(getString(R.string.key_app_language));
            if(languagePreferences != null) {
                languagePreferences.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

            }

            Preference sendFeedbackPreference = findPreference(getString(R.string.key_send_feedback));
            sendFeedbackPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    sendFeedback(requireActivity());
                    return true;
                }
            });
        }
    }

    public static void sendFeedback(@NotNull Context context) {
        String body = null;
        try {
            body = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            body = "\n\n-----------------------------\nPlease don't remove this information\n Device OS: Android \n Device OS version: " +
                    Build.VERSION.RELEASE + "\n App Version: " + body + "\n Device Brand: " + Build.BRAND +
                    "\n Device Model: " + Build.MODEL + "\n Device Manufacturer: " + Build.MANUFACTURER;
        } catch (PackageManager.NameNotFoundException e) {
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"csertant@edu.bme.hu"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback from android app");
        intent.putExtra(Intent.EXTRA_TEXT, body);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.choose_email_client)));
    }
}