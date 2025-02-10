package com.example.todo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.core.Core
import org.opencv.android.Utils
import org.opencv.core.MatOfPoint

class ImageProcessor(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String) {

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    fun setup() {
        System.loadLibrary("opencv_java4")
        val model = FileUtil.loadMappedFile(context, modelPath)

        val options = Interpreter.Options()
        options.numThreads = 4
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        numChannel = outputShape[1]
        numElements = outputShape[2]

        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clear() {
        interpreter?.close()
        interpreter = null
    }

    // my python code converted to Kotlin by chatgpt
    fun cropMicroscopeRegion(inputBitmap: Bitmap): Bitmap {

        CoroutineScope(Dispatchers.Main).launch {
            // Perform the image processing on a background thread
            val result = withContext(Dispatchers.IO) {

            }
        }

        // Convert Bitmap to Mat (OpenCV's format)
        val mat = Mat()
        Utils.bitmapToMat(inputBitmap, mat)

        // Convert the image to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Create a binary mask where the microscope field is white and everything else is black
        val binaryMask = Mat()
        Imgproc.threshold(grayMat, binaryMask, 20.0, 255.0, Imgproc.THRESH_BINARY)

        // Find contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binaryMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Find the largest contour
        val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }

        if (largestContour != null) {
            // Get the bounding rectangle of the largest contour
            val boundingRect = Imgproc.boundingRect(largestContour)

            // Crop the image using the bounding box
            val croppedMat = Mat(mat, boundingRect)

            // Convert Mat back to Bitmap
            val croppedBitmap = Bitmap.createBitmap(croppedMat.cols(), croppedMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(croppedMat, croppedBitmap)

            // Clean up
            mat.release()
            grayMat.release()
            binaryMask.release()
            croppedMat.release()

            return croppedBitmap
        }

        // If no contour was found, return the original image
        return inputBitmap
    }

    //https://github.com/AarohiSingla/Object-Detection-Android-App/blob/main/android_app/android_app/app/src/main/java/com/surendramaran/yolov8tflite/Detector.kt

    fun detect(frame: Bitmap): List<BoundingBox>? {
        Log.d("RUE", "ImgProcessr/Detecing")
        interpreter ?: return null
        if (tensorWidth == 0) return null
        if (tensorHeight == 0) return null
        if (numChannel == 0) return null
        if (numElements == 0) return null

        var inferenceTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1 , numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, output.buffer)


        val parasiteBoxes = getParasiteBoxes(output.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        return parasiteBoxes

    }

    private fun getParasiteBoxes(array: FloatArray) : List<BoundingBox> {
        Log.d("RUE", "ImgProcessr/getparasites")

        val boundingBoxes = mutableListOf<BoundingBox>()

        // parse array to extract bounding box information
        for (i in array.indices step 6) {
            val xMin = array[i]
            val yMin = array[i + 1]
            val xMax = array[i + 2]
            val yMax = array[i + 3]
            val confidence = array[i + 4]
            val classIndex = array[i + 5].toInt()

            if (confidence >= 0.5) {
                val boundingBox = BoundingBox(
                    x1 = xMin,
                    y1 = yMin,
                    x2 = xMax,
                    y2 = yMax,
                    conf = confidence,
                    cls = classIndex
                )

                boundingBoxes.add(boundingBox)
            }
        }

        Log.d("RUE: Image Processor", "final bounding box amt: " + boundingBoxes.size)
        return boundingBoxes
    }


    fun overlayBoundingBoxes(original: Bitmap, boxes: List<BoundingBox>): Bitmap {
        Log.d("RUE: ImageProcessor", "overlay bounding boxes")

        val outputBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        val class_counts = IntArray(9) { 0 }

        boxes.forEach { box ->
            //if(box.cls in 3 ..8 ){
                val x1 = (box.x1 * original.width).toInt()
                val y1 = (box.y1 * original.height).toInt()
                val x2 = (box.x2 * original.width).toInt()
                val y2 = (box.y2 * original.height).toInt()
                canvas.drawRect(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)
            //}

            if (box.cls in class_counts.indices) {
                class_counts[box.cls] += 1
            }
        }

        Log.d("RUE: ImageProcessor", "classes: " + class_counts.toString())

        return outputBitmap
    }

    fun saveImage(bitmap: Bitmap): Uri? {
        val filename = "output_${System.currentTimeMillis()}.jpg"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)

        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            //after saving as file, get URI using FileProvider
            val uri = FileProvider.getUriForFile(context, "com.example.todo.fileprovider", file)
            return uri
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
    }
}