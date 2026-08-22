package com.malletcrafts.sitephotos.pano

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Does the mark survive the trip?
 *
 * The whole mechanism rests on one physical claim: a QR painted into the
 * caption bar is still readable after ImageMeter has opened the picture,
 * drawn on it, and written a new JPEG — possibly smaller, certainly
 * recompressed. If that claim is false the app finds nothing and says so
 * politely, which is the worst kind of failure: silent and plausible.
 *
 * So it is measured here rather than discovered on a phone. This reproduces
 * FaceWriter's geometry (strip height, stamp box, position) in pure AWT,
 * pushes the result through real JPEG encode/decode at several scales and
 * qualities, and reads it back with the same ZXing decoder StampScan uses.
 *
 * The numbers are deliberately harsher than reality: quality 0.5 and half
 * scale are worse than any photo exporter would do. Margin is the point —
 * a stamp that only just survives the test case will not survive the world.
 */
class StampSurvivalTest {

    private val id = "MCAP-4b2014dcba5e"
    private val face = "front"

    /** FaceWriter's own numbers, so this test moves when that code does. */
    private val minStampPx = 108

    private fun captioned(faceW: Int, faceH: Int): BufferedImage {
        val strip = maxOf((faceH * 0.052).toInt().coerceAtLeast(28), minStampPx + 12)
        val img = BufferedImage(faceW, faceH + strip, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        // A busy grey field stands in for a photograph: a white one would
        // give the decoder contrast it will not have in life.
        g.color = Color(140, 140, 140)
        g.fillRect(0, 0, faceW, faceH)
        g.color = Color(17, 24, 31)
        g.fillRect(0, faceH, faceW, strip)

        val box = (strip * 0.82f).toInt().coerceAtLeast(minStampPx)
        val m = Stamp.matrix(Stamp.payload(id, face), box)
        val left = (faceW - box - strip * 0.09f).toInt()
        val top = faceH + (strip - box) / 2
        val scale = box.toFloat() / m.size
        g.color = Color.WHITE
        g.fillRect(left, top, box, box)
        g.color = Color.BLACK
        for (y in m.indices) for (x in m[y].indices) {
            if (!m[y][x]) continue
            g.fillRect((left + x * scale).toInt(), (top + y * scale).toInt(),
                maxOf(1, (scale + 0.5f).toInt()), maxOf(1, (scale + 0.5f).toInt()))
        }
        g.dispose()
        return img
    }

    /** A real JPEG round trip at the given quality, optionally rescaled —
     *  which is what "ImageMeter exported it" amounts to. */
    private fun reencode(src: BufferedImage, quality: Float, scale: Double): BufferedImage {
        val w = (src.width * scale).toInt()
        val h = (src.height * scale).toInt()
        val scaled = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        scaled.createGraphics().apply {
            setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(src, 0, 0, w, h, null)
            dispose()
        }
        val out = ByteArrayOutputStream()
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        writer.output = ImageIO.createImageOutputStream(out)
        val param = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality
        }
        writer.write(null, IIOImage(scaled, null, null), param)
        writer.dispose()
        return ImageIO.read(ByteArrayInputStream(out.toByteArray()))
    }

    /** The same decode StampScan does, over the bottom strip. */
    private fun read(img: BufferedImage): String? {
        val top = (img.height * 0.78).toInt().coerceIn(0, img.height - 1)
        val crop = img.getSubimage(0, top, img.width, img.height - top)
        val px = IntArray(crop.width * crop.height)
        crop.getRGB(0, 0, crop.width, crop.height, px, 0, crop.width)
        return try {
            MultiFormatReader().decode(
                BinaryBitmap(HybridBinarizer(
                    RGBLuminanceSource(crop.width, crop.height, px))),
                mapOf(DecodeHintType.TRY_HARDER to true),
            ).text
        } catch (e: Exception) {
            null
        }
    }

    private fun survives(faceW: Int, faceH: Int, quality: Float, scale: Double) {
        val text = read(reencode(captioned(faceW, faceH), quality, scale))
        assertNotNull(text, "unreadable after JPEG q=$quality scale=$scale " +
            "on a ${faceW}x$faceH face — the stamp is too small to survive")
        val mark = Stamp.parse(text)
        assertEquals(id, mark?.captureId, "wrong id after q=$quality scale=$scale")
        assertEquals(face, mark?.face, "wrong face after q=$quality scale=$scale")
    }

    @Test
    fun `a projected face survives a normal export`() {
        survives(1536, 1536, 0.9f, 1.0)
    }

    @Test
    fun `it survives hard compression`() {
        // Quality 0.5 is well below anything a photo tool ships.
        survives(1536, 1536, 0.5f, 1.0)
    }

    @Test
    fun `it survives being halved`() {
        // A downscale is the real danger: it shrinks the modules themselves.
        survives(1536, 1536, 0.8f, 0.5)
    }

    @Test
    fun `a big phone photo survives too`() {
        // A 12MP snap: the strip is proportionally larger, so this should be
        // the easy case — and if it ever is not, the proportional sizing is
        // what broke.
        survives(4032, 3024, 0.8f, 1.0)
    }

    @Test
    fun `a small face survives, because the strip grows rather than the stamp shrinking`() {
        // 640px: 5.2% would be a 33px strip, far too small for a QR. The
        // floor at MIN_STAMP_PX is what saves this case, and this test is
        // what stops someone "tidying up" that floor.
        survives(640, 640, 0.8f, 1.0)
    }
}
