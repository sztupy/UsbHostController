package hu.sztupy.android.usbhostcontroller.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import android.util.Log;

public abstract class ExecuteAsRootBase {
	private boolean retval;
	private String message;

	public String GetLastMessage() {
		return message;
	}

	public boolean canRunRootCommands() {
		retval = false;
		Thread t = new Thread(new Runnable() {
			public void run() {
				Process suProcess = null;

				try {
					suProcess = new ProcessBuilder().command("su").start();

					DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
					DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

					if (null != os && null != osRes) {
						// Getting the id of the current user to check if this
						// is root
						os.writeBytes("id\n");
						os.flush();

						String currUid = "";
						
						String s = osRes.readLine();
						if (s!=null) currUid += s;
						if (s!=null && osRes.available()>0) s = osRes.readLine();
						if (s!=null) currUid += s;
						boolean exitSu = false;
						if (currUid.equals("")) {
							retval = false;
							exitSu = false;
							Log.d("ROOT", "Can't get root access or denied by user");
						} else if (true == currUid.contains("uid=0")) {
							retval = true;
							exitSu = true;
							Log.d("ROOT", "Root access granted");
						} else {
							retval = false;
							exitSu = true;
							Log.d("ROOT", "Root access rejected: " + currUid);
						}

						if (exitSu) {
							os.writeBytes("exit\n");
							os.flush();
						}
						osRes.close();
					}
				} catch (Exception e) {
					// Can't get root !
					// Probably broken pipe exception on trying to write to
					// output
					// stream after su failed, meaning that the device is not
					// rooted

					retval = false;
					Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
				} finally {
					if (suProcess!=null) suProcess.destroy();
				}
			}
		});
		t.start();
		try {
			t.join(2000);
		} catch (InterruptedException e) {
		}
		if (t.isAlive()) {
			t.interrupt();
			Log.d("ROOT","Root thread deadlocked");
			return false;
		}
		return retval;
	}

	public final boolean execute() {
		retval = false;
		message = null;
		Thread t = new Thread(new Runnable() {
			public void run() {

				try {
					ArrayList<String> commands = getCommandsToExecute();
					if (null != commands && commands.size() > 0) {
						Process suProcess = new ProcessBuilder().command("su").redirectErrorStream(true).start();

						DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
						Scanner osRes = new Scanner(suProcess.getInputStream());

						// Execute commands that require root access
						for (String currCommand : commands) {
							os.writeBytes(currCommand + "\n");
							os.flush();
						}

						os.writeBytes("exit\n");
						os.flush();

						try {
							int suProcessRetval = suProcess.waitFor();
							if (255 != suProcessRetval) {
								// Root access granted
								retval = true;
							} else {
								// Root access denied
								retval = false;
							}
							os.close();
							StringBuilder sb = new StringBuilder();
							while (osRes.hasNextLine()) {
								sb.append(osRes.nextLine() + "\n");
							}
							message = sb.toString();
							osRes.close();
							suProcess.destroy();
						} catch (Exception ex) {
							Log.e("ROOT", "Error executing root action", ex);
						}
					}
				} catch (IOException ex) {
					Log.w("ROOT", "Can't get root access", ex);
				} catch (SecurityException ex) {
					Log.w("ROOT", "Can't get root access", ex);
				} catch (Exception ex) {
					Log.w("ROOT", "Error executing internal operation", ex);
				}
			}
		});
		t.start();
		try {
			t.join(2000);
		} catch (InterruptedException e) {
		}
		if (t.isAlive()) {
			t.interrupt();
			Log.d("ROOT","Root thread deadlocked");
			return false;
		}
		return retval;
	}

	protected abstract ArrayList<String> getCommandsToExecute();
}