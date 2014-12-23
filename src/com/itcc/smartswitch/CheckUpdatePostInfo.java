package com.itcc.smartswitch;

public class CheckUpdatePostInfo {
	private int type;
	private int versioncode;
	private String versionname;
	private String buildversion;
	private String channelid;
	
	public CheckUpdatePostInfo() {
		super();
		type = 2;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getVersioncode() {
		return versioncode;
	}

	public void setVersioncode(int versioncode) {
		this.versioncode = versioncode;
	}

	public String getVersionname() {
		return versionname;
	}

	public void setVersionname(String versionname) {
		this.versionname = versionname;
	}

	public String getBuildversion() {
		return buildversion;
	}

	public void setBuildversion(String buildversion) {
		this.buildversion = buildversion;
	}

	public String getChannelid() {
		return channelid;
	}

	public void setChannelid(String channelid) {
		this.channelid = channelid;
	};
	
}
