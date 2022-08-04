package multi.translator.onscreenocr.floatings.translationSelectPanel

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.databinding.FloatingTranslationSelectPanelBinding
import multi.translator.onscreenocr.databinding.ItemLangListBinding
import multi.translator.onscreenocr.floatings.base.FloatingView
import multi.translator.onscreenocr.floatings.menu.MenuView
import multi.translator.onscreenocr.utils.Logger
import multi.translator.onscreenocr.utils.clickOnce
import multi.translator.onscreenocr.utils.setTextOrGone

class TranslationSelectPanel(context: Context) : FloatingView(context) {
    override val layoutId: Int
        get() = R.layout.floating_translation_select_panel

    override val layoutWidth: Int
        get() = WindowManager.LayoutParams.MATCH_PARENT

    override val layoutHeight: Int
        get() = WindowManager.LayoutParams.MATCH_PARENT

    private val logger: Logger by lazy { Logger(TranslationSelectPanel::class) }

    private val viewModel: TranslationSelectPanelViewModel by lazy {
        TranslationSelectPanelViewModel(viewScope)
    }

    private val binding: FloatingTranslationSelectPanelBinding =
        FloatingTranslationSelectPanelBinding.bind(rootLayout)

    private val translationProviderMenuView: MenuView by lazy {
        MenuView(context, checkable = true).apply {
            setAnchor(binding.tvTranslationProvider)
            onItemSelected = { view, key ->
                logger.debug("onItemSelected: $key")

                view.detachFromScreen()
                viewModel.onTranslationProviderSelected(key)
            }
        }
    }

    private lateinit var ocrLangListAdapter: LangListAdapter<OCRLangItem>
    private lateinit var translationLangListAdapter: LangListAdapter<TranslateLangItem>

    init {
        setViews()
        loadData()
    }

    private fun setViews() {
        binding.viewOutside.clickOnce { detachFromScreen() }
        binding.btClose.clickOnce { detachFromScreen() }

        with(binding.rvOcrLang) {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            ocrLangListAdapter = LangListAdapter(
                context = context,
                diffCallback = object : DiffUtil.ItemCallback<OCRLangItem>() {
                    override fun areItemsTheSame(
                        oldItem: OCRLangItem, newItem: OCRLangItem
                    ): Boolean =
                        oldItem.code == newItem.code && oldItem.recognizer == newItem.recognizer

                    override fun areContentsTheSame(
                        oldItem: OCRLangItem, newItem: OCRLangItem
                    ): Boolean = oldItem == newItem
                },
                onItemClicked = {
                    logger.debug("on OCR lang checked, $it")

                    viewModel.onOCRLangSelected(it)
                })
            adapter = ocrLangListAdapter
        }

        with(binding.rvTranslationLang) {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            translationLangListAdapter = LangListAdapter(
                context = context,
                diffCallback = object : DiffUtil.ItemCallback<TranslateLangItem>() {
                    override fun areItemsTheSame(
                        oldItem: TranslateLangItem, newItem: TranslateLangItem
                    ): Boolean = oldItem.code == newItem.code

                    override fun areContentsTheSame(
                        oldItem: TranslateLangItem,
                        newItem: TranslateLangItem
                    ): Boolean = oldItem == newItem
                },
                onItemClicked = {
                    logger.debug("on translation lang checked, $it")

                    viewModel.onTranslationLangChecked(it.code)
                })
            adapter = translationLangListAdapter
        }

        binding.tvTranslationProvider.clickOnce {
            viewModel.onTranslationProviderClicked()
        }

        viewModel.ocrLanguageList.observe(lifecycleOwner) {
            logger.debug("on ocrLanguageList changed: $it")

            ocrLangListAdapter.submitList(it) {
                binding.rvOcrLang.scrollToPosition(
                    it.indexOfFirst { item -> item.selected }
                        .coerceAtLeast(0))
            }
        }

        viewModel.selectedTranslationProviderName.observe(lifecycleOwner) {
            binding.tvTranslationProvider.text = it
        }

        viewModel.translationLangList.observe(lifecycleOwner) {
            translationLangListAdapter.submitList(it) {
                binding.rvTranslationLang.scrollToPosition(
                    it.indexOfFirst { item -> item.selected }
                        .coerceAtLeast(0)
                )
            }
        }

        viewModel.displayTranslationHint.observe(lifecycleOwner) {
            binding.tvTranslationLangHint.setTextOrGone(it)
        }

        viewModel.displayTranslateProviders.observe(lifecycleOwner) {
            translationProviderMenuView.apply {
                updateData(
                    itemList = it.map { it.key to it.displayName }.toMap(),
                    selectedKey = it.firstOrNull { it.selected }?.key
                )
            }.attachToScreen()
        }
    }

    override fun onBackButtonPressed(): Boolean {
        detachFromScreen()
        return true
    }

    private fun loadData() {
        viewModel.load()
    }

    private class LangListAdapter<T : LangItem>(
        private val context: Context,
        diffCallback: DiffUtil.ItemCallback<T>,
        private val onItemClicked: (lang: T) -> Unit,
    ) :
        ListAdapter<T, LangListAdapter.ViewHolder>(diffCallback) {
        class ViewHolder(val binding: ItemLangListBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemLangListBinding.inflate(LayoutInflater.from(context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)

            with(holder.binding.lang) {
                text = item.displayName
                isChecked = item.selected
                val drawable =
                    if (item.showDownloadIcon)
                        ContextCompat.getDrawable(context, R.drawable.ic_download)
                    else null
                setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (item.unrecommended) R.color.alert else R.color.foreground,
                    )
                )
            }

            holder.itemView.clickOnce { onItemClicked.invoke(item) }
        }
    }
}
