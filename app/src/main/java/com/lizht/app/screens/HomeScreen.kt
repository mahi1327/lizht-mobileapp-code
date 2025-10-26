package com.lizht.app.screens

import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lizht.app.components.ProductCard as WishlistProductCard
import com.lizht.app.model.Product
import com.lizht.app.network.RetrofitInstance
import com.lizht.app.repository.ProductRepository
import com.lizht.app.viewmodel.ProductViewModel
import com.lizht.app.viewmodel.ProductViewModelFactory
import com.lizht.app.viewmodel.WishlistViewModel

// 🔹 Analytics
import com.lizht.app.analytics.TrackScreenTime
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

@Composable
fun HomeScreen(
    navController: NavController,
    wishlistVm: WishlistViewModel
) {
    // ⏱ Track screen-time for this screen
    TrackScreenTime("home")

    // --- Debug: confirm single VM instance across screens ---
    LaunchedEffect(Unit) {
        println("WishlistVM @Home = ${wishlistVm.hashCode()}")
    }

    // Optional: see what the VM thinks is in the wishlist (ids)
    val wishState by wishlistVm.state.collectAsState()
    LaunchedEffect(wishState.items) {
        println("Wishlist ids in VM (Home) = ${wishState.items.map { it.productId }}")
    }

    // Products VM (reads from your public products API)
    val repository = remember { ProductRepository(RetrofitInstance.api) }
    val viewModel: ProductViewModel = viewModel(factory = ProductViewModelFactory(repository))

    val products by viewModel.products.collectAsState()
    val grouped: Map<String, List<Product>> = remember(products) { products.groupBy { it.category } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
    ) {
        if (grouped.isEmpty()) {
            item { Text("", modifier = Modifier.padding(16.dp)) }
        }

        grouped.forEach { (category, items) ->
            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = category, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        // 🔹 Analytics: user tapped "View All" for a category
                        val b = Bundle().apply { putString("category", category) }
                        Firebase.analytics.logEvent("view_all_category", b)

                        navController.navigate("category/${Uri.encode(category)}")
                    }) {
                        Text("View All")
                    }
                }
            }

            // Horizontal product list
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items.take(10), key = { it._id }) { product ->
                        // Shared ProductCard (heart state derived from wishlistVm.state)
                        WishlistProductCard(
                            product = product,
                            wishlistVm = wishlistVm
                        )
                    }
                }
            }

            // Spacer between sections
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
