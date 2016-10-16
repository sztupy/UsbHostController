package hu.sztupy.android.usbhostcontroller.utils;

import hu.sztupy.android.usbhostcontroller.commands.RunHelperCommand;
import hu.sztupy.android.usbhostcontroller.commands.SetAdbWirelessStateCommand;
import hu.sztupy.android.usbhostcontroller.commands.SetChargerModeCommand;
import hu.sztupy.android.usbhostcontroller.commands.SetDriverCommand;
import hu.sztupy.android.usbhostcontroller.commands.SetUsbOperationModeCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

public class SystemCapabilities {
	public enum Drivers {
		DRIVER_UNKNOWN, DRIVER_S3CFSLS, DRIVER_S3CHS, DRIVER_DWC
	};

	public enum OperationMode {
		OP_UNKNOWN, OP_CLIENT, OP_HOST, OP_OTG, OP_AUTOHOST
	}

	public enum OperationState {
		STATE_UNKNOWN, STATE_CLIENT, STATE_HOST_S3CFSLS, STATE_HOST_S3CHS, STATE_HOST_DWC
	}

	public enum CableState {
		CABLE_UNKNOWN, CABLE_DISCONNECTED, CABLE_USB, CABLE_OTG
	}

	static private SystemCapabilities instance = null;
	static private Context c;

	// these don't change
	private boolean S3CDriverAvailable;
	private boolean S3CChargerDisableAvailable;
	private int S3CDriverVersion;
	private String chargerSysLocation;
	private Drivers[] availableDrivers;

	// these do change
	private boolean rootHubAvailable;
	private boolean chargerDisabled;
	private Drivers driverUsed;
	private OperationMode actualOperationMode;
	private CableState actualCableState;
	private OperationState actualOperationState;
	private int adbWirelessPort;

	public boolean isChargerDisabled() {
		return chargerDisabled;
	}

	public int getAdbWirelessPort() {
		return adbWirelessPort;
	}

	public boolean isRootHubAvailable() {
		return rootHubAvailable;
	}

	public Drivers getDriverUsed() {
		return driverUsed;
	}

	public OperationMode getActualOperationMode() {
		return actualOperationMode;
	}

	public CableState getActualCableState() {
		return actualCableState;
	}

	public OperationState getActualOperationState() {
		return actualOperationState;
	}

	public Drivers[] getAvailableDrivers() {
		return availableDrivers;
	}

	public boolean isS3CChargerDisableAvailable() {
		return S3CChargerDisableAvailable;
	}

	public boolean isS3CDriverAvailable() {
		return S3CDriverAvailable;
	}

	public int getS3CDriverVersion() {
		return S3CDriverVersion;
	}

	// check for some strange conditions
	public boolean hasProblem() {
		if (!isS3CDriverAvailable()) return false;
		if (isRootHubAvailable() && getActualOperationState() == OperationState.STATE_CLIENT) return true;
		if (isRootHubAvailable() && getActualOperationMode() == OperationMode.OP_CLIENT) return true;
		return false;
	}

	public void SetAdbWirelessPort(int newport) {
		SetAdbWirelessStateCommand cmd = new SetAdbWirelessStateCommand(newport);
		if (cmd.canRunRootCommands()) {
			cmd.execute();
		}
		ReScan();
	}

	public void SetOperationMode(OperationMode newmode) {
		if (newmode != OperationMode.OP_UNKNOWN) {
			SetUsbOperationModeCommand cmd = new SetUsbOperationModeCommand(newmode.ordinal() - 1);
			if (cmd.canRunRootCommands()) {
				cmd.execute();
			}
			ReScan();
		}
	}

	public void SetDriver(Drivers newdrv) {
		if (Arrays.asList(availableDrivers).contains(newdrv)) {
			SetDriverCommand cmd = new SetDriverCommand(newdrv.ordinal() - 1);
			if (cmd.canRunRootCommands()) {
				cmd.execute();
			}
			ReScan();
		}
	}

	public void SetChargerDisable(boolean state) {
		if (S3CChargerDisableAvailable) {
			SetChargerModeCommand cmd = new SetChargerModeCommand(state, chargerSysLocation);
			if (cmd.canRunRootCommands()) {
				cmd.execute();
			}
		}
		ReScan();
	}

	public void ReScan() {
		actualOperationMode = OperationMode.OP_UNKNOWN;
		actualCableState = CableState.CABLE_UNKNOWN;
		actualOperationState = OperationState.STATE_UNKNOWN;
		driverUsed = Drivers.DRIVER_UNKNOWN;
		adbWirelessPort = -1;

		File dir = new File("/sys/bus/usb/devices/usb1");
		if (dir.isDirectory()) {
			rootHubAvailable = true;
		} else {
			rootHubAvailable = false;
		}

		String line = SystemPropertiesProxy.get(c, "service.adb.tcp.port");
		if (line==null || line.equals("-1") || line.equals("")) {
			adbWirelessPort = -1;
		} else {
			try {
				adbWirelessPort = Integer.parseInt(line);
			} catch (NumberFormatException e) {
				adbWirelessPort = -1;
			}
		}

		if (!S3CDriverAvailable) return;

		FileInputStream fIn;
		try {
			fIn = new FileInputStream("/sys/devices/platform/s3c-usbgadget/opmode");
			Scanner s = new Scanner(fIn);
			line = null;
			if (s.hasNextLine()) line = s.nextLine();
			if (line != null) {
				Pattern p = Pattern.compile("(.*) \\(cable: (.*); state: (.*)\\)");
				Matcher m = p.matcher(line);
				if (m.matches()) {
					String aSt = m.group(1);
					String aCb = m.group(2);
					String aSh = m.group(3);
					if (aSt.equals("client")) {
						actualOperationMode = OperationMode.OP_CLIENT;
					} else if (aSt.equals("host")) {
						actualOperationMode = OperationMode.OP_HOST;
					} else if (aSt.equals("otg")) {
						actualOperationMode = OperationMode.OP_OTG;
					} else if (aSt.equals("auto-host")) {
						actualOperationMode = OperationMode.OP_AUTOHOST;
					}

					if (aCb.equals("disconnected")) {
						actualCableState = CableState.CABLE_DISCONNECTED;
					} else if (aCb.equals("usb connected")) {
						actualCableState = CableState.CABLE_USB;
					} else if (aCb.equals("otg connected")) {
						actualCableState = CableState.CABLE_OTG;
					}

					if (aSh.equals("gadget")) {
						actualOperationState = OperationState.STATE_CLIENT;
					} else if (aSh.equals("host")) {
						actualOperationState = OperationState.STATE_HOST_S3CHS;
					} else if (aSh.equals("host (S3C/HS)")) {
						actualOperationState = OperationState.STATE_HOST_S3CHS;
					} else if (aSh.equals("host (S3C/FSLS)")) {
						actualOperationState = OperationState.STATE_HOST_S3CFSLS;
					} else if (aSh.equals("host (DWC)")) {
						actualOperationState = OperationState.STATE_HOST_DWC;
					}
				}
			}
			s.close();
			fIn.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

		try {
			fIn = new FileInputStream("/sys/devices/platform/s3c-usbgadget/hostdriver");
			Scanner s = new Scanner(fIn);
			line = null;
			if (s.hasNextLine()) line = s.nextLine();
			if (line != null) {
				if (line.charAt(0) == 'h') {
					driverUsed = Drivers.DRIVER_S3CHS;
				} else if (line.charAt(0) == 'l') {
					driverUsed = Drivers.DRIVER_S3CFSLS;
				} else if (line.charAt(0) == 'd') {
					driverUsed = Drivers.DRIVER_DWC;
				}
			}
			s.close();
			fIn.close();
		} catch (FileNotFoundException e) {
			if (S3CDriverVersion <= 4) driverUsed = Drivers.DRIVER_S3CHS;
		} catch (IOException e) {
		}

		if (S3CChargerDisableAvailable) {
			chargerDisabled = false;
			try {
				fIn = new FileInputStream(chargerSysLocation);
				Scanner s = new Scanner(fIn);
				line = null;
				if (s.hasNextLine()) line = s.nextLine();
				if (line != null) {
					chargerDisabled = line.charAt(0) == '1';
				}
				s.close();
				fIn.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
	}

	private SystemCapabilities() {
		try {
			FileInputStream fIn = new FileInputStream("/sys/devices/platform/s3c-usbgadget/opmode");
			S3CDriverAvailable = true;
			try {
				FileInputStream f2 = new FileInputStream("/sys/devices/platform/s3c-usbgadget/version");
				Scanner s = new Scanner(f2);
				while (s.hasNextLine()) {
					String l = s.nextLine();
					Pattern p = Pattern.compile("(.*): (.*)");
					Matcher m = p.matcher(l);
					if (m.matches()) {
						String param = m.group(1);
						String value = m.group(2);
						if (param.equals("version")) {
							S3CDriverVersion = Integer.parseInt(value.split(" ")[1]);
						} else if (param.equals("drivers")) {
							ArrayList<Drivers> list = new ArrayList<Drivers>();
							for (String drv : value.split(" ")) {
								if (drv.equals("S3CHS")) {
									list.add(Drivers.DRIVER_S3CHS);
								} else if (drv.equals("S3CFSLS")) {
									list.add(Drivers.DRIVER_S3CFSLS);
								} else if (drv.equals("DWC")) {
									list.add(Drivers.DRIVER_DWC);
								}
							}
							availableDrivers = list.toArray(new Drivers[list.size()]);
						}
					}
				}
				f2.close();
			} catch (FileNotFoundException e) {
				S3CDriverVersion = 4;
				availableDrivers = new Drivers[] { Drivers.DRIVER_S3CHS };
			} catch (IOException e) {
			}
			fIn.close();
		} catch (FileNotFoundException e) {
			S3CDriverAvailable = false;
		} catch (IOException e) {
		}
		S3CChargerDisableAvailable = false;
		File dir = FileUtils.FindFile(new File("/sys/devices/platform"), "max8998-charger",
				"/power_supply/battery/disable_charger", 5);
		if (dir != null) {
			Log.d("charger", dir.getAbsolutePath());
			String loc = dir.getAbsolutePath() + "/power_supply/battery/disable_charger";
			if (new File(loc).isFile()) {
				S3CChargerDisableAvailable = true;
				chargerSysLocation = loc;
			}
		}

		RunHelperCommand.createExternalFile(c);

		ReScan();
	}

	public static String getipAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (inetAddress instanceof Inet4Address) {
						if (!inetAddress.isLoopbackAddress()) {
							String ipaddress = inetAddress.getHostAddress().toString();
							Log.e("ip address", "" + ipaddress);
							return ipaddress;
						}
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("Socket exception in GetIP Address of Utilities", ex.toString());
		}
		return null;
	}

	public static synchronized SystemCapabilities getCap(Context _c) {
		c = _c;
		if (instance == null) instance = new SystemCapabilities();
		return instance;
	}
}
