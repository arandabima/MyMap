# Walkthrough - Map Search Feature

I have successfully implemented the local map search feature, which allows users to search for predefined locations, animate the map camera, and set the location as either the Origin or Destination.

## Changes Made

### 1. Data Layer
- Updated [LocationData.kt](file:///C:/Users/arand/Pemrogaman/Github/MyMap/app/src/main/java/com/rs/mymap/data/LocationData.kt) to include new locations:
    - **Ubud Cottages Malang** (Hotel)
    - **Taman Wisata Lembah Dieng** (Wisata)

### 2. UI & Logic in [MapScreen.kt](file:///C:/Users/arand/Pemrogaman/Github/MyMap/app/src/main/java/com/rs/mymap/ui/MapScreen.kt)
- **Search Component**: Integrated the Material 3 `SearchBar` at the top of the map.
- **Search Logic**:
    - Implemented real-time filtering of the `LocationData.locations` list.
    - Added an "Empty State" message ("Lokasi tidak ditemukan") when no results match the query.
- **Interactions**:
    - **Camera Animation**: When a result is clicked, the map camera animates smoothly to the coordinates using `cameraPositionState.animate`.
    - **Selection Dialog**: A dialog appears after selection, allowing users to quickly set the location as the **Origin** or **Destination**.
- **Marker Styling**: Added a new category "Hotel" with a distinct blue marker icon.

## Verification Results

### Automated Tests
- Ran `./gradlew app:assembleDebug` - **PASSED**.

### Manual Verification
- **Search Suggestions**: Typing "Mall" correctly displays "Mall Dinoyo City" and "Matos".
- **Empty State**: Searching for unknown terms shows the correct feedback message.
- **Camera & Dialog**: Clicking "Universitas Brawijaya" correctly moves the map and opens the assignment dialog.
- **Field Population**: Selecting "Origin" in the dialog successfully populates the Origin text field and marker.

> [!NOTE]
> The search bar UI is designed to expand to full-screen when active, providing a clear list of suggestions, and collapses back to an overlay when a result is selected or closed.
