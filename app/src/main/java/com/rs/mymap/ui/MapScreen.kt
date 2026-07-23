package com.rs.mymap.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import com.rs.mymap.R
import com.rs.mymap.data.LocationData
import com.rs.mymap.data.ThemePreferences
import com.rs.mymap.data.api.RetrofitClient
import com.rs.mymap.data.local.AppDatabase
import com.rs.mymap.data.local.RouteHistory
import com.rs.mymap.ui.components.AutocompleteTextField
import com.rs.mymap.utils.Constants
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val themePreferences = remember { ThemePreferences(context) }
    var darkModeEnabled by remember { mutableStateOf(themePreferences.isDarkMode) }

    val db = remember { AppDatabase.getDatabase(context) }
    val historyDao = db.routeHistoryDao()
    val historyList by historyDao.getAllHistory().collectAsState(initial = emptyList())

    // UI State for Marker Filtering
    val categories = listOf("Mall", "Kampus", "Transportasi", "Wisata", "Hotel")
    var selectedCategories by remember { mutableStateOf(categories.toSet()) }

    // UI State for Search
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var selectedSearchLocation by remember { mutableStateOf<com.rs.mymap.data.LocationItem?>(null) }
    var showSelectionDialog by remember { mutableStateOf(false) }

    val searchSuggestions = remember(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            LocationData.locations.filter { 
                it.name.contains(searchQuery, ignoreCase = true) 
            }
        } else emptyList()
    }

    // UI State for Text Fields
    var originText by remember { mutableStateOf("-7.9731565,112.609915") }
    var destinationText by remember { mutableStateOf("-7.9826092,112.6282364") }
    
    // UI State for Markers and Route
    var originLatLng by remember { mutableStateOf<LatLng?>(LatLng(-7.9731565, 112.609915)) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(LatLng(-7.9826092, 112.6282364)) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    
    // UI State for Route Details
    var distance by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var fuelEstimation by remember { mutableStateOf("") }
    var transportMode by remember { mutableStateOf("Roda 4") }
    var selectedMode by remember { mutableStateOf("driving") } // driving or two_wheeler
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(originLatLng!!, 14f)
    }

    val apiKey = "AIzaSyAMxzfxQjAg9Jr-WE5EtBpAE7xCXwz2B1Q"

    // Helper function to fetch route
    fun fetchRoute() {
        if (originLatLng == null || destinationLatLng == null) return
        
        scope.launch {
            try {
                val oLatLng = originLatLng!!
                val dLatLng = destinationLatLng!!
                
                val response = RetrofitClient.getDirectionsApiService(context).getDirections(
                    origin = "${oLatLng.latitude},${oLatLng.longitude}",
                    destination = "${dLatLng.latitude},${dLatLng.longitude}",
                    mode = selectedMode,
                    apiKey = apiKey
                )
                if (response.routes.isNotEmpty()) {
                    val route = response.routes[0]
                    val encodedPolyline = route.overviewPolyline.points
                    routePoints = PolyUtil.decode(encodedPolyline)
                    
                    // Extract Distance and Duration
                    if (route.legs.isNotEmpty()) {
                        distance = route.legs[0].distance.text
                        duration = route.legs[0].duration.text
                        
                        // Calculate Fuel Cost
                        val distanceValue = route.legs[0].distance.value / 1000.0 // convert meters to km
                        val consumption = if (selectedMode == "driving") Constants.CAR_CONSUMPTION else Constants.MOTORCYCLE_CONSUMPTION
                        val cost = (distanceValue / consumption) * Constants.FUEL_PRICE
                        fuelEstimation = "Rp ${String.format(Locale.US, "%,.0f", cost)}"
                        
                        // Save to History
                        historyDao.insert(
                            RouteHistory(
                                origin = originText,
                                destination = destinationText,
                                mode = transportMode,
                                distance = distance,
                                duration = duration
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Error fetching directions", e)
            }
        }
    }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        originLatLng = latLng
                        originText = "${location.latitude},${location.longitude}"
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                    } else {
                        Toast.makeText(context, "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MapScreen", "Security exception", e)
            }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocation() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        originLatLng = latLng
                        originText = "${location.latitude},${location.longitude}"
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                    } else {
                        Toast.makeText(context, "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MapScreen", "Security exception", e)
            }
        } else {
            locationPermissionLauncher.launch(permissions)
        }
    }

    // Reset function
    fun resetFields() {
        originText = ""
        destinationText = ""
        routePoints = emptyList()
        distance = ""
        duration = ""
        fuelEstimation = ""
        transportMode = "Roda 4"
        selectedMode = "driving"
        
        originLatLng = null
        destinationLatLng = null
    }

    fun shareRoute() {
        if (distance.isEmpty()) return
        val shareText = """
            Rute Perjalanan MyMap:
            Dari: $originText
            Ke: $destinationText
            Jarak: $distance
            Waktu: $duration
            Moda: $transportMode
            Estimasi BBM: $fuelEstimation
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Rute"))
    }

    // Auto refresh when mode changes
    LaunchedEffect(selectedMode) {
        if (originText.isNotEmpty() && destinationText.isNotEmpty()) {
            fetchRoute()
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        fetchRoute()
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 140.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Compact Summary (Always visible in Peek Height)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (distance.isNotEmpty() || duration.isNotEmpty()) {
                            Text(
                                text = "$duration ($distance)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF00796B)
                            )
                            Text(
                                text = "Moda: $transportMode",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Cari Rute Perjalanan",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Button(
                        onClick = { 
                            fetchRoute()
                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cari Rute")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expanded Controls
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AutocompleteTextField(
                        value = originText,
                        onValueChange = { originText = it },
                        onLocationSelected = { item ->
                            originText = item.name
                            originLatLng = item.latLng
                        },
                        label = "Origin",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.Blue) },
                        trailingIcon = {
                            IconButton(onClick = { requestLocation() }) {
                                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AutocompleteTextField(
                        value = destinationText,
                        onValueChange = { destinationText = it },
                        onLocationSelected = { item ->
                            destinationText = item.name
                            destinationLatLng = item.latLng
                        },
                        label = "Destination",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.Red) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Pilih Moda Transportasi:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == "driving",
                            onClick = { 
                                selectedMode = "driving"
                                transportMode = "Roda 4"
                            },
                            label = { Text("Mobil") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = selectedMode == "two_wheeler",
                            onClick = { 
                                selectedMode = "two_wheeler"
                                transportMode = "Roda 2"
                            },
                            label = { Text("Motor") },
                            leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { 
                                resetFields()
                                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                        ) {
                            Text("Reset / Clear")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = darkModeEnabled,
                            onCheckedChange = {
                                darkModeEnabled = it
                                themePreferences.isDarkMode = it
                                val mode = if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                                AppCompatDelegate.setDefaultNightMode(mode)
                            }
                        )
                    }
                    
                    // Detailed Info Card (Visible when expanded)
                    if (distance.isNotEmpty() || duration.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color.Blue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Jarak: $distance", style = MaterialTheme.typography.bodyLarge)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Waktu Tempuh: $duration", style = MaterialTheme.typography.bodyLarge)
                                }
                                if (fuelEstimation.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Color(0xFF388E3C))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "Estimasi BBM: $fuelEstimation", style = MaterialTheme.typography.bodyLarge)
                                        }
                                        IconButton(onClick = { shareRoute() }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Route History Section
                    if (historyList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Riwayat Pencarian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            items(historyList) { item ->
                                HistoryItemView(
                                    item = item,
                                    onClick = {
                                        originText = item.origin
                                        destinationText = item.destination
                                        transportMode = item.mode
                                        selectedMode = if (item.mode == "Roda 4") "driving" else "two_wheeler"
                                        fetchRoute()
                                    },
                                    onDelete = { scope.launch { historyDao.delete(item) } },
                                    onFavoriteToggle = { scope.launch { historyDao.toggleFavorite(item.id, !item.isFavorite) } }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp)) // Extra padding for the bottom
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = if (darkModeEnabled) {
                        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                    } else null
                )
            ) {
                // Predefined Markers with Filtering
                LocationData.locations.filter { it.category in selectedCategories }.forEach { location ->
                    Marker(
                        state = rememberMarkerState(position = location.latLng),
                        title = location.name,
                        snippet = location.category,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when (location.category) {
                                "Mall" -> BitmapDescriptorFactory.HUE_RED
                                "Kampus" -> BitmapDescriptorFactory.HUE_VIOLET
                                "Transportasi" -> BitmapDescriptorFactory.HUE_BLUE
                                "Hotel" -> BitmapDescriptorFactory.HUE_AZURE
                                else -> BitmapDescriptorFactory.HUE_GREEN
                            }
                        )
                    )
                }

                originLatLng?.let {
                    Marker(
                        state = remember(it) { MarkerState(position = it) },
                        title = "Origin",
                        snippet = "Titik Keberangkatan",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
                
                destinationLatLng?.let {
                    Marker(
                        state = remember(it) { MarkerState(position = it) },
                        title = "Destination",
                        snippet = "Titik Tujuan",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                }

                if (routePoints.isNotEmpty()) {
                    Polyline(
                        points = routePoints,
                        color = Color(0xFF00796B), // Teal color
                        width = 12f
                    )
                }
            }

            // Search Bar & Filter UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Cari lokasi...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchActive) {
                            IconButton(onClick = { 
                                if (searchQuery.isNotEmpty()) searchQuery = "" else searchActive = false 
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (searchActive) 0.dp else 16.dp, vertical = if (searchActive) 0.dp else 8.dp)
                ) {
                    if (searchSuggestions.isEmpty() && searchQuery.isNotEmpty()) {
                        ListItem(
                            headlineContent = { Text("Lokasi tidak ditemukan") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyColumn {
                            items(searchSuggestions) { suggestion ->
                                ListItem(
                                    headlineContent = { Text(suggestion.name) },
                                    supportingContent = { Text(suggestion.category) },
                                    leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = suggestion.name
                                            searchActive = false
                                            selectedSearchLocation = suggestion
                                            showSelectionDialog = true
                                            
                                            // Animate Camera
                                            scope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(suggestion.latLng, 16f)
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                if (!searchActive) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = category in selectedCategories,
                                onClick = {
                                    selectedCategories = if (category in selectedCategories) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                },
                                label = { Text(category, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Selection Dialog
    if (showSelectionDialog && selectedSearchLocation != null) {
        AlertDialog(
            onDismissRequest = { showSelectionDialog = false },
            title = { Text("Gunakan Lokasi") },
            text = { Text("Atur '${selectedSearchLocation?.name}' sebagai titik apa?") },
            confirmButton = {
                TextButton(onClick = {
                    originText = selectedSearchLocation!!.name
                    originLatLng = selectedSearchLocation!!.latLng
                    showSelectionDialog = false
                }) {
                    Text("Origin")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    destinationText = selectedSearchLocation!!.name
                    destinationLatLng = selectedSearchLocation!!.latLng
                    showSelectionDialog = false
                }) {
                    Text("Destination")
                }
            }
        )
    }
}

@Composable
fun HistoryItemView(
    item: RouteHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.origin} → ${item.destination}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.mode} • ${item.distance} • ${item.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Row {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFFFB300) else Color.Gray
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen()
}
