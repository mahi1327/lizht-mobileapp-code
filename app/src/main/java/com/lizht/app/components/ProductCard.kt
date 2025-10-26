package com.lizht.app.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import coil.compose.rememberAsyncImagePainter
import com.lizht.app.model.Product
import com.lizht.app.viewmodel.WishlistViewModel
import com.lizht.app.data.remote.WishlistItemDto

@Composable
fun ProductCard(
    product: Product,
    wishlistVm: WishlistViewModel,          // <- use the shared VM (injected from parent/screen)
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by wishlistVm.state.collectAsState()
    val isWishlisted = state.items.any { it.productId == product._id }

    Card(
        modifier = modifier
            .width(160.dp)
            .height(250.dp)
            .padding(4.dp)
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
                painter = rememberAsyncImagePainter(product.image),
                contentDescription = product.title,
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.title,
                maxLines = 2,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${product.price}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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
                        wishlistVm.toggle(dto)   // optimistic add/remove + backend call
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
