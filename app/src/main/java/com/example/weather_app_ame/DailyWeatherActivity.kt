package com.example.weather_app_ame

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.weather_app_ame.databinding.ActivityDailyWeatherBinding
import java.text.SimpleDateFormat
import java.util.*

class DailyWeatherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDailyWeatherBinding
    lateinit var adapterWeather: WeatherAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRecycler()
        binding.btnBack.setOnClickListener { onBackPressed() }

    }

    private fun initRecycler() {

        val bundle = intent.extras
        val latitude = bundle?.get("INTENT_LATITUDE").toString()
        val longitude = bundle?.get("INTENT_LONGITUDE").toString()
        val city = bundle?.get("INTENT_CITY").toString()

        var weatherList = mutableListOf<Weather>()

        val open_current_weather_map_key = getString(R.string.open_current_weather_map_key)
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.openweathermap.org/data/2.5/onecall?lat=$latitude&lon=$longitude&exclude=current,minutely,hourly,alerts&units=metric&appid=$open_current_weather_map_key"
        var i = 0
        var itemWeather: Weather
        // Request a Json response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                while(i<8){
                    val sdf = SimpleDateFormat("EEE,\ndd MMM", Locale.ENGLISH)

                    itemWeather = Weather(
                        sdf.format(Date(response.getJSONArray("daily").getJSONObject(i)["dt"].toString().toLong()*1000)).toString(),
                        "${response.getJSONArray("daily").getJSONObject(i).getJSONObject("temp")["min"]}º /\n ${response.getJSONArray("daily").getJSONObject(i).getJSONObject("temp")["max"]}º",
                        response.getJSONArray("daily").getJSONObject(i)["humidity"].toString(),
                        response.getJSONArray("daily").getJSONObject(i)["wind_speed"].toString(),
                        response.getJSONArray("daily").getJSONObject(i)["pressure"].toString(),
                        response.getJSONArray("daily").getJSONObject(i).getJSONArray("weather").getJSONObject(0)["icon"].toString()
                        )
                    weatherList.add(itemWeather)
                    i++
                }
                binding.tvTitle.text = "7 days forecast of $city"
                adapterWeather = WeatherAdapter(this, weatherList)
                binding.rvWeatherList.layoutManager = LinearLayoutManager(this)
                binding.rvWeatherList.adapter = adapterWeather
            },
            { binding.tvTitle.text = "That didn't work!" })
        //binding.rvWeatherList.visibility = View.GONE
        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest)
    }
}