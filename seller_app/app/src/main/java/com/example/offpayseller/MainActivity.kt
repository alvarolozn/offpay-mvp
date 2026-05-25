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
import androidx.compose.ui.Alignment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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

    // Se dejan por compatibilidad con ConfigScreen.
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

    var loggedFullName by remember { mutableStateOf("") }
    var loggedRole by remember { mutableStateOf("") }
    var loggedLinkedUserId by remember { mutableStateOf("") }

    fun clearDemoSession() {
        loggedFullName = ""
        loggedRole = ""
        loggedLinkedUserId = ""
    }

    when (currentScreen) {
        "home" -> HomeScreen(
            onClienteClick = { currentScreen = "login_client" },
            onVendedorClick = { currentScreen = "login_seller" },
            onConfigClick = { currentScreen = "config" }
        )

        "login_client" -> DemoLoginScreen(
            backendUrl = backendUrl,
            role = "CLIENT",
            title = "Login Cliente",
            exampleUsername = "cliente1",
            onLoginSuccess = { fullName, linkedUserId ->
                loggedFullName = fullName
                loggedRole = "CLIENT"
                loggedLinkedUserId = linkedUserId
                currentScreen = "cliente"
            },
            onBack = { currentScreen = "home" }
        )

        "login_seller" -> DemoLoginScreen(
            backendUrl = backendUrl,
            role = "SELLER",
            title = "Login Vendedor",
            exampleUsername = "vendedor1",
            onLoginSuccess = { fullName, linkedUserId ->
                loggedFullName = fullName
                loggedRole = "SELLER"
                loggedLinkedUserId = linkedUserId
                currentScreen = "vendedor"
            },
            onBack = { currentScreen = "home" }
        )

        "cliente" -> ClienteScreen(
            backendUrl = backendUrl,
            clientId = loggedLinkedUserId,
            onBack = {
                clearDemoSession()
                currentScreen = "home"
            }
        )

        "vendedor" -> VendedorScreen(
            backendUrl = backendUrl,
            sellerId = loggedLinkedUserId,
            onBack = {
                clearDemoSession()
                currentScreen = "home"
            }
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


private val OffPayBackground = Color(0xFFF4F7FB)
private val OffPayPrimary = Color(0xFF155EEF)
private val OffPayPrimaryDark = Color(0xFF0B3B8F)
private val OffPayAccent = Color(0xFF00A676)
private val OffPayWarning = Color(0xFFFFB020)
private val OffPayDanger = Color(0xFFD92D20)
private val OffPayCardColor = Color(0xFFFFFFFF)
private val OffPayMuted = Color(0xFF667085)
private val OffPaySoftBlue = Color(0xFFEAF1FF)
private val OffPaySoftGreen = Color(0xFFEAF7E8)
private val OffPaySoftYellow = Color(0xFFFFF6DF)
private val OffPaySoftRed = Color(0xFFFFEAEA)

@Composable
fun OffPayPage(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffPayBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "OffPay",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OffPayPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF101828)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = OffPayMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        content()
    }
}

@Composable
fun OffPayCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    containerColor: Color = OffPayCardColor,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828)
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OffPayMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun OffPayPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OffPayPrimary,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFCBD5E1),
            disabledContentColor = Color.White
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OffPaySecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEAF1FF),
            contentColor = OffPayPrimaryDark,
            disabledContainerColor = Color(0xFFE4E7EC),
            disabledContentColor = Color(0xFF98A2B3)
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OffPayMuted,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF101828)
        )
    }
}

@Composable
fun MiniStat(
    title: String,
    value: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = OffPayMuted,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF101828),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusMessageCard(message: String) {
    val isSuccess = message.startsWith("Wallet") ||
            message.startsWith("Saldo recargado") ||
            message.startsWith("Saldo offline preparado") ||
            message.startsWith("Payload y QR generados") ||
            message.startsWith("Tokens locales") ||
            message.contains("devuelto")

    val bg = if (isSuccess) OffPaySoftGreen else Color(0xFFF9FAFB)
    val title = if (isSuccess) "Operación exitosa" else "Estado actual"

    OffPayCard(
        title = title,
        containerColor = bg
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF101828)
        )
    }
}


@Composable
fun HomeScreen(
    onClienteClick: () -> Unit,
    onVendedorClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffPayBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = OffPayCardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "OffPay",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = OffPayPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pagos offline seguros para clientes y comercios.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OffPayMuted
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OffPayPrimaryButton(
                        text = "Entrar como Cliente",
                        onClick = onClienteClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OffPaySecondaryButton(
                        text = "Entrar como Vendedor",
                        onClick = onVendedorClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OffPaySecondaryButton(
                        text = "Configuración",
                        onClick = onConfigClick
                    )
                }
            }
        }
    }
}
@Composable
fun DemoLoginScreen(
    backendUrl: String,
    role: String,
    title: String,
    exampleUsername: String,
    onLoginSuccess: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ingresa tu username demo.") }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun doDemoLogin() {
        val cleanUsername = username.trim()

        if (cleanUsername.isBlank()) {
            statusMessage = "Debes ingresar un username."
            return
        }

        loading = true
        statusMessage = "Validando usuario..."

        thread {
            try {
                val url = URL("${backendUrl.trim()}/auth/demo-login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject().apply {
                    put("username", cleanUsername)
                    put("role", role)
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

                if (responseCode !in 200..299) {
                    mainHandler.post {
                        loading = false
                        statusMessage = "Error HTTP: $responseCode"
                    }
                    return@thread
                }

                val obj = JSONObject(responseText)
                val success = obj.optBoolean("success", false)

                if (!success) {
                    val message = obj.optString("message", "Login inválido")
                    mainHandler.post {
                        loading = false
                        statusMessage = message
                    }
                    return@thread
                }

                val user = obj.getJSONObject("user")
                val fullName = user.optString("full_name", "")
                val linkedUserId = user.optString("linked_user_id", "")

                if (linkedUserId.isBlank()) {
                    mainHandler.post {
                        loading = false
                        statusMessage = "El usuario no tiene linked_user_id válido."
                    }
                    return@thread
                }

                mainHandler.post {
                    loading = false
                    statusMessage = "Login correcto"
                    onLoginSuccess(fullName, linkedUserId)
                }

            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    statusMessage = "Error de conexión: ${e.message}"
                }
            }
        }
    }

    OffPayPage(
        title = title,
        subtitle = "Accede con el usuario demo asignado para continuar."
    ) {
        OffPayCard(
            title = "Inicio de sesión",
            subtitle = "Rol esperado: $role"
        ) {
            InfoLine(label = "Backend", value = backendUrl)
            Spacer(modifier = Modifier.height(12.dp))
            InfoLine(label = "Usuario de prueba", value = exampleUsername)

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OffPayPrimaryButton(
                text = if (loading) "Ingresando..." else "Ingresar",
                onClick = { doDemoLogin() },
                enabled = !loading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatusMessageCard(statusMessage)

        Spacer(modifier = Modifier.height(16.dp))

        OffPaySecondaryButton(
            text = "Volver",
            onClick = onBack
        )
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

    var tokenHistory by remember { mutableStateOf(JSONArray()) }
    val selectedRefundCodes = remember { mutableStateListOf<String>() }

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

    fun getLocalStatusByTokenId(): Map<String, String> {
        val root = getLocalTokensRoot()
        val tokens = root.optJSONArray("tokens") ?: JSONArray()
        val result = mutableMapOf<String, String>()

        for (i in 0 until tokens.length()) {
            val token = tokens.getJSONObject(i)
            val tokenId = token.optString("id", "")
            if (tokenId.isNotBlank()) {
                result[tokenId] = token.optString("local_status", "AVAILABLE")
            }
        }

        return result
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
                val preservedLocalStatus = oldLocalStatusById[tokenId]
                    ?: token.optString("local_status", "AVAILABLE")

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

    fun fetchClientTokensFromBackend(cleanBackendUrl: String, cleanClientId: String): JSONArray {
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

    fun fetchWalletObject(cleanBackendUrl: String, userId: String): JSONObject {
        val url = URL("$cleanBackendUrl/wallets/$userId")
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
            throw Exception("Error HTTP al consultar wallet: $responseCode")
        }

        return JSONObject(responseText)
    }

    fun fetchTransactionsByTokenId(
        cleanBackendUrl: String,
        cleanClientId: String
    ): Map<String, JSONObject> {
        return try {
            val url = URL("$cleanBackendUrl/transactions")
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
                emptyMap()
            } else {
                val root = JSONObject(responseText)
                val transactions = root.optJSONArray("transactions") ?: JSONArray()
                val result = mutableMapOf<String, JSONObject>()

                for (i in 0 until transactions.length()) {
                    val transaction = transactions.getJSONObject(i)
                    val txClientId = transaction.optString("client_id", "")
                    val tokenId = transaction.optString("token_id", "")

                    if (txClientId == cleanClientId && tokenId.isNotBlank()) {
                        result[tokenId] = transaction
                    }
                }

                result
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun fetchSellerCommerceName(cleanBackendUrl: String, sellerId: String): String {
        if (sellerId.isBlank()) return ""

        return try {
            val sellerWallet = fetchWalletObject(cleanBackendUrl, sellerId)
            sellerWallet.optString("commerce_name", "")
                .ifBlank { sellerWallet.optString("full_name", "") }
        } catch (_: Exception) {
            ""
        }
    }

    fun buildTokenHistoryForClient(cleanBackendUrl: String, cleanClientId: String): JSONArray {
        val backendTokens = fetchClientTokensFromBackend(cleanBackendUrl, cleanClientId)
        val localStatusById = getLocalStatusByTokenId()
        val transactionsByTokenId = fetchTransactionsByTokenId(cleanBackendUrl, cleanClientId)
        val commerceCache = mutableMapOf<String, String>()

        for (i in 0 until backendTokens.length()) {
            val token = backendTokens.getJSONObject(i)
            val tokenId = token.optString("id", "")
            val status = token.optString("status", "")

            if (status == "AVAILABLE") {
                token.put("local_status", localStatusById[tokenId] ?: "AVAILABLE")
            } else {
                token.put("local_status", "-")
            }

            if (status == "USED") {
                val transaction = transactionsByTokenId[tokenId]
                if (transaction != null) {
                    val sellerId = transaction.optString("seller_id", "")
                    token.put("seller_id", sellerId)
                    token.put("used_transaction_id", transaction.optString("id", ""))

                    if (sellerId.isNotBlank()) {
                        val commerceName = commerceCache.getOrPut(sellerId) {
                            fetchSellerCommerceName(cleanBackendUrl, sellerId)
                        }

                        if (commerceName.isNotBlank()) {
                            token.put("commerce_name", commerceName)
                        }
                    }
                }
            }
        }

        return backendTokens
    }

    fun refreshTokenHistoryLocalStatusesOnly() {
        val localStatusById = getLocalStatusByTokenId()
        val updatedHistory = JSONArray(tokenHistory.toString())

        for (i in 0 until updatedHistory.length()) {
            val token = updatedHistory.getJSONObject(i)
            val tokenId = token.optString("id", "")
            val status = token.optString("status", "")

            if (status == "AVAILABLE") {
                token.put("local_status", localStatusById[tokenId] ?: "AVAILABLE")
            }
        }

        tokenHistory = updatedHistory
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
        statusMessage = "Sincronizando tokens locales e historial..."

        thread {
            try {
                val cleanBackendUrl = backendUrl.trim()
                val cleanClientId = clientId.trim()

                val backendTokens = buildTokenHistoryForClient(cleanBackendUrl, cleanClientId)
                replaceLocalTokensWithBackendAvailable(backendTokens)

                mainHandler.post {
                    loading = false
                    tokenHistory = backendTokens
                    selectedRefundCodes.clear()
                    refreshLocalTokenStats()
                    generatedPayload = ""
                    qrBitmap = null
                    statusMessage = "Tokens locales e historial sincronizados correctamente"
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
        statusMessage = "Consultando wallet e historial..."

        thread {
            try {
                val cleanBackendUrl = backendUrl.trim()
                val cleanClientId = clientId.trim()

                val obj = fetchWalletObject(cleanBackendUrl, cleanClientId)

                val newFullName = obj.optString("full_name", "")
                val newRole = obj.optString("role", "")
                val newAvailable = obj.optLong("available_balance_cop", 0).toString()
                val newBlocked = obj.optLong("blocked_balance_cop", 0).toString()

                val backendTokens = buildTokenHistoryForClient(cleanBackendUrl, cleanClientId)
                replaceLocalTokensWithBackendAvailable(backendTokens)

                mainHandler.post {
                    loading = false
                    fullName = newFullName
                    role = newRole
                    availableBalance = newAvailable
                    blockedBalance = newBlocked
                    tokenHistory = backendTokens
                    selectedRefundCodes.clear()
                    statusMessage = "Wallet e historial cargados correctamente"
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

                val backendTokens = buildTokenHistoryForClient(cleanBackendUrl, cleanClientId)
                replaceLocalTokensWithBackendAvailable(backendTokens)

                val newAvailable = wallet.optLong("available_balance_cop", 0).toString()
                val newBlocked = wallet.optLong("blocked_balance_cop", 0).toString()

                mainHandler.post {
                    loading = false
                    availableBalance = newAvailable
                    blockedBalance = newBlocked
                    tokenHistory = backendTokens
                    selectedRefundCodes.clear()
                    generatedPayload = ""
                    qrBitmap = null
                    refreshLocalTokenStats()
                    statusMessage = "Saldo offline preparado e historial actualizado correctamente"
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
            selectedRefundCodes.clear()
            refreshTokenHistoryLocalStatusesOnly()
            statusMessage = "Payload y QR generados correctamente"
        } catch (e: Exception) {
            qrBitmap = null
            statusMessage = e.message ?: "Error generando payload"
        }
    }

    fun refundOneToken(cleanBackendUrl: String, cleanClientId: String, paymentCode: String) {
        val url = URL("$cleanBackendUrl/tokens/refund")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 60000
        connection.readTimeout = 60000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.setRequestProperty("Accept", "application/json")

        val requestBody = JSONObject().apply {
            put("client_id", cleanClientId)
            put("payment_code", paymentCode)
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

        if (responseCode !in 200..299) {
            val detail = try {
                JSONObject(responseText).optString("detail", responseText)
            } catch (_: Exception) {
                responseText
            }

            throw Exception("HTTP $responseCode al devolver token: $detail")
        }
    }

    fun refundSelectedTokens() {
        if (selectedRefundCodes.isEmpty()) {
            statusMessage = "Selecciona al menos un token AVAILABLE para devolver."
            return
        }

        loading = true
        statusMessage = "Devolviendo tokens seleccionados..."

        val codesToRefund = selectedRefundCodes.toList()

        thread {
            var refundedCount = 0

            try {
                val cleanBackendUrl = backendUrl.trim()
                val cleanClientId = clientId.trim()

                for (paymentCode in codesToRefund) {
                    refundOneToken(cleanBackendUrl, cleanClientId, paymentCode)
                    refundedCount++
                }

                val wallet = fetchWalletObject(cleanBackendUrl, cleanClientId)
                val backendTokens = buildTokenHistoryForClient(cleanBackendUrl, cleanClientId)
                replaceLocalTokensWithBackendAvailable(backendTokens)

                val newAvailable = wallet.optLong("available_balance_cop", 0).toString()
                val newBlocked = wallet.optLong("blocked_balance_cop", 0).toString()

                mainHandler.post {
                    loading = false
                    availableBalance = newAvailable
                    blockedBalance = newBlocked
                    tokenHistory = backendTokens
                    selectedRefundCodes.clear()
                    generatedPayload = ""
                    qrBitmap = null
                    refreshLocalTokenStats()
                    statusMessage = "$refundedCount token(es) devuelto(s) correctamente"
                }
            } catch (e: Exception) {
                try {
                    val cleanBackendUrl = backendUrl.trim()
                    val cleanClientId = clientId.trim()
                    val wallet = fetchWalletObject(cleanBackendUrl, cleanClientId)
                    val backendTokens = buildTokenHistoryForClient(cleanBackendUrl, cleanClientId)
                    replaceLocalTokensWithBackendAvailable(backendTokens)

                    mainHandler.post {
                        loading = false
                        availableBalance = wallet.optLong("available_balance_cop", 0).toString()
                        blockedBalance = wallet.optLong("blocked_balance_cop", 0).toString()
                        tokenHistory = backendTokens
                        selectedRefundCodes.clear()
                        refreshLocalTokenStats()
                        statusMessage = "Se devolvieron $refundedCount token(es), pero ocurrió un error: ${e.message}"
                    }
                } catch (_: Exception) {
                    mainHandler.post {
                        loading = false
                        statusMessage = "Error devolviendo tokens: ${e.message}"
                    }
                }
            }
        }
    }

    fun tokenStatusCount(status: String): Int {
        var count = 0
        for (i in 0 until tokenHistory.length()) {
            val token = tokenHistory.getJSONObject(i)
            if (token.optString("status", "") == status) count++
        }
        return count
    }

    fun tokenDisplayValue(token: JSONObject): String {
        return "${token.optLong("value_cop", 0)} COP"
    }

    fun tokenDisplayDate(token: JSONObject): String {
        return token.optString("created_at", "-")
    }

    fun tokenDisplayCommerce(token: JSONObject): String {
        val status = token.optString("status", "")
        val commerceName = token.optString("commerce_name", "")
            .ifBlank { token.optString("seller_commerce_name", "") }
            .ifBlank { token.optString("commerce_used", "") }
        val sellerId = token.optString("seller_id", "")

        return when {
            commerceName.isNotBlank() -> commerceName
            sellerId.isNotBlank() -> "Seller ID: $sellerId"
            status == "USED" -> "Pendiente: el backend no retornó comercio"
            else -> "-"
        }
    }

    val blockedExpectedTokens = blockedBalance.toLongOrNull()?.div(10000) ?: 0
    val availableHistoryCount = tokenStatusCount("AVAILABLE")
    val usedHistoryCount = tokenStatusCount("USED")
    val returnedHistoryCount = tokenStatusCount("RETURNED")
    val selectedRefundTotal = selectedRefundCodes.size * 10000

    LaunchedEffect(Unit) {
        refreshLocalTokenStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffPayBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "OffPay",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OffPayPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Billetera del cliente",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF101828)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Administra saldo, tokens offline, pagos QR, historial y devoluciones.",
            style = MaterialTheme.typography.bodyMedium,
            color = OffPayMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        OffPayCard(
            title = "Cuenta conectada",
            subtitle = "Información técnica de la sesión actual"
        ) {
            InfoLine(label = "Backend", value = backendUrl)
            Spacer(modifier = Modifier.height(12.dp))
            InfoLine(label = "Client ID", value = clientId)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MiniStat(
                title = "Disponible",
                value = if (availableBalance.isBlank()) "-" else "$availableBalance COP",
                containerColor = OffPaySoftGreen,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            MiniStat(
                title = "Bloqueado",
                value = if (blockedBalance.isBlank()) "-" else "$blockedBalance COP",
                containerColor = OffPaySoftBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Acciones rápidas",
            subtitle = "Consulta y sincroniza antes de generar pagos o devoluciones."
        ) {
            OffPayPrimaryButton(
                text = if (loading) "Consultando..." else "Consultar saldo e historial",
                onClick = { loadWallet() },
                enabled = !loading
            )

            Spacer(modifier = Modifier.height(12.dp))

            OffPaySecondaryButton(
                text = if (loading) "Procesando..." else "Sincronizar tokens locales e historial",
                onClick = { syncLocalTokensFromBackend() },
                enabled = !loading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatusMessageCard(statusMessage)

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Recargar saldo",
            subtitle = "Aumenta el saldo disponible del cliente."
        ) {
            OutlinedTextField(
                value = rechargeAmount,
                onValueChange = { rechargeAmount = it },
                label = { Text("Monto a recargar") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OffPayPrimaryButton(
                text = if (loading) "Procesando..." else "Recargar saldo",
                onClick = { rechargeWallet() },
                enabled = !loading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Preparar saldo offline",
            subtitle = "Aparta saldo y genera tokens para pagar sin internet."
        ) {
            OutlinedTextField(
                value = offlineAmount,
                onValueChange = { offlineAmount = it },
                label = { Text("Monto a apartar offline") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OffPayPrimaryButton(
                text = if (loading) "Procesando..." else "Preparar saldo offline",
                onClick = { prepareOfflineBalance() },
                enabled = !loading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Pago local",
            subtitle = "Selecciona tokens disponibles y genera el QR del pago."
        ) {
            OutlinedTextField(
                value = payAmount,
                onValueChange = { payAmount = it },
                label = { Text("Monto a pagar") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OffPayPrimaryButton(
                text = "Generar pago local",
                onClick = { prepareLocalPayment() },
                enabled = !loading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Resumen del cliente",
            subtitle = "Estado actual de la billetera y tokens locales."
        ) {
            InfoLine(label = "Nombre", value = if (fullName.isBlank()) "-" else fullName)
            Spacer(modifier = Modifier.height(10.dp))
            InfoLine(label = "Rol", value = if (role.isBlank()) "-" else role)
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MiniStat(
                    title = "Locales",
                    value = localTokenCountTotal.toString(),
                    containerColor = OffPaySoftBlue,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                MiniStat(
                    title = "Para pagar",
                    value = localTokenCountAvailable.toString(),
                    containerColor = OffPaySoftGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            MiniStat(
                title = "Esperados por saldo bloqueado",
                value = blockedExpectedTokens.toString(),
                containerColor = Color(0xFFF2F4F7),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Historial y devolución",
            subtitle = "Tokens clasificados por estado según el backend."
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MiniStat(
                    title = "Disponible",
                    value = availableHistoryCount.toString(),
                    containerColor = OffPaySoftGreen,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                MiniStat(
                    title = "Usado",
                    value = usedHistoryCount.toString(),
                    containerColor = OffPaySoftBlue,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                MiniStat(
                    title = "Devuelto",
                    value = returnedHistoryCount.toString(),
                    containerColor = OffPaySoftYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Seleccionados para devolución: ${selectedRefundCodes.size} token(es) = $selectedRefundTotal COP",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OffPayPrimaryButton(
                text = if (loading) "Procesando..." else "Devolver tokens seleccionados",
                onClick = { refundSelectedTokens() },
                enabled = !loading && selectedRefundCodes.isNotEmpty()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val statusGroups = listOf(
            Triple("DISPONIBLE", "AVAILABLE", OffPaySoftGreen),
            Triple("USADO", "USED", OffPaySoftBlue),
            Triple("DEVUELTO", "RETURNED", OffPaySoftYellow)
        )

        for (group in statusGroups) {
            val groupTitle = group.first
            val groupStatus = group.second
            val groupColor = group.third

            OffPayCard(
                title = groupTitle,
                subtitle = "Listado de tokens en estado $groupStatus",
                containerColor = groupColor
            ) {
                var foundAny = false

                for (i in 0 until tokenHistory.length()) {
                    val token = tokenHistory.getJSONObject(i)
                    val status = token.optString("status", "")

                    if (status == groupStatus) {
                        foundAny = true
                        val paymentCode = token.optString("payment_code", "")
                        val tokenId = token.optString("id", "-")
                        val localStatus = token.optString("local_status", "-")
                        val isSelected = selectedRefundCodes.contains(paymentCode)
                        val canSelectForRefund = status == "AVAILABLE" &&
                                paymentCode.isNotBlank() &&
                                localStatus == "AVAILABLE"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Token #${token.optLong("counter", 0)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF101828)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                InfoLine(label = "Valor", value = tokenDisplayValue(token))
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoLine(label = "Fecha de creación", value = tokenDisplayDate(token))
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoLine(label = "Estado actual", value = status)
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoLine(label = "Estado local", value = localStatus)
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoLine(label = "Comercio donde fue usado", value = tokenDisplayCommerce(token))
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoLine(label = "ID", value = tokenId)

                                if (canSelectForRefund) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OffPaySecondaryButton(
                                        text = if (isSelected) "Quitar de devolución" else "Seleccionar para devolver",
                                        onClick = {
                                            if (isSelected) {
                                                selectedRefundCodes.remove(paymentCode)
                                            } else {
                                                selectedRefundCodes.add(paymentCode)
                                            }
                                        },
                                        enabled = !loading
                                    )
                                } else if (status == "AVAILABLE" && localStatus != "AVAILABLE") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Este token no se puede devolver ahora porque ya fue expuesto localmente en un QR.",
                                        color = OffPayMuted
                                    )
                                }
                            }
                        }
                    }
                }

                if (!foundAny) {
                    Text(
                        text = "No hay tokens en este estado.",
                        color = OffPayMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (qrBitmap != null) {
            OffPayCard(
                title = "QR del pago",
                subtitle = "Preséntalo al vendedor para validar el paquete OffPay."
            ) {
                Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "QR del pago OffPay",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        OffPayCard(
            title = "Payload del pago",
            subtitle = "Contenido técnico generado para el QR."
        ) {
            Text(
                if (generatedPayload.isBlank()) {
                    "Todavía no se ha generado ningún payload."
                } else {
                    generatedPayload
                },
                color = Color(0xFF101828)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OffPaySecondaryButton(
            text = "Volver",
            onClick = onBack
        )
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

    OffPayPage(
        title = "Modo Vendedor",
        subtitle = "Escanea o pega el paquete QR para validar pagos de forma segura."
    ) {
        OffPayCard(
            title = "Comercio conectado",
            subtitle = "Datos de operación"
        ) {
            InfoLine(label = "Backend", value = backendUrl)
            Spacer(modifier = Modifier.height(12.dp))
            InfoLine(label = "Seller ID", value = sellerId)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Validar pago",
            subtitle = "Usa el escáner o pega manualmente el JSON del QR."
        ) {
            OffPayPrimaryButton(
                text = "Escanear QR",
                onClick = { scanQr() },
                enabled = !loading
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = qrJsonText,
                onValueChange = { qrJsonText = it },
                label = { Text("JSON del QR") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OffPayPrimaryButton(
                text = if (loading) "Validando..." else "Validar pago",
                onClick = { validatePaymentPackage() },
                enabled = !loading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val resultColor = if (resultMessage.startsWith("PAGO APROBADO")) OffPaySoftGreen else Color(0xFFF9FAFB)
        OffPayCard(
            title = "Resultado",
            containerColor = resultColor
        ) {
            Text(resultMessage)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OffPayCard(
            title = "Respuesta del backend",
            subtitle = "Información técnica para depuración"
        ) {
            Text(
                if (rawResponse.isBlank()) {
                    "Todavía no hay respuesta."
                } else {
                    rawResponse
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OffPaySecondaryButton(
            text = "Volver",
            onClick = onBack
        )
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

    OffPayPage(
        title = "Configuración",
        subtitle = "Ajusta la conexión del backend y los IDs demo."
    ) {
        OffPayCard(
            title = "Conexión del sistema",
            subtitle = "Estos datos se guardan localmente en el teléfono."
        ) {
            OutlinedTextField(
                value = backendUrl,
                onValueChange = { backendUrl = it },
                label = { Text("Backend URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = clientId,
                onValueChange = { clientId = it },
                label = { Text("Client ID") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = sellerId,
                onValueChange = { sellerId = it },
                label = { Text("Seller ID") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            OffPaySecondaryButton(
                text = if (loading) "Probando..." else "Probar conexión",
                onClick = { testConnection() },
                enabled = !loading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val statusBg = if (connectionStatus.startsWith("Conexión exitosa")) OffPaySoftGreen else Color(0xFFF9FAFB)
        OffPayCard(
            title = "Estado",
            containerColor = statusBg
        ) {
            Text(connectionStatus)
        }

        Spacer(modifier = Modifier.height(20.dp))

        OffPayPrimaryButton(
            text = "Guardar configuración",
            onClick = {
                onSave(
                    backendUrl.trim(),
                    clientId.trim(),
                    sellerId.trim()
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OffPaySecondaryButton(
            text = "Volver",
            onClick = onBack
        )
    }
}

@Composable
fun SimpleScreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    OffPayPage(
        title = title,
        subtitle = description
    ) {
        OffPayCard(title = title) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF101828)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OffPaySecondaryButton(
            text = "Volver",
            onClick = onBack
        )
    }
}