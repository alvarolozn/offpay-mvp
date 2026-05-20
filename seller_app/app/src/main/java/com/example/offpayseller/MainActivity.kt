package com.example.offpayseller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offpayseller.ui.theme.OffPaySellerTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OffPaySellerTheme {
                OffPayApp()
            }
        }
    }
}

@Composable
fun OffPayApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("offpay_config", Context.MODE_PRIVATE)
    }

    var currentScreen by remember { mutableStateOf("home") }

    var backendUrl by remember {
        mutableStateOf(
            prefs.getString("backend_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        )
    }

    var clientId by remember {
        mutableStateOf(
            prefs.getString("client_id", "89ffc7fe-3ee7-4e74-82db-0aa441fff6e0")
                ?: "89ffc7fe-3ee7-4e74-82db-0aa441fff6e0"
        )
    }

    var sellerId by remember {
        mutableStateOf(
            prefs.getString("seller_id", "3491f260-3b41-4b3d-b002-98aa78e3566a")
                ?: "3491f260-3b41-4b3d-b002-98aa78e3566a"
        )
    }

    when (currentScreen) {
        "home" -> HomeScreen(
            onClienteClick = { currentScreen = "cliente" },
            onVendedorClick = { currentScreen = "vendedor" },
            onConfigClick = { currentScreen = "config" }
        )

        "cliente" -> ClienteScreen(
            backendUrl = backendUrl,
            clientId = clientId,
            onBack = { currentScreen = "home" }
        )

        "vendedor" -> VendedorScreen(
            backendUrl = backendUrl,
            sellerId = sellerId,
            onBack = { currentScreen = "home" }
        )

        "config" -> ConfigScreen(
            initialBackendUrl = backendUrl,
            initialClientId = clientId,
            initialSellerId = sellerId,
            onSave = { newBackendUrl, newClientId, newSellerId ->
                backendUrl = newBackendUrl
                clientId = newClientId
                sellerId = newSellerId

                prefs.edit()
                    .putString("backend_url", newBackendUrl)
                    .putString("client_id", newClientId)
                    .putString("seller_id", newSellerId)
                    .apply()

                currentScreen = "home"
            },
            onBack = { currentScreen = "home" }
        )
    }
}

@Composable
fun HomeScreen(
    onClienteClick: () -> Unit,
    onVendedorClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OffPay",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Selecciona cómo quieres entrar",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onClienteClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar como Cliente")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onVendedorClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar como Vendedor")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConfigClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Configuración")
        }
    }
}

@Composable
fun ClienteScreen(
    backendUrl: String,
    clientId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenPrefs = remember {
        context.getSharedPreferences("offpay_tokens", Context.MODE_PRIVATE)
    }

    var loading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Sin consultar todavía") }

    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var availableBalance by remember { mutableStateOf("") }
    var blockedBalance by remember { mutableStateOf("") }

    var rechargeAmount by remember { mutableStateOf("10000") }
    var offlineAmount by remember { mutableStateOf("10000") }
    var payAmount by remember { mutableStateOf("10000") }

    var localTokenCountTotal by remember { mutableStateOf(0) }
    var localTokenCountAvailable by remember { mutableStateOf(0) }

    var generatedPayload by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun getLocalTokensRoot(): JSONObject {
        val savedJson = tokenPrefs.getString("local_tokens_json", null)

        return if (savedJson.isNullOrBlank()) {
            JSONObject().apply {
                put("client_id", clientId.trim())
                put("tokens", JSONArray())
            }
        } else {
            try {
                JSONObject(savedJson)
            } catch (_: Exception) {
                JSONObject().apply {
                    put("client_id", clientId.trim())
                    put("tokens", JSONArray())
                }
            }
        }
    }

    fun saveLocalTokensRoot(root: JSONObject) {
        tokenPrefs.edit()
            .putString("local_tokens_json", root.toString())
            .apply()
    }

    fun refreshLocalTokenStats() {
        val root = getLocalTokensRoot()
        val tokens = root.optJSONArray("tokens") ?: JSONArray()

        var total = 0
        var available = 0

        for (i in 0 until tokens.length()) {
            val token = tokens.getJSONObject(i)
            val backendStatus = token.optString("status", "")
            val localStatus = token.optString("local_status", "AVAILABLE")

            if (backendStatus == "AVAILABLE") {
                total++
                if (localStatus == "AVAILABLE") {
                    available++
                }
            }
        }

        localTokenCountTotal = total
        localTokenCountAvailable = available
    }

    fun replaceLocalTokensWithBackendAvailable(backendTokens: JSONArray) {
        val oldRoot = getLocalTokensRoot()
        val oldTokens = oldRoot.optJSONArray("tokens") ?: JSONArray()

        val oldLocalStatusById = mutableMapOf<String, String>()
        for (i in 0 until oldTokens.length()) {
            val oldToken = oldTokens.getJSONObject(i)
            oldLocalStatusById[oldToken.optString("id")] =
                oldToken.optString("local_status", "AVAILABLE")
        }

        val cleanedTokens = mutableListOf<JSONObject>()

        for (i in 0 until backendTokens.length()) {
            val token = backendTokens.getJSONObject(i)
            val backendStatus = token.optString("status", "")
            val tokenId = token.optString("id")

            if (backendStatus == "AVAILABLE") {
                val preservedLocalStatus = oldLocalStatusById[tokenId] ?: "AVAILABLE"
                token.put("local_status", preservedLocalStatus)
                cleanedTokens.add(token)
            }
        }

        cleanedTokens.sortBy { it.optLong("counter", 0) }

        val finalArray = JSONArray()
        cleanedTokens.forEach { finalArray.put(it) }

        val newRoot = JSONObject().apply {
            put("client_id", clientId.trim())
            put("tokens", finalArray)
        }

        saveLocalTokensRoot(newRoot)
    }

    fun fetchAvailableBackendTokens(cleanBackendUrl: String, cleanClientId: String): JSONArray {
        val url = URL("$cleanBackendUrl/tokens/client/$cleanClientId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 60000
        connection.readTimeout = 60000
        connection.setRequestProperty("Accept", "application/json")

        val responseCode = connection.responseCode
        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }

        if (responseCode !in 200..299) {
            throw Exception("Error HTTP al consultar tokens: $responseCode")
        }

        val backendRoot = JSONObject(responseText)
        return backendRoot.optJSONArray("tokens") ?: JSONArray()
    }

    fun generateQrBitmap(content: String): Bitmap {
        val bitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            900,
            900
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
                )
            }
        }

        return bitmap
    }

    fun buildPaymentPayload(amount: Int): String {
        val root = getLocalTokensRoot()
        val tokens = root.optJSONArray("tokens") ?: JSONArray()

        val neededCount = amount / 10000
        val candidates = mutableListOf<Triple<Int, Long, String>>()

        for (i in 0 until tokens.length()) {
            val token = tokens.getJSONObject(i)
            val backendStatus = token.optString("status", "")
            val localStatus = token.optString("local_status", "AVAILABLE")
            val counter = token.optLong("counter", 0)
            val paymentCode = token.optString("payment_code", "")

            if (backendStatus == "AVAILABLE" && localStatus == "AVAILABLE") {
                candidates.add(Triple(i, counter, paymentCode))
            }
        }

        candidates.sortBy { it.second }

        if (candidates.size < neededCount) {
            throw Exception("No hay suficientes tokens locales disponibles para este pago")
        }

        val selected = candidates.take(neededCount)
        val selectedCodes = selected.map { it.third }

        for (item in selected) {
            val tokenIndex = item.first
            val token = tokens.getJSONObject(tokenIndex)
            token.put("local_status", "EXPOSED_LOCAL")
        }

        root.put("tokens", tokens)
        saveLocalTokensRoot(root)
        refreshLocalTokenStats()

        val payload = JSONObject().apply {
            put("type", "OFFPAY_PACKAGE")
            put("client_id", clientId.trim())
            put("amount_cop", amount)
            put("token_count", neededCount)

            val codesArray = JSONArray()
            selectedCodes.forEach { code ->
                codesArray.put(code)
            }
            put("payment_codes", codesArray)
        }

        return payload.toString(2)
    }

    fun syncLocalTokensFromBackend() {
        loading = true
        statusMessage = "Sincronizando tokens locales..."

        thread {
            try {
                val cleanBackendUrl = backendUrl.trim()
                val cleanClientId = clientId.trim()

                val backendTokens = fetchAvailableBackendTokens(cleanBackendUrl, cleanClientId)
                replaceLocalTokensWithBackendAvailable(backendTokens)

                mainHandler.post {
                    loading = false
                    refreshLocalTokenStats()
                    generatedPayload = ""
                    qrBitmap = null
                    statusMessage = "Tokens locales sincronizados correctamente"
                }
            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    statusMessage = "Error de conexión: ${e.message}"
                }
            }
        }
    }

    fun loadWallet() {
        loading = true
        statusMessage = "Consultando wallet..."

        thread {
            try {
                val cleanBackendUrl = backendUrl.trim()
                val cleanClientId = clientId.trim()

                val url = URL("$cleanBackendUrl/wallets/$cleanClientId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 60000
                connection.readTimeout = 60000
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (responseCode !in 200..299) {
                    mainHandler.post {
                        loading = false
                        statusMessage = "Error HTTP: $responseCode"
                    }
                    return@thread
                }

                val obj = JSONObject(responseText)

                val newFullName = obj.optString("full_name", "")
                val newRole = obj.optString("role", "")
                val newAvailable = obj.optLong("available_balance_cop", 0).toString()
                val newBlocked = obj.optLong("blocked_balance_cop", 0).toString()

                val backendTokens = fetchAvailableBackendTokens(cleanBackendUrl, cleanClientId)
                replaceLocalTokensWithBackendAvailable(backendTokens)

                mainHandler.post {
                    loading = false
                    fullName = newFullName
                    role = newRole
                    availableBalance = newAvailable
                    blockedBalance = newBlocked
                    statusMessage = "Wallet cargada correctamente"
                    refreshLocalTokenStats()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    statusMessage = "Error de conexión: ${e.message}"
                }
            }
        }
    }

    fun rechargeWallet() {
        val amount = rechargeAmount.trim().toIntOrNull()

        if (amount == null || amount <= 0) {
            statusMessage = "Monto inválido para recarga"
            return
        }

        loading = true
        statusMessage = "Recargando saldo..."

        thread {
            try {
                val cleanBackendUrl = backendUrl.trim()
                val cleanClientId = clientId.trim()

                val url = URL("$cleanBackendUrl/wallets/recharge")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 60000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject().apply {
                    put("user_id", cleanClientId)
                    put("amount_cop", amount)
                }.toString()

                BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8")).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (responseCode in 200..299) {
                    val obj = JSONObject(responseText)
                    val wallet = obj.getJSONObject("wallet")

                    val newAvailable = wallet.optLong("available_balance_cop", 0).toString()
                    val newBlocked = wallet.optLong("blocked_balance_cop", 0).toString()

                    mainHandler.post {
                        loading = false
                        availableBalance = newAvailable
                        blockedBalance = newBlocked
                        statusMessage = "Saldo recargado correctamente"
                    }
                } else {
                    mainHandler.post {
                        loading = false
                        statusMessage = "Error HTTP: $responseCode"
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    statusMessage = "Error de conexión: ${e.message}"
                }
            }
        }
    }

    fun prepareOfflineBalance() {
        val amount = offlineAmount.trim().toIntOrNull()

        if (amount == null || amount <= 0) {
            statusMessage = "Monto inválido para preparar offline"
            return
        }

        loading = true
        statusMessage = "Preparando saldo offline..."

        thread {
            try {
                val cleanBackendUrl = backendUrl.trim()
                val cleanClientId = clientId.trim()

                val generateUrl = URL("$cleanBackendUrl/tokens/generate")
                val generateConnection = generateUrl.openConnection() as HttpURLConnection
                generateConnection.requestMethod = "POST"
                generateConnection.connectTimeout = 60000
                generateConnection.readTimeout = 60000
                generateConnection.doOutput = true
                generateConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                generateConnection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject().apply {
                    put("client_id", cleanClientId)
                    put("amount_cop", amount)
                }.toString()

                BufferedWriter(OutputStreamWriter(generateConnection.outputStream, "UTF-8")).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val generateCode = generateConnection.responseCode
                val generateText = if (generateCode in 200..299) {
                    generateConnection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    generateConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (generateCode !in 200..299) {
                    mainHandler.post {
                        loading = false
                        statusMessage = "Error HTTP: $generateCode"
                    }
                    return@thread
                }

                val generateObj = JSONObject(generateText)
                val wallet = generateObj.getJSONObject("wallet")

                val backendTokens = fetchAvailableBackendTokens(cleanBackendUrl, cleanClientId)
                replaceLocalTokensWithBackendAvailable(backendTokens)

                val newAvailable = wallet.optLong("available_balance_cop", 0).toString()
                val newBlocked = wallet.optLong("blocked_balance_cop", 0).toString()

                mainHandler.post {
                    loading = false
                    availableBalance = newAvailable
                    blockedBalance = newBlocked
                    generatedPayload = ""
                    qrBitmap = null
                    refreshLocalTokenStats()
                    statusMessage = "Saldo offline preparado y tokens resincronizados correctamente"
                }
            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    statusMessage = "Error de conexión: ${e.message}"
                }
            }
        }
    }

    fun prepareLocalPayment() {
        val amount = payAmount.trim().toIntOrNull()

        if (amount == null || amount <= 0) {
            statusMessage = "Monto inválido para pagar"
            return
        }

        if (amount % 10000 != 0) {
            statusMessage = "El monto debe ser múltiplo de 10000"
            return
        }

        try {
            val payload = buildPaymentPayload(amount)
            generatedPayload = payload
            qrBitmap = generateQrBitmap(payload)
            statusMessage = "Payload y QR generados correctamente"
        } catch (e: Exception) {
            qrBitmap = null
            statusMessage = e.message ?: "Error generando payload"
        }
    }

    val blockedExpectedTokens = blockedBalance.toLongOrNull()?.div(10000) ?: 0

    LaunchedEffect(Unit) {
        refreshLocalTokenStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Modo Cliente",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Aquí vamos a consultar wallet, recargar, preparar offline, sincronizar tokens y generar el QR del pago.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Backend", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(backendUrl)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Client ID", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(clientId)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { loadWallet() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Consultando..." else "Consultar saldo")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { syncLocalTokensFromBackend() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Procesando..." else "Sincronizar tokens locales")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = rechargeAmount,
            onValueChange = { rechargeAmount = it },
            label = { Text("Monto a recargar") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { rechargeWallet() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Procesando..." else "Recargar saldo")
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = offlineAmount,
            onValueChange = { offlineAmount = it },
            label = { Text("Monto a apartar offline") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { prepareOfflineBalance() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Procesando..." else "Preparar saldo offline")
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = payAmount,
            onValueChange = { payAmount = it },
            label = { Text("Monto a pagar") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { prepareLocalPayment() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generar pago local")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (
                    statusMessage.startsWith("Wallet cargada") ||
                    statusMessage.startsWith("Saldo recargado") ||
                    statusMessage.startsWith("Saldo offline preparado") ||
                    statusMessage.startsWith("Payload y QR generados") ||
                    statusMessage.startsWith("Tokens locales sincronizados")
                ) {
                    Color(0xFFD8F5D0)
                } else {
                    Color(0xFFF5F5F5)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estado", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusMessage)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Datos del cliente", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Nombre: ${if (fullName.isBlank()) "-" else fullName}")
                Text("Rol: ${if (role.isBlank()) "-" else role}")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Saldo disponible: ${if (availableBalance.isBlank()) "-" else "$availableBalance COP"}")
                Text("Saldo bloqueado: ${if (blockedBalance.isBlank()) "-" else "$blockedBalance COP"}")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Tokens locales guardados (debe coincidir con saldo bloqueado/10000): $localTokenCountTotal")
                Text("Tokens disponibles para pagar ahora: $localTokenCountAvailable")
                Text("Tokens esperados por saldo bloqueado: $blockedExpectedTokens")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (qrBitmap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("QR del pago", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR del pago OffPay",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Payload del pago", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (generatedPayload.isBlank()) {
                        "Todavía no se ha generado ningún payload."
                    } else {
                        generatedPayload
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}

@Composable
fun VendedorScreen(
    backendUrl: String,
    sellerId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var qrJsonText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("Todavía no se ha validado ningún pago.") }
    var rawResponse by remember { mutableStateOf("") }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun scanQr() {
        if (activity == null) {
            resultMessage = "No se pudo acceder a la actividad para abrir el escáner."
            return
        }

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(activity, options)

        resultMessage = "Abriendo escáner..."

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue

                if (rawValue.isNullOrBlank()) {
                    resultMessage = "El QR se leyó, pero no traía contenido útil."
                } else {
                    qrJsonText = rawValue
                    resultMessage = "QR leído correctamente. Ahora toca validar pago."
                }
            }
            .addOnCanceledListener {
                resultMessage = "Escaneo cancelado."
            }
            .addOnFailureListener { e ->
                resultMessage = "Error al escanear: ${e.message}"
            }
    }

    fun validatePaymentPackage() {
        val trimmedJson = qrJsonText.trim()

        if (trimmedJson.isBlank()) {
            resultMessage = "Debes pegar o escanear el JSON del QR."
            return
        }

        loading = true
        resultMessage = "Validando pago..."
        rawResponse = ""

        thread {
            try {
                val qrObj = JSONObject(trimmedJson)
                val paymentCodes = qrObj.optJSONArray("payment_codes")

                if (paymentCodes == null || paymentCodes.length() == 0) {
                    mainHandler.post {
                        loading = false
                        resultMessage = "El JSON no contiene payment_codes válidos."
                    }
                    return@thread
                }

                val requestBody = JSONObject().apply {
                    put("seller_id", sellerId.trim())
                    put("payment_codes", paymentCodes)
                }.toString()

                val url = URL("${backendUrl.trim()}/payments/validate-package")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 60000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")

                BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8")).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                mainHandler.post {
                    loading = false
                    rawResponse = responseText

                    try {
                        val obj = JSONObject(responseText)
                        val approved = obj.optBoolean("approved", false)

                        if (approved) {
                            val amount = obj.optInt("total_amount_cop", 0)
                            val tokensUsed = obj.optInt("tokens_used", 0)
                            resultMessage = "PAGO APROBADO\nMonto: $amount COP\nTokens usados: $tokensUsed"
                        } else {
                            val reason = obj.optString("reason", "PAGO RECHAZADO")
                            val message = obj.optString("message", "")
                            resultMessage = "PAGO RECHAZADO\n$reason\n$message"
                        }
                    } catch (_: Exception) {
                        resultMessage = "Respuesta inválida del backend"
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    resultMessage =
                        "Error de red o timeout: ${e.message}\nNo reintentes el mismo pago sin antes verificar si el backend sí lo procesó."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Modo Vendedor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Aquí vamos a escanear o pegar el JSON del QR y validar el pago contra el backend.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Backend", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(backendUrl)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Seller ID", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(sellerId)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { scanQr() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Escanear QR")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = qrJsonText,
            onValueChange = { qrJsonText = it },
            label = { Text("JSON del QR") },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { validatePaymentPackage() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Validando..." else "Validar pago")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (resultMessage.startsWith("PAGO APROBADO")) {
                    Color(0xFFD8F5D0)
                } else {
                    Color(0xFFF5F5F5)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Resultado", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(resultMessage)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Respuesta cruda del backend", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (rawResponse.isBlank()) {
                        "Todavía no hay respuesta."
                    } else {
                        rawResponse
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
@Composable
fun ConfigScreen(
    initialBackendUrl: String,
    initialClientId: String,
    initialSellerId: String,
    onSave: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var backendUrl by remember { mutableStateOf(initialBackendUrl) }
    var clientId by remember { mutableStateOf(initialClientId) }
    var sellerId by remember { mutableStateOf(initialSellerId) }

    var connectionStatus by remember { mutableStateOf("Sin probar conexión todavía") }
    var loading by remember { mutableStateOf(false) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun testConnection() {
        loading = true
        connectionStatus = "Probando conexión..."

        thread {
            try {
                val url = URL("${backendUrl.trim()}/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }

                if (responseCode in 200..299) {
                    val obj = JSONObject(responseText)
                    val status = obj.optString("status", "unknown")
                    val app = obj.optString("app", "OffPay")
                    val version = obj.optString("version", "")

                    mainHandler.post {
                        loading = false
                        connectionStatus = "Conexión exitosa: $app | status=$status | version=$version"
                    }
                } else {
                    mainHandler.post {
                        loading = false
                        connectionStatus = "Error HTTP: $responseCode"
                    }
                }

            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    connectionStatus = "Error de conexión: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Aquí puedes guardar la URL del backend y los IDs de prueba.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = backendUrl,
            onValueChange = { backendUrl = it },
            label = { Text("Backend URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text("Client ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = sellerId,
            onValueChange = { sellerId = it },
            label = { Text("Seller ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { testConnection() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Probando..." else "Probar conexión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (connectionStatus.startsWith("Conexión exitosa")) {
                    Color(0xFFD8F5D0)
                } else {
                    Color(0xFFF5F5F5)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Estado",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(connectionStatus)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(
                    backendUrl.trim(),
                    clientId.trim(),
                    sellerId.trim()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}

@Composable
fun SimpleScreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}