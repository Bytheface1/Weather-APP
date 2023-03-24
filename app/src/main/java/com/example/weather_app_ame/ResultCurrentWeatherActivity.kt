package com.example.weather_app_ame

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.android.volley.ExecutorDelivery
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.weather_app_ame.databinding.ActivityResultCurrentWeatherBinding
import com.squareup.picasso.Picasso
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

class ResultCurrentWeatherActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityResultCurrentWeatherBinding
    //Mobile sensor lux parameters
    private lateinit var sensorManager: SensorManager
    private var mLight: Sensor? = null

    //Bluetooth parameters
    private lateinit var city: String
    private val MAC_DEVICE = "00:0E:EA:CF:61:62"        //Modify MAC device here
    private val NAME_DEVICE = "HMSoft"                  //Modify name device here
    companion object{
        private var bluetoothAdapter: BluetoothAdapter? = null
        lateinit var myUUID: UUID
    }

    inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("Weather App", myUUID)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true

            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e("TAG", "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }

                socket?.also {

                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }


            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e("TAG", "Could not close the connect socket", e)
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.

            bluetoothAdapter?.cancelDiscovery()


            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()
                    manageMyConnectedSocket(socket)
                }catch (e: Exception){
                    Log.e("ERROR", "Socket fail, no device connected");
                }

            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("TAG", "Could not close the client socket", e)
            }
        }
    }

    private fun pairedDeviceList(): BluetoothDevice {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val list: ArrayList<BluetoothDevice> = ArrayList()

        if (pairedDevices != null) {
            if (pairedDevices.isNotEmpty()) {
                for (device: BluetoothDevice in pairedDevices) {
                    if (device.address == MAC_DEVICE && device.name == NAME_DEVICE) {
                        list.add(device)
                    }
                    Log.i("device", device.address)
                }
            } else {
                Toast.makeText(this, "No paired bluetooth devices found", Toast.LENGTH_SHORT).show()
            }
        }
        myUUID = list[0].uuids[0].uuid
        return list[0]
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {
        try{
            val handler = object: Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    val aux = String(msg.obj as ByteArray,0, msg.arg1)

                    val parts = aux.split("|")
                    if (parts.size == 2) {
                        binding.tvAmbientLux.text = "${parts[0]} Lux"
                        binding.tvAmbientTemperature.text = "${parts[1]}º"
                    }
                }
            }
            val myBluetoothService = MyBluetoothService(handler)


            myBluetoothService.ConnectedThread(socket).run()

        }catch(e: Exception){
            Log.e("ERROR", "Socket fail, no device connected");
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultCurrentWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getAndShowWeather()
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter


        if (bluetoothAdapter == null) {
            Toast.makeText(this, "The device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter!!.isEnabled){
            Toast.makeText(this, "Bluetooth have to been enabled", Toast.LENGTH_SHORT).show()
        }
        try {
            ConnectThread(pairedDeviceList()).start()
            AcceptThread().start()
        }catch (e: Exception){
            Toast.makeText(this, "Bluetooth device not found, restart the application", Toast.LENGTH_SHORT).show()

        }


        // Initialize the variable sensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        } else {
            binding.tvAmbientLuxMobile.text = "No sensor!"
        }

        binding.btnBack.setOnClickListener { onBackPressed() }
        binding.btnDailyWeather.setOnClickListener { goDailyWeather() }

    }

    private fun getAndShowWeather() {
        val bundle = intent.extras
        val latitude = bundle?.get("INTENT_LATITUDE").toString()
        val longitude = bundle?.get("INTENT_LONGITUDE").toString()

        val open_current_weather_map_key = getString(R.string.open_current_weather_map_key)
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$open_current_weather_map_key"
        val sdf = SimpleDateFormat("EEEE, dd 'of' MMMM 'at' HH:mm", Locale.ENGLISH)

        // Request a Json response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                city = response["name"].toString()
                binding.tvCity.text = city
                binding.tvTime.text = sdf.format(Date(response["dt"].toString().toLong()*1000)).toString()
                binding.tvCountry.text = "${response.getJSONObject("sys")["country"]}"
                binding.tvDegree.text = "${response.getJSONObject("main")["temp"]} ºC"
                binding.tvMinmax.text = "${response.getJSONObject("main")["temp_min"]}º / ${response.getJSONObject("main")["temp_max"]}º"
                binding.tvForecast.text = "${response.getJSONArray("weather").getJSONObject(0)["main"]}"
                binding.tvHumidity.text = "${response.getJSONObject("main")["humidity"]} %"
                binding.tvWind.text = "${response.getJSONObject("wind")["speed"]} m/s"
                binding.tvPressure.text = "${response.getJSONObject("main")["pressure"]} mb"
                val image = response.getJSONArray("weather").getJSONObject(0)["icon"].toString()

                Picasso.get().load("https://openweathermap.org/img/wn/$image%404x.png").into(binding.ivImage)
            },
            { binding.tvCity.text = "That didn't work!" })

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest)
    }

    private fun goDailyWeather() {
        val bundle = intent.extras
        val latitude = bundle?.get("INTENT_LATITUDE").toString()
        val longitude = bundle?.get("INTENT_LONGITUDE").toString()
        val intentAdd = Intent(this, DailyWeatherActivity::class.java)
        intentAdd.putExtra("INTENT_LATITUDE", latitude)
        intentAdd.putExtra("INTENT_LONGITUDE", longitude)
        intentAdd.putExtra("INTENT_CITY", city)
        startActivity(intentAdd)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // The light sensor returns a single value.
        // Do something with this sensor value.
        binding.tvAmbientLuxMobile.text = "${Math.round(event.values[0]*100)/100} Lux"

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Do something here if sensor accuracy changes.
    }
    override fun onResume() {
        super.onResume()
        mLight?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}