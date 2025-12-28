package eu.faircode.netguard

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Date

class AdapterDns(context: Context, cursor: Cursor) : CursorAdapter(context, cursor, 0) {
    private val colorExpired: Int
    private val colTime: Int
    private val colQName: Int
    private val colAName: Int
    private val colResource: Int
    private val colTTL: Int
    private val colUid: Int

    init {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        colorExpired =
            if (prefs.getBoolean("dark_theme", false)) {
                Color.argb(128, Color.red(Color.DKGRAY), Color.green(Color.DKGRAY), Color.blue(Color.DKGRAY))
            } else {
                Color.argb(128, Color.red(Color.LTGRAY), Color.green(Color.LTGRAY), Color.blue(Color.LTGRAY))
            }

        colTime = cursor.getColumnIndex("time")
        colQName = cursor.getColumnIndex("qname")
        colAName = cursor.getColumnIndex("aname")
        colResource = cursor.getColumnIndex("resource")
        colTTL = cursor.getColumnIndex("ttl")
        colUid = cursor.getColumnIndex("uid")
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.dns, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val time = cursor.getLong(colTime)
        val qname = cursor.getString(colQName)
        val aname = cursor.getString(colAName)
        val resource = cursor.getString(colResource)
        val ttl = cursor.getInt(colTTL)
        val uid = cursor.getInt(colUid)

        val now = Date().time
        val expired = time + ttl < now
        view.setBackgroundColor(if (expired) colorExpired else Color.TRANSPARENT)

        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvQName = view.findViewById<TextView>(R.id.tvQName)
        val tvAName = view.findViewById<TextView>(R.id.tvAName)
        val tvResource = view.findViewById<TextView>(R.id.tvResource)
        val tvTTL = view.findViewById<TextView>(R.id.tvTTL)
        val tvUid = view.findViewById<TextView>(R.id.tvUid)

        tvTime.text = SimpleDateFormat("dd HH:mm").format(time)
        tvQName.text = qname
        tvAName.text = aname
        tvResource.text = resource
        tvTTL.text = "+" + (ttl / 1000).toString()
        tvUid.text = if (uid > 0) uid.toString() else null
    }
}
