package multi.translator.onscreenocr.floatings.history

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import multi.translator.onscreenocr.floatings.base.FloatingViewModel
import multi.translator.onscreenocr.repo.GeneralRepository
import multi.translator.onscreenocr.utils.Logger
import multi.translator.onscreenocr.utils.Utils

class VersionHistoryViewModel(viewScope: CoroutineScope) : FloatingViewModel(viewScope) {

    private val _recordList = MutableLiveData<List<GeneralRepository.Record>>()
    val recordList: LiveData<List<GeneralRepository.Record>> = _recordList

    private val context: Context by lazy { Utils.context }
    private val logger: Logger by lazy { Logger(VersionHistoryViewModel::class) }

    private val repo: GeneralRepository by lazy { GeneralRepository() }

    fun load() {
        viewScope.launch {
            _recordList.value = repo.getVersionHistory().first()
        }
    }
}
