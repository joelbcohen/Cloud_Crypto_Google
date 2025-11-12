# Cloud Crypto - Wear OS Device Registration App

A modern Wear OS application built with Material Design 3 that allows users to register their watch device by entering a serial number. The app collects device identifying information and sends it to a registration service.

## Features

- **Material Design 3 UI**: Modern, expressive interface optimized for Wear OS
- **Device Registration**: Enter serial number via on-screen numeric input
- **Device Identification**: Collects Android ID, device model, and Wearable Node ID
- **Network Integration**: Communicates with registration service via HTTPS
- **Complication Support**: Watch face complication displays registration status
- **Persistent State**: Remembers registration status across app restarts
- **FCM Ready**: Architecture supports future FCM integration for push updates

## Architecture

The app follows modern Android architecture patterns:

- **MVVM Pattern**: ViewModel manages UI state and business logic
- **Repository Pattern**: Separates data layer from presentation
- **Kotlin Coroutines**: Asynchronous operations with proper lifecycle management
- **StateFlow**: Reactive UI updates using Jetpack Compose
- **Material 3**: Latest design system for Wear OS

## Project Structure

```
wear/src/main/java/io/callista/cloudcrypto/
├── data/
│   ├── DeviceInfoManager.kt       # Collects device identifying information
│   ├── RegistrationApi.kt         # Retrofit API interface and factory
│   └── RegistrationRepository.kt  # Data repository with SharedPreferences
├── presentation/
│   ├── MainActivity.kt            # Main UI with Material 3 components
│   ├── theme/
│   │   └── Theme.kt              # Material 3 theme configuration
│   └── viewmodel/
│       └── RegistrationViewModel.kt # UI state management
├── complication/
│   └── MainComplicationService.kt # Watch face complication (shows REG status)
└── tile/
    └── MainTileService.kt         # Wear OS tile service
```

## Device Identification

The app collects the following device information for registration:

1. **Android ID**: Unique identifier per device and app installation
2. **Wearable Node ID**: Unique identifier from Google Play Services
3. **Device Model**: Manufacturer and model information
4. **Device Brand**: Device brand name
5. **OS Version**: Android SDK version

These are combined to create a composite device identifier that's sent to the registration endpoint.

## Registration Flow

1. **User Opens App**: Displays registration input screen
2. **Enter Serial Number**: User inputs serial number using on-screen buttons
3. **Click SAVE**: Triggers registration process
4. **Loading State**: Shows progress indicator while registering
5. **Success**: Displays "Registered" screen with serial number
6. **Error Handling**: Shows error message with retry option if registration fails

## API Integration

The app sends a GET request to:
```
https://fusio.callista.io/public/bgc/static-response
```

With the following query parameters:
- `serialNumber`: User-entered serial number
- `imei`: Composite device identifier (Android ID + Node ID)
- `id`: Android ID
- `deviceModel`: Device model information
- `deviceBrand`: Device brand
- `osVersion`: Android OS version

Example request:
```
GET /public/bgc/static-response?serialNumber=123456&imei=abc123-node456&id=abc123&deviceModel=Samsung-Galaxy_Watch&deviceBrand=samsung&osVersion=34
```

## Dependencies

### Core Libraries
- **Wear Compose Material 3**: Latest Material Design for Wear OS
- **Jetpack Compose**: Modern UI toolkit
- **Lifecycle ViewModel**: State management
- **Coroutines**: Asynchronous operations

### Networking
- **Retrofit 2.11.0**: HTTP client
- **OkHttp 4.12.0**: HTTP/HTTPS networking
- **Gson**: JSON serialization

### Wear OS Specific
- **Play Services Wearable**: Wearable node identification
- **Watch Face Complications**: Complication data source
- **Tiles**: Wear OS tile support
- **Horologist**: Google's Wear OS utilities

## Permissions

The app requires the following permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## UI Components

### Registration Input Screen
- Title: "Device Registration"
- Serial number input with numeric keypad (0-9)
- Backspace button for corrections
- SAVE button (enabled when serial number is entered)

### Loading Screen
- Circular progress indicator
- "Registering..." text

### Registered Screen
- Checkmark icon
- "Registered" title
- Displays serial number
- "Re-register" button to reset

### Error Screen
- Warning icon
- "Registration Failed" title
- Error message
- "Retry" button

## Data Persistence

Registration status is stored in SharedPreferences:
- `is_registered`: Boolean flag
- `serial_number`: Stored serial number
- `registration_timestamp`: Registration time (Unix timestamp)

## Watch Face Complication

The app provides a SHORT_TEXT complication that displays:
- **"REG"**: When device is registered
- **"---"**: When device is not registered

This can be updated when FCM messages are received (future enhancement).

## Future FCM Integration

The architecture is designed to support FCM push notifications:

1. Add FCM dependencies to build.gradle
2. Implement FirebaseMessagingService
3. Handle incoming messages with device-specific payloads
4. Update complication via `MainComplicationService.requestUpdate()`
5. Update internal database/SharedPreferences
6. Refresh UI if app is in foreground

### Example FCM Handler Structure
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Extract payload data
        val data = remoteMessage.data

        // Update local storage
        updateRegistrationData(data)

        // Update complication
        MainComplicationService.requestUpdate(this)
    }
}
```

## Building the App

```bash
# Build debug APK
./gradlew :wear:assembleDebug

# Build release APK
./gradlew :wear:assembleRelease

# Install to connected watch
./gradlew :wear:installDebug
```

## Testing

### Prerequisites
- Wear OS emulator or physical Wear OS device
- Android Studio with Wear OS support
- Minimum SDK 30 (Android 11)

### Test Scenarios

1. **First Launch**
   - App should show registration input screen
   - Serial number field should be empty

2. **Registration Success**
   - Enter serial number
   - Click SAVE
   - Verify "Registered" screen appears
   - Check complication shows "REG"

3. **Network Error**
   - Disable internet
   - Attempt registration
   - Verify error screen with retry option

4. **Persistence**
   - Register device
   - Close app
   - Reopen app
   - Verify "Registered" screen shows immediately

5. **Re-registration**
   - On "Registered" screen
   - Click "Re-register"
   - Verify returns to input screen
   - Enter new serial number

## Security Considerations

1. **HTTPS Only**: All network communication uses HTTPS
2. **No IMEI Access**: Due to Android restrictions, uses Android ID instead
3. **Scoped Storage**: SharedPreferences are private to the app
4. **Permission Minimization**: Only requests necessary permissions

## Troubleshooting

### Common Issues

**Issue**: App crashes on launch
- **Solution**: Check Android API level (min SDK 30 required)

**Issue**: Network request fails
- **Solution**: Verify INTERNET permission is granted
- **Solution**: Check network connectivity on watch

**Issue**: Can't get Node ID
- **Solution**: Ensure Play Services Wearable is up to date
- **Solution**: App falls back to "node-unavailable" if unavailable

**Issue**: Complication not updating
- **Solution**: Remove and re-add complication to watch face
- **Solution**: Check app permissions in watch settings

## License

Copyright © 2024 Callista

## Support

For issues or questions, please contact the development team or refer to the Wear OS documentation at:
https://developer.android.com/training/wearables
