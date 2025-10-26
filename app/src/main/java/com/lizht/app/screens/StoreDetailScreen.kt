package com.lizht.app.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.lizht.app.analytics.TrackScreenTime
import com.lizht.app.model.Store

object StoreDetailHolder {
    var store: Store? = null
}

// Change this to your backend URL if different
private const val BASE_URL = "https://backend-lizht.onrender.com"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoreDetailScreen(
    navController: NavController,
    store: Store,
    onPayBill: (Store) -> Unit = {} // kept for compatibility; not used for Custom Tabs
) {
    TrackScreenTime("store_detail")
    val context = LocalContext.current
    val pageCount = remember(store.images) { store.images.size.coerceAtLeast(1) }
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Enable only when Razorpay Button is configured for this store
    val canPay = remember(store) {
        store.paymentEnabled &&
                store.paymentProvider == "razorpay_button" &&
                (
                        (store.razorpayButtonId?.isNotBlank() == true) ||
                                (store.razorpayButtonHtml?.isNotBlank() == true)
                        )
    }

    fun openRazorpayButton() {
        // Removed unresolved references to extractUserIdFromJwt/authRepo
        val url = "${BASE_URL.trimEnd('/')}/api/pay/button/${store._id}"
        println("Opening Razorpay Button via Custom Tabs: $url")
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse(url))
        } catch (_: ActivityNotFoundException) {
            // Fallback to external browser if Custom Tabs not available
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Scaffold(containerColor = Color.White) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
        ) {
            // Header image pager
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val url = store.images.getOrNull(page)
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Back (pure circle)
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCCFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                // Pager dots
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 18.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) Color.White
                                    else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = store.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )

                Text(
                    text = store.address,
                    color = Color(0xFF6E6E6E),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )

                // status + timings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    val statusColor = if (store.active) Color(0xFF1DB954) else Color(0xFFE53935)
                    Text(
                        text = if (store.active) "Open" else "Closed",
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = "⏰", color = Color(0xFF6E6E6E), fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = store.timings,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }

                if (store.contact.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.contact}"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Call now")
                    }
                }

                if (store.discountPercent > 0) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .wrapContentWidth()
                            .height(40.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "${store.discountPercent}% OFF",
                                color = Color(0xFF0B57D0),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Pay bill button -> open Custom Tabs to backend pay page
            Button(
                onClick = { if (canPay) openRazorpayButton() },
                enabled = canPay,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text(
                    if (canPay) "Pay bill" else "Payments unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
