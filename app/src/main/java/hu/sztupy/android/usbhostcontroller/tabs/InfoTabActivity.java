package hu.sztupy.android.usbhostcontroller.tabs;

import hu.sztupy.android.usbhostcontroller.R;
import hu.sztupy.android.usbhostcontroller.utils.SystemCapabilities;
import hu.sztupy.android.usbhostcontroller.utils.SystemCapabilities.Drivers;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class InfoTabActivity extends Activity {

	private TextView textview;
	private LoadInformationTask infotask;
	private ProgressDialog dialog = null;
	
	public void onResume() {
		super.onResume();
		Refresh();
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(1);
		Button refreshbtn = new Button(this);
		refreshbtn.setText(R.string.refresh);
		refreshbtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Refresh();
			}
		});
		ScrollView scroll = new ScrollView(this);
		textview = new TextView(this);
		layout.addView(refreshbtn);
		layout.addView(textview);
		scroll.addView(layout);
		setContentView(scroll);
		infotask = null;
		dialog = null;
		Refresh();
	}

	public void Refresh() {
		if (infotask!=null) {
			infotask.cancel(false);
		}
		infotask = new LoadInformationTask();
		infotask.execute();
	}
	
	@Override
	protected void onPause() {
		super.onPause();		
		if (infotask!=null) {
			infotask.cancel(false);
			infotask = null;
		}
		if (dialog!=null)
			dialog = null;
	}


	private class LoadInformationTask extends AsyncTask<Void, Void, String> {
		@Override
		protected void onPreExecute() {
			if (dialog!=null) {
				dialog.dismiss();
			}
			dialog = ProgressDialog.show(InfoTabActivity.this, "", getText(R.string.please_wait));
		}

		@Override
		protected String doInBackground(Void... args) {
			StringBuilder sb = new StringBuilder();
			SystemCapabilities s = SystemCapabilities.getCap(InfoTabActivity.this);
			Resources r = getResources();

			s.ReScan();
			
			sb.append(getString(R.string.app_name) + "\n");
			sb.append(getString(R.string.version) + ": ");
			sb.append(getString(R.string.version_desc) + "\n\n");

			if (s.isS3CDriverAvailable()) {
				sb.append(getString(R.string.s3c_driver_yes) + "\n");
				sb.append(getString(R.string.available_drivers) + ":");
				for (Drivers d : s.getAvailableDrivers()) {
					sb.append(" " + r.getStringArray(R.array.cap_drivers)[d.ordinal()] + ";");
				}
				sb.append("\n");
				sb.append(getString(R.string.actual_driver) + ": "
						+ r.getStringArray(R.array.cap_drivers)[s.getDriverUsed().ordinal()] + "\n");
				sb.append(getString(R.string.op_mode) + ": "
						+ r.getStringArray(R.array.cap_opmode)[s.getActualOperationMode().ordinal()] + "\n");
				sb.append(getString(R.string.cable_state) + ": "
						+ r.getStringArray(R.array.cap_cablestate)[s.getActualCableState().ordinal()] + "\n");
				sb.append(getString(R.string.op_state) + ": "
						+ r.getStringArray(R.array.cap_opstate)[s.getActualOperationState().ordinal()] + "\n");
				sb.append(getString(R.string.can_disable_charger) + ": "
						+ (s.isS3CChargerDisableAvailable() ? getString(R.string.yes) : getString(R.string.no)) + "\n");
				if (s.isS3CChargerDisableAvailable()) {
					sb.append(getString(R.string.is_charger_dis) + ": "
							+ (s.isChargerDisabled() ? getString(R.string.yes) : getString(R.string.no)) + "\n");
				}
				sb.append("\n");
			} else {
				if (!s.isRootHubAvailable()) {
					sb.append(getString(R.string.s3c_driver_no) + "\n\n");
				}
			}

			if (s.isRootHubAvailable()) {
				sb.append(getString(R.string.root_hub_yes));
			} else {
				sb.append(getString(R.string.root_hub_no));
			}
			
			if (s.hasProblem()) {
				sb.append("\n\n"+getString(R.string.should_restart));
			}			

			return sb.toString();
		}

		@Override
		protected void onCancelled(String result) {
			if (dialog!=null) {
				dialog.dismiss();
				dialog = null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			textview.setText(result);
			if (dialog!=null) {
				dialog.dismiss();
				dialog = null;
			}
		}
	}
}
