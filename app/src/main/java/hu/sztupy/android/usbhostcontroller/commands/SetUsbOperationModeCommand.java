package hu.sztupy.android.usbhostcontroller.commands;

import java.util.ArrayList;

public class SetUsbOperationModeCommand extends ExecuteAsRootBase {

	private int _type;

	public SetUsbOperationModeCommand(int type) {
		_type = type;
	}

	@Override
	protected ArrayList<String> getCommandsToExecute() {
		ArrayList<String> s = new ArrayList<String>();
		switch (_type) {
		case 0:
			s.add("echo c > /sys/devices/platform/s3c-usbgadget/opmode");
			break;
		case 1:
			s.add("echo h > /sys/devices/platform/s3c-usbgadget/opmode");
			break;
		case 2:
			s.add("echo o > /sys/devices/platform/s3c-usbgadget/opmode");
			break;
		case 3:
			s.add("echo a > /sys/devices/platform/s3c-usbgadget/opmode");
			break;
		}
		return s;
	}

}
