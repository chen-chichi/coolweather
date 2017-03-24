package com.coolweather.android.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenTool {


	public static final String SP_NAME = "config";
	private static SharedPreferences sp;

	public static void saveBoolean(Context mContext, String key, boolean value) {
		if (sp == null) {
			sp = mContext.getSharedPreferences(SP_NAME,
					Context.MODE_PRIVATE);
		}
		sp.edit().putBoolean(key, value).commit();
	}

	public static boolean getBoolean(Context mContext, String key,
			boolean defValue) {
		if (sp == null) {
			sp = mContext.getSharedPreferences(SP_NAME,
					Context.MODE_PRIVATE);
		}
		boolean flag = sp.getBoolean(key, defValue);

		return flag;
	}
	
	public static void saveString(Context mContext, String key, String value) {
		if (sp == null) {
			sp = mContext.getSharedPreferences(SP_NAME,
					Context.MODE_PRIVATE);
		}
		sp.edit().putString(key, value).commit();
	}

	public static String getString(Context mContext, String key,
			String defValue) {
		if (sp == null) {
			sp = mContext.getSharedPreferences(SP_NAME,
					Context.MODE_PRIVATE);
		}
		String result = sp.getString(key, defValue);

		return result;
	}

	public static void clearAll() {
		SharedPreferences.Editor edit = sp.edit();
		edit.clear();
		edit.commit();
	}
}
