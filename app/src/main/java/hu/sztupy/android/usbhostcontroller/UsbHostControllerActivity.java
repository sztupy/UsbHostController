package hu.sztupy.android.usbhostcontroller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import hu.sztupy.android.usbhostcontroller.commands.SetUsbOperationModeCommand;
import hu.sztupy.android.usbhostcontroller.tabs.InfoTabActivity;
import hu.sztupy.android.usbhostcontroller.tabs.SettingsTabActivity;
import hu.sztupy.android.usbhostcontroller.tabs.UsbTabActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Surface;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class UsbHostControllerActivity extends TabActivity implements OnSharedPreferenceChangeListener {
	private AsyncTask<Void, String, String> dmesg_task;
	private Timer dmesg_timer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (savedInstanceState == null) {
			if (prefs != null) {
				String initstr = prefs.getString("driver_def_mode", "nochange");
				int type = -1;
				if (initstr != null) {
					if (initstr.equals("client")) type = 0;
					if (initstr.equals("host")) type = 1;
					if (initstr.equals("otg")) type = 2;
					if (initstr.equals("auto-host")) type = 3;
					if (type != -1) {
						SetUsbOperationModeCommand cmd = new SetUsbOperationModeCommand(type);
						if (cmd.canRunRootCommands()) {
							cmd.execute();
						}
					}
				}
			}
		}
		setContentView(R.layout.main);

		if (prefs!=null) {
			prefs.registerOnSharedPreferenceChangeListener(this);
			// add advertisement
			if (!prefs.getBoolean("disable_ads", false)
				&& (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0)) {

				MobileAds.initialize(getApplicationContext(), "ca-app-pub-3770178749249223~3419558246");

				AdView adView = (AdView) findViewById(R.id.advertisement);
				if (adView!=null) {
					AdRequest ar = new AdRequest.Builder().build();
					adView.loadAd(ar);
				}
			}
		}

		// tabs
		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, InfoTabActivity.class);

		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("info").setIndicator(getText(R.string.info), res.getDrawable(R.drawable.ic_tab_info))
				.setContent(intent);
		tabHost.addTab(spec);

		// Do the same for the other tabs
		intent = new Intent().setClass(this, UsbTabActivity.class);
		spec = tabHost.newTabSpec("usb").setIndicator(getText(R.string.usb), res.getDrawable(R.drawable.ic_tab_usb))
				.setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, SettingsTabActivity.class);
		spec = tabHost.newTabSpec("settings")
				.setIndicator(getText(R.string.preferences), res.getDrawable(R.drawable.ic_tab_settings))
				.setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);
		dmesg_task = null;
		dmesg_timer = null;
		if (PreferenceManager.getDefaultSharedPreferences(this).getString("dmesg_mode", "none").equals("none")) {
			ScrollView s = (ScrollView) findViewById(R.id.dmesg_data);
			s.setVisibility(ScrollView.GONE);
		}
	}

	public void onPause() {
		super.onPause();
		if (dmesg_task != null) dmesg_task.cancel(true);
		if (dmesg_timer != null) dmesg_timer.cancel();
	}

	public void onResume() {
		super.onResume();
		if (dmesg_task != null) dmesg_task.cancel(true);
		if (dmesg_timer != null) dmesg_timer.cancel();
		if (PreferenceManager.getDefaultSharedPreferences(this).getString("dmesg_mode", "none").equals("simple")) {
			dmesg_timer = new Timer();
			dmesg_timer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (dmesg_task != null) dmesg_task.cancel(true);
					dmesg_task = new DmesgReaderTask();
					dmesg_task.execute();
				}
			}, 5000, 5000);
		} else if (PreferenceManager.getDefaultSharedPreferences(this).getString("dmesg_mode", "none")
				.equals("continuous")) {
			dmesg_timer = null;
			dmesg_task = new DmesgRootReaderTask();
			dmesg_task.execute();
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals("dmesg_mode")) {
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		}
		if (key.equals("disable_ads")) {
			if (!prefs.getBoolean("disable_ads", false)) {
				MobileAds.initialize(getApplicationContext(), "ca-app-pub-3770178749249223~3419558246");

				AdView adView = (AdView) findViewById(R.id.advertisement);
				adView.loadAd(new AdRequest.Builder().build());
			} else {
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
		}
		// etc
	}

	private class DmesgRootReaderTask extends AsyncTask<Void, String, String> {

		protected String doInBackground(Void... params) {
			try {
				Process p = new ProcessBuilder().command("su").redirectErrorStream(true).start();
				try {
					Scanner s = new Scanner(p.getInputStream());
					OutputStreamWriter out = new OutputStreamWriter(p.getOutputStream());

					if (PreferenceManager.getDefaultSharedPreferences(UsbHostControllerActivity.this).getBoolean(
							"dmesg_save", false)) {
						out.write("cat /proc/kmsg | tee -a /mnt/sdcard/dmesg_save.txt\n");
					} else {
						out.write("cat /proc/kmsg\n");
					}

					out.flush();

					while (s.hasNextLine()) {
						if (isCancelled()) break;
						publishProgress(s.nextLine());
					}
					out.write("exit\n");
					out.flush();
					out.close();
					s.close();
				} finally {
					p.destroy();
				}
			} catch (IOException e) {
			}

			return null;
		}

		protected void onProgressUpdate(String... progress) {
			TextView dmesg = (TextView) findViewById(R.id.dmesg_text);
			String s = progress[0] + "\n" + dmesg.getText();
			if (s.length() > 4096) s = s.substring(0, 4096);
			dmesg.setText(s);
		}

	}

	private class DmesgReaderTask extends AsyncTask<Void, String, String> {

		protected String doInBackground(Void... params) {
			StringBuffer sb = new StringBuffer();
			try {
				Process p;
				if (PreferenceManager.getDefaultSharedPreferences(UsbHostControllerActivity.this).getBoolean(
						"dmesg_save", false)) {
					p = new ProcessBuilder()
							.command("busybox", "sh", "-c", "busybox dmesg -s 1024 | tee -a /mnt/sdcard/dmesg_save.txt")
							.redirectErrorStream(true).start();
				} else {
					p = new ProcessBuilder().command("busybox", "dmesg", "-s", "1024").redirectErrorStream(true)
							.start();
				}
				try {
					Scanner s = new Scanner(p.getInputStream());
					OutputStream out = p.getOutputStream();

					ArrayList<String> sa = new ArrayList<String>();
					while (s.hasNextLine()) {
						if (isCancelled()) return null;
						sa.add(s.nextLine());
					}
					for (int i = sa.size(); i > 0; i--) {
						sb.append(sa.get(i - 1));
						sb.append("\n");
					}
					out.close();
					s.close();
				} finally {
					p.destroy();
				}
			} catch (IOException e) {
			}

			return sb.toString();
		}

		protected void onPostExecute(String result) {
			TextView dmesg = (TextView) findViewById(R.id.dmesg_text);
			dmesg.setText(result);
		}
	}
}