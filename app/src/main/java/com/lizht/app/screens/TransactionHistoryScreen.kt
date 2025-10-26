package com.lizht.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lizht.app.data.remote.TransactionDto
import com.lizht.app.repository.AuthRepository
import com.lizht.app.viewmodel.TransactionsUiState
import com.lizht.app.viewmodel.TransactionsViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    storeId: String? = null,
    // If you already build the VM elsewhere (Hilt/Activity), pass it in and remove the default:
    vm: TransactionsViewModel = viewModel(factory = transactionsVmFactory())
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(storeId) {
        vm.load(storeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (storeId == null) "My Transactions" else "My Store Transactions",
                            color = Color.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (storeId == null) "Across all stores" else "Filtered by store",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.load(storeId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { inner ->
        when {
            ui.loading -> Box(
                Modifier.padding(inner).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                Modifier.padding(inner).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.load(storeId) }) { Text("Retry") }
                }
            }

            ui.items.isEmpty() -> Box(
                Modifier.padding(inner).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("No transactions yet") }

            else -> LazyColumn(
                modifier = Modifier.padding(inner).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ui.items) { txn ->
                    TxnCard(txn)
                }
            }
        }
    }
}

/* ---------- Card ---------- */

@Composable
private fun TxnCard(txn: TransactionDto) {
    val status = txn.paymentStatus.lowercase(Locale.getDefault())
    val (labelColor, bgColor) = when (status) {
        "success" -> Color(0xFF16A34A) to Color(0x3316A34A)
        "pending" -> Color(0xFFF59E0B) to Color(0x33F59E0B)
        else      -> Color(0xFFDC2626) to Color(0x33DC2626)
    }

    val whenText = remember(txn.createdAt) { parseIsoToDisplay(txn.createdAt) }
    val paid = (txn.discountedAmount ?: txn.billAmount ?: 0.0)
    val method = txn.method?.uppercase(Locale.getDefault()) ?: "-"
    val provider = txn.provider ?: "-"

    ElevatedCard {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    txn.storeName ?: "Store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    status.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    color = labelColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                whenText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Bill Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${(txn.billAmount ?: 0.0).format2()}", fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Discount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(txn.discountPercent ?: 0.0).format0()}%")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Paid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${paid.format2()}")
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            KeyVal("Method", method)
            KeyVal("Provider", provider)
            if (!txn.providerPaymentId.isNullOrBlank()) {
                KeyVal("Txn ID", txn.providerPaymentId!!)
            }
        }
    }
}

@Composable
private fun KeyVal(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ---------- Utils ---------- */

private fun Double.format2(): String = String.format(Locale.US, "%.2f", this)
private fun Double.format0(): String = String.format(Locale.US, "%.0f", this)

private fun parseIsoToDisplay(iso: String): String {
    val candidates = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    val outFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))
    for (pat in candidates) {
        try {
            val inFmt = SimpleDateFormat(pat, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val d = inFmt.parse(iso)
            if (d != null) return outFmt.format(d)
        } catch (_: ParseException) { }
    }
    return iso
}

/* ---------- Tiny VM factory (replace with DI/Hilt in your app) ---------- */

private fun transactionsVmFactory(): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val authRepo = getAuthRepository() // <-- implement this to return your shared AuthRepository
            return TransactionsViewModel(authRepo) as T
        }
    }

/** Replace with your actual way to obtain the shared AuthRepository */
private fun getAuthRepository(): AuthRepository {
    // Example if you keep it in your Application class:
    // val app = LocalContext.current.applicationContext as MyApp
    // return app.authRepository
    throw IllegalStateException("Wire AuthRepository here or pass vm from caller")
}
