const Flutterwave = require('flutterwave-node-v3');

// Initialize Flutterwave with your API keys
const flw = new Flutterwave(
  process.env.FLW_PUBLIC_KEY,
  process.env.FLW_SECRET_KEY
);

export default async function handler(req, res) {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const {
      cardNumber,
      expiryMonth,
      expiryYear,
      cvv,
      amount,
      currency,
      email,
      fullname,
      phoneNumber,
      txRef
    } = req.body;

    // Validate required fields
    if (!cardNumber || !expiryMonth || !expiryYear || !cvv || !amount || !email) {
      return res.status(400).json({
        error: 'Missing required fields',
        required: ['cardNumber', 'expiryMonth', 'expiryYear', 'cvv', 'amount', 'email']
      });
    }

    // Prepare card charge data
    const cardChargeData = {
      enckey: process.env.FLW_ENCRYPTION_KEY,
      tx_ref: txRef || `POS_${Date.now()}`,
      amount: parseFloat(amount),
      currency: currency || 'KES',
      card_number: cardNumber,
      cvv: cvv,
      expiry_month: expiryMonth,
      expiry_year: expiryYear,
      email: email,
      fullname: fullname || 'POS Customer',
      phone_number: phoneNumber || '08012345678',
      device_fingerprint: `POS_${Date.now()}`,
      redirect_url: 'https://your-pos-app.com/payment-callback'
    };

    console.log('Processing card charge:', {
      tx_ref: cardChargeData.tx_ref,
      amount: cardChargeData.amount,
      currency: cardChargeData.currency,
      email: cardChargeData.email
    });

    // Process the card charge
    const response = await flw.Charge.card(cardChargeData);

    console.log('Flutterwave response:', response);

    // Return the response
    res.status(200).json({
      success: true,
      data: response
    });

  } catch (error) {
    console.error('Error processing card charge:', error);
    
    res.status(500).json({
      success: false,
      error: error.message || 'Internal server error',
      details: error.response?.data || null
    });
  }
}
