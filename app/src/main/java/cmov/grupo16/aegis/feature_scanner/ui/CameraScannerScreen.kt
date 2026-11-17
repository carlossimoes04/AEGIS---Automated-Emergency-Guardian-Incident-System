package cmov.grupo16.aegis.feature_scanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

private val HudDarkBg = Color(0xFF090D16)
private val HudSurface = Color(0xD91E293B)
private val HudCyan = Color(0xFF00E5FF)
private val HudBorder = Color(0xFF38BDF8)
private val HudAlertRed = Color(0xFFEF4444)

@Composable
fun CameraScannerScreen(
    uiState: ScannerUiState,
    onImageCaptured: (Bitmap, Int) -> Unit,
    onCameraError: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        PermissionRequiredContent(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = modifier
        )
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                onCameraError(e.message ?: "Failed to bind camera use cases.")
            }
        }, executor)

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HudDarkBg)
    ) {
        // Live CameraX Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Tactical Reticle HUD
        TacticalReticleOverlay(modifier = Modifier.fillMaxSize())

        // Top HUD Header
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .background(HudSurface, RoundedCornerShape(8.dp))
                .border(1.dp, HudBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "A.E.G.I.S. // VISUAL THREAT SCANNER",
                color = HudCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Analyzing Overlay
        if (uiState is ScannerUiState.Analyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB3090D16)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(HudSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, HudCyan, RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(color = HudCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ANALYSING THREAT SIGNATURE...",
                        color = HudCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Error Banner
        if (uiState is ScannerUiState.Error) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .background(HudSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HudAlertRed, RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = HudAlertRed,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.message,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = HudAlertRed)
                ) {
                    Text("RETRY SCAN", fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Tactical Capture Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            IconButton(
                onClick = {
                    val executor = ContextCompat.getMainExecutor(context)
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                val rotation = image.imageInfo.rotationDegrees
                                image.close()
                                onImageCaptured(bitmap, rotation)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                onCameraError(exception.message ?: "Capture failed.")
                            }
                        }
                    )
                },
                enabled = uiState !is ScannerUiState.Analyzing,
                modifier = Modifier
                    .size(76.dp)
                    .background(HudSurface, CircleShape)
                    .border(2.dp, HudCyan, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Scan Threat",
                    tint = HudCyan,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun TacticalReticleOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val boxSize = size.minDimension * 0.65f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f
        val right = left + boxSize
        val bottom = top + boxSize
        val cornerLen = 40.dp.toPx()
        val stroke = 3.dp.toPx()

        // Top-Left
        drawLine(HudCyan, Offset(left, top), Offset(left + cornerLen, top), stroke)
        drawLine(HudCyan, Offset(left, top), Offset(left, top + cornerLen), stroke)
        // Top-Right
        drawLine(HudCyan, Offset(right, top), Offset(right - cornerLen, top), stroke)
        drawLine(HudCyan, Offset(right, top), Offset(right, top + cornerLen), stroke)
        // Bottom-Left
        drawLine(HudCyan, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke)
        drawLine(HudCyan, Offset(left, bottom), Offset(left, bottom - cornerLen), stroke)
        // Bottom-Right
        drawLine(HudCyan, Offset(right, bottom), Offset(right - cornerLen, bottom), stroke)
        drawLine(HudCyan, Offset(right, bottom), Offset(right, bottom - cornerLen), stroke)
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HudDarkBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CAMERA ACCESS REQUIRED FOR TACTICAL SCANNER",
            color = HudCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = HudCyan,
                contentColor = HudDarkBg
            )
        ) {
            Text(
                text = "GRANT CAMERA PERMISSION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}