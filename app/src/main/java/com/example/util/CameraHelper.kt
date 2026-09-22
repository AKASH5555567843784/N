package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(private val context: Context) {
    private val TAG = "CameraHelper"

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    interface CameraCallback {
        fun onSuccess(file: File)
        fun onError(exception: Exception)
    }

    /**
     * Binds CameraX Preview and ImageCapture use cases to the given LifecycleOwner.
     */
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Configure Preview if surface provider is available
                val preview = Preview.Builder().build().also {
                    if (surfaceProvider != null) {
                        it.setSurfaceProvider(surfaceProvider)
                    }
                }

                // Configure ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Use back camera as default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind previous use cases
                cameraProvider.unbindAll()

                // Bind to lifecycle
                if (surfaceProvider != null) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture
                    )
                }

                Log.d(TAG, "CameraX bound successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                onError?.invoke(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Takes a photo and saves it locally, then triggers callbacks.
     */
    fun takePhoto(callback: CameraCallback) {
        val imageCaptureInstance = imageCapture ?: run {
            callback.onError(IllegalStateException("Camera not bound or initialized yet."))
            return
        }

        // Create output file
        val outputDirectory = context.cacheDir
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCaptureInstance.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    callback.onSuccess(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    callback.onError(exception)
                }
            }
        )
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
