package com.nexgo.n92pos.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nexgo.n92pos.R
import com.nexgo.n92pos.model.Transaction
import com.nexgo.n92pos.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryAdapter(
    private val transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder>() {
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tvTransactionId)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        holder.tvId.text = transaction.id
        holder.tvAmount.text = "$${transaction.amount}"
        holder.tvType.text = transaction.type
        holder.tvStatus.text = transaction.status.name
        holder.tvTime.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date(transaction.timestamp))
        
        // Set status color
        when (transaction.status) {
            TransactionStatus.SUCCESS -> {
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.success_color))
            }
            TransactionStatus.FAILED -> {
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.error_color))
            }
            TransactionStatus.PENDING -> {
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.warning_color))
            }
            else -> {
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }
    
    override fun getItemCount(): Int = transactions.size
}
