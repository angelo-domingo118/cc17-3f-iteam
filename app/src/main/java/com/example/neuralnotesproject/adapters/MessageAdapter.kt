package com.example.neuralnotesproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.neuralnotesproject.R
import com.example.neuralnotesproject.data.Message
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class MessageAdapter(
    private var messages: List<Message>,
    private val onSaveClick: (Message) -> Unit,
    private val onCopyClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tv_message)
        private val timeText: TextView = itemView.findViewById(R.id.tv_time)
        private val copyButton: ImageButton? = itemView.findViewById(R.id.btn_copy)
        private val saveButton: ImageButton? = itemView.findViewById(R.id.btn_save)

        fun bind(message: Message) {
            messageText.text = message.content
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            timeText.text = formattedTime

            if (!message.isUser) {
                copyButton?.visibility = View.VISIBLE
                saveButton?.visibility = View.VISIBLE
                copyButton?.setOnClickListener { onCopyClick(message) }
                saveButton?.setOnClickListener { onSaveClick(message) }
            } else {
                copyButton?.visibility = View.GONE
                saveButton?.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == 0) R.layout.item_message_user else R.layout.item_message_ai
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 0 else 1
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }
} 