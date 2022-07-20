package multi.translator.onscreenocr.pages.setting

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import multi.translator.onscreenocr.R

class SettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.perference, rootKey)
    }
}
