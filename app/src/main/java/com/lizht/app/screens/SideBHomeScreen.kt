package com.lizht.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.gson.Gson
import com.lizht.app.analytics.TrackScreenTime
import com.lizht.app.model.Store
import com.lizht.app.network.StoreRetrofitInstance
import com.lizht.app.repository.StoreRepository
import com.lizht.app.viewmodel.StoreViewModel
import com.lizht.app.viewmodel.StoreViewModelFactory

@Composable
fun SideBHomeScreen(navController: NavController) {
    TrackScreenTime("sideb_home")
    val viewModel: StoreViewModel = viewModel(
        factory = StoreViewModelFactory(StoreRepository(StoreRetrofitInstance.storeApi))
    )
    val stores by viewModel.stores.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "STORES",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(stores) { store ->
                StoreCard(
                    store = store,
                    onClick = {
                        // Pass store as JSON -> SavedStateHandle (no Parcelize)
                        val json = Gson().toJson(store)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("store_json", json)
                        navController.navigate("storeDetail")
                    }
                )
            }
        }
    }
}

@Composable
fun StoreCard(
    store: Store,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EAFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = store.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val open = store.active
                Text(
                    text = if (open) "🕒 Open Now" else "🕒 Closed",
                    color = if (open) Color(0xFF00C853) else Color.Gray,
                    fontSize = 14.sp
                )

                // concise address: first 2 comma parts
                Text(
                    text = "📍 " + store.address.split(",").take(2).joinToString(", ").trim(),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                store.discountPercent?.let { pct ->
                    Text(
                        text = "💸 $pct% off",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2979FF),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            // Right: image flush with card edges
            val ctx = LocalContext.current
            val firstUrl = store.images.firstOrNull().orEmpty()
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(ctx)
                        .data(firstUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(130.dp) // a bit wider so it hugs the edge like your mock
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            )
        }
    }
}
