# Cloud Crypto - Wear OS Device Registration App

A modern Wear OS application built with Material Design 3 that allows users to register their watch device by entering a serial number and IMEI. The app collects device identifying information, obtains an FCM token, and sends everything to a registration service for push notification targeting.

## Features

- **Material Design 3 UI**: Modern, expressive interface optimized for Wear OS
- **Dual Input Registration**: Enter serial number (numeric) and IMEI (alphanumeric) via on-screen keyboards
- **Device Identification**: Collects Android ID, device model, and Wearable Node ID
- **FCM Integration**: Full Firebase Cloud Messaging support for push notifications
- **FCM Token Targeting**: Sends FCM token during registration so backend can target this specific watch
- **Network Integration**: Communicates with registration service via HTTPS
- **Complication Support**: Watch face complication displays registration status
- **Persistent State**: Remembers registration status across app restarts
- **Message Handling**: Processes incoming FCM messages and updates complications

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
│   ├── FcmTokenManager.kt         # Manages Firebase Cloud Messaging tokens
│   ├── RegistrationApi.kt         # Retrofit API interface and factory
│   └── RegistrationRepository.kt  # Data repository with SharedPreferences
├── presentation/
│   ├── MainActivity.kt            # Main UI with Material 3 keyboards
│   ├── theme/
│   │   └── Theme.kt              # Material 3 theme configuration
│   └── viewmodel/
│       └── RegistrationViewModel.kt # UI state management
├── service/
│   └── CloudCryptoMessagingService.kt # FCM message receiver
├── complication/
│   └── MainComplicationService.kt # Watch face complication (shows REG status)
└── tile/
    └── MainTileService.kt         # Wear OS tile service
```

## Device Identification & Targeting

The app collects the following information for registration:

### User-Entered Information
1. **Serial Number**: User enters via numeric keyboard
2. **IMEI**: User enters via alphanumeric keyboard

### Automatically Collected
3. **FCM Token**: Firebase Cloud Messaging registration token (enables push notifications)
4. **Android ID**: Unique identifier per device and app installation
5. **Wearable Node ID**: Unique identifier from Google Play Services
6. **Device Model**: Manufacturer and model information
7. **Device Brand**: Device brand name
8. **OS Version**: Android SDK version

The **FCM Token** is the critical piece that allows the backend to send targeted push notifications to this specific watch. The backend should store this token alongside the serial number and IMEI for future message targeting.

## Registration Flow

1. **User Opens App**: Displays registration input screen with two keyboards
2. **Enter Serial Number**: User inputs serial number using numeric keyboard (0-9)
3. **Enter IMEI**: User inputs IMEI using alphanumeric keyboard (0-9, A-J)
4. **Click SAVE**: Triggers registration process
   - Collects FCM token from Firebase
   - Collects device information
   - Sends everything to backend
5. **Loading State**: Shows progress indicator while registering
6. **Success**: Displays "Registered" screen with serial number and IMEI
7. **Error Handling**: Shows error message with retry option if registration fails

## API Integration

The app sends a GET request to:
```
https://fusio.callista.io/public/bgc/static-response
```

With the following query parameters:
- `serialNumber`: User-entered serial number (numeric)
- `imei`: User-entered IMEI (alphanumeric)
- `id`: Android ID (auto-collected)
- `fcmToken`: Firebase Cloud Messaging token (CRITICAL for push notifications)
- `nodeId`: Wearable Node ID (auto-collected)
- `deviceModel`: Device model information (auto-collected)
- `deviceBrand`: Device brand (auto-collected)
- `osVersion`: Android OS version (auto-collected)

Example request:
```
GET /public/bgc/static-response?serialNumber=123456&imei=ABC123DEF456&id=abc123&fcmToken=fPzX8h9Kg...(long-token)&nodeId=node456&deviceModel=Samsung-Galaxy_Watch&deviceBrand=samsung&osVersion=34
```

**IMPORTANT**: The backend MUST store the `fcmToken` to enable push notifications to this specific watch. This token uniquely identifies this device installation for Firebase Cloud Messaging.

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

### Firebase
- **Firebase BOM 33.7.0**: Firebase platform
- **Firebase Cloud Messaging**: Push notifications
- **Firebase Analytics**: Analytics support

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
- **Serial Number Section**:
  - Label and display field
  - Numeric keyboard (0-9, backspace, clear)
  - 3x3 grid layout with bottom row for controls
- **IMEI Section**:
  - Label and display field
  - Alphanumeric keyboard (0-9, A-J, backspace, clear)
  - 5-column compact layout for small watch screens
- SAVE button (enabled only when both fields are filled)

### Loading Screen
- Circular progress indicator
- "Registering..." text

### Registered Screen
- Checkmark icon
- "Registered" title
- Displays both serial number and IMEI
- "Re-register" button to reset

### Error Screen
- Warning icon
- "Registration Failed" title
- Error message
- "Retry" button

## Data Persistence

Registration status is stored in SharedPreferences:
- `is_registered`: Boolean flag
- `serial_number`: User-entered serial number
- `imei`: User-entered IMEI
- `registration_timestamp`: Registration time (Unix timestamp)

FCM token is stored separately in SharedPreferences for quick access:
- `fcm_token`: Firebase Cloud Messaging registration token

## Watch Face Complication

The app provides a SHORT_TEXT complication that displays:
- **"REG"**: When device is registered
- **"---"**: When device is not registered

The complication automatically updates when FCM messages are received.

## Firebase Cloud Messaging Integration

The app includes full FCM support for receiving push notifications:

### FCM Token Management
- `FcmTokenManager`: Manages FCM token retrieval and caching
- Token is automatically obtained during registration
- Token is sent to backend for device targeting
- Token is refreshed automatically by Firebase when needed

### Message Reception
`CloudCryptoMessagingService` handles incoming FCM messages with three message types:

#### 1. Registration Update
```json
{
  "type": "registration_update",
  "serialNumber": "ABC123",
  "status": "active",
  "message": "Your device registration has been confirmed"
}
```

#### 2. Configuration Update
```json
{
  "type": "config_update",
  "data": { ...configuration settings... }
}
```

#### 3. Status Update
```json
{
  "type": "status_update",
  "status": "active|pending|expired"
}
```

### Automatic Complication Updates
When FCM messages are received, the watch face complication is automatically updated via:
```kotlin
ComplicationDataSourceUpdateRequester.create(
    context,
    ComponentName(context, MainComplicationService::class.java)
).requestUpdateAll()
```

### Setting Up Firebase

1. **Download google-services.json**:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a project or select existing project
   - Add Android app with package name: `io.callista.cloudcrypto`
   - Download `google-services.json`
   - Replace placeholder file at `wear/google-services.json`

2. **Configure FCM**:
   - The app automatically requests FCM token on first launch
   - Token is included in registration API call
   - Backend should store token for sending targeted messages

3. **Send Test Message**:
   ```bash
   curl -X POST https://fcm.googleapis.com/fcm/send \
     -H "Authorization: key=YOUR_SERVER_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "to": "DEVICE_FCM_TOKEN",
       "data": {
         "type": "status_update",
         "status": "active",
         "message": "Test notification"
       }
     }'
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
   - Both serial number and IMEI fields should be empty
   - SAVE button should be disabled

2. **Input Testing**
   - Enter serial number using numeric keyboard
   - Enter IMEI using alphanumeric keyboard
   - Test backspace and clear buttons
   - SAVE button should enable when both fields have values

3. **Registration Success**
   - Enter serial number (e.g., "123456")
   - Enter IMEI (e.g., "ABC123")
   - Click SAVE
   - Verify loading screen appears
   - Verify "Registered" screen shows both values
   - Check complication shows "REG"

4. **Network Error**
   - Disable internet
   - Attempt registration
   - Verify error screen with retry option

5. **Persistence**
   - Register device
   - Close app
   - Reopen app
   - Verify "Registered" screen shows immediately with saved values

6. **Re-registration**
   - On "Registered" screen
   - Click "Re-register"
   - Verify returns to input screen with cleared fields

7. **FCM Testing**
   - Register device
   - Send test FCM message from backend
   - Verify complication updates
   - Check app SharedPreferences for message data
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
