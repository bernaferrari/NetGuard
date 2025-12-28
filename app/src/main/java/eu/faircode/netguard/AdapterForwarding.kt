package eu.faircode.netguard

import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView

class AdapterForwarding(context: Context, cursor: Cursor) : CursorAdapter(context, cursor, 0) {
    private val colProtocol = cursor.getColumnIndex("protocol")
    private val colDPort = cursor.getColumnIndex("dport")
    private val colRAddr = cursor.getColumnIndex("raddr")
    private val colRPort = cursor.getColumnIndex("rport")
    private val colRUid = cursor.getColumnIndex("ruid")

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.forward, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val protocol = cursor.getInt(colProtocol)
        val dport = cursor.getInt(colDPort)
        val raddr = cursor.getString(colRAddr)
        val rport = cursor.getInt(colRPort)
        val ruid = cursor.getInt(colRUid)

        val tvProtocol = view.findViewById<TextView>(R.id.tvProtocol)
        val tvDPort = view.findViewById<TextView>(R.id.tvDPort)
        val tvRAddr = view.findViewById<TextView>(R.id.tvRAddr)
        val tvRPort = view.findViewById<TextView>(R.id.tvRPort)
        val tvRUid = view.findViewById<TextView>(R.id.tvRUid)

        tvProtocol.text = Util.getProtocolName(protocol, 0, false)
        tvDPort.text = dport.toString()
        tvRAddr.text = raddr
        tvRPort.text = rport.toString()
        tvRUid.text = TextUtils.join(", ", Util.getApplicationNames(ruid, context))
    }
}
