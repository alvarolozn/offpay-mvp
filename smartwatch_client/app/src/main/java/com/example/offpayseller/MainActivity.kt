package com.example.offpayseller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// INFORMACIÓN HARDCODEADA PARA LA DEMO: AQUÍ SE DEJAN LOS VALORES PREDETERMINADOS DEL USUARIO Y DEL BACKEND.
private const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000"
private const val DEFAULT_CLIENT_ID = "89ffc7fe-3ee7-4e74-82db-0aa441fff6e0"
private const val DEFAULT_SELLER_ID = "3491f260-3b41-4b3d-b002-98aa78e3566a"
private const val TOKEN_VALUE_COP = 10000

class MainActivity : ComponentActivity() {

    private lateinit var tokenPrefs: android.content.SharedPreferences
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var rootContainer: FrameLayout

    private var currentScreen: String = "PAY"
    private var loading: Boolean = false
    private var selectedAmount: Int = TOKEN_VALUE_COP
    private var localTokenCount: Int = 0
    private var qrBitmap: Bitmap? = null
    private var paymentCodesForCurrentQr: List<String> = emptyList()

    private val availableBalance: Int
        get() = localTokenCount * TOKEN_VALUE_COP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenPrefs = getSharedPreferences("offpay_tokens", Context.MODE_PRIVATE)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(rootContainer)

        refreshLocalTokenCount()
        renderCurrentScreen()
        syncTokensFromBackend(showSuccessToast = false)
    }

    private fun renderCurrentScreen() {
        rootContainer.removeAllViews()
        when (currentScreen) {
            "PAY" -> rootContainer.addView(buildPayScreen())
            "QR" -> rootContainer.addView(buildQrScreen())
        }
    }

    private fun buildPayScreen(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(6), dp(20), dp(6))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        val balanceCard = createBalanceCard(
            label = "SALDO DISPONIBLE",
            value = formatCop(availableBalance)
        )
        val balanceParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        balanceParams.bottomMargin = dp(6)
        root.addView(balanceCard, balanceParams)

        val selectedCard = createBalanceCard(
            label = "SALDO SELECCIONADO",
            value = formatCop(selectedAmount)
        )
        val selectedParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        selectedParams.bottomMargin = dp(6)
        root.addView(selectedCard, selectedParams)

        val rowButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.bottomMargin = dp(6)

        val plusButton = createActionButton("+10000", bgColor = Color.parseColor("#1E1F23"), textColor = Color.WHITE).apply {
            isEnabled = !loading
            setOnClickListener {
                if (availableBalance <= 0) {
                    showToast("No hay saldo disponible")
                    return@setOnClickListener
                }

                if (selectedAmount + TOKEN_VALUE_COP <= availableBalance) {
                    selectedAmount += TOKEN_VALUE_COP
                    renderCurrentScreen()
                } else {
                    showToast("No puedes superar el saldo disponible")
                }
            }
        }

        if (selectedAmount > TOKEN_VALUE_COP) {
            val minusButton = createActionButton("-10000", bgColor = Color.parseColor("#1E1F23"), textColor = Color.WHITE).apply {
                isEnabled = !loading
                setOnClickListener {
                    selectedAmount -= TOKEN_VALUE_COP
                    renderCurrentScreen()
                }
            }

            val minusParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            minusParams.marginEnd = dp(5)

            val plusParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            plusParams.marginStart = dp(5)

            rowButtons.addView(minusButton, minusParams)
            rowButtons.addView(plusButton, plusParams)
        } else {
            val plusParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38)
            )

            rowButtons.addView(plusButton, plusParams)
        }

        root.addView(rowButtons, rowParams)

        val payButton = createActionButton("PAGAR", bgColor = Color.parseColor("#21C16B"), textColor = Color.BLACK).apply {
            setTypeface(typeface, Typeface.BOLD)
            isEnabled = !loading
            setOnClickListener {
                preparePaymentQr()
            }
        }

        val payParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(40)
        )

        root.addView(payButton, payParams)

        return root
    }

    private fun buildQrScreen(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        if (qrBitmap != null) {
            val qrHolder = FrameLayout(this).apply {
                background = roundedDrawable(Color.WHITE, 14f)
                setPadding(dp(7), dp(7), dp(7), dp(7))
            }

            val qrImage = ImageView(this).apply {
                setImageBitmap(qrBitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            val qrSize = dp(145)

            val qrImageParams = FrameLayout.LayoutParams(qrSize, qrSize)
            qrHolder.addView(qrImage, qrImageParams)

            val qrHolderParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            qrHolderParams.bottomMargin = dp(8)

            root.addView(qrHolder, qrHolderParams)
        }

        val paidButton = createActionButton("PAGADO", bgColor = Color.parseColor("#21C16B"), textColor = Color.BLACK).apply {
            setTypeface(typeface, Typeface.BOLD)
            isEnabled = !loading
            setOnClickListener {
                confirmPaid()
            }
        }

        val paidParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(38)
        )

        root.addView(paidButton, paidParams)

        return root
    }

    private fun createBalanceCard(label: String, value: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedDrawable(Color.parseColor("#121418"), 16f)
            setPadding(dp(9), dp(6), dp(9), dp(6))
        }

        val labelView = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#BDBDBD"))
            textSize = 8f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        }

        val valueView = TextView(this).apply {
            text = value
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        }

        card.addView(labelView)
        card.addView(valueView)

        return card
    }

    private fun createActionButton(text: String, bgColor: Int, textColor: Int): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(textColor)
            textSize = 11f
            isAllCaps = false
            background = roundedDrawable(bgColor, 18f)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            includeFontPadding = false
        }
    }

    private fun roundedDrawable(color: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(color)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun formatCop(value: Int): String {
        val formatted = value.toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()

        return "${'$'}$formatted COP"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getLocalTokensRoot(): JSONObject {
        val savedJson = tokenPrefs.getString("local_tokens_json", null)

        return if (savedJson.isNullOrBlank()) {
            JSONObject().apply {
                put("client_id", DEFAULT_CLIENT_ID)
                put("tokens", JSONArray())
            }
        } else {
            try {
                JSONObject(savedJson)
            } catch (_: Exception) {
                JSONObject().apply {
                    put("client_id", DEFAULT_CLIENT_ID)
                    put("tokens", JSONArray())
                }
            }
        }
    }

    private fun saveLocalTokensRoot(root: JSONObject) {
        tokenPrefs.edit()
            .putString("local_tokens_json", root.toString())
            .apply()
    }

    private fun refreshLocalTokenCount() {
        val root = getLocalTokensRoot()
        val tokens = root.optJSONArray("tokens") ?: JSONArray()
        var count = 0

        for (i in 0 until tokens.length()) {
            val token = tokens.getJSONObject(i)
            val backendStatus = token.optString("status", "")
            val localStatus = token.optString("local_status", "AVAILABLE")

            if (backendStatus == "AVAILABLE" && localStatus == "AVAILABLE") {
                count++
            }
        }

        localTokenCount = count
    }

    private fun syncTokensFromBackend(showSuccessToast: Boolean = false) {
        loading = true
        renderCurrentScreen()

        thread {
            try {
                val url = URL("$DEFAULT_BACKEND_URL/tokens/client/$DEFAULT_CLIENT_ID")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (responseCode in 200..299) {
                    val backendRoot = JSONObject(responseText)
                    val backendTokens = backendRoot.optJSONArray("tokens") ?: JSONArray()

                    val localRoot = getLocalTokensRoot()
                    val localTokens = localRoot.optJSONArray("tokens") ?: JSONArray()

                    val localStatusById = mutableMapOf<String, String>()
                    for (i in 0 until localTokens.length()) {
                        val token = localTokens.getJSONObject(i)
                        val tokenId = token.optString("id")
                        val localStatus = token.optString("local_status", "AVAILABLE")
                        localStatusById[tokenId] = localStatus
                    }

                    val mergedTokens = JSONArray()
                    for (i in 0 until backendTokens.length()) {
                        val token = backendTokens.getJSONObject(i)
                        val tokenId = token.optString("id")
                        val backendStatus = token.optString("status", "")

                        if (backendStatus == "AVAILABLE") {
                            val preservedLocalStatus = localStatusById[tokenId] ?: "AVAILABLE"
                            token.put("local_status", preservedLocalStatus)
                            mergedTokens.put(token)
                        }
                    }

                    val newRoot = JSONObject().apply {
                        put("client_id", DEFAULT_CLIENT_ID)
                        put("tokens", mergedTokens)
                    }

                    saveLocalTokensRoot(newRoot)

                    mainHandler.post {
                        loading = false
                        refreshLocalTokenCount()
                        renderCurrentScreen()
                        if (showSuccessToast) {
                            showToast("Saldo actualizado desde la base de datos")
                        }
                    }
                } else {
                    mainHandler.post {
                        loading = false
                        renderCurrentScreen()
                        showToast("Error sincronizando tokens: HTTP $responseCode")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    renderCurrentScreen()
                    showToast("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun generateQrBitmap(content: String): Bitmap {
        val bitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            650,
            650
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                )
            }
        }

        return bitmap
    }

    private fun selectPaymentCodes(amount: Int): List<String> {
        val root = getLocalTokensRoot()
        val tokens = root.optJSONArray("tokens") ?: JSONArray()
        val neededCount = amount / TOKEN_VALUE_COP
        val selectedCodes = mutableListOf<String>()

        for (i in 0 until tokens.length()) {
            val token = tokens.getJSONObject(i)
            val backendStatus = token.optString("status", "")
            val localStatus = token.optString("local_status", "AVAILABLE")

            if (backendStatus == "AVAILABLE" && localStatus == "AVAILABLE") {
                selectedCodes.add(token.optString("payment_code"))
                if (selectedCodes.size == neededCount) break
            }
        }

        if (selectedCodes.size < neededCount) {
            throw Exception("No hay suficientes tokens disponibles")
        }

        return selectedCodes
    }

    private fun buildPaymentPayload(amount: Int, paymentCodes: List<String>): String {
        val codesArray = JSONArray()
        paymentCodes.forEach { code -> codesArray.put(code) }

        return JSONObject().apply {
            put("type", "OFFPAY_PACKAGE")
            put("client_id", DEFAULT_CLIENT_ID)
            put("amount_cop", amount)
            put("token_count", paymentCodes.size)
            put("payment_codes", codesArray)
        }.toString(2)
    }

    private fun markSelectedTokensAsUsedLocally(usedCodes: List<String>) {
        val usedCodesSet = usedCodes.toSet()
        val root = getLocalTokensRoot()
        val tokens = root.optJSONArray("tokens") ?: JSONArray()

        for (i in 0 until tokens.length()) {
            val token = tokens.getJSONObject(i)
            val paymentCode = token.optString("payment_code")

            if (usedCodesSet.contains(paymentCode)) {
                token.put("local_status", "USED_LOCAL")
                token.put("status", "USED")
            }
        }

        root.put("tokens", tokens)
        saveLocalTokensRoot(root)
        refreshLocalTokenCount()
    }

    private fun preparePaymentQr() {
        if (selectedAmount < TOKEN_VALUE_COP) {
            selectedAmount = TOKEN_VALUE_COP
            renderCurrentScreen()
            showToast("El mínimo es ${formatCop(TOKEN_VALUE_COP)}")
            return
        }

        if (selectedAmount % TOKEN_VALUE_COP != 0) {
            showToast("El monto debe ser múltiplo de ${formatCop(TOKEN_VALUE_COP)}")
            return
        }

        if (selectedAmount > availableBalance) {
            showToast("Saldo insuficiente")
            return
        }

        try {
            val selectedCodes = selectPaymentCodes(selectedAmount)
            val payload = buildPaymentPayload(selectedAmount, selectedCodes)

            paymentCodesForCurrentQr = selectedCodes
            qrBitmap = generateQrBitmap(payload)
            currentScreen = "QR"
            renderCurrentScreen()
        } catch (e: Exception) {
            showToast(e.message ?: "Error generando QR")
        }
    }

    private fun confirmPaid() {
        if (paymentCodesForCurrentQr.isEmpty()) {
            showToast("No hay tokens seleccionados")
            return
        }

        loading = true
        renderCurrentScreen()

        thread {
            try {
                val url = URL("$DEFAULT_BACKEND_URL/payments/validate-package")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")

                val codesArray = JSONArray()
                paymentCodesForCurrentQr.forEach { code -> codesArray.put(code) }

                val requestBody = JSONObject().apply {
                    put("seller_id", DEFAULT_SELLER_ID)
                    put("payment_codes", codesArray)
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
                    mainHandler.post {
                        markSelectedTokensAsUsedLocally(paymentCodesForCurrentQr)
                        paymentCodesForCurrentQr = emptyList()
                        qrBitmap = null
                        selectedAmount = TOKEN_VALUE_COP
                        currentScreen = "PAY"
                        loading = false
                        renderCurrentScreen()
                        showToast("Pago confirmado.")
                        syncTokensFromBackend(showSuccessToast = false)
                    }
                } else {
                    mainHandler.post {
                        loading = false
                        renderCurrentScreen()
                        showToast("Error confirmando pago: HTTP $responseCode $responseText")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    loading = false
                    renderCurrentScreen()
                    showToast("Error de conexión: ${e.message}")
                }
            }
        }
    }
}
