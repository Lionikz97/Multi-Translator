package multi.translator.onscreenocr.floatings.readme

import android.content.Context
import android.view.View
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.floatings.dialog.DialogView


class ReadmeView(context: Context) :
    DialogView(context) {

    companion object {
        var VERSION = "2.2.0"
    }

    init {
        setTitle("How to use Multi Translator")
        setDialogType(DialogType.CONFIRM_ONLY)
        setContentView(View.inflate(context, R.layout.view_help, null))
    }
}