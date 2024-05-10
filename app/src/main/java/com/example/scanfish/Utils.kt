package com.example.scanfish

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import com.example.scanfish.ml.FreshnessModel
import com.example.scanfish.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Utils {
}

private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())


fun getImageUri(context: Context): Uri {
    var uri: Uri? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
        }
        uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }
    return uri ?: getImageUriForPreQ(context)
}

private fun getImageUriForPreQ(context: Context): Uri {
    val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File(filesDir, "/MyCamera/$timeStamp.jpg")
    if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdir()
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        imageFile
    )
}

fun resizeBitmap(inputBitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    var width = inputBitmap.width
    var height = inputBitmap.height

    val aspectRatio: Float = width.toFloat() / height.toFloat()

    // Resizing if needed
    if (width > maxWidth || height > maxHeight) {
        if (width > height) {
            width = maxWidth
            height = (width / aspectRatio).toInt()
        } else {
            height = maxHeight
            width = (height * aspectRatio).toInt()
        }
    }

    return Bitmap.createScaledBitmap(inputBitmap, width, height, true)
}

fun processImage(imageSize: Int, resize: Bitmap): TensorBuffer {
    val inputFeature0 =
        TensorBuffer.createFixedSize(
            intArrayOf(1, imageSize, imageSize, 3),
            DataType.FLOAT32
        )

    val intValues = IntArray(resize.width * resize.height)
    resize.getPixels(intValues, 0, resize.width, 0, 0, resize.width, resize.height)

    val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
    byteBuffer.order(ByteOrder.nativeOrder())

    var pixel = 0

    for (i in 0 until imageSize) {
        for (j in 0 until imageSize) {
            val `val` = intValues[pixel++] // RGB
            byteBuffer.putFloat(((`val` shr 16) and 0xFF).toFloat() * (1f / 255f))
            byteBuffer.putFloat(((`val` shr 8) and 0xFF).toFloat() * (1f / 255f))
            byteBuffer.putFloat((`val` and 0xFF).toFloat() * (1f / 255f))
        }
    }
    inputFeature0.loadBuffer(byteBuffer)

    return inputFeature0
}

fun predictWithModel1(
    context: Context,
    inputFeature0: TensorBuffer,
    threshold: Float = 0.5f
): String {
    val model = FreshnessModel.newInstance(context)
    val outputs = model.process(inputFeature0)
    val outputFeature0 = outputs.outputFeature0AsTensorBuffer
    val confidences = outputFeature0.floatArray

    var maxIdx = 0
    confidences.forEachIndexed { index, fl ->
        if (confidences[maxIdx] < fl) {
            maxIdx = index
        }
    }

    val predictedLabel = if (confidences[maxIdx] < threshold) {
        "Segar"
    } else {
        "Tidak Segar"
    }
    model.close()

    return predictedLabel
}

fun predictWithModel2(context: Context, inputFeature0: TensorBuffer): String {
    val model = Model.newInstance(context)
    val outputs = model.process(inputFeature0)
    val outputFeature0 = outputs.outputFeature0AsTensorBuffer
    val confidences = outputFeature0.floatArray

    var maxPos = 0
    var maxConfidence = 0f
    for (i in confidences.indices) {
        if (confidences[i] > maxConfidence) {
            maxConfidence = confidences[i]
            maxPos = i
        }
    }

    val labels =
        context.assets.open("labels.txt").bufferedReader().readLines()

    val predictedLabel = labels[maxPos]

    model.close()

    return predictedLabel
}

fun popupSuccessfull(title: String, message: String, context: Context) {
    val dialog = Dialog(context)
    dialog.setContentView(R.layout.popup_costum)

    val popupTitle: TextView = dialog.findViewById(R.id.title_popup)
    popupTitle.text = title

    val popupMessage: TextView = dialog.findViewById(R.id.deskripsi_popup)
    popupMessage.text = message

    val popupButton: Button = dialog.findViewById(R.id.button_popup)
    popupButton.text = context.getString(R.string.oke)
    popupButton.setOnClickListener {
        dialog.dismiss()
    }

    val buttonYes: Button = dialog.findViewById(R.id.button_popup_yes)
    buttonYes.visibility = View.GONE

    val window = dialog.window
    val layoutParams = window?.attributes
    val displayMetrics = DisplayMetrics()
    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
    val screenWidth = displayMetrics.widthPixels
    layoutParams?.width = (screenWidth * 1)
    window?.attributes = layoutParams

    dialog.show()
}

fun popupFailed(title: String, message: String, context: Context) {
    val dialog = Dialog(context)
    dialog.setContentView(R.layout.popup_costum)

    val imagePopup: ImageView = dialog.findViewById(R.id.image_popup)
    imagePopup.setImageResource(R.drawable.check_x)

    val popupTitle: TextView = dialog.findViewById(R.id.title_popup)
    popupTitle.text = title

    val popupMessage: TextView = dialog.findViewById(R.id.deskripsi_popup)
    popupMessage.text = message

    val popupButton: Button = dialog.findViewById(R.id.button_popup)
    popupButton.text = context.getString(R.string.oke)
    popupButton.setOnClickListener {
        dialog.dismiss()
    }

    val buttonYes: Button = dialog.findViewById(R.id.button_popup_yes)
    buttonYes.visibility = View.GONE

    val window = dialog.window
    val layoutParams = window?.attributes
    val displayMetrics = DisplayMetrics()
    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
    val screenWidth = displayMetrics.widthPixels
    layoutParams?.width = (screenWidth * 1)
    window?.attributes = layoutParams

    dialog.show()
}




