/**
 * Kings Play - Listen Together: QR code of the room link, and a camera scanner to join.
 */

package com.metrolist.music.ui.component

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Takes the room code out of a scanned text (an invite link, or just the code). */
internal fun kingsParseRoomCode(text: String): String? {
    val t = text.trim()
    Regex("[?&]code=([A-Za-z0-9]{4,8})").find(t)?.let { return it.groupValues[1].uppercase() }
    if (Regex("^[A-Za-z0-9]{4,8}$").matches(t)) return t.uppercase()
    return null
}

/** QR code of the room link, drawn in the room card. */
@Composable
fun KingsRoomQr(link: String) {
    val matrix = remember(link) {
        runCatching {
            QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1))
        }.getOrNull()
    }
    if (matrix == null) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp),
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val n = matrix.width
                val cell = size.width / n
                for (y in 0 until n) {
                    for (x in 0 until n) {
                        if (matrix.get(x, y)) {
                            drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.6f, cell + 0.6f))
                        }
                    }
                }
            }
        }
        Text(
            text = "Scan to join",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** "Scan QR" button for the Join section: opens the camera, reads the QR, hands back the room code. */
@Composable
fun KingsQrScanButton(onCode: (String) -> Unit) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            show = true
        } else {
            Toast.makeText(context, "Camera permission is needed to scan the QR code", Toast.LENGTH_SHORT).show()
        }
    }

    OutlinedButton(
        onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                show = true
            } else {
                permission.launch(Manifest.permission.CAMERA)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Scan the room QR code")
    }

    if (show) {
        Dialog(
            onDismissRequest = { show = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                KingsQrCamera(
                    onCode = { code ->
                        show = false
                        onCode(code)
                    },
                )
                Text(
                    text = "Point the camera at the QR code of the room",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp),
                )
                TextButton(
                    onClick = { show = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is LifecycleOwner) return c
        c = c.baseContext
    }
    return null
}

private val kgQrReader = MultiFormatReader().apply {
    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
}

private fun decodeQr(image: ImageProxy): String? {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    val source = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
    return try {
        kgQrReader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    } catch (e: Exception) {
        null
    } finally {
        kgQrReader.reset()
    }
}

@Composable
private fun KingsQrCamera(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val owner = remember { context.findLifecycleOwner() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val finished = remember { AtomicBoolean(false) }
    val providerHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    DisposableEffect(Unit) {
        onDispose {
            providerHolder[0]?.unbindAll()
            executor.shutdown()
        }
    }

    if (owner == null) return

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                providerHolder[0] = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    try {
                        val text = decodeQr(image)
                        val code = text?.let { kingsParseRoomCode(it) }
                        if (code != null && finished.compareAndSet(false, true)) {
                            previewView.post { onCode(code) }
                        }
                    } finally {
                        image.close()
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}