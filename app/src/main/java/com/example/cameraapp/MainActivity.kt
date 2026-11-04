
package com.example.cameraapp

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                CameraApp()
            }
        }
    }
}

@Composable
fun CameraApp() {

    val context = LocalContext.current

    var photoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var pendingUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var capturedBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    // Create a file to store the captured photograph
    fun createPhotoUri(): Uri {

        val imageDirectory = File(
            context.cacheDir,
            "camera_images"
        )

        imageDirectory.mkdirs()

        val imageFile = File(
            imageDirectory,
            "photo_${System.currentTimeMillis()}.jpg"
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    // Open camera and save the photograph
    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {
                photoUri = pendingUri
            } else {
                Toast.makeText(
                    context,
                    "Photo capture cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Request camera permission
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                pendingUri?.let {
                    cameraLauncher.launch(it)
                }

            } else {

                Toast.makeText(
                    context,
                    "Camera permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Load captured photograph
    LaunchedEffect(photoUri) {

        capturedBitmap = photoUri?.let { uri ->

            withContext(Dispatchers.IO) {

                context.contentResolver
                    .openInputStream(uri)
                    ?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
            }
        }
    }

    // Open camera button action
    fun openCamera() {

        val uri = createPhotoUri()
        pendingUri = uri

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {

            cameraLauncher.launch(uri)

        } else {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // Share captured photograph
    fun sharePhoto() {

        val uri = photoUri ?: return

        val shareIntent = Intent(
            Intent.ACTION_SEND
        ).apply {

            type = "image/jpeg"

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            clipData = ClipData.newUri(
                context.contentResolver,
                "Captured Photo",
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                "Share Photo"
            )
        )
    }

    // Application UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Simple Camera App",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Display captured photograph
            if (capturedBitmap != null) {

                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Photograph",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "No Photo Captured Yet"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Open Camera Button
            Button(
                onClick = { openCamera() },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text("Open Camera")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Share Button
            Button(
                onClick = { sharePhoto() },
                enabled = photoUri != null &&
                        capturedBitmap != null,
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text("Share Photo")
            }
        }
    }
}
