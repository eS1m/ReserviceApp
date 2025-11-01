package com.example.firebaseauthtesting.Pages

import android.R.attr.enabled
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.firebaseauthtesting.ViewModels.AuthState
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.firebaseauthtesting.R
import androidx.compose.runtime.livedata.observeAsState
import com.example.firebaseauthtesting.Screen

@Composable
fun Login(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {

    //Coolors "Freshy Greens"
    val gradientColors = listOf(
        Color(0xFF1b4332),
        Color(0xFF52b788)
    )
    val interphasesFamily = FontFamily(
        Font(R.font.interphases)
    )
    val pantonFamily = FontFamily(
        Font(R.font.panton)
    )

    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    val authState by authViewModel.authState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors=gradientColors)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            fontSize = 32.sp,
            color = Color.White,
            fontFamily = interphasesFamily
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text(
                    text = "Email",
                    color = Color.White)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text(
                    text = "Password",
                    color = Color.White
                )
                    },
            singleLine = true,
            visualTransformation = if(passwordVisible)
            { VisualTransformation.None }
            else { PasswordVisualTransformation() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                val description = if (passwordVisible) "Hide password"
                else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }
                ) {
                    Icon(imageVector = image, description)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.login(email.trim(),password) },
            enabled = authState != AuthState.Loading) {
            if (authState == AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), Color.White)
            } else {
                Text(text = "Login", fontFamily = pantonFamily)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        TextButton(onClick = {
            navController.navigate(Screen.Signup.route)
        }) {
            Text(
                text = "No account? Sign up!",
                fontFamily = pantonFamily,
                color = Color.White
            )
        }
    }
}