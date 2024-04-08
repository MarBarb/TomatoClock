package com.example.myapplication
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.TimerData

class TimerAdapter(private val dataList: MutableList<TimerData>) : RecyclerView.Adapter<TimerAdapter.TimerViewHolder>(){
    class TimerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val timerNameTextView: TextView = itemView.findViewById(R.id.timerNameTextView)
        val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        // 创建并返回 ViewHolder
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timer, parent, false)
        return TimerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        val timerData = dataList[position]

        holder.timeTextView.text = timerData.time
        holder.timerNameTextView.text = timerData.res
        holder.durationTextView.text = timerData.duration
    }

    fun updateData(newDataList: MutableList<TimerData>) {
        dataList.clear()
        dataList.addAll(newDataList)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return dataList.size
    }
}