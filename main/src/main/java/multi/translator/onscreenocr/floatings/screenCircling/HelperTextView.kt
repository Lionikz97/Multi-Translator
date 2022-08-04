package multi.translator.onscreenocr.floatings.screenCircling

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.CountDownTimer
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import multi.translator.onscreenocr.R
import multi.translator.onscreenocr.utils.Logger
import kotlin.properties.Delegates

class HelperTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    companion object {
        private const val DELAY_TEXT_HIDDEN_ANIM: Long = 2000
        private const val TIME_TEXT_HIDDEN_ANIM: Long = 800
        private const val INTERVAL_TEXT_HIDDEN_ANIM: Long = 10

        private val MAX_ALPHA = 200
    }

    private val logger: Logger by lazy { Logger(HelperTextView::class) }

    private val textPaint: Paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimensionPixelSize(R.dimen.circlingView_helperTextSize).toFloat()
        color = ContextCompat.getColor(context, R.color.circlingView_helperText)
    }

    var hasBox: Boolean by Delegates.observable(false) { _, _, _ ->
        invalidate()
    }

    var isDrawing: Boolean by Delegates.observable(false) { _, _, _ ->
        invalidate()
    }

    private var redrawHelpTextTimer: CountDownTimer? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isInEditMode) return

        drawHelpText(canvas)
    }

    private fun drawHelpText(canvas: Canvas) {
        if (isDrawing) return

        val message: String = if (hasBox)
            context.getString(R.string.msg_recreate_selection_area)
        else {
            textPaint.alpha = 255
            context.getString(R.string.msg_create_selection_area)
        }

        val xPos = canvas.width / 2
        val yPos = (canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2).toInt()
        //((textPaint.descent() + textPaint.ascent()) / 2) is the distance from the baseline to the center.

        canvas.drawText(message, xPos.toFloat(), yPos.toFloat(), textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnim()
    }

    fun startAnim() {
        stopAnim()

        if (!hasBox) {
            invalidate()
            return
        }

        redrawHelpTextTimer = object : CountDownTimer(
            TIME_TEXT_HIDDEN_ANIM + DELAY_TEXT_HIDDEN_ANIM,
            INTERVAL_TEXT_HIDDEN_ANIM
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val alpha: Int = if (millisUntilFinished < TIME_TEXT_HIDDEN_ANIM) {
                    (millisUntilFinished.toFloat() / TIME_TEXT_HIDDEN_ANIM.toFloat() * MAX_ALPHA.toFloat()).toInt()
                    //                    logger.info("Alpha: " + alpha);
                } else {
                    MAX_ALPHA
                    //                    logger.info("Alpha: fixed " + alpha);
                }
                textPaint.alpha = alpha
                //                logger.info("borderAnimationProgress: " + borderAnimationProgress);
                invalidate()
            }

            override fun onFinish() {
                textPaint.alpha = 0
                //                logger.info("borderAnimationProgress: finished");
                invalidate()
            }
        }.start()
    }

    fun stopAnim() {
        redrawHelpTextTimer?.cancel()
        redrawHelpTextTimer = null
    }
}
