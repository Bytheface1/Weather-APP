package com.example.weather_app_ame

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.weather_app_ame.databinding.ItemWeatherBinding
import com.squareup.picasso.Picasso

class WeatherAdapter(val context: Context, private var weatherList: MutableList<Weather>) : RecyclerView.Adapter<WeatherAdapter.WeatherHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherHolder {

        val layoutInflater = LayoutInflater.from(parent.context)    // Class that allows creating views
        return WeatherHolder(layoutInflater.inflate(R.layout.item_weather, parent, false))
    }

    override fun getItemCount(): Int = weatherList.size

    override fun onBindViewHolder(holder: WeatherHolder, position: Int) {
        holder.render(weatherList[position])
    }

    class WeatherHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        private val binding = ItemWeatherBinding.bind(view)

        fun render(weather: Weather) {

            binding.tvDay.text = weather.dt
            binding.tvDegree.text = weather.degree
            binding.tvHumidity.text = "Humidity: ${weather.humidity} %"
            binding.tvWind.text = "Wind: ${weather.wind} m/s"
            binding.tvPressure.text = "Pressure: ${weather.pressure} mb"

            Picasso.get().load("https://openweathermap.org/img/wn/${weather.icon}%404x.png").into(binding.ivImage)

            view.setOnClickListener {
                Toast.makeText(
                    view.context,
                    "You have selected ${weather.dt}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}