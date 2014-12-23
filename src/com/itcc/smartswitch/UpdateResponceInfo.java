package com.itcc.smartswitch;

public class UpdateResponceInfo {
	private String url;
	private String[] market;
	private String app_name;
	private String version_name;

	public String getVersion_name() {
		return version_name;
	}

	public void setVersion_name(String version_name) {
		this.version_name = version_name;
	}

	public String getApp_name() {
		return app_name;
	}

	public void setApp_name(String app_name) {
		this.app_name = app_name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String[] getMarket() {
		return market;
	}

	public void setMarket(String[] market) {
		this.market = market;
	}

	@Override
	public String toString() {
		return "UpdateResponceInfo [ url = " + url + ", " + "market" + market +  "]";
	}

}
