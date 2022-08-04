package multi.translator.onscreenocr.pages.launch.permissions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.utils.PermissionUtil
import multi.translator.onscreenocr.utils.Toaster
import multi.translator.onscreenocr.utils.clickOnce

class PermissionFloatWindowFragment : Fragment(R.layout.permission_float_window_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(view)
    }

    private fun setViews(view: View) {
        view.findViewById<Button>(R.id.bt_requestPermission).clickOnce {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                goPermissionPage()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val granted = PermissionUtil.canDrawOverlays(requireContext())

        if (granted) {
            goCaptureScreenPage()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun goPermissionPage() {
        var intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )

        try {
            resultLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            resultLauncher.launch(intent)
        }
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (PermissionUtil.canDrawOverlays(requireContext())) {
                goCaptureScreenPage()
            } else {
                Toaster.show(R.string.msg_evertranslator_needs_display_over_other_apps_permission_to_show_a_floating_window_for_easily_using)
            }
        }

    private fun goCaptureScreenPage() {
        val action = PermissionFloatWindowFragmentDirections.actionRequestCaptureScreenPage()
        findNavController().navigate(action)
    }
}
