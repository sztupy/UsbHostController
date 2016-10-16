package hu.sztupy.android.usbhostcontroller.commands;

import java.util.ArrayList;

public class MountDeviceCommand extends ExecuteAsRootBase {

	private String _dev;
	private String _node;
	private boolean _type;
	private String _mpoint;

	public MountDeviceCommand(String dev, String node, String mpoint, boolean type) {
		_type = type;
		_dev = dev;
		_node = node;
		_mpoint = mpoint;
	}

	@Override
	protected ArrayList<String> getCommandsToExecute() {
		ArrayList<String> s = new ArrayList<String>();
		s.add("umount "+_mpoint+" 2>/dev/null");
		s.add("mkdir -p "+_mpoint);
		if (_type) {
			s.add("mknod " + _dev + " b " + _node.replace(':', ' ') + " 2>/dev/null");
			s.add("mount -t vfat "+_dev+" "+_mpoint);
		} 
		return s;
	}

}
