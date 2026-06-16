package com.example.weatherapp.ui.forecast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weatherapp.data.remote.WeatherApiConfig
import com.example.weatherapp.databinding.ItemForecastBinding
import com.example.weatherapp.model.ForecastDay
import kotlin.math.roundToInt

/**
 * FORECAST ADAPTER
 *
 * ListAdapter uses DiffUtil to only redraw changed rows — smooth, efficient.
 * ViewBinding (ItemForecastBinding) means zero findViewByIds.
 * Glide loads weather icons from the OpenWeatherMap CDN.
 */
class ForecastAdapter : ListAdapter<ForecastDay, ForecastAdapter.ForecastViewHolder>(DiffCallback()) {

    inner class ForecastViewHolder(private val binding: ItemForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: ForecastDay) {
            // Day name (Mon, Tue, etc.)
            binding.textViewDay.text = day.date

            // Temperature range: "23° / 17°"
            binding.textViewTempMax.text = "${day.tempMax.roundToInt()}°"
            binding.textViewTempMin.text = "${day.tempMin.roundToInt()}°"

            // Weather description capitalised
            binding.textViewDesc.text = day.description.replaceFirstChar { it.uppercase() }

            // Chance of rain percentage
            binding.textViewRain.text = "${day.chanceOfRain}%"

            /**
             * GLIDE — loads the weather icon image from OpenWeatherMap's CDN.
             * URL format: https://openweathermap.org/img/wn/11d@2x.png
             * iconCode is returned by the API (e.g. "11d" = thunderstorm day)
             */
            val iconUrl = "${WeatherApiConfig.ICON_BASE_URL}${day.iconCode}@2x.png"
            Glide.with(binding.root.context)
                .load(iconUrl)
                .into(binding.imageViewIcon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** DiffCallback: tells ListAdapter which items changed so it animates only those rows */
    class DiffCallback : DiffUtil.ItemCallback<ForecastDay>() {
        override fun areItemsTheSame(oldItem: ForecastDay, newItem: ForecastDay) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ForecastDay, newItem: ForecastDay) =
            oldItem == newItem
    }
}
