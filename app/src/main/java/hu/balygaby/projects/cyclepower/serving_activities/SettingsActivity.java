package hu.balygaby.projects.cyclepower.serving_activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import hu.balygaby.projects.cyclepower.R;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
