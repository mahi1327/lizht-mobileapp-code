package com.lizht.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.lizht.app.components.SideSwitcherPopup
import com.lizht.app.data.remote.AuthApi
import com.lizht.app.data.remote.RetrofitProvider
import com.lizht.app.data.remote.WishlistApi
import com.lizht.app.model.Store
import com.lizht.app.repository.AuthRepository
import com.lizht.app.repository.WishlistRepository
import com.lizht.app.screens.*
import com.lizht.app.ui.theme.LizhtTheme
import com.lizht.app.viewmodel.AuthViewModel
import com.lizht.app.viewmodel.AuthVmFactory
import com.lizht.app.viewmodel.TransactionsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LizhtTheme {
                val baseUrl = "https://backend-lizht.onrender.com/"
                val retrofit = remember { RetrofitProvider.build(this, baseUrl) }

                // ---- Auth DI ----
                val authApi = remember { retrofit.create(AuthApi::class.java) }
                val authRepo = remember { AuthRepository(authApi, this) }
                val authVm: AuthViewModel = viewModel(
                    factory = AuthVmFactory(application, authRepo)
                )
                val authState by authVm.state.collectAsStateWithLifecycle()

                // ---- Transactions VM (uses same AuthRepository) ----
                val txnsVm: TransactionsViewModel = viewModel(
                    key = "txns-shared",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return TransactionsViewModel(authRepo) as T
                        }
                    }
                )

                // ---- Wishlist DI (Side A only) ----
                val wishlistApi = remember { retrofit.create(WishlistApi::class.java) }
                val wishlistRepo = remember { WishlistRepository(wishlistApi) }
                val wishlistVm: com.lizht.app.viewmodel.WishlistViewModel = viewModel(
                    key = "wishlist-shared",
                    factory = com.lizht.app.viewmodel.WishlistVmFactory(wishlistRepo)
                )

                // Optional analytics tagging
                LaunchedEffect(authState.isSignedIn) {
                    if (authState.isSignedIn) {
                        Firebase.analytics.setUserProperty("preferred_side", "A")
                    } else {
                        Firebase.analytics.setUserId(null)
                    }
                }

                if (!authState.isSignedIn) {
                    AuthNav(onSignedIn = { }, authVm = authVm)
                    return@LizhtTheme
                }

                val navController = rememberNavController()

                // Auto log screen_view
                LaunchedEffect(navController) {
                    navController.addOnDestinationChangedListener { _, dest, _ ->
                        val name = dest.route ?: dest.id.toString()
                        val b = Bundle().apply {
                            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
                            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "Compose")
                        }
                        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, b)
                    }
                }

                var selectedTab by remember { mutableStateOf(0) }
                var showSwitcher by remember { mutableStateOf(false) }
                var currentSide by remember { mutableStateOf("A") } // "A" or "B"

                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()
                val showBottomBar = currentRoute != "storeDetail"

                Scaffold(
                    topBar = { if (currentRoute == "home") LizhtTopBar() },
                    bottomBar = {
                        BottomNavBar(
                            selectedIndex = selectedTab,
                            onItemSelected = { index ->
                                selectedTab = index
                                when (index) {
                                    0 -> navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                    1 -> if (currentSide == "A")
                                        navController.navigate("searchA")
                                    else
                                        navController.navigate("searchB")
                                    2 -> if (currentSide == "A")
                                        navController.navigate("wishlist")
                                    else
                                        navController.navigate("transactions")
                                    3 -> navController.navigate("profile")
                                }
                            },
                            onSwitchSide = { showSwitcher = true },
                            sideLabel = "Side $currentSide",
                            show = showBottomBar,
                            isSideB = currentSide == "B"
                        )
                    }
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "home") {

                            composable("home") {
                                if (currentSide == "A")
                                    HomeScreen(navController = navController, wishlistVm = wishlistVm)
                                else
                                    SideBHomeScreen(navController = navController)
                            }

                            composable("category/{category}") { backStackEntry ->
                                val category = backStackEntry.arguments?.getString("category").orEmpty()
                                CategoryProductsScreen(
                                    category = category,
                                    navController = navController,
                                    onBack = {
                                        selectedTab = 0
                                        navController.popBackStack()
                                    },
                                    wishlistVm = wishlistVm
                                )
                            }

                            // Search
                            composable("searchA") { SearchScreen(navController, wishlistVm) }
                            composable("searchB") { StoreSearchScreen(navController) }

                            // Store details — trigger Razorpay via Chrome Custom Tabs
                            composable("storeDetail") {
                                val context = LocalContext.current
                                val json = navController.previousBackStackEntry
                                    ?.savedStateHandle?.get<String>("store_json")
                                val store = remember(json) { json?.let { Gson().fromJson(it, Store::class.java) } }

                                // Build uid once from JWT
                                val uid = remember(authState.isSignedIn) {
                                    extractUserIdFromJwt(authRepo.accessToken())
                                }

                                if (store != null) {
                                    StoreDetailScreen(
                                        navController = navController,
                                        store = store,
                                        onPayBill = { st ->
                                            val url = "$baseUrl/api/pay/button/${st._id}?uid=$uid"
                                            CustomTabsIntent.Builder()
                                                .setShowTitle(true)
                                                .build()
                                                .launchUrl(context, Uri.parse(url))
                                        }
                                    )
                                } else {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }

                            // Side A: Wishlist
                            composable("wishlist") {
                                WishlistScreen(
                                    wishlistVm = wishlistVm,
                                    onBack = {
                                        selectedTab = 0
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // Side B: Transactions (user-scoped)
                            composable("transactions") {
                                TransactionHistoryScreen(
                                    navController = navController,
                                    storeId = null,
                                    vm = txnsVm
                                )
                            }

                            composable("profile") {
                                ProfileSignedIn(onLogout = { authVm.logout() })
                            }
                        }

                        if (showSwitcher) {
                            SideSwitcherPopup(
                                onDismiss = { showSwitcher = false },
                                onSideASelected = {
                                    currentSide = "A"
                                    showSwitcher = false
                                    selectedTab = 0
                                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                },
                                onSideBSelected = {
                                    currentSide = "B"
                                    showSwitcher = false
                                    selectedTab = 0
                                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LizhtTopBar() {
    TopAppBar(
        title = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ic_lizht_logo),
                    contentDescription = "Lizht Logo",
                    modifier = Modifier.height(48.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color.Black
        )
    )
}

@Composable
fun BottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onSwitchSide: () -> Unit,
    sideLabel: String,
    show: Boolean = true,
    isSideB: Boolean = false
) {
    if (!show) return

    Box {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            val homeIcon = Icons.Default.Home
            val searchIcon = Icons.Default.Search
            val thirdIcon = if (isSideB) Icons.Default.History else Icons.Default.Favorite
            val profileIcon = Icons.Default.Person

            val homeLabel = "Home"
            val searchLabel = "Search"
            val thirdLabel = if (isSideB) "History" else "Wishlist"
            val profileLabel = "Profile"

            NavigationBarItem(
                selected = selectedIndex == 0,
                onClick = { onItemSelected(0) },
                icon = { Icon(homeIcon, contentDescription = homeLabel) },
                label = { Text(homeLabel, fontSize = 11.sp) }
            )
            NavigationBarItem(
                selected = selectedIndex == 1,
                onClick = { onItemSelected(1) },
                icon = { Icon(searchIcon, contentDescription = searchLabel) },
                label = { Text(searchLabel, fontSize = 11.sp) }
            )

            Spacer(Modifier.width(60.dp))

            NavigationBarItem(
                selected = selectedIndex == 2,
                onClick = { onItemSelected(2) },
                icon = { Icon(thirdIcon, contentDescription = thirdLabel) },
                label = { Text(thirdLabel, fontSize = 11.sp) }
            )
            NavigationBarItem(
                selected = selectedIndex == 3,
                onClick = { onItemSelected(3) },
                icon = { Icon(profileIcon, contentDescription = profileLabel) },
                label = { Text(profileLabel, fontSize = 11.sp) }
            )
        }

        // Floating side switcher button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFD725EF), Color(0xFF303AD1))
                        )
                    )
                    .clickable { onSwitchSide() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = sideLabel, color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

/** Auth-only Nav with SignIn / SignUp */
@Composable
private fun AuthNav(onSignedIn: () -> Unit, authVm: com.lizht.app.viewmodel.AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "signin") {
        composable("signin") {
            SignInScreen(
                authVm = authVm,
                onSuccess = onSignedIn,
                onGoToSignUp = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignUpScreen(
                authVm = authVm,
                onSuccess = onSignedIn,
                onGoToSignIn = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun ProfileSignedIn(onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLogout) { Text("Log out") }
    }
}



/** Decode user id from JWT access token (supports _id / id / sub) */
private fun extractUserIdFromJwt(token: String?): String {
    if (token.isNullOrBlank()) return ""
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return ""
        val payloadJson = String(
            android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        )
        val obj = org.json.JSONObject(payloadJson)
        obj.optString("_id",
            obj.optString("id",
                obj.optString("sub", "")))
    } catch (_: Exception) {
        ""
    }
}
