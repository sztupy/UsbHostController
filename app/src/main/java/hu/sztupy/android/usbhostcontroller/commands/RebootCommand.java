package hu.sztupy.android.usbhostcontroller.commands;

import java.util.ArrayList;

public class RebootCommand extends ExecuteAsRootBase {
	public RebootCommand() {
	}

	@Override
	protected ArrayList<String> getCommandsToExecute() {
		ArrayList<String> s = new ArrayList<String>();
		s.add("reboot");
		return s;
	}

}
