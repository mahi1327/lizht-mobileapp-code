package com.lizht.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lizht.app.model.Store
import com.lizht.app.network.StoreRetrofitInstance
import com.lizht.app.repository.StoreRepository
import com.lizht.app.viewmodel.StoreViewModel
import com.lizht.app.viewmodel.StoreViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSearchScreen(
    navController: NavController,
    // If you ever want to inject a different card, keep this hook:
    storeCard: @Composable (Store, () -> Unit) -> Unit = { s, onClick ->
        StoreCard(store = s, onClick = onClick)
    }
) {
    val viewModel: StoreViewModel = viewModel(
        factory = StoreViewModelFactory(StoreRepository(StoreRetrofitInstance.storeApi))
    )
    val allStores by viewModel.stores.collectAsState()

    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val results by remember(debouncedQuery, allStores) {
        mutableStateOf(
            if (debouncedQuery.isBlank()) emptyList()
            else allStores.filter { s ->
                s.name.contains(debouncedQuery, ignoreCase = true) ||
                        s.address.contains(debouncedQuery, ignoreCase = true)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    debounceJob?.cancel()
                    debounceJob = scope.launch {
                        delay(300)
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

        Spacer(Modifier.height(16.dp))

        if (debouncedQuery.isNotBlank()) {
            Text("Search Results", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(results, key = { it._id }) { s ->
                    storeCard(s) {
                        // Pass selected store to details (uses your holder singleton)
                        StoreDetailHolder.store = s
                        navController.navigate("storeDetails")
                    }
                }
            }
        }
        // No "You may be interested" section for Side B
    }
}
