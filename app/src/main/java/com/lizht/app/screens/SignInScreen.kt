package com.lizht.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizht.app.viewmodel.AuthViewModel
import com.lizht.app.R

@Composable
fun SignInScreen(
    authVm: AuthViewModel,
    onSuccess: () -> Unit,
    onGoToSignUp: () -> Unit = {}
) {
    val state by authVm.state.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val pink = Color(0xFFD725EF)
    val blue = Color(0xFF303AD1)
    val gradient = Brush.linearGradient(listOf(pink, blue))
    val pill = RoundedCornerShape(28.dp)

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSuccess()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Lizht Logo
        Image(
            painter = painterResource(id = R.drawable.ic_lizht_logo),
            contentDescription = "Lizht Logo",
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Welcome Back",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // Gradient Button (pill)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(pill)
                .background(gradient)
                .clickable(
                    enabled = email.isNotBlank() && password.length >= 6 && !state.loading
                ) { authVm.login(email.trim(), password) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sign In",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (state.loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Row {
            Text("Don't have an account? ")
            Text(
                "Sign up",
                color = pink,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onGoToSignUp() }
            )
        }
    }
}
