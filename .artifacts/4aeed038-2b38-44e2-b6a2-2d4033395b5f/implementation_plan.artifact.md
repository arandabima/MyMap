# Implementation Plan - Map Search Feature

Add a search bar at the top of the map to search for local locations, animate the camera to the selected location, and provide options to set it as Origin or Destination.

## User Review Required

> [!NOTE]
> **Local Search Only**: The search will use the local `LocationData.locations` list as the data source, as requested.
> [!IMPORTANT]
> **Camera Animation**: When a location is selected, the camera will zoom and animate to the target coordinates. A dialog will then appear to let you choose between setting the location as the Origin or Destination.

## Proposed Changes

### Data Layer

#### [MODIFY] [LocationData.kt](file:///C:/Users/arand/Pemrogaman/Github/MyMap/app/src/main/java/com/rs/mymap/data/LocationData.kt)
- Add missing locations: "Ubud Cottages Malang" (Wisata/Hotel) and "Taman Wisata Lembah Dieng" (Wisata).
- Ensure categories are consistent.

---

### UI Components

#### [MODIFY] [MapScreen.kt](file:///C:/Users/arand/Pemrogaman/Github/MyMap/app/src/main/java/com/rs/mymap/ui/MapScreen.kt)
- **Search State**: Add `searchQuery`, `showSuggestions`, and `searchResultLocation`.
- **Top Bar UI**:
    - Add a Search Bar at the top of the screen (above the category filters).
    - Implement a suggestion list (using `DropdownMenu` or a custom `LazyColumn` overlay) that filters `LocationData.locations`.
    - Show an "Empty State" (e.g., "Lokasi tidak ditemukan") when no matches are found for a valid query.
- **Selection Logic**:
    - On suggestion click:
        - Trigger camera animation using `cameraPositionState.animate`.
        - Show an `AlertDialog` to ask if the location should be set as "Origin" or "Destination".
    - Update `originText`/`originLatLng` or `destinationText`/`destinationLatLng` accordingly.

## Verification Plan

### Manual Verification
1.  **Search Input**: Type "mall" in the search bar and verify "Mall Dinoyo City" and "Matos" appear in the suggestions.
2.  **Empty State**: Type a non-existent location (e.g., "Jakarta") and verify the "Lokasi tidak ditemukan" message appears.
3.  **Selection & Camera**: Select "Universitas Brawijaya" from the suggestions. Verify the map camera animates and zooms into UB.
4.  **Set as Origin/Destination**: After selecting UB, verify the dialog appears. Choose "Origin" and verify the Origin field in the bottom sheet is updated with "Universitas Brawijaya".
5.  **Persistence**: Verify Dark Mode and Category Filters still work correctly with the new search bar.
