package io.github.emmanuelquintana.capri

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Her Night City HUD (capri.css), for the app's own screen: the warm near-black, the neon yellow,
 * cyan data, red for danger, plates outlined in yellow with one corner cut off and a cyan copy
 * knocked out of register behind them. Flat, hard-edged: no gradients, no soft shadows.
 */
object Hud {
    // capri.css :root, oklch converted to sRGB
    val INK = Color.rgb(19, 18, 13)        // --ink
    val INK_2 = Color.rgb(31, 30, 23)      // --ink-2
    val PAPER = Color.rgb(242, 241, 232)   // --paper
    val PAPER_2 = Color.rgb(207, 206, 197) // --paper-2
    val PAPER_3 = Color.rgb(146, 145, 136) // --paper-3
    val YELLOW = Color.rgb(246, 236, 0)    // --yellow
    val CYAN = Color.rgb(0, 223, 234)      // --cyan
    val RED = Color.rgb(255, 32, 82)       // --red
    // her own colours (fx.js MANE, the --green of her mask): the app wears them next to the yellow
    val MANE = Color.rgb(250, 4, 72)       // her mane
    val GREEN = Color.rgb(54, 255, 122)    // her mask and eyes
    val LINE = Color.argb(26, 242, 241, 232) // --line
    val LINE_STRONG = Color.argb(64, 242, 241, 232)

    /**
     * A plate: `fill` inside, a `frame` outline, the top right corner cut by `cut` px, and an
     * `echo` copy offset down-right behind it (none when 0). Pressed, it fills with the frame colour.
     */
    class Plate(
        private val fill: Int,
        private val frame: Int,
        private val echo: Int = 0,
        private val cut: Float,
        private val stroke: Float,
        private val offset: Float,
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        private var pressed = false

        override fun isStateful() = true

        override fun onStateChange(state: IntArray): Boolean {
            val now = state.contains(android.R.attr.state_pressed)
            if (now == pressed) return false
            pressed = now
            invalidateSelf()
            return true
        }

        private fun shape(l: Float, t: Float, r: Float, b: Float) {
            path.reset()
            path.moveTo(l, t)
            path.lineTo(r - cut, t)
            path.lineTo(r, t + cut)
            path.lineTo(r, b)
            path.lineTo(l, b)
            path.close()
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val o = if (echo != 0) offset else 0f
            val l = b.left.toFloat()
            val t = b.top.toFloat()
            val r = b.right - o
            val bt = b.bottom - o
            if (echo != 0) {
                shape(l + o, t + o, r + o, bt + o)
                paint.style = Paint.Style.FILL
                paint.color = echo
                canvas.drawPath(path, paint)
            }
            shape(l, t, r, bt)
            paint.style = Paint.Style.FILL
            paint.color = if (pressed) frame else fill
            canvas.drawPath(path, paint)
            if (stroke > 0) {
                val h = stroke / 2
                shape(l + h, t + h, r - h, bt - h)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = stroke
                paint.color = frame
                canvas.drawPath(path, paint)
            }
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    /** Her screen's backdrop: the ink, with the faint scanlines of a HUD. */
    class Backdrop(private val step: Float) : Drawable() {
        // one tile (a line, then ink) repeated by the GPU: not hundreds of lines drawn every frame (1.9.0)
        private val paint = Paint().apply {
            val h = step.toInt().coerceAtLeast(2)
            val tile = android.graphics.Bitmap.createBitmap(1, h, android.graphics.Bitmap.Config.ARGB_8888)
            tile.eraseColor(INK)
            tile.setPixel(0, 0, blend(INK, Color.argb(10, 242, 241, 232)))
            shader = android.graphics.BitmapShader(tile, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
        }

        private fun blend(under: Int, over: Int): Int {
            val a = Color.alpha(over) / 255f
            fun mix(u: Int, o: Int) = (u + (o - u) * a).toInt().coerceIn(0, 255)
            return Color.rgb(mix(Color.red(under), Color.red(over)), mix(Color.green(under), Color.green(over)), mix(Color.blue(under), Color.blue(over)))
        }

        override fun draw(canvas: Canvas) {
            canvas.save()
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.drawRect(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(), paint)
            canvas.restore()
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.OPAQUE
    }
}
