package hu.sztupy.android.usbhostcontroller.commands;

import java.util.ArrayList;

public class SetDriverCommand extends ExecuteAsRootBase {

	private int _type;

	public SetDriverCommand(int type) {
		_type = type;
	}

	@Override
	protected ArrayList<String> getCommandsToExecute() {
		ArrayList<String> s = new ArrayList<String>();
		switch (_type) {
		case 0:
			s.add("echo l > /sys/devices/platform/s3c-usbgadget/hostdriver");
			break;
		case 1:
			s.add("echo h > /sys/devices/platform/s3c-usbgadget/hostdriver");
			break;
		case 2:
			s.add("echo d > /sys/devices/platform/s3c-usbgadget/hostdriver");
			break;
		}
		return s;
	}

}
