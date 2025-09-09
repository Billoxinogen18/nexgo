# Nexgo N92 POS Terminal Application

A complete, end-to-end payment processing application for the Nexgo N92 POS terminal with full card reading, EMV processing, PIN entry, and receipt printing capabilities.

## 🚀 Features

### 💰 **REAL Payment Processing**
- **PayPal LIVE Integration**: Primary payment processor with your LIVE API keys
- **Stripe Integration**: Backup payment processor
- **Binance Integration**: Real crypto conversion using your API keys
- **Real Money Movement**: Actual money deducted from customer cards
- **Real Crypto Conversion**: Live USD to ETH conversion
- **Real Transaction IDs**: Actual transaction IDs from payment processors
- **LIVE Environment**: Using PayPal's live API (not sandbox) for real transactions

### 💳 Payment Processing
- **Card Reading**: Support for chip (ICC), magnetic stripe (swipe), and contactless/NFC cards
- **EMV Processing**: Complete EMV Level 1 and Level 2 implementation
- **PIN Entry**: Secure PIN pad integration with DUKPT encryption
- **Manual Card Entry**: Enter card details manually when physical card reading is not available
- **Transaction Types**: Sale, refund, void, and pre-authorization

### 🖨️ Receipt Printing
- Thermal receipt printing with customizable templates
- QR code and barcode support
- Professional receipt formatting
- Test printing functionality

### ⚙️ Settings & Configuration
- **Terminal Configuration**: Set terminal ID, merchant ID, currency, and timeout settings
- **Device Testing**: Test all hardware components (card reader, PIN pad, printer, EMV)
- **Connection Testing**: Verify network connectivity
- **Device Information**: View device details and capabilities

### 📊 Transaction Management
- **Transaction History**: View all processed transactions
- **Transaction Details**: Detailed view of individual transactions
- **Reports**: Generate daily, weekly, monthly, and summary reports
- **Status Tracking**: Real-time transaction status updates

### 🔒 Security Features
- EMV compliance with proper AID and CAPK configuration
- PIN block encryption using DUKPT
- Secure transaction processing
- Proper key management

## 🔑 **Pre-configured API Keys**

Your POS system comes pre-configured with your real API keys:

- **PayPal Client ID**: `AUHMyl0I90mgdQTjrUWFL8JswSCll_MpMuIFV299HogEiuU9C6za_powpTXhP29tUWtzRxl2b-fsdIX5`
- **PayPal Client Secret**: `EHdgWhzDvcjejewg0_7QjX3Zcpw3aaPUXTVNbA2R7CYw7peX5Mb8hatGVOnjk08gAP2krySgi5RkZu91`
- **Binance API Key**: `ghhAUdvCyMrYImYzFnaeom1cVXvHopy5gKWmQ9O7hPZK13ImJa66BJZ8L7Gps6C8`
- **Binance Secret Key**: `Xj0OCpB7H7t4YT6LD87ShEE1JMys0ppRI6aU1Xy2wIfU3VYoN2sZfAp8uvz3MEce`
- **Crypto Wallet**: `0x29014271d71e3691cBaF01a26A4AC1502e2C4048`

## 🛠️ Technical Details

### SDK Integration
- **Nexgo SDK v3.06.001**: Latest version with full N92 support
- **Native Libraries**: Complete ARM64 and ARMv7 JNI libraries
- **EMV Configuration**: Pre-configured AID and CAPK files
- **All Payment Methods**: Visa, Mastercard, Amex, Discover, and more

### Architecture
- **MVVM Pattern**: Clean architecture with ViewModels
- **Service Layer**: Separate services for card reading, EMV, PIN pad, and printing
- **Material Design**: Modern, intuitive UI following Material Design guidelines
- **Kotlin**: 100% Kotlin implementation

## 📱 User Interface

### Main Menu
- **Payment**: Process new transactions
- **Settings**: Configure terminal and test components
- **History**: View transaction history
- **Reports**: Generate various reports

### Payment Screen
- Amount entry with numeric keypad
- Card detection and processing
- PIN entry interface
- Transaction status display

### Settings Screen
- Component testing (printer, card reader, PIN pad, EMV)
- Manual card entry dialog
- Terminal configuration
- Connection testing
- Device information

## 🚀 Installation & Deployment

### Prerequisites
- Android Studio
- Nexgo N92 POS Terminal
- USB debugging enabled on the terminal

### Build & Deploy
```bash
# Clone the repository
git clone <repository-url>
cd nexgo

# Build the application
./gradlew assembleDebug

# Deploy to device
./gradlew installDebug

# Or use the deployment script
./build_and_deploy.sh
```

### Manual Deployment
```bash
# Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the application
adb shell am start -n com.nexgo.n92pos/.ui.MainActivity
```

## 🔧 Configuration

### Terminal Settings
1. Open the app and go to Settings
2. Tap "Terminal Configuration"
3. Set your terminal ID, merchant ID, currency, and timeout
4. Save the configuration

### EMV Configuration
The app comes pre-configured with:
- AID (Application Identifier) data
- CAPK (Certification Authority Public Key) data
- EMV transaction parameters

### Card Reader Setup
The app automatically detects and configures:
- Chip card reader (ICC1)
- Magnetic stripe reader (SWIPE)
- Contactless/NFC reader (RF)

## 🧪 Testing

### Component Testing
1. Go to Settings
2. Test each component:
   - **Printer Test**: Prints a test receipt
   - **Card Reader Test**: Insert/swipe/tap a card
   - **PIN Pad Test**: Enter a PIN
   - **EMV Test**: Process a test EMV transaction

### Manual Card Entry
1. Go to Settings
2. Tap "Manual Card Entry"
3. Enter card details:
   - Card number
   - Expiry date (MM/YY)
   - CVV
   - Cardholder name
4. Process the payment

## 📋 Transaction Flow

1. **Amount Entry**: Enter transaction amount using keypad
2. **Card Detection**: Insert, swipe, or tap card
3. **EMV Processing**: Process EMV transaction if chip card
4. **PIN Entry**: Enter PIN if required
5. **Authorization**: Process transaction with payment processor
6. **Receipt Printing**: Print customer and merchant receipts
7. **Transaction Complete**: Save transaction to history

## 🔍 Troubleshooting

### Common Issues
- **Card not detected**: Check card reader connections and test in Settings
- **PIN pad not working**: Verify PIN pad service initialization
- **Printer not working**: Check paper and test in Settings
- **EMV errors**: Verify EMV configuration and test in Settings

### Debug Information
- Check device logs: `adb logcat | grep com.nexgo.n92pos`
- View device information in Settings
- Test all components individually

## 📚 API Reference

### CardReaderService
- `searchCard()`: Search for cards in specified slots
- `onCardInfo()`: Callback when card is detected
- `onSwipeIncorrect()`: Callback for incorrect swipe
- `onMultipleCards()`: Callback for multiple cards detected

### EMVService
- `processEMV()`: Process EMV transaction
- `initEMV()`: Initialize EMV configuration
- `setTerminalConfiguration()`: Set terminal parameters

### PinPadService
- `inputPin()`: Input PIN from user
- `inputOnlinePin()`: Input online PIN
- `inputOfflinePin()`: Input offline PIN

### PrinterService
- `printReceipt()`: Print transaction receipt
- `printTestPage()`: Print test page
- `appendPrnStr()`: Append text to print buffer

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly on N92 terminal
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For technical support or questions:
1. Check the troubleshooting section
2. Review device logs
3. Test individual components in Settings
4. Create an issue in the repository

## 🔄 Updates

### Version 1.0.0
- Initial release
- Complete payment processing
- EMV support
- Receipt printing
- Transaction history
- Settings and configuration
- Manual card entry
- Component testing

---

**Note**: This application is specifically designed for the Nexgo N92 POS terminal and requires the Nexgo SDK for proper functionality.