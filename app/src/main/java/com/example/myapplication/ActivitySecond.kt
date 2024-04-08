package com.example.myapplication

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.MainUI.Companion.dataList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.sql.Time

class ActivitySecond : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        // 初始化 RecyclerView 和适配器
        recyclerView = findViewById(R.id.recyclerView)
        adapter = TimerAdapter(dataList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        adapter.notifyItemInserted(dataList.size-1)
        val clearButton: Button = findViewById(R.id.clear_button)
        clearButton.setOnClickListener{
            // 清空 dataList
            dataList.clear()

            // 通知适配器数据集发生变化
            adapter.notifyDataSetChanged()
        }
    }


}