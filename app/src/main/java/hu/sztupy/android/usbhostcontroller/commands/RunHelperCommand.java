package hu.sztupy.android.usbhostcontroller.commands;

import hu.sztupy.android.usbhostcontroller.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;

public class RunHelperCommand extends ExecuteAsRootBase {
	public enum Commands {
		RSTPRT,
		RSTHDD,
		RSTUSB
	} 
	private Commands _type;
	private String _dev;
	private String _node;

	private static File external_file = null;

	static public boolean createExternalFile(Context c) {
		InputStream uhctrl = c.getResources().openRawResource(R.raw.uhctrl);
		try {
			FileOutputStream s = new FileOutputStream(c.getFilesDir().getAbsolutePath() + "/your_binary");
			byte[] b = new byte[4096];
			int read;
			while ((read = uhctrl.read(b)) != -1) {
				s.write(b, 0, read);
			}
			s.close();
			uhctrl.close();
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		external_file = new File(c.getFilesDir().getAbsolutePath() + "/your_binary");
		return true;
	}

	public RunHelperCommand(Commands type, String dev, String node) {
		_type = type;
		_dev = dev;
		_node = node;
	}

	@Override
	protected ArrayList<String> getCommandsToExecute() {
		ArrayList<String> s = new ArrayList<String>();
		s.add("chmod 755 " + external_file.getAbsolutePath());

		switch (_type) {
		case RSTPRT:
			s.add("mknod " + _dev + " b " + _node.replace(':', ' ') + " 2>/dev/null");
			s.add(external_file.getAbsolutePath() + " RSTPRT " + _dev);
			break;
		case RSTHDD:
			s.add("mknod " + _dev + " b " + _node.replace(':', ' ') + " 2>/dev/null");
			s.add(external_file.getAbsolutePath() + " RSTHDD " + _dev);
			break;
		case RSTUSB:
			s.add(external_file.getAbsolutePath() + " RSTUSB " + _node);
			break;
		}
		return s;
	}

}
