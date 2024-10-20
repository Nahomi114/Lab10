package com.example.lab10.view

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lab10.data.SerieApiService
import com.example.lab10.data.SerieModel
import kotlinx.coroutines.launch

@Composable
fun ContenidoSeriesListado(navController: NavHostController, servicio: SerieApiService) {
    var listaSeries = remember { mutableStateListOf<SerieModel>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        refreshSeriesList(servicio, listaSeries)
    }

    LazyColumn {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ID", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.1f))
                Text("SERIE", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                Text("Accion", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f))
            }
        }

        items(listaSeries) { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${item.id}", modifier = Modifier.weight(0.1f))
                Text(item.name, modifier = Modifier.weight(0.6f))
                IconButton(
                    onClick = { navController.navigate("serieVer/${item.id}") },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(Icons.Outlined.Edit, "Editar")
                }
                IconButton(
                    onClick = { navController.navigate("serieDel/${item.id}") },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(Icons.Outlined.Delete, "Eliminar")
                }
            }
        }
    }
}

@Composable
fun ContenidoSerieEditar(navController: NavHostController, servicio: SerieApiService, pid: Int = 0) {
    var id by remember { mutableStateOf(pid) }
    var name by remember { mutableStateOf("") }
    var release_date by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pid) {
        if (pid != 0) {
            try {
                val response = servicio.selectSerie(pid.toString())
                if (response.isSuccessful) {
                    response.body()?.let { serie ->
                        name = serie.name
                        release_date = serie.release_date
                        rating = serie.rating.toString()
                        category = serie.category
                    }
                } else {
                    errorMessage = "Error al cargar la serie: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        TextField(
            value = release_date,
            onValueChange = { release_date = it },
            label = { Text("Fecha de lanzamiento") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        TextField(
            value = rating,
            onValueChange = { rating = it },
            label = { Text("Calificación") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        TextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Categoría") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val serie = SerieModel(id, name, release_date, rating.toIntOrNull() ?: 0, category)
                        val response = if (id == 0) {
                            servicio.insertSerie(serie)
                        } else {
                            servicio.updateSerie(id.toString(), serie)
                        }

                        if (response.isSuccessful) {
                            Log.d("API", "Serie ${if (id == 0) "added" else "updated"} successfully")
                            navController.navigate("series")
                        } else {
                            errorMessage = "Error: ${response.errorBody()?.string()}"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Procesando..." else "Guardar")
        }
    }
}

@Composable
fun ContenidoSerieEliminar(navController: NavHostController, servicio: SerieApiService, id: Int) {
    var showDialog by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                navController.navigate("series")
            },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Está seguro de eliminar esta Serie?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val response = servicio.deleteSerie(id.toString())
                                if (response.isSuccessful) {
                                    Log.d("API", "Serie deleted successfully")
                                    navController.navigate("series")
                                } else {
                                    errorMessage = "Error al eliminar: ${response.errorBody()?.string()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                                showDialog = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Procesando..." else "Confirmar")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                    navController.navigate("series")
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

suspend fun refreshSeriesList(servicio: SerieApiService, listaSeries: SnapshotStateList<SerieModel>) {
    try {
        val response = servicio.selectSeries()
        listaSeries.clear()
        listaSeries.addAll(response)
    } catch (e: Exception) {
        Log.e("API", "Error refreshing series list: ${e.message}")
    }
}