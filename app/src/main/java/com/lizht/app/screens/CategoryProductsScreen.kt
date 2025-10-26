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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.navigation.NavHostController
import com.lizht.app.analytics.TrackScreenTime
import com.lizht.app.model.Product
import com.lizht.app.network.RetrofitInstance
import com.lizht.app.repository.ProductRepository
import com.lizht.app.viewmodel.ProductViewModel
import com.lizht.app.viewmodel.ProductViewModelFactory

// wishlist imports
import com.lizht.app.viewmodel.WishlistViewModel
import com.lizht.app.data.remote.WishlistItemDto

@Composable
fun CategoryProductsScreen(
    category: String,
    navController: NavHostController,
    onBack: () -> Unit,
    wishlistVm: WishlistViewModel, // ← shared VM injected from MainActivity
) {

    TrackScreenTime("category_products")
    val context = LocalContext.current
    val repository = remember { ProductRepository(RetrofitInstance.api) }
    val viewModel: ProductViewModel = viewModel(factory = ProductViewModelFactory(repository))

    val products by viewModel.products.collectAsState()
    val filtered = remember(products, category) { products.filter { it.category == category } }

    // observe wishlist state once for deriving icons
    val wishState by wishlistVm.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Back + title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = category, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it._id }) { product ->
                // derive wishlisted status from backend state
                val isWishlisted = remember(wishState.items, product._id) {
                    wishState.items.any { it.productId == product._id }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.affiliateLink))
                            context.startActivity(intent)
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(product.title, maxLines = 2, fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(4.dp))

                        // Price + Heart
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

                            IconButton(
                                onClick = {
                                    val dto = WishlistItemDto(
                                        productId = product._id,
                                        title = product.title,
                                        image = product.image,
                                        price = product.price.toDouble(),
                                        affiliateLink = product.affiliateLink
                                    )
                                    wishlistVm.toggle(dto) // optimistic + backend
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Wishlist",
                                    tint = if (isWishlisted) Color.Red else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
