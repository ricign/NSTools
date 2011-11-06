/**
 * VoltageControlActivity.java
 * Nov 6, 2011 7:27:58 PM
 */
package mobi.cyann.nstools;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.widget.EditText;

/**
 * @author arif
 *
 */
public class VoltageControlActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	private final static String LOG_TAG = "NSTools.VoltageControlActivity";
	
	private SharedPreferences preferences;
	
	private List<String> armVoltages;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		addPreferencesFromResource(R.xml.voltage);

		readVoltages();
	}

	private void readVoltages() {
		SysCommand sc = SysCommand.getInstance();
		// read max arm volt
		EditTextPreference p = (EditTextPreference)findPreference(getString(R.string.key_max_arm_volt));
		if(sc.suRun("cat", "/sys/class/misc/customvoltage/max_arm_volt") >= 0) {
			String volt = sc.getLastResult(0).split(" ")[0];
			
			// Max arm volt
			p.setText(volt);
			p.setSummary(volt + " mV");
			p.setOnPreferenceChangeListener(this);
			EditText editText = ((EditTextPreference)p).getEditText();
			editText.setKeyListener(DigitsKeyListener.getInstance(false,true));
			saveVoltage(getString(R.string.key_max_arm_volt), volt, null);
			
			PreferenceCategory c = (PreferenceCategory)findPreference(getString(R.string.key_arm_volt_pref));
			int count = sc.suRun("cat", "/sys/class/misc/customvoltage/arm_volt");
			armVoltages = new ArrayList<String>();
			for(int i = 0; i < count; ++i) {
				String line = sc.getLastResult(i);
				String parts[] = line.split(":");
				volt = parts[1].substring(1, parts[1].length()-3);
	
				Log.d(LOG_TAG, line);
				
				EditTextPreference ed = new EditTextPreference(this);
				ed.setKey("armvolt_" + i);
				ed.setTitle(parts[0]);
				ed.setDialogTitle(parts[0]);
				ed.setSummary(parts[1]);
				ed.setText(volt);
				ed.setOnPreferenceChangeListener(this);
				ed.getEditText().setKeyListener(DigitsKeyListener.getInstance(false,true));
				c.addPreference(ed);
				
				armVoltages.add(volt);
			}
			saveVoltages(getString(R.string.key_arm_volt_pref), armVoltages, null);
		}else {
			// disable it
			p.setEnabled(false);
			p.setSummary(getString(R.string.status_not_available));
			saveVoltage(getString(R.string.key_max_arm_volt), "-1", null);
		}
	}
	
	private void saveVoltage(String key, String value, String deviceString) {
		if(deviceString != null) {
			SysCommand.getInstance().suRun("echo", value, ">", deviceString);
		}
		// save to xml pref
		Editor ed = preferences.edit();
		ed.putString(key, value);
		ed.commit();
	}

	private void saveVoltages(String key, List<String> voltageList, String deviceString) {
		StringBuilder s = new StringBuilder();
		for(int i = 0; i < voltageList.size(); ++i) {
			s.append(voltageList.get(i));
			s.append(" ");
		}
		Log.d(LOG_TAG, "voltages:" + s.toString());
		if(deviceString != null) {
			SysCommand.getInstance().suRun("echo", "\""+s.toString()+"\"", ">", deviceString);
		}
		// save to xml pref
		Editor ed = preferences.edit();
		ed.putString(key, s.toString());
		ed.commit();
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference.getKey().startsWith("armvolt_")) {
			String parts[] = preference.getKey().split("_");
			int i = Integer.parseInt(parts[1]);
			armVoltages.set(i, newValue.toString());
			preference.setSummary(newValue + " mV");
			((EditTextPreference)preference).setText(newValue.toString());
			saveVoltages(getString(R.string.key_arm_volt_pref), armVoltages, "/sys/class/misc/customvoltage/arm_volt");
		}else if(preference.getKey().equals(getString(R.string.key_max_arm_volt))) {
			preference.setSummary(newValue + " mV");
			((EditTextPreference)preference).setText(newValue.toString());
			saveVoltage(getString(R.string.key_max_arm_volt), newValue.toString(), "/sys/class/misc/customvoltage/max_arm_volt");
		}
		return false;
	}
	
	
}
