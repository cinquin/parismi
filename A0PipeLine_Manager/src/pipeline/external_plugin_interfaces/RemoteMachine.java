/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.external_plugin_interfaces;

public class RemoteMachine {

	private String machineName;
	private String userName;
	private String userPassword;
	private String guestPrefix;
	private String hostPrefix;

	public String getHostPrefix() {
		return hostPrefix;
	}

	public void setHostPrefix(String hostPrefix) {
		this.hostPrefix = hostPrefix;
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public String getGuestPrefix() {
		return guestPrefix;
	}

	public void setGuestPrefix(String filePrefix) {
		this.guestPrefix = filePrefix;
	}

	public RemoteMachine(String machineName, String userName, String userPassword, String guestPrefix, String hostPrefix) {
		this.machineName = machineName;
		this.userName = userName;
		this.userPassword = userPassword;
		this.guestPrefix = guestPrefix;
		this.hostPrefix = hostPrefix;
	}

}
