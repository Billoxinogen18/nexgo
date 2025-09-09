#!/bin/bash

# Nexgo N92 POS Terminal - Build and Deploy Script
# This script builds the application and deploys it to the N92 terminal

echo "🏪 Nexgo N92 POS Terminal - Build and Deploy Script"
echo "=================================================="

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Please install Android SDK and add to PATH"
    exit 1
fi

# Check if device is connected
echo "🔍 Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ No devices connected. Please connect your N92 terminal via USB"
    echo "   Make sure USB debugging is enabled on the terminal"
    exit 1
fi

echo "✅ Found $DEVICES device(s) connected"

# Clean and build the project
echo "🔨 Building project..."
./gradlew clean assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above"
    exit 1
fi

echo "✅ Build successful"

# Install the APK
echo "📱 Installing APK to device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "❌ Installation failed. Please check the device connection"
    exit 1
fi

echo "✅ Installation successful"

# Launch the application
echo "🚀 Launching application..."
adb shell am start -n com.nexgo.n92pos/.ui.MainActivity

if [ $? -ne 0 ]; then
    echo "❌ Failed to launch application"
    exit 1
fi

echo "✅ Application launched successfully"
echo ""
echo "🎉 Deployment complete!"
echo "   The N92 POS application should now be running on your terminal"
echo ""
echo "📋 Next steps:"
echo "   1. Test the printer functionality"
echo "   2. Test card reading (chip, swipe, NFC)"
echo "   3. Test PIN entry"
echo "   4. Process a test transaction"
echo ""
echo "🔧 For troubleshooting, check the Settings menu in the app"
