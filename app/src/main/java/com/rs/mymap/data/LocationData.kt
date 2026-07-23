package com.rs.mymap.data

import com.google.android.gms.maps.model.LatLng

data class LocationItem(
    val name: String,
    val latLng: LatLng,
    val category: String
)

object LocationData {
    val locations = listOf(
        LocationItem("Mall Dinoyo City", LatLng(-7.9424, 112.6075), "Mall"),
        LocationItem("Politeknik Negeri Malang", LatLng(-7.9467, 112.6157), "Kampus"),
        LocationItem("Universitas Brawijaya", LatLng(-7.9525, 112.6144), "Kampus"),
        LocationItem("Universitas Negeri Malang", LatLng(-7.9626, 112.6177), "Kampus"),
        LocationItem("Matos (Malang Town Square)", LatLng(-7.9568, 112.6176), "Mall"),
        LocationItem("Stasiun Malang", LatLng(-7.9774, 112.6370), "Transportasi"),
        LocationItem("Alun-Alun Malang", LatLng(-7.9826, 112.6308), "Wisata"),
        LocationItem("Ubud Cottages Malang", LatLng(-7.9547, 112.6035), "Hotel"),
        LocationItem("Taman Wisata Lembah Dieng", LatLng(-7.9676, 112.6027), "Wisata")
    )
}