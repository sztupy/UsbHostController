package hu.sztupy.android.usbhostcontroller.commands;

import java.util.ArrayList;

public class SetAdbWirelessStateCommand extends ExecuteAsRootBase {

	private String _port;

	public SetAdbWirelessStateCommand(int port) {
		_port = new Integer(port).toString();
	}

	@Override
	protected ArrayList<String> getCommandsToExecute() {
		ArrayList<String> s = new ArrayList<String>();
		s.add("setprop service.adb.tcp.port " + _port);
		s.add("stop adbd");
		s.add("start adbd");
		return s;
	}

}
