package com.itcc.smartswitch;

public class SwitchState {
	public static final int STATE_DISABLED = 0;
	public static final int STATE_ENABLED = 1;
	public static final int STATE_TURNING_ON = 2;
	public static final int STATE_TURNING_OFF = 3;
	public static final int STATE_UNKNOWN = 4;
	public static final int STATE_INTERMEDIATE = 5;
	public static final int STATE_DISABLED_NOCLICK = 6;
	
	//extends state for multi state use
	public static final int STATE_BRIGHTNESS_AUTO = 11;
	public static final int STATE_BRIGHTNESS_ON = 12;
	public static final int STATE_BRIGHTNESS_OFF = 13;
	public static final int STATE_BRIGHTNESS_MID = 14;
	
	public static final int STATE_RINGMODE_SILENT = 21;
	public static final int STATE_RINGMODE_VIBRATE = 22;
	public static final int STATE_RINGMODE_NORMAL = 23;

}