package com.github.rydohg.smstest

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.MotionEvent
import android.text.method.Touch.onTouchEvent
import android.view.GestureDetector
import java.io.Serializable


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Ask for permission
        val permissions: Array<String> = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
        //Get SMS/MMS Conversations
        val contentResolver = contentResolver
        val projection = arrayOf("*")
        val uri = Uri.parse("content://mms-sms/conversations?simple=true")
        val query = contentResolver.query(uri, projection, null, null, null)
        val convoList = ArrayList<Conversation>()

        if (query.moveToFirst()) {
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
        recyclerView.addOnItemTouchListener(
                RecyclerItemClickListener(this, recyclerView,
                        object : RecyclerItemClickListener.OnItemClickListener {
                            override fun onItemClick(view: View, position: Int) {
                                val intent = Intent(applicationContext, ConvoActivity::class.java)
                                val bundle = Bundle()
                                bundle.putSerializable("convo", convoList[position])
                                intent.putExtras(bundle)
                                startActivity(intent)
                            }

                            override fun onLongItemClick(view: View?, position: Int) {

                            }
                        }
                )
        )

        val mLayoutManager = LinearLayoutManager(parent)
        recyclerView.layoutManager = mLayoutManager

        /*val mDividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                mLayoutManager.orientation
        )

        recyclerView.addItemDecoration(mDividerItemDecoration)*/

        recyclerView.adapter = ConvoAdapter(convoList)
    }

    private fun getContactNumber(recipientId: Long): String {
        var number: String = ""
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

    fun openNewConvoActivity(view: View) {
        val intent = Intent(this, NewConvoActivity::class.java)
        startActivity(intent)
    }
}

data class Conversation(
        val convoID: Long,
        val recipientId: Long,
        val number: String,
        val displayName: String,
        val date: Long,
        val lastMessageContent: String?) : Serializable

fun nameOrPhoneNumber(displayName: String, phoneNumber: String): String {
    return if (displayName !== "") {
        displayName
    } else {
        phoneNumber
    }
}

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

        holder.senderTextView.text = nameOrPhoneNumber(convo.displayName, number)

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

class MyApplication : Application()

class RecyclerItemClickListener(context: Context, recyclerView: RecyclerView, private val mListener: OnItemClickListener?) : RecyclerView.OnItemTouchListener {

    private var mGestureDetector: GestureDetector

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)

        fun onLongItemClick(view: View?, position: Int)
    }

    init {
        mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val child = recyclerView.findChildViewUnder(e.x, e.y)
                if (child != null && mListener != null) {
                    mListener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child))
                }
            }
        })
    }

    override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)
        if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
            mListener.onItemClick(childView, view.getChildAdapterPosition(childView))
            return true
        }
        return false
    }

    override fun onTouchEvent(view: RecyclerView, motionEvent: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}