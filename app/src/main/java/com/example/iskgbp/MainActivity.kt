package com.example.iskgbp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.iskgbp.ui.theme.IskGbpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "CurrencyPrefs"
    private val KEY_RATE = "cached_rate"
    private val KEY_DATE = "cached_date"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load cached values on startup
        val cachedRate = sharedPreferences.getFloat(KEY_RATE, 0.0056f).toDouble()
        val cachedDate = sharedPreferences.getString(KEY_DATE, "Offline mode - cached rate") ?: "Cached"

        val uiState = mutableStateOf(CurrencyUiState(rate = cachedRate, lastUpdated = cachedDate))

        setContent {
            IskGbpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CurrencyConverterApp(
                        uiState = uiState.value,
                        onRefresh = {
                            refreshRates(uiState)
                        }
                    )
                }
            }
        }

        // Auto-refresh rate from API on app launch
        refreshRates(uiState)
    }

    private fun refreshRates(state: MutableState<CurrencyUiState>) {
        state.value = state.value.copy(isLoading = true, errorMessage = null)

        lifecycleScope.launch {
            try {
                val result = fetchExchangeRate()
                val rate = result.first
                val date = result.second

                // Cache the fresh values
                sharedPreferences.edit()
                    .putFloat(KEY_RATE, rate.toFloat())
                    .putString(KEY_DATE, date)
                    .apply()

                state.value = CurrencyUiState(
                    isLoading = false,
                    rate = rate,
                    lastUpdated = date,
                    errorMessage = null
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isLoading = false,
                    errorMessage = "Could not update rates: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    private suspend fun fetchExchangeRate(): Pair<Double, String> = withContext(Dispatchers.IO) {
        // Method 1: Try Primary API (ExchangeRate-API)
        try {
            val url = URL("https://open.er-api.com/v6/latest/ISK")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                if (json.getString("result") == "success") {
                    val rates = json.getJSONObject("rates")
                    val rate = rates.getDouble("GBP")
                    
                    // Format the date string if available
                    val rawDate = json.optString("time_last_update_utc", "")
                    val formattedDate = if (rawDate.isNotEmpty()) {
                        cleanDateString(rawDate)
                    } else {
                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        sdf.format(Date())
                    }
                    return@withContext Pair(rate, formattedDate)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Method 2: Try Fallback API (Frankfurter API)
        try {
            val url = URL("https://api.frankfurter.app/latest?from=ISK&to=GBP")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val rates = json.getJSONObject("rates")
                val rate = rates.getDouble("GBP")
                val rawDate = json.optString("date", "")
                val formattedDate = "ECB Rate ($rawDate)"
                return@withContext Pair(rate, formattedDate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        throw Exception("Failed to reach rate APIs")
    }

    private fun cleanDateString(rawDate: String): String {
        return try {
            // Raw: "Sat, 22 Aug 2026 00:00:01 +0000"
            // Clean up to show: "22 Aug 2026"
            val parts = rawDate.split(" ")
            if (parts.size >= 4) {
                "${parts[1]} ${parts[2]} ${parts[3]}"
            } else {
                rawDate
            }
        } catch (e: Exception) {
            rawDate
        }
    }
}

data class CurrencyUiState(
    val isLoading: Boolean = false,
    val rate: Double? = null,
    val lastUpdated: String? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterApp(
    uiState: CurrencyUiState,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val dfRate = remember { DecimalFormat("#,##0.000000") }
    val dfConvert = remember { DecimalFormat("#,##0.00") }

    // State for live conversion inputs
    var iskInput by remember { mutableStateOf("") }
    var gbpInput by remember { mutableStateOf("") }

    // Focus state to determine input source
    var isIskFocused by remember { mutableStateOf(false) }
    var isGbpFocused by remember { mutableStateOf(false) }

    val rate = uiState.rate ?: 0.0056 // Fallback default

    // React to error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Rate",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Current Rate Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Today's Conversion Rate",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1 ISK = ${dfRate.format(rate)} GBP",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last Updated: ${uiState.lastUpdated ?: "Fetching..."}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // 2. Interactive Calculator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.title_calculator),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Clear Button
                        if (iskInput.isNotEmpty() || gbpInput.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    iskInput = ""
                                    gbpInput = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Inputs", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ISK Input
                        OutlinedTextField(
                            value = iskInput,
                            onValueChange = { newValue ->
                                // Allow digits and single decimal point
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    iskInput = newValue
                                    if (isIskFocused && newValue.isNotEmpty()) {
                                        val iskValue = newValue.toDoubleOrNull()
                                        if (iskValue != null) {
                                            gbpInput = dfConvert.format(iskValue * rate).replace(",", "")
                                        }
                                    } else if (isIskFocused && newValue.isEmpty()) {
                                        gbpInput = ""
                                    }
                                }
                            },
                            label = { Text(stringResource(id = R.string.label_isk)) },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged {
                                    isIskFocused = it.isFocused
                                },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = "=",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // GBP Input
                        OutlinedTextField(
                            value = gbpInput,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    gbpInput = newValue
                                    if (isGbpFocused && newValue.isNotEmpty()) {
                                        val gbpValue = newValue.toDoubleOrNull()
                                        if (gbpValue != null) {
                                            // Calculate ISK = GBP / rate
                                            iskInput = dfConvert.format(gbpValue / rate).replace(",", "")
                                        }
                                    } else if (isGbpFocused && newValue.isEmpty()) {
                                        iskInput = ""
                                    }
                                }
                            },
                            label = { Text(stringResource(id = R.string.label_gbp)) },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged {
                                    isGbpFocused = it.isFocused
                                },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 3. Preset Conversions Title
            Text(
                text = stringResource(id = R.string.label_presets),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            // 4. Quick Preset Grid
            val presets = listOf(1000, 3000, 5000, 6000, 8000, 10000, 15000, 20000, 25000)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Display in 2-column chunks manually for better compatibility in scrolls
                for (i in presets.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (j in 0..1) {
                            if (i + j < presets.size) {
                                val iskAmount = presets[i + j]
                                val gbpEquivalent = iskAmount * rate

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            iskInput = iskAmount.toString()
                                            gbpInput = dfConvert.format(gbpEquivalent).replace(",", "")
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${DecimalFormat("#,###").format(iskAmount)} ISK",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "£${dfConvert.format(gbpEquivalent)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
