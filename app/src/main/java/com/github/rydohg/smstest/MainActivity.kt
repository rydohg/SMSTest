package com.github.rydohg.smstest

import android.app.Application
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.Telephony
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.text.DateFormat
import android.provider.ContactsContract

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Get SMS/MMS Conversations
        val contentResolver = contentResolver
        val projection = arrayOf("*")
        val uri = Uri.parse("content://mms-sms/conversations?simple=true")
        val query = contentResolver.query(uri, projection, null, null, null)
        val convoList = ArrayList<Conversation>()
        if (query.moveToFirst()){
            for (col in query.columnNames){
                Log.d("ColNames", col + ": " + query.getShort(query.getColumnIndex(col)))
            }
            do {
                /***
                 * These apply to both SMS and MMS according to
                 * @link(https://android.googlesource.com/platform/packages/providers/TelephonyProvider/+/jb-mr1.1-release/src/com/android/providers/telephony/MmsSmsProvider.java)
                 * */
                val threadID = query.getLong(query.getColumnIndex(BaseColumns._ID))
                val date = query.getLong(query.getColumnIndex(Telephony.Mms.DATE))
                val recipientId = query.getLong(query.getColumnIndex("recipient_ids"))
                val number = getContactNumber(recipientId)
                val displayName = getContactName(number)
                val snippet = query.getString(query.getColumnIndex("snippet"))

                convoList.add(Conversation(threadID, recipientId, number, displayName, date, snippet))
            } while (query.moveToNext())
        }
        query.close()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(parent)
        recyclerView.layoutManager = mLayoutManager

        /*val mDividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                mLayoutManager.orientation
        )

        recyclerView.addItemDecoration(mDividerItemDecoration)*/

        recyclerView.adapter = ConvoAdapter(convoList)
    }

    private fun getContactNumber(recipientId: Long): String? {
        var number: String? = null
        val c = contentResolver.query(ContentUris
                .withAppendedId(Uri.parse("content://mms-sms/canonical-address"), recipientId), null, null, null, null)
        if (c!!.moveToFirst()) {
            number = c.getString(0)
        }
        c.close()
        return number
    }

    private fun getContactName(phoneNumber: String?): String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        var contactName = ""
        val cursor = contentResolver.query(uri, projection, null, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(0)
            }
            cursor.close()
        }

        return contactName
    }

    fun openNewConvoActivity(view: View){
        val intent = Intent(this, NewConvoActivity::class.java)
        startActivity(intent)
    }
}

data class Conversation(val convoID: Long, val recipientId: Long, val number: String?, val displayName: String?, val date: Long, val lastMessageContent: String?)

class ConvoAdapter constructor(private val convoList: ArrayList<Conversation>) : RecyclerView.Adapter<ConvoAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CustomViewHolder {
        val layout = LayoutInflater.from(parent?.context)
                .inflate(R.layout.list_item_convo, parent, false) as ConstraintLayout
        return CustomViewHolder(layout)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val convo = convoList[position]

        val number = convo.number
        val lastMessageContent = convo.lastMessageContent
        val date = DateFormat.getDateInstance().format(convo.date)

        if (!convo.displayName.equals("")){
            holder.senderTextView.text = convo.displayName
        } else {
            holder.senderTextView.text = number
        }

        holder.lastMessageTextView.text = lastMessageContent
        holder.dateTextView.text = date
    }

    override fun getItemCount(): Int = convoList.size

    inner class CustomViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        var senderTextView: TextView = rootView.findViewById(R.id.sender_text_view)
        var lastMessageTextView: TextView = rootView.findViewById(R.id.last_message_text_view)
        var dateTextView: TextView = rootView.findViewById(R.id.date_text_view)
    }
}

class MyApplication: Application()