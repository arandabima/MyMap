package com.rs.mymap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.google.android.gms.maps.model.LatLng
import com.rs.mymap.data.LocationData
import com.rs.mymap.data.LocationItem

@Composable
fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (LocationItem) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(value) {
        if (value.length >= 2) {
            LocationData.locations.filter { it.name.contains(value, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )

        if (expanded && suggestions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f),
                properties = PopupProperties(focusable = false)
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion.name) },
                        onClick = {
                            onLocationSelected(suggestion)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}