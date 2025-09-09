package com.nexgo.n92pos.ui.crypto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityCryptoWalletBinding
import com.nexgo.n92pos.service.CryptoService
import com.nexgo.n92pos.service.BankService
import java.text.SimpleDateFormat
import java.util.*

class CryptoWalletActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCryptoWalletBinding
    private lateinit var cryptoService: CryptoService
    private lateinit var bankService: BankService
    private lateinit var adapter: CryptoTransactionAdapter
    private val cryptoTransactions = mutableListOf<CryptoService.CryptoTransaction>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cryptoService = CryptoService(this)
        bankService = BankService(this)
        
        setupUI()
        loadWalletData()
    }
    
    private fun setupUI() {
        binding.apply {
            btnBack.setOnClickListener { finish() }
            btnRefresh.setOnClickListener { loadWalletData() }
            btnBankInfo.setOnClickListener { showBankInfo() }
        }
        
        adapter = CryptoTransactionAdapter(cryptoTransactions)
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@CryptoWalletActivity)
            adapter = this@CryptoWalletActivity.adapter
        }
    }
    
    private fun loadWalletData() {
        binding.tvLoading.text = "Loading wallet data..."
        
        // Load crypto wallet info
        val walletAddress = cryptoService.getWalletAddress()
        val cryptoBalance = cryptoService.getWalletBalance()
        val cryptoTransactions = cryptoService.getTransactionHistory()
        
        // Load bank info
        val bankInfo = bankService.getBankAccountInfo()
        val bankBalance = bankService.getAccountBalance()
        
        binding.apply {
            tvWalletAddress.text = walletAddress
            tvCryptoBalance.text = "$cryptoBalance ETH"
            tvBankAccount.text = bankInfo.accountNumber
            tvBankBalance.text = "$${bankBalance}"
            tvBankName.text = bankInfo.bankName
        }
        
        // Update transactions
        this.cryptoTransactions.clear()
        this.cryptoTransactions.addAll(cryptoTransactions)
        adapter.notifyDataSetChanged()
        
        binding.tvLoading.text = "Wallet data loaded"
        
        Toast.makeText(this, "Wallet data refreshed", Toast.LENGTH_SHORT).show()
    }
    
    private fun showBankInfo() {
        val bankInfo = bankService.getBankAccountInfo()
        val bankBalance = bankService.getAccountBalance()
        
        val bankDetails = """
            Bank: ${bankInfo.bankName}
            Account: ${bankInfo.accountNumber}
            Routing: ${bankInfo.routingNumber}
            Type: ${bankInfo.accountType}
            Balance: $${bankBalance}
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bank Account Details")
            .setMessage(bankDetails)
            .setPositiveButton("OK", null)
            .show()
    }
}

class CryptoTransactionAdapter(
    private val transactions: List<CryptoService.CryptoTransaction>
) : androidx.recyclerview.widget.RecyclerView.Adapter<CryptoTransactionAdapter.TransactionViewHolder>() {
    
    class TransactionViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val tvHash: android.widget.TextView = itemView.findViewById(R.id.tvTxHash)
        val tvFrom: android.widget.TextView = itemView.findViewById(R.id.tvFrom)
        val tvTo: android.widget.TextView = itemView.findViewById(R.id.tvTo)
        val tvValue: android.widget.TextView = itemView.findViewById(R.id.tvValue)
        val tvTime: android.widget.TextView = itemView.findViewById(R.id.tvTime)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TransactionViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crypto_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        holder.tvHash.text = transaction.hash.take(10) + "..."
        holder.tvFrom.text = transaction.from.take(10) + "..."
        holder.tvTo.text = transaction.to.take(10) + "..."
        
        // Convert Wei to ETH
        val ethValue = java.math.BigDecimal(transaction.value)
            .divide(java.math.BigDecimal("1000000000000000000"))
            .setScale(6, java.math.BigDecimal.ROUND_HALF_UP)
        holder.tvValue.text = "$ethValue ETH"
        
        val date = Date(transaction.timestamp * 1000)
        holder.tvTime.text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(date)
    }
    
    override fun getItemCount(): Int = transactions.size
}
