package hu.sztupy.android.usbhostcontroller.tabs;

import hu.sztupy.android.usbhostcontroller.R;
import hu.sztupy.android.usbhostcontroller.commands.RebootCommand;
import hu.sztupy.android.usbhostcontroller.utils.SystemCapabilities;
import hu.sztupy.android.usbhostcontroller.utils.SystemCapabilities.Drivers;
import hu.sztupy.android.usbhostcontroller.utils.SystemCapabilities.OperationMode;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;

public class SettingsTabActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager.setDefaultValues(SettingsTabActivity.this, R.xml.preferences, false);

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			initSummary(getPreferenceScreen().getPreference(i));
		}

		SystemCapabilities s = SystemCapabilities.getCap(this);

		if (!s.isS3CDriverAvailable()) {
			getPreferenceScreen().removePreference(findPreference("driver_box"));
		} else if (!s.isS3CChargerDisableAvailable()) {
			findPreference("disable_charger").setEnabled(false);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		SystemCapabilities.getCap(this).ReScan();

		// Set up a listener whenever a key changes
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		updatePrefSummary(findPreference("driver_mode"), true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePrefSummary(findPreference(key), false);
	}

	private void initSummary(Preference p) {
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updatePrefSummary(p, true);
		}

	}

	private void updatePrefSummary(Preference p, boolean isInit) {
		SystemCapabilities s = SystemCapabilities.getCap(this);
		Resources res = getResources();
		if (p == null) return;
		if (p.getKey() == null) return;
		if (p.getKey().equals("disable_charger")) {
			if (!isInit) {
				s.SetChargerDisable((((CheckBoxPreference) p).isChecked()));
			}
			((CheckBoxPreference) p).setChecked(s.isChargerDisabled());
		} else if (p.getKey().equals("wireless_adb")) {
			if (!isInit) {
				if (((CheckBoxPreference) p).isChecked()) {
					s.SetAdbWirelessPort(5555);
				} else {
					s.SetAdbWirelessPort(-1);
				}
			}
			int port = s.getAdbWirelessPort();
			if (port == -1) {
				p.setSummary(getText(R.string.wireless_adb_off));
				((CheckBoxPreference) p).setChecked(false);
			} else {
				p.setSummary(getText(R.string.wireless_adb_on) + SystemCapabilities.getipAddress() + ":" + port);
				((CheckBoxPreference) p).setChecked(true);
			}
		} else if (p.getKey().equals("driver_choose")) {
			if (!isInit) {
				Drivers type = Drivers.DRIVER_UNKNOWN;
				if (((ListPreference) p).getValue().equals("S3CLSFS")) type = Drivers.DRIVER_S3CFSLS;
				if (((ListPreference) p).getValue().equals("S3CHS")) type = Drivers.DRIVER_S3CHS;
				if (((ListPreference) p).getValue().equals("DWC")) type = Drivers.DRIVER_DWC;
				if (type != Drivers.DRIVER_UNKNOWN) {
					s.SetDriver(type);
				}
			}
			if (s.getDriverUsed() != Drivers.DRIVER_UNKNOWN) {
				((ListPreference) p).setValueIndex(s.getDriverUsed().ordinal() - 1);
				p.setSummary(res.getStringArray(R.array.drivers_entries)[s.getDriverUsed().ordinal() - 1]);
			} else {
				p.setSummary(getString(R.string.s3c_driver_invalid_mode));
			}
		} else if (p.getKey().equals("driver_mode")) {
			if (!isInit) {
				OperationMode type = OperationMode.OP_UNKNOWN;
				if (((ListPreference) p).getValue().equals("client")) type = OperationMode.OP_CLIENT;
				if (((ListPreference) p).getValue().equals("host")) type = OperationMode.OP_HOST;
				if (((ListPreference) p).getValue().equals("otg")) type = OperationMode.OP_OTG;
				if (((ListPreference) p).getValue().equals("auto-host")) type = OperationMode.OP_AUTOHOST;
				if (type != OperationMode.OP_UNKNOWN) {
					s.SetOperationMode(type);
				}
			}
			if (s.getActualOperationMode() != OperationMode.OP_UNKNOWN) {
				((ListPreference) p).setValueIndex(s.getActualOperationMode().ordinal() - 1);
				p.setSummary(res.getStringArray(R.array.op_mode_entries)[s.getActualOperationMode().ordinal() - 1]);
			} else {
				p.setSummary(getString(R.string.s3c_driver_invalid_mode));
			}
		} else if (p.getKey().equals("restart_phone")) {
			if (!isInit) {
				RebootCommand cmd = new RebootCommand();
				if (cmd.canRunRootCommands()) cmd.execute();
			}
			((CheckBoxPreference) p).setChecked(false);
			if (s.hasProblem()) {
				p.setSummary(getString(R.string.should_restart));
			}
		} else if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		} else if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}

	}
}
