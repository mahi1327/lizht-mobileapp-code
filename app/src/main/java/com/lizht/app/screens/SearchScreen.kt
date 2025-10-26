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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.lizht.app.analytics.TrackScreenTime
import com.lizht.app.model.Product
import com.lizht.app.network.RetrofitInstance
import com.lizht.app.repository.ProductRepository
import com.lizht.app.viewmodel.ProductViewModel
import com.lizht.app.viewmodel.ProductViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ✅ wishlist imports
import com.lizht.app.viewmodel.WishlistViewModel
import com.lizht.app.data.remote.WishlistItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    wishlistVm: WishlistViewModel // ← shared VM from MainActivity
) {

    TrackScreenTime("searchA")
    val context = LocalContext.current
    val repository = remember { ProductRepository(RetrofitInstance.api) }
    val viewModel: ProductViewModel = viewModel(factory = ProductViewModelFactory(repository))

    val products by viewModel.products.collectAsState()
    val shuffledProducts = remember(products) { products.shuffled().take(6) }

    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    var searchResults by remember { mutableStateOf(listOf<Product>()) }

    // wishlist backend state
    val wishState by wishlistVm.state.collectAsState()

    LaunchedEffect(debouncedQuery, products) {
        searchResults = if (debouncedQuery.isNotBlank()) {
            products.filter {
                it.title.contains(debouncedQuery, ignoreCase = true) ||
                        it.category.contains(debouncedQuery, ignoreCase = true)
            }
        } else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Row with Back Arrow and TextField
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = query,
                onValueChange = {
                    query = it
                    debounceJob?.cancel()
                    debounceJob = coroutineScope.launch {
                        delay(300) // debounce delay
                        debouncedQuery = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = { Text("What are you looking for?", fontSize = 16.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (debouncedQuery.isNotBlank()) {
            Text(text = "Search Results", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ProductGrid(
                products = searchResults,
                isWishlisted = { id -> wishState.items.any { it.productId == id } },
                onToggle = { product ->
                    val dto = WishlistItemDto(
                        productId = product._id,
                        title = product.title,
                        image = product.image,
                        price = product.price.toDouble(),
                        affiliateLink = product.affiliateLink
                    )
                    wishlistVm.toggle(dto)
                }
            )
        } else {
            Text(text = "You may be interested in:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ProductGrid(
                products = shuffledProducts,
                isWishlisted = { id -> wishState.items.any { it.productId == id } },
                onToggle = { product ->
                    val dto = WishlistItemDto(
                        productId = product._id,
                        title = product.title,
                        image = product.image,
                        price = product.price.toDouble(),
                        affiliateLink = product.affiliateLink
                    )
                    wishlistVm.toggle(dto)
                }
            )
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<Product>,
    isWishlisted: (productId: String) -> Boolean,
    onToggle: (Product) -> Unit
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(products, key = { it._id }) { product ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.affiliateLink))
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2E8FB))
            ) {
                Column(Modifier.padding(8.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context).data(product.image).crossfade(true).build()
                        ),
                        contentDescription = product.title,
                        modifier = Modifier
                            .height(140.dp)
                            .fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(product.title, maxLines = 2, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹${product.price}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onToggle(product) }) {
                            Icon(
                                imageVector = if (isWishlisted(product._id))
                                    Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
