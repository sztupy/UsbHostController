package hu.sztupy.android.usbhostcontroller.tabs;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.*;

import hu.sztupy.android.usbhostcontroller.R;
import hu.sztupy.android.usbhostcontroller.commands.MountDeviceCommand;
import hu.sztupy.android.usbhostcontroller.commands.RunHelperCommand;
import hu.sztupy.android.usbhostcontroller.commands.RunHelperCommand.Commands;
import hu.sztupy.android.usbhostcontroller.utils.FileUtils;
import hu.sztupy.android.usbhostcontroller.utils.SystemCapabilities;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;

public class UsbTabActivity extends Activity {
	LinearLayout list;
	ProgressBar bar;
	FillDevicesTask task;

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
		bar = new ProgressBar(this);
		bar.setLayoutParams(new LayoutParams(50,50));
		list = new LinearLayout(this);
		list.setOrientation(1);
		task = null;
		layout.setGravity(Gravity.CENTER_HORIZONTAL);
		layout.addView(refreshbtn);
		layout.addView(list);
		layout.addView(bar);
		scroll.addView(layout);
		setContentView(scroll);
		Refresh();
	}

	public void Refresh() {
		if (task != null) {
			task.cancel(true);
		}
		task = new FillDevicesTask();
		task.execute();
	}

	private class FillDevicesTask extends AsyncTask<Void, View, Void> {

		Map<String, String> mounted;

		private View[] generateUSBNode(File element) {
			ArrayList<View> rval = new ArrayList<View>();
			try {
				File cn = element.getCanonicalFile();
				if (cn.isDirectory()) {
					String position = element.getName();
					String dev = FileUtils.ReadFile(cn, "dev");
					String idProduct = FileUtils.ReadFile(cn, "idProduct");
					String idVendor = FileUtils.ReadFile(cn, "idVendor");
					String product = FileUtils.ReadFile(cn, "product");
					String manufacturer = FileUtils.ReadFile(cn, "manufacturer");
					String configuration = FileUtils.ReadFile(cn, "configuration");

					if (idProduct != null && idVendor != null) {
						LinearLayout l = new LinearLayout(UsbTabActivity.this);
						l.setGravity(Gravity.CENTER_VERTICAL);

						TextView t = new TextView(UsbTabActivity.this);
						t.setText(position);
						t.setWidth(80);
						t.setTypeface(Typeface.DEFAULT_BOLD);
						LinearLayout l2 = new LinearLayout(UsbTabActivity.this);
						l2.setOrientation(1);

						TextView t2 = new TextView(UsbTabActivity.this);
						if (configuration != null && !configuration.equals("")) {
							t2.setText(idProduct + ":" + idVendor + " (" + configuration + ")");
						} else {
							t2.setText(idProduct + ":" + idVendor);
						}
						l2.addView(t2);

						TextView t3 = new TextView(UsbTabActivity.this);
						String text = "";
						if (manufacturer != null && !manufacturer.equals("")) text += manufacturer + " ";
						if (product != null && !product.equals("")) text += product;
						t3.setText(text);
						l2.addView(t3);

						l.addView(t);
						l.addView(l2);

						l.setPadding(0, 10, 0, 10);
						
						l.setOnClickListener(new ExtendedOptionsListener(null, dev, new Commands[] {Commands.RSTUSB}));
						l.setBackgroundResource(R.color.click_selector);
						rval.add(l);

						View view = new View(UsbTabActivity.this);
						view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 2));
						view.setBackgroundResource(R.color.gray);
						rval.add(view);
					}
				}
			} catch (IOException e) {
			}
			return rval.toArray(new View[rval.size()]);
		}

		private View[] generateStorageNode(File element) {
			ArrayList<View> rval = new ArrayList<View>();
			try {
				File cn = element.getCanonicalFile();
				if (cn.isDirectory()) {
					String devicename = element.getName();
					String size = FileUtils.ReadFile(cn, "size");
					String dev = FileUtils.ReadFile(cn, "dev");
					File device = FileUtils.GetFile(cn, "device");
					String vendor = FileUtils.ReadFile(device, "vendor");
					String model = FileUtils.ReadFile(device, "model");

					if (dev != null) {
						LinearLayout l = new LinearLayout(UsbTabActivity.this);
						l.setGravity(Gravity.CENTER_VERTICAL);

						TextView t = new TextView(UsbTabActivity.this);
						t.setText(devicename);
						t.setWidth(80);
						t.setTypeface(Typeface.DEFAULT_BOLD);
						LinearLayout l2 = new LinearLayout(UsbTabActivity.this);
						l2.setOrientation(1);
						
						LinearLayout ldev = new LinearLayout(UsbTabActivity.this);
						ldev.setOrientation(1);
						
						Double lsize = new Double(Long.valueOf(size, 10)) * 512.0d / 1024.0d / 1024.0d;

						TextView t2 = new TextView(UsbTabActivity.this);
						t2.setText(lsize.toString() + " Mb");
						ldev.addView(t2);

						TextView t3 = new TextView(UsbTabActivity.this);
						String text = "";
						if (vendor != null && !vendor.equals("")) text += vendor + " ";
						if (model != null && !model.equals("")) text += model;
						t3.setText(text);
						ldev.addView(t3);

						ldev.setOnClickListener(new ExtendedOptionsListener(devicename, dev, new Commands[] {Commands.RSTHDD, Commands.RSTPRT}));
						ldev.setBackgroundResource(R.color.click_selector);
						
						l2.addView(ldev);
						l.addView(t);
						l.addView(l2);

						for (File partitions : cn.listFiles()) {
							File cp = partitions.getCanonicalFile();
							if (cp.isDirectory() && cp.getName().startsWith(cn.getName())) {
								final String partname = cp.getName();
								String psize = FileUtils.ReadFile(cp, "size");
								final String pdev = FileUtils.ReadFile(cp, "dev");
								Double plsize = new Double(Long.valueOf(psize, 10)) * 512.0d / 1024.0d / 1024.0d;
								LinearLayout pl = new LinearLayout(UsbTabActivity.this);
								pl.setGravity(Gravity.CENTER_VERTICAL);

								TextView pt = new TextView(UsbTabActivity.this);
								pt.setText(partname);
								pt.setWidth(80);
								pt.setTypeface(Typeface.DEFAULT_BOLD);
								LinearLayout pl2 = new LinearLayout(UsbTabActivity.this);
								pl2.setOrientation(1);

								TextView pt2 = new TextView(UsbTabActivity.this);
								pt2.setText(plsize.toString() + " Mb");
								pl2.addView(pt2);

								final TextView pt3 = new TextView(UsbTabActivity.this);
								if (mounted.containsKey(partname)) {
									pt3.setText(getString(R.string.unmount)+" "+mounted.get(partname));
								} else {
									pt3.setText(R.string.mount);
								}
								pt3.setTextSize(15);
								pt3.setTypeface(Typeface.DEFAULT_BOLD);
								pl2.addView(pt3);

								pl.addView(pt);
								pl.addView(pl2);

								pl.setOnClickListener(new MountClickListener(partname, pdev));
								pl.setBackgroundResource(R.color.click_selector);

								View pview = new View(UsbTabActivity.this);
								pview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 2));
								pview.setBackgroundResource(R.color.gray);
								l2.addView(pview);
								l2.addView(pl);
							}
						}

						l.setPadding(0, 10, 0, 10);
						rval.add(l);

						View view = new View(UsbTabActivity.this);
						view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 2));
						view.setBackgroundResource(R.color.gray);
						rval.add(view);
					}
				}
			} catch (IOException e) {
			}

			return rval.toArray(new View[rval.size()]);
		}

		private void ShowMessage(String message) {
			if (message != null && !message.trim().equals("")) {
				new AlertDialog.Builder(UsbTabActivity.this).setTitle(getString(R.string.message)).setMessage(message)
						.setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						}).show();
			}
		}
		
		private class ExtendedOptionsListener implements View.OnClickListener {
			String partition_name;
			String partition_device;
			Commands[] allowed_commands;
			
			public ExtendedOptionsListener(String partition_name, String partition_device, Commands[] allowed_commands) {
				this.partition_name = partition_name;
				this.partition_device = partition_device;
				this.allowed_commands = allowed_commands;
			}
			
			public void onClick(View v) {
				Resources res = v.getResources();
				final String[] options = new String[allowed_commands.length];
				for (int i=0; i< allowed_commands.length; i++) {
					options[i] = res.getTextArray(R.array.extended_commands)[allowed_commands[i].ordinal()].toString();
				}
				
				new AlertDialog.Builder(UsbTabActivity.this).setTitle(getString(R.string.adv_commands))
				.setItems(options, new OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						RunHelperCommand cmd = new RunHelperCommand(allowed_commands[item], "/dev/" + partition_name,
								partition_device);
						if (cmd.canRunRootCommands()) {
							cmd.execute();
						}
						ShowMessage(cmd.GetLastMessage());
						Refresh();
					}
				}).show();
			}
			
		}

		private class MountClickListener implements View.OnClickListener {
			String partition_name;
			String partition_device;

			public MountClickListener(String partition_name, String partition_device) {
				this.partition_name = partition_name;
				this.partition_device = partition_device;
			}

			public void onClick(View v) {

				final String[] mpoints = PreferenceManager
						.getDefaultSharedPreferences(getParent())
						.getString("mount_points",
								"/mnt/sdcard/ehdd\n/mnt/sdcard/ehdd2\n/mnt/ehdd\n/mnt/sd-ext\n/mnt/emmc").split("\n");

				if (mounted.containsKey(partition_name)) {
					MountDeviceCommand cmd = new MountDeviceCommand("/dev/" + partition_name, partition_device,
							mounted.get(partition_name), false);
					if (cmd.canRunRootCommands()) {
						cmd.execute();
					}
					ShowMessage(cmd.GetLastMessage());
					Refresh();
				} else {
					new AlertDialog.Builder(UsbTabActivity.this).setTitle(getString(R.string.mount_point_choose))
							.setItems(mpoints, new OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {
									MountDeviceCommand cmd = new MountDeviceCommand("/dev/" + partition_name,
											partition_device, mpoints[item], true);
									if (cmd.canRunRootCommands()) {
										cmd.execute();
									}
									ShowMessage(cmd.GetLastMessage());
									Refresh();
								}
							}).show();
				}
			}
		}

		@Override
		protected Void doInBackground(Void... args) {
			mounted = new Hashtable<String, String>();

			try {
				FileInputStream fIn;
				fIn = new FileInputStream("/proc/mounts");
				Scanner r = new Scanner(fIn);
				Pattern devp = Pattern.compile(".*/dev/(sd[^\\s]*)\\s*([^\\s]*).*");
				while (r.hasNextLine()) {
					String line = r.nextLine();
					Matcher m = devp.matcher(line);
					if (m.matches()) {
						mounted.put(m.group(1), m.group(2));
					}
				}
			} catch (FileNotFoundException e) {
			}

			TextView header;
			
			if (SystemCapabilities.getCap(UsbTabActivity.this).hasProblem()) {
				header = new TextView(UsbTabActivity.this);
				header.setText(getString(R.string.should_restart));
				header.setBackgroundResource(R.color.gray);
				header.setTypeface(Typeface.DEFAULT_BOLD);
				header.setPadding(1, 1, 1, 1);
				publishProgress(header);
			}
			
			header = new TextView(UsbTabActivity.this);
			header.setText(getString(R.string.conn_usb_dev));
			header.setBackgroundResource(R.color.gray);
			header.setTypeface(Typeface.DEFAULT_BOLD);
			header.setPadding(1, 1, 1, 1);
			publishProgress(header);
			try {
				File hubs = new File("/sys/bus/usb/devices").getCanonicalFile();
				if (hubs.isDirectory()) {
					for (File element : hubs.listFiles()) {
						if (isCancelled()) return null;
						if (!element.getName().startsWith("usb")) {
							publishProgress(generateUSBNode(element));
						}
					}
				}
			} catch (IOException e) {
			}

			header = new TextView(UsbTabActivity.this);
			header.setText(getString(R.string.conn_usb_strg));
			header.setBackgroundResource(R.color.gray);
			header.setTypeface(Typeface.DEFAULT_BOLD);
			header.setPadding(1, 1, 1, 1);
			publishProgress(header);
			try {
				File blocks = new File("/sys/block").getCanonicalFile();
				if (blocks.isDirectory()) {
					for (File element : blocks.listFiles()) {
						if (isCancelled()) return null;
						if (element.getName().startsWith("sd")) {
							publishProgress(generateStorageNode(element));
						}
					}
				}
			} catch (IOException e) {
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			bar.setVisibility(View.GONE);
		}

		@Override
		protected void onPostExecute(Void result) {
			bar.setVisibility(View.GONE);
		}

		@Override
		protected void onPreExecute() {
			list.removeAllViews();
			bar.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onProgressUpdate(View... values) {
			if (!isCancelled())
			for (View v : values) {
				list.addView(v);
			}
		}

	}

}
