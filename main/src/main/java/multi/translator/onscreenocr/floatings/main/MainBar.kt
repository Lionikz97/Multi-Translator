package multi.translator.onscreenocr.floatings.main

import android.content.Context
import android.graphics.Point
import android.view.View
import android.widget.TextView
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.floatings.base.MovableFloatingView
import multi.translator.onscreenocr.floatings.history.VersionHistoryView
import multi.translator.onscreenocr.floatings.manager.FloatingStateManager
import multi.translator.onscreenocr.floatings.manager.State
import multi.translator.onscreenocr.floatings.menu.MenuView
import multi.translator.onscreenocr.floatings.readme.ReadmeView
import multi.translator.onscreenocr.floatings.translationSelectPanel.TranslationSelectPanel
import multi.translator.onscreenocr.log.FirebaseEvent
import multi.translator.onscreenocr.pages.setting.SettingActivity
import multi.translator.onscreenocr.pages.setting.SettingManager
import multi.translator.onscreenocr.pref.AppPref
import multi.translator.onscreenocr.utils.Utils
import multi.translator.onscreenocr.utils.clickOnce
import multi.translator.onscreenocr.utils.showOrHide

class MainBar(context: Context) : MovableFloatingView(context) {
    override val layoutId: Int
        get() = R.layout.floating_main_bar

    override val initialPosition: Point
        get() =
            if (SettingManager.restoreMainBarPosition) AppPref.lastMainBarPosition
            else Point(0, 0)

    override val enableDeviceDirectionTracker: Boolean
        get() = true

    override val moveToEdgeAfterMoved: Boolean
        get() = true

    override val fadeOutAfterMoved: Boolean
        get() = !arrayOf(State.ScreenCircling, State.ScreenCircled)
            .contains(FloatingStateManager.currentState)
                && !menuView.attached
                && SettingManager.enableFadingOutWhileIdle
    override val fadeOutDelay: Long
        get() = SettingManager.timeoutToFadeOut
    override val fadeOutDestinationAlpha: Float
        get() = SettingManager.opaquePercentageToFadeOut

    private val btLangSelector: View = rootView.findViewById(R.id.bt_langSelector)
    private val tvLang: TextView = rootView.findViewById(R.id.tv_lang)
    private val ivGoogleTranslator: View = rootView.findViewById(R.id.iv_googleTranslator)
    private val btSelect: View = rootView.findViewById(R.id.bt_select)
    private val btTranslate: View = rootView.findViewById(R.id.bt_translate)

    //    private val btOCROnly: View = rootView.findViewById(R.id.bt_ocrOnly)
    private val btClose: View = rootView.findViewById(R.id.bt_close)
    private val btMenu: View = rootView.findViewById(R.id.bt_menu)

    private val menuView: MenuView by lazy {
        MenuView(context, false).apply {
            setAnchor(btMenu)

            onAttached = { rescheduleFadeOut() }
            onDetached = { rescheduleFadeOut() }
            onItemSelected = { view, key ->
                view.detachFromScreen()
                viewModel.onMenuItemClicked(key)
                rescheduleFadeOut()
            }
        }
    }

    private val viewModel: MainBarViewModel by lazy { MainBarViewModel(viewScope) }

    init {
        setViews()
        setDragView(btMenu)
    }

    private fun setViews() {
        btLangSelector.clickOnce {
            rescheduleFadeOut()
            TranslationSelectPanel(context).attachToScreen()
        }

        btSelect.clickOnce {
            FloatingStateManager.startScreenCircling()
        }

        btTranslate.clickOnce {
            FirebaseEvent.logClickTranslationStartButton()
            FloatingStateManager.startScreenCapturing(viewModel.selectedOCRLang)
        }

        btClose.clickOnce {
            FloatingStateManager.cancelScreenCircling()
        }

        btMenu.clickOnce {
            viewModel.onMenuButtonClicked()
        }

        viewModel.languageText.observe(lifecycleOwner) {
            tvLang.text = it
            moveToEdgeIfEnabled()
        }

        viewModel.displayGoogleTranslateIcon.observe(lifecycleOwner) {
            ivGoogleTranslator.showOrHide(it)
            moveToEdgeIfEnabled()
        }

        viewModel.displaySelectButton.observe(lifecycleOwner) {
            btSelect.showOrHide(it)
            moveToEdgeIfEnabled()
        }

        viewModel.displayTranslateButton.observe(lifecycleOwner) {
            btTranslate.showOrHide(it)
            moveToEdgeIfEnabled()
        }

        viewModel.displayCloseButton.observe(lifecycleOwner) {
            btClose.showOrHide(it)
            moveToEdgeIfEnabled()
        }

        viewModel.displayMenuItems.observe(lifecycleOwner) {
            with(menuView) {
                updateData(it)
                attachToScreen()
            }
        }

        viewModel.rescheduleFadeOut.observe(lifecycleOwner) {
            rescheduleFadeOut()
        }

        viewModel.showSettingPage.observe(lifecycleOwner) {
            SettingActivity.start(context)
        }

        viewModel.openBrowser.observe(lifecycleOwner) {
            Utils.openBrowser(it)
        }

        viewModel.showVersionHistory.observe(lifecycleOwner) {
            VersionHistoryView(context).attachToScreen()
        }

        viewModel.showReadme.observe(lifecycleOwner) {
            ReadmeView(context).attachToScreen()
        }
    }

    override fun onAttachedToScreen() {
        super.onAttachedToScreen()
        viewModel.onAttachedToScreen()
    }

    override fun onDetachedFromScreen() {
        super.onDetachedFromScreen()
        viewModel.saveLastPosition(params.x, params.y)
    }
}
