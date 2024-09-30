package com.example.neuralnotesproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val onSaveButtonClick: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
    }

    class AIMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val btnCopy: MaterialButton = view.findViewById(R.id.btnCopy)
        val btnLike: MaterialButton = view.findViewById(R.id.btnLike)
        val btnDislike: MaterialButton = view.findViewById(R.id.btnDislike)
        val btnSave: MaterialButton = view.findViewById(R.id.btnSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_AI -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_ai, parent, false)
                AIMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> {
                holder.messageText.text = message.content
            }
            is AIMessageViewHolder -> {
                holder.messageText.text = message.content
                setupAIMessageButtons(holder, message)
            }
        }
    }

    private fun setupAIMessageButtons(holder: AIMessageViewHolder, message: Message) {
        holder.btnCopy.setOnClickListener {
            val clipboard = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AI Message", message.content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(holder.itemView.context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        holder.btnLike.setOnClickListener {
            toggleLikeDislike(holder, message, true)
        }

        holder.btnDislike.setOnClickListener {
            toggleLikeDislike(holder, message, false)
        }

        holder.btnSave.setOnClickListener {
            onSaveButtonClick(message)
        }

        // Set initial like/dislike button states
        updateLikeDislikeButtonStates(holder, message)
    }

    private   fun toggleLikeDislike(holder: AIMessageViewHolder, message: Message, isLike: Boolean) {
        val context = holder.itemView.context
        if (isLike) {
            message.isLiked = !message.isLiked
            message.isDisliked = false
        } else {
            message.isDisliked = !message.isDisliked
            message.isLiked = false
        }
        updateLikeDislikeButtonStates(holder, message)
    }

    private fun updateLikeDislikeButtonStates(holder: AIMessageViewHolder, message: Message) {
        val context = holder.itemView.context
        holder.btnLike.iconTint = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (message.isLiked) R.color.like_button_active else R.color.like_button_inactive)
        )
        holder.btnDislike.iconTint = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (message.isDisliked) R.color.like_button_active else R.color.like_button_inactive)
        )
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    // Remove this method as it's no longer needed
    // fun getMessages(): List<Message> = messages
}