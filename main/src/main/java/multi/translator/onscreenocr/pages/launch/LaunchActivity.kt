package multi.translator.onscreenocr.pages.launch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import multi.translator.onscreenocr.databinding.ActivityLaunchBinding
import multi.translator.onscreenocr.remoteconfig.RemoteConfigManager


class LaunchActivity : AppCompatActivity() {

    companion object {
        fun getLaunchIntent(context: Context): Intent =
            Intent(context, LaunchActivity::class.java).apply {
                flags += Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }

    private lateinit var binding: ActivityLaunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)



        RemoteConfigManager.tryFetchNew()
    }
}
