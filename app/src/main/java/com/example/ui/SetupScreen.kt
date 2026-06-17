package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(prefs: PreferencesManager, onSaved: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configuración SDMX") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ingresa tus credenciales de SDMX VIP", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Usuario SDMX") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña SDMX") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (user.isNotBlank() && pass.isNotBlank()) {
                        prefs.userSdmx = user
                        prefs.passSdmx = pass
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar y continuar")
            }
        }
    }
}
