package com.example.neuralnotesproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.neuralnotesproject.R
import com.example.neuralnotesproject.data.Message

class MessageAdapter(
    private var messages: List<Message>,
    private val onMessageClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userMessageLayout: View = itemView.findViewById(R.id.user_message_layout)
        private val aiMessageLayout: View = itemView.findViewById(R.id.ai_message_layout)
        private val userMessageText: TextView = itemView.findViewById(R.id.user_message_text)
        private val aiMessageText: TextView = itemView.findViewById(R.id.ai_message_text)

        fun bind(message: Message) {
            if (message.isUser) {
                userMessageLayout.visibility = View.VISIBLE
                aiMessageLayout.visibility = View.GONE
                userMessageText.text = message.content
            } else {
                userMessageLayout.visibility = View.GONE
                aiMessageLayout.visibility = View.VISIBLE
                aiMessageText.text = message.content
            }

            itemView.setOnClickListener { onMessageClick(message) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
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