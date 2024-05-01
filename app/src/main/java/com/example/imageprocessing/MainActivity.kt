package com.example.imageprocessing
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.icu.text.SimpleDateFormat
import android.location.LocationManager
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val STORAGE_PERMISSION_REQUEST_CODE = 101
    private val LOCATION_PERMISSION_REQUEST_CODE = 102
    private val REQUEST_IMAGE_CAPTURE = 103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestCameraPermission()

        val imageView = findViewById<ImageView>(R.id.imageView)

        val cameraBtn = findViewById<Button>(R.id.cameraBtn)

        val saveImage = findViewById<Button>(R.id.saveImage)

        cameraBtn.setOnClickListener { dispatchTakePictureIntent() }

        saveImage.setOnClickListener {
            requestStoragePermission()
            saveImageToGallery(imageView.drawable.toBitmap())
        }

    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val imageFileName = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) + ".jpg"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, imageFileName)

        try {
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()

            // Notify the gallery about the new file
            MediaScannerConnection.scanFile(
                this,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val imageView = findViewById<ImageView>(R.id.imageView)

            // Add text to the image
            val imageWithText = addTextToImage(imageBitmap,)
            imageView.setImageBitmap(imageWithText)
        }
    }

    private fun addTextToImage(imageBitmap: Bitmap): Bitmap {
        requestLocationPermission()
        val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.textSize = 8f // Adjust font size as needed
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Adjust font style as needed

        // Retrieve location data
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val locationText = "Location: ${location?.latitude}, ${location?.longitude}"

        // Retrieve time data
        val currentTime = Calendar.getInstance().time
        val timeText = "Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentTime)}"

        // Calculate the position of the text
        val textBounds1 = Rect()
        val textBounds2 = Rect()
        paint.getTextBounds(locationText, 0, locationText.length, textBounds1)
        paint.getTextBounds(timeText, 0, timeText.length, textBounds2)
        val x1 = mutableBitmap.width - textBounds1.width() - 20 // Adjust margin from right
        val y1 = mutableBitmap.height - textBounds1.height() - 20 // Adjust margin from bottom
        val x2 = mutableBitmap.width - textBounds2.width() - 20 // Adjust margin from right
        val y2 = mutableBitmap.height - textBounds2.height() - 40 - textBounds1.height() // Adjust margin from bottom

        // Draw text onto the canvas
        canvas.drawText(locationText, x1.toFloat(), y1.toFloat(), paint)
        canvas.drawText(timeText, x2.toFloat(), y2.toFloat(), paint)

        return mutableBitmap
    }


    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            // Permission already granted, open camera
            dispatchTakePictureIntent()
        }
    }

    private fun requestStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), STORAGE_PERMISSION_REQUEST_CODE)
        } else {
            // If storage permissions are already granted, request location permission
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Location permission granted, you can perform location-related tasks here.
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Permission granted, you can perform the desired action here.
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted, you can perform location-related tasks here.
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}