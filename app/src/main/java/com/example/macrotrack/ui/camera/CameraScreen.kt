package com.example.macrotrack.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macrotrack.util.AppViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun CameraScreen(factory: AppViewModelFactory) {
    val context = LocalContext.current
    val viewModel: MealCaptureViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedPath by remember { mutableStateOf<String?>(null) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !hasCameraPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("MacroTrack a besoin de la camera pour photographier tes plats.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Autoriser la camera")
                    }
                }
            }

            capturedBitmap == null -> {
                CameraPreviewWithCapture { bitmap, path ->
                    capturedBitmap = bitmap
                    capturedPath = path
                    showDescriptionDialog = true
                }
            }

            else -> {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Photo du plat",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (state is MealCaptureState.Analyzing) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Gemini analyse ton plat...")
                }
            }
        }

        val resultState = state as? MealCaptureState.Result
        if (resultState != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Analyse Gemini") },
                text = {
                    Column {
                        Text("Calories : ${resultState.analysis.calories} kcal")
                        Text("Proteines : ${resultState.analysis.proteinG} g")
                        Text("Glucides : ${resultState.analysis.carbsG} g")
                        Text("Lipides : ${resultState.analysis.fatG} g")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.confirmSave(resultState.analysis, resultState.photoPath, resultState.description)
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        viewModel.reset()
                        capturedBitmap = null
                        capturedPath = null
                    }) { Text("Reprendre la photo") }
                }
            )
        }

        val errorState = state as? MealCaptureState.Error
        if (errorState != null) {
            AlertDialog(
                onDismissRequest = { viewModel.reset() },
                title = { Text("Oups") },
                text = { Text(errorState.message) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.reset()
                        capturedBitmap = null
                        capturedPath = null
                    }) { Text("OK") }
                }
            )
        }

        if (state is MealCaptureState.Saved) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Repas enregistre !", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    viewModel.reset()
                    capturedBitmap = null
                    capturedPath = null
                    description = ""
                }) { Text("Ajouter un autre repas") }
            }
        }

        if (showDescriptionDialog) {
            AlertDialog(
                onDismissRequest = { showDescriptionDialog = false },
                title = { Text("Decris ton plat (optionnel)") },
                text = {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Ex : j'ai rajoute une cuillere d'huile d'olive") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showDescriptionDialog = false
                        capturedBitmap?.let { bmp ->
                            viewModel.analyze(bmp, capturedPath.orEmpty(), description)
                        }
                    }) { Text("Analyser") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDescriptionDialog = false }) { Text("Annuler") }
                }
            )
        }
    }
}

@Composable
private fun CameraPreviewWithCapture(onCaptured: (Bitmap, String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder().build()
                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        // Camera indisponible (emulateur sans camera, etc.)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        FloatingActionButton(
            onClick = {
                val capture = imageCapture ?: return@FloatingActionButton
                val photoFile = File(
                    context.filesDir,
                    "meal_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap != null) {
                                onCaptured(bitmap, photoFile.absolutePath)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // On pourrait afficher un message d'erreur ici.
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
            )
        }
    }
}
