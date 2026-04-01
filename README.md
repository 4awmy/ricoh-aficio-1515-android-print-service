# Ricoh Aficio 1515 Android Print Service

A custom Android Print Service plugin specifically designed for the legacy **Ricoh Aficio 1515** printer. This plugin allows direct, wireless printing from any Android application using the standard system print menu.

## 🚀 What we built
Since the Ricoh Aficio 1515 (2005 model) lacks modern AirPrint/Mopria support, we developed this custom driver to bridge the gap:
- **Native Print Service**: Registers as a system-wide printer plugin on Android.
- **PCL 5e Protocol**: Implemented the specific legacy Page Description Language (PCL) required by this hardware.
- **Arabic Language Support**: Uses high-density rasterization (converting documents to images) to ensure Arabic fonts and RTL layouts print perfectly without breaking.
- **Streaming Architecture**: Optimized to process and send pages one-by-one, preventing memory crashes (OutOfMemoryErrors) on mobile devices.
- **Direct Socket Printing**: Communicates directly with the printer via TCP Port 9100.

## 📦 How to Install

### 1. Download the APK
1.  Go to the **Actions** tab in this GitHub repository.
2.  Select the most recent successful workflow run (marked with a green checkmark).
3.  Scroll down to the **Artifacts** section and download the `app-debug.apk`.

### 2. Install on Android
1.  Transfer the `.apk` file to your Android phone.
2.  Open the file to install it. (You may need to enable "Install from Unknown Sources" in your phone settings).

### 3. Configuration
1.  Open the **Ricoh Print Service** app on your phone.
2.  Enter the printer's IP address (Default: `192.168.0.50`).
3.  Tap **Save**.

### 4. Activate the Service
1.  Go to your phone's **Settings**.
2.  Search for **"Printing"** or go to **Connected Devices > Connection Preferences > Printing**.
3.  Find **Ricoh Print Service** in the list and toggle it to **ON**.

## 🖨️ How to Print
1.  Open any document, PDF, or photo on your phone (e.g., in Chrome, Gmail, or WhatsApp).
2.  Tap **Share** or **Print**.
3.  Select **Ricoh Aficio 1515** from the printer list.
4.  Tap the print icon!

