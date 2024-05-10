package com.example.scanfish.ui.beranda

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.scanfish.R
import com.example.scanfish.databinding.ActivityBerandaBinding
import com.example.scanfish.getImageUri
import com.example.scanfish.popupFailed
import com.example.scanfish.popupSuccessfull
import com.example.scanfish.predictWithModel1
import com.example.scanfish.predictWithModel2
import com.example.scanfish.processImage
import com.example.scanfish.resizeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Beranda : AppCompatActivity() {

    private var _binding: ActivityBerandaBinding? = null
    private val binding get() = _binding!!
    private var currentImageUri: Uri? = null

    private lateinit var bitmap: Bitmap
    private lateinit var resize: Bitmap
    private val targetWidth = 800
    private val targetHeight = 700
    private val imageSize = 250

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityBerandaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding.btnGallery.setOnClickListener { selectImage() }
        binding.btnCamera.setOnClickListener { startCamera() }
        binding.btnScan.setOnClickListener { performPrediction() }


    }

    private fun selectImage() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage(currentImageUri)
        }
    }

    private fun showImage(uri: Uri?) {
        uri?.let {
            Log.d("Image URI", "showImage: $it")
            onActivityResult(100, Activity.RESULT_OK, Intent().apply { this.data = uri })
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    currentImageUri = uri
                    bitmap =
                        MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

                    // Check if the bitmap size is too large, and resize if needed
                    if (bitmap.byteCount > MAX_BITMAP_SIZE_BYTES) {
                        bitmap = resizeBitmap(bitmap, targetWidth, targetHeight)
                    }
                    resize = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false)
                    binding.hasilGambar.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("Error", "Error loading bitmap: ${e.message}")
                }
            } else {
                Log.d("Photo Picker", "No media selected")
            }
        }
    }

    private fun performPrediction() {
        if (!::resize.isInitialized) {
            popupFailed(
                getString(R.string.scan_failed),
                "Silahkan pilih gambar terlebih dahulu",
                this@Beranda
            )
            return
        }
        buttonClickable(false)
        showLoading(true)
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                // Proses gambar di background thread
                val processedImage = withContext(Dispatchers.Default) {
                    processImage(imageSize, resize)
                }
                val prediction1 = withContext(Dispatchers.Default) {
                    predictWithModel1(this@Beranda, processedImage)
                }
                val prediction2 = withContext(Dispatchers.Default) {
                    predictWithModel2(this@Beranda, processedImage)
                }
                // Setelah selesai, kembali ke main thread untuk melakukan tindakan selanjutnya
                withContext(Dispatchers.Main) {
                    buttonClickable(true)
                    showLoading(false)
                    popupSuccessfull(
                        getString(R.string.scan_successfull),
                        "Ikan : $prediction2 \nStatus Kesegaran: $prediction1",
                        this@Beranda
                    )
                    Log.d("Hasil Prediksi", "Hasil 1: $prediction1, Hasil 2: $prediction2")
                }
            }
        } catch (e: Exception) {
            buttonClickable(true)
            showLoading(false)
            popupFailed(
                getString(R.string.scan_failed),
                "Terjadi Kesalahan \nSaat Scan Gambar \nSilahkan Pilih Gambar Lagi ",
                this@Beranda
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun buttonClickable(isTrue: Boolean) {
        binding.btnScan.isClickable = isTrue
        if (!isTrue) {
            binding.btnScan.text = "Scanning..."
        } else {
            binding.btnScan.text = "Scan Ikan"
        }
        binding.btnGallery.isClickable = isTrue
        binding.btnCamera.isClickable = isTrue
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val MAX_BITMAP_SIZE_BYTES = 6 * 1024 * 1024

    }
}