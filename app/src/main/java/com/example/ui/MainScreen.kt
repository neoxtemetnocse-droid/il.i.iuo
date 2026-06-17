package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    prefs: PreferencesManager,
    onRunNow: () -> Unit,
    onAddUser: (String, String) -> Unit,
    onIntervalChanged: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var newUser by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf(prefs.lastRunLogs) }
    var nextRunText by remember { mutableStateOf("Calculando...") }
    
    // Auto-refresh logs basically roughly every second
    LaunchedEffect(Unit) {
        while(true) {
            logs = prefs.lastRunLogs
            delay(2000)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showDialog = false },
            title = { Text("Agregar usuario de IPTV") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUser,
                        onValueChange = { newUser = it },
                        label = { Text("Usuario IPTV") },
                        enabled = !isAdding,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Contraseña IPTV") },
                        enabled = !isAdding,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isAdding) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUser.isNotBlank() && newPass.isNotBlank()) {
                            isAdding = true
                            // Run in background and close dialog
                            scope.launch {
                                onAddUser(newUser, newPass)
                                isAdding = false
                                showDialog = false
                                newUser = ""
                                newPass = ""
                            }
                        }
                    },
                    enabled = !isAdding
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    enabled = !isAdding
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    var selectedInterval by remember { mutableStateOf(prefs.workIntervalHours) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SDMX Auto-Renew") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Usuario: ${prefs.userSdmx}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Intervalo repetición automática: $selectedInterval hs")
            Slider(
                value = selectedInterval.toFloat(),
                onValueChange = { selectedInterval = it.toInt() },
                valueRange = 1f..24f,
                steps = 23,
                onValueChangeFinished = {
                    prefs.workIntervalHours = selectedInterval
                    onIntervalChanged(selectedInterval)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { onRunNow() }) {
                    Text("Ejecutar ahora")
                }
                Button(onClick = { showDialog = true }) {
                    Text("+ Agregar usuario")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Próxima ejecución en: ${selectedInterval}H (aprox, ver notificaciones)", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Log de estado:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = logs,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
