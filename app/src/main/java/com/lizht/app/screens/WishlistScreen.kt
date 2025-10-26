package com.lizht.app.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.lizht.app.viewmodel.WishlistViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WishlistScreen(
    wishlistVm: WishlistViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val state by wishlistVm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    }

    // Keep or remove; safe if your VM uses lastMutationId guard
    LaunchedEffect(Unit) { wishlistVm.refresh() }

    // Debug logs (optional)
    LaunchedEffect(Unit) { println("WishlistVM @WishlistScreen = ${wishlistVm.hashCode()}") }
    LaunchedEffect(state.items) { println("Wishlist ids (WishlistScreen) = ${state.items.map { it.productId }}") }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("Wishlist", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items in wishlist", style = MaterialTheme.typography.bodyLarge)
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(state.items, key = { it.productId }) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clickable {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(product.affiliateLink))
                                        )
                                    }
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(product.image)
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = product.title,
                                        modifier = Modifier
                                            .height(140.dp)
                                            .fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(product.title, maxLines = 2, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            currency.format(product.price),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = {
                                            val removed = product
                                            wishlistVm.remove(product.productId)
                                            scope.launch {
                                                val res = snackbar.showSnackbar(
                                                    message = "Removed from wishlist",
                                                    actionLabel = "UNDO",
                                                    withDismissAction = true
                                                )
                                                if (res == SnackbarResult.ActionPerformed) {
                                                    wishlistVm.add(removed)
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = "Unwishlist",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
