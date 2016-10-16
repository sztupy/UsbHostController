package hu.sztupy.android.usbhostcontroller.commands;

import java.util.ArrayList;

public class SetChargerModeCommand extends ExecuteAsRootBase {

	private boolean _type;
	private String _location;

	public SetChargerModeCommand(boolean type, String location) {
		_type = type;
		_location = location;
	}

	@Override
	protected ArrayList<String> getCommandsToExecute() {
		ArrayList<String> s = new ArrayList<String>();
		if (_type) {
			s.add("echo 1 > " + _location);
		} else {
			s.add("echo 0 > " + _location);
		}
		return s;
	}

}
