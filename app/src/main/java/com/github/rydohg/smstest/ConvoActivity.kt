package com.github.rydohg.smstest

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.IOException
import java.io.InputStream


class ConvoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convo)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
        supportActionBar?.setIcon(R.drawable.ic_person_white_24dp)

        val conversation: Conversation = intent.extras.getSerializable("convo") as Conversation
        supportActionBar?.title = nameOrPhoneNumber(conversation.displayName, conversation.number)
        Log.d("ConvoActivity", conversation.convoID.toString())

        val projection = arrayOf("_id", "address", "body", "ct_t", "type", "msg_box")

        val uri = Uri.parse("content://mms-sms/conversations/" + conversation.convoID)
        val c = contentResolver.query(uri, projection, null, null, null)

        val messages = ArrayList<Message>()
        var counter = 0
        if (c.moveToFirst()) {
            do {
                val body = c.getString(c.getColumnIndex("body"))
                val address = c.getString(c.getColumnIndex("address"))

                val smsTypeColumn = c.getString(c.getColumnIndex("type"))
                val mmsTypeColumn = c.getString(c.getColumnIndex("msg_box"))

                var received = true
                if (smsTypeColumn != null) {
                    if (smsTypeColumn.toInt() == 2) {
                        received = false
                    }
                }
                if (mmsTypeColumn != null) {
                    if (mmsTypeColumn.toInt() == 2) {
                        received = false
                        /*if (counter == 0) {
                            val mmsURI = Uri.parse("content://mms/")
                            val selection = "_id = " + c.getLong(c.getColumnIndex("_id"))
                            val cursor = contentResolver.query(mmsURI, null, selection, null, null)
                            cursor.moveToFirst()
                            for (i in cursor.columnNames) {
                                Log.d("ColName", "Name: " + i + " " + cursor.getString(cursor.getColumnIndex(i)))
                            }
                            cursor.close()
                            counter++
                        }*/
                    }
                }
                // For now SMS is type 1 and MMS is type 2
//                Log.d("Messages", "SMS Type: $smsTypeColumn MMS Type: $mmsTypeColumn")
                var type = 1
                if (smsTypeColumn == null) {
                    type = 2
                }
                // 1L for msg_box is temporary
                messages.add(Message(body, address, type, mmsTypeColumn?.toLong(), received))
            } while (c.moveToNext())
        }
        c.close()

        val recyclerView: RecyclerView = findViewById(R.id.message_recycler_view)
        recyclerView.setHasFixedSize(true)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MessageAdapter(messages)

        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun getMmsImage(_id: String): Bitmap? {
        val partURI = Uri.parse("content://mms/part/" + _id)
        var inputStream: InputStream? = null
        var bitmap: Bitmap? = null
        try {
            inputStream = contentResolver.openInputStream(partURI)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return bitmap
    }
}

//Body is null for MMS messages
data class Message(val body: String?, val address: String?, val type: Int, val msg_box: Long?, val received: Boolean)

class MessageAdapter constructor(private val messageList: ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CustomViewHolder {
         if (viewType == 1) {
            return SentMessageViewHolder(
                    LayoutInflater.from(parent?.context)
                            .inflate(R.layout.list_item_sent_message,
                                    parent,
                                    false)
                            as ConstraintLayout
            )
        } else if (viewType == 2){
            return ReceivedMessageViewHolder(
                    LayoutInflater.from(parent?.context)
                            .inflate(R.layout.list_item_received_message,
                                    parent,
                                    false)
                            as ConstraintLayout
            )
        } else {
             return ReceivedMessageViewHolder(
                     LayoutInflater.from(parent?.context)
                             .inflate(R.layout.list_item_received_message,
                                     parent,
                                     false)
                             as ConstraintLayout
             )
         }

    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].received) {
            if (messageList[position].msg_box != null){
                3
            } else {
                2
            }
        } else {
            1
        }
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val message = messageList[position]

        holder.messageTextView.text = message.body
    }

    override fun getItemCount(): Int = messageList.size

    abstract inner class CustomViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        abstract var messageTextView: TextView
    }

    inner class SentMessageViewHolder(rootView: View) : CustomViewHolder(rootView) {
        override var messageTextView: TextView = rootView.findViewById(R.id.sent_message_text_view)
    }

    inner class ReceivedMessageViewHolder(rootView: View) : CustomViewHolder(rootView) {
        override var messageTextView: TextView = rootView.findViewById(R.id.received_message_text_view)
    }
}
