package com.example.myapplication

/*
* Developer: 马啸（1120211314）, 李乘黄（1120211158）
* ContactUs: lch0810@163.com
*/

import androidx.appcompat.app.AppCompatActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainUI : AppCompatActivity(), SensorEventListener {
    //倒计时部分
    private var isCountdownRunning = false
    private var isWorking = true
    private var isFirstStart = true
    private var isDefeat = false
    private var outProcessflag = 0

    private lateinit var countDownTimer: CountDownTimer
    private var timeLeftInMillis: Long = 25 * 60 * 1000  //设定倒计时时间，单位为毫秒,默认为25分钟
    private var timeLeftInMillisForWork: Long = 0
    private var timeLeftInMillisForRest: Long = 5 * 60 * 1000  //设定休息倒计时时间，单位为毫秒,默认为5分钟
    private var roundLeft: Int = 3  //设定轮次，默认为3
    private var roundRem: Int = 0

    private lateinit var timePicker: NumberPicker
    private lateinit var timePickerForRest: NumberPicker
    private lateinit var roundPicker: NumberPicker

    //传感器控制部分
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val gravity = FloatArray(3)
    private val linear_acceleration = FloatArray(3)
    private var orientationTextView: TextView? = null

    //存储用户上次设置部分
    private lateinit var prefsForWork: SharedPreferences
    private lateinit var prefsForRest: SharedPreferences
    private lateinit var prefsForRound: SharedPreferences

    //退出应用发送通知部分
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "My Channel"

    //计时器控制部分
    private var makeOnce = true
    private var makeOnceForRest = true

    companion object {
        var dataList: MutableList<TimerData> = mutableListOf()
        private const val PREFS_NAME = "MyPrefs"
        private const val DATA_LIST_KEY = "DataList"
    }
    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentTime = Date()
        return dateFormat.format(currentTime)
    }
    private fun saveDataListToPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(dataList)
        prefs.edit().putString(DATA_LIST_KEY, json).apply()
    }
    private fun restoreDataListFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(DATA_LIST_KEY, null)
        val type = object : TypeToken<MutableList<TimerData>>() {}.type
        dataList = gson.fromJson(json, type) ?: mutableListOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setTitle("专注番茄钟")

        restoreDataListFromPrefs()
        // 初始化 SharedPreferences 对象
        prefsForWork = getPreferences(MODE_PRIVATE)
        prefsForRest = getPreferences(MODE_PRIVATE)
        prefsForRound = getPreferences(MODE_PRIVATE)

        orientationTextView = findViewById(R.id.DetectionText)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        //读取存储的用户上次的设置
        val savedTimeForWork = prefsForWork.getLong("countdown_time", 0)
        if (savedTimeForWork > 0) {
            timeLeftInMillis = savedTimeForWork
            timeLeftInMillisForWork = savedTimeForWork
        }

        val savedTimeForRest = prefsForRest.getLong("countdown_time_rest", 0)
        if (savedTimeForRest > 0) {
            timeLeftInMillisForRest = savedTimeForRest
        }

        val savedTimeForRound = prefsForRound.getInt("countdown_round", 0)
        if (savedTimeForRound > 0) {
            roundLeft = savedTimeForRound
            roundRem = savedTimeForRound
        }

        //初始化

        updateCountdownTimer()
        updateCountdownTimerForRest()
        updateRound()

        val timePickerValues = arrayOf("10", "15", "20", "25", "30", "35", "40", "45",
            "50", "55", "60", "65", "70", "75", "80", "85", "90")
        timePicker = findViewById(R.id.time_picker)
        timePicker.displayedValues = timePickerValues
        timePicker.minValue = 2 // 最小值为10分钟
        timePicker.maxValue = 18 // 最大值为90分钟
        timePicker.value = 5 // 默认值为25分钟
        timePicker.wrapSelectorWheel = true // 滚轮循环显示
        timePicker.setOnValueChangedListener { _, _, Picker ->
            // 滚轮的值改变时，将timeLeftInMillis设置为对应的毫秒数
            timeLeftInMillisForWork = timeLeftInMillis      //记住工作时间
            //timeLeftInMillis = Picker * 1000L
            timeLeftInMillis = Picker * 5 * 60 * 1000L
            updateCountdownTimer()
        }

        val timePickerValuesForRest = arrayOf("5", "10", "15", "20", "25", "30", "35", "40", "45")
        timePickerForRest = findViewById(R.id.time_picker_rest)
        timePickerForRest.displayedValues = timePickerValuesForRest
        timePickerForRest.minValue = 1 // 最小值为5分钟
        timePickerForRest.maxValue = 9 // 最大值为45分钟
        timePickerForRest.value = 1 // 默认值为5分钟
        timePickerForRest.wrapSelectorWheel = true // 滚轮循环显示
        timePickerForRest.setOnValueChangedListener { _, _, Picker ->
            // 滚轮的值改变时，将timeLeftInMillis设置为对应的毫秒数

            //timeLeftInMillisForRest = Picker * 1000L
            timeLeftInMillisForRest = Picker * 5 * 60 * 1000L
            updateCountdownTimerForRest()
        }

        roundPicker = findViewById(R.id.time_picker_round)
        roundPicker.minValue = 1 // 最小值为1轮
        roundPicker.maxValue = 10 // 最大值为10轮
        roundPicker.value = 3 // 默认值为3轮
        roundPicker.wrapSelectorWheel = true // 滚轮循环显示
        roundPicker.setOnValueChangedListener { _, _, Picker ->
            // 滚轮的值改变时，将timeLeftInMillis设置为对应的毫秒数
            roundLeft = Picker
            roundRem = roundLeft
            updateRound()
        }

        /**/
        val countdownButton: Button = findViewById(R.id.countdown_button)
        countdownButton.setOnClickListener {

            orientationTextView?.setText("翻转手机以开始计时")
            orientationTextView?.setTextSize(25F)

            val situationText = findViewById<TextView>(R.id.SituationText)
            situationText.text = "拨动滚轮以设定番茄钟......"

            cleanProcess()

            countdownButton.isEnabled = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == R.id.about) {
            Toast.makeText(applicationContext, "联系我们: lch0810@163.com", Toast.LENGTH_SHORT).show()
            return true
        } else if (id == R.id.rem){
            val intent = Intent(this, ActivitySecond::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item);
    }

    fun updateCountdownTimer() {
        val countdownTimer = findViewById<TextView>(R.id.countdown_timer)
        val hours = (timeLeftInMillis / 1000) / 3600
        val minutes = ((timeLeftInMillis / 1000) % 3600) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        countdownTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d",
            hours, minutes, seconds)
        countdownTimer.textSize = 69f
        countdownTimer.gravity = Gravity.CENTER_HORIZONTAL
    }

    fun updateCountdownTimerForRest() {
        val countdownTimerForRest = findViewById<TextView>(R.id.countdown_timer_rest)
        val hours = (timeLeftInMillisForRest / 1000) / 3600
        val minutes = ((timeLeftInMillisForRest / 1000) % 3600) / 60
        val seconds = (timeLeftInMillisForRest / 1000) % 60
        countdownTimerForRest.text = String.format(Locale.getDefault(), "%02d:%02d:%02d",
            hours, minutes, seconds)
        countdownTimerForRest.textSize = 35f
        countdownTimerForRest.gravity = Gravity.CENTER_HORIZONTAL
    }

    fun updateRound() {
        val Round = findViewById<TextView>(R.id.RoundText)
        if(roundLeft <= 0){
            val situationText = findViewById<TextView>(R.id.SituationText)
            situationText.text = ""
            Round.text = "已完成！"

            val countdownButton: Button = findViewById(R.id.countdown_button)
            countdownButton.isEnabled = true

            Round.gravity = Gravity.CENTER_HORIZONTAL

            orientationTextView?.setText("有效工作时长已记录！")
            orientationTextView?.setTextSize(25F)
            situationText.text = "轻触“重新开始”以开启下一轮番茄钟......"
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(600)

            val currentTime = getCurrentTime()
            dataList.add(TimerData(currentTime,"成功",(timeLeftInMillisForWork*roundRem /1000).toString() + "秒"))
        } else {
            Round.text = String.format(
                Locale.getDefault(), "剩余轮次: %02d", roundLeft)
            Round.gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    fun cleanProcess() {
        roundLeft = roundRem
        updateRound()

        timeLeftInMillis = timeLeftInMillisForWork
        updateCountdownTimer()
        updateCountdownTimerForRest()

        isDefeat = false
        isCountdownRunning = false
        isFirstStart = true
        makeOnce = true
        makeOnceForRest = true
        isWorking = true

        timePicker.isEnabled = true
        timePickerForRest.isEnabled = true
        roundPicker.isEnabled = true
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        saveDataListToPrefs()

        if(!isFirstStart && isWorking && roundLeft > 0){
            // 在此处编写代码以创建和发送通知
            val intent = Intent(this, MainUI::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            val appIconId = packageManager.getApplicationInfo(packageName, 0).icon
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("专注番茄钟")
                .setContentText("提醒: 请返回应用")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

            outProcessflag += 1
            if(outProcessflag==3){
                val currentTime = getCurrentTime()
                dataList.add(TimerData(currentTime,"失败","-"))

                orientationTextView?.setText("你失败了！")
                orientationTextView?.setTextSize(25F)

                val situationText = findViewById<TextView>(R.id.SituationText)
                situationText.text = "轻触“重新开始”以重新启动番茄钟......"
                val countdownButton: Button = findViewById(R.id.countdown_button)
                countdownButton.isEnabled = true

                isWorking = false
                isDefeat = true

                val countdownTimer = findViewById<TextView>(R.id.countdown_timer)
                countdownTimer.text = "--:--:--"
                countdownTimer.textSize = 69f
                val countdownTimerForRest = findViewById<TextView>(R.id.countdown_timer_rest)
                countdownTimerForRest.text = "--:--:--"
                countdownTimerForRest.textSize = 35f
                val Round = findViewById<TextView>(R.id.RoundText)
                Round.text = "剩余轮次: --"
                Round.gravity = Gravity.CENTER_HORIZONTAL

            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 处理传递的数据
    }

    override fun onSensorChanged(event: SensorEvent) {

        val alpha = 0.8f
        val detectAccuracy = 0.9f       //精度参数，数值越大越不敏感

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        //消除噪声，提升精准度
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values.get(0)
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values.get(1)
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values.get(2)

        linear_acceleration[0] = event.values.get(0) - gravity[0]
        linear_acceleration[1] = event.values.get(1) - gravity[1]
        linear_acceleration[2] = event.values.get(2) - gravity[2]

        //监测数据并执行相应操作
        if (linear_acceleration[2] > detectAccuracy) {      //手机屏幕向上
            if(!isDefeat) {
                if (isFirstStart) {
                    orientationTextView?.setText("翻转手机以开始计时")
                    orientationTextView?.setTextSize(25F)
                } else if (isWorking && roundLeft > 0) {
                    orientationTextView?.setText("翻转手机以继续计时")
                    orientationTextView?.setTextSize(25F)
                } else if (roundLeft > 0) {
                    orientationTextView?.setText("休息一下吧！")
                    orientationTextView?.setTextSize(25F)
                } else {
                    orientationTextView?.setText("有效工作时长已记录！")
                    orientationTextView?.setTextSize(25F)
                    val situationText = findViewById<TextView>(R.id.SituationText)
                    situationText.text = "轻触“重新开始”以开启下一轮番茄钟......"
                }

                if (isCountdownRunning) {
                    // 如果计时器已经开始，则暂停计时器
                    timeLeftInMillis += 1000  //需要加回来一秒
                    countDownTimer.cancel()
                    isCountdownRunning = false
                    makeOnce = true
                } else if (makeOnceForRest && !isWorking) {
                    vibrator.cancel()
                    updateCountdownTimer()
                    countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {

                        override fun onTick(millisUntilFinished: Long) {
                            updateCountdownTimer()
                            timeLeftInMillis -= 1000
                        }

                        override fun onFinish() {
                            // 倒计时结束后的操作
                            val countdownTimer = findViewById<TextView>(R.id.countdown_timer)
                            val situationText = findViewById<TextView>(R.id.SituationText)
                            countdownTimer.text = "00:00:00"
                            timeLeftInMillis = timeLeftInMillisForWork
                            situationText.text = "正在工作中......"
                            orientationTextView?.setText("翻转手机以开始计时")
                            orientationTextView?.setTextSize(25F)
                            roundLeft -= 1
                            updateRound()
                            isWorking = true
                            isCountdownRunning = false

                            if(roundLeft > 0) {
                                val pattern = longArrayOf(200, 100, 200, 100, 200, 400)
                                vibrator.vibrate(pattern, 0)
                            }
                        }
                    }
                    countDownTimer.start()
                    makeOnce = true
                    makeOnceForRest = false
                }
            }
        } else if (linear_acceleration[2] < -detectAccuracy) {      //手机屏幕向下
            if (!isDefeat) {
                if (roundLeft > 0) {
                    if (isWorking) {
                        orientationTextView?.setText("正在计时......")
                        orientationTextView?.setTextSize(25F)
                    }
                    if (makeOnce && isWorking) {
                        vibrator.cancel()
                        if (!isCountdownRunning) {
                            countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {

                                override fun onTick(millisUntilFinished: Long) {
                                    updateCountdownTimer()
                                    timeLeftInMillis -= 1000
                                }

                                override fun onFinish() {
                                    // 倒计时结束后的操作
                                    val countdownTimer =
                                        findViewById<TextView>(R.id.countdown_timer)
                                    val situationText = findViewById<TextView>(R.id.SituationText)
                                    countdownTimer.text = "00:00:00"
                                    timeLeftInMillis = timeLeftInMillisForRest
                                    situationText.text = "正在休息中......"
                                    orientationTextView?.setText("休息一下吧！")
                                    orientationTextView?.setTextSize(25F)
                                    isWorking = false
                                    isCountdownRunning = false
                                    makeOnceForRest = true

                                    if(roundLeft > 0) {
                                        val pattern = longArrayOf(200, 100, 200, 100, 200, 400)
                                        vibrator.vibrate(pattern, 0)
                                    }
                                }
                            }
                        }
                        // 如果计时器未开始，则恢复计时器
                        if (isFirstStart) {
                            //如果是首次开始，则保存用户设置
                            prefsForWork.edit().putLong("countdown_time", timeLeftInMillisForWork)
                                .apply()
                            prefsForRest.edit()
                                .putLong("countdown_time_rest", timeLeftInMillisForRest)
                                .apply()
                            prefsForRound.edit().putInt("countdown_round", roundLeft).apply()

                            val situationText = findViewById<TextView>(R.id.SituationText)
                            situationText.text = "正在工作中......"

                            outProcessflag = 0

                            val countdownTimerForRest =
                                findViewById<TextView>(R.id.countdown_timer_rest)
                            countdownTimerForRest.text = ""

                            timePicker.isEnabled = false
                            timePickerForRest.isEnabled = false
                            roundPicker.isEnabled = false

                            updateRound()
                            isFirstStart = false
                        }
                        countDownTimer.start()
                        isCountdownRunning = true
                        makeOnce = false
                    }
                } else {
                    orientationTextView?.setText("有效工作时长已记录！")
                    orientationTextView?.setTextSize(25F)

                    val situationText = findViewById<TextView>(R.id.SituationText)
                    situationText.text = "轻触“重新开始”以开启下一轮番茄钟......"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}