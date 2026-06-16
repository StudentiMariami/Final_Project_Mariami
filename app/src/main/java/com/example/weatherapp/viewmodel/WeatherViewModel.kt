package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.weatherapp.data.local.WeatherDatabase
import com.example.weatherapp.data.remote.RetrofitInstance
import com.example.weatherapp.model.CurrentWeather
import com.example.weatherapp.model.ForecastDay
import com.example.weatherapp.repository.Result
import com.example.weatherapp.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * VIEWMODEL — the "VM" in MVVM
 *
 * Survives screen rotation (unlike Activity/Fragment).
 * Holds all UI state as LiveData that Fragments observe.
 * Never references Activity, Fragment, or any View — prevents memory leaks.
 *
 * AndroidViewModel gives access to Application context (needed for Room).
 */
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeatherRepository = WeatherDatabase.getDatabase(application).let { db ->
        WeatherRepository(db.weatherDao(), db.forecastDao(), RetrofitInstance.api)
    }

    // The city currently being displayed
    private val _currentCity = MutableLiveData<String>("Tbilisi")
    val currentCity: LiveData<String> get() = _currentCity

    // Loading state — shows/hides the progress bar in the UI
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Error/info messages shown as Snackbar
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> get() = _message

    // LiveData for current weather (observed by HomeFragment)
    // switchMap re-subscribes to Room whenever the city name changes
    val currentWeather: LiveData<CurrentWeather?> = _currentCity.switchMap { city ->
        repository.getCachedWeather(city)
    }

    // LiveData for 7-day forecast (observed by ForecastFragment)
    val forecast: LiveData<List<ForecastDay>> = _currentCity.switchMap { city ->
        repository.getCachedForecast(city)
    }

    init {
        // Load default city on first launch
        fetchWeather("Tbilisi")
    }

    /**
     * Search for weather by city name (called from the search menu item).
     * Dispatchers.IO: runs on background thread — DB + network must not block UI.
     */
    fun fetchWeather(city: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = repository.fetchWeatherByCity(city)
            when (result) {
                is Result.Success -> {
                    // Update the current city so LiveData switchMap re-subscribes
                    _currentCity.postValue(result.data.cityName)
                }
                is Result.Error -> {
                    _message.postValue(result.message)
                }
                else -> {}
            }
            _isLoading.postValue(false)
        }
    }

    /**
     * Fetch weather using GPS coordinates — NEW FEATURE.
     * Called when user taps the location button and grants permission.
     */
    fun fetchWeatherByLocation(lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val result = repository.fetchWeatherByLocation(lat, lon)
            when (result) {
                is Result.Success -> {
                    _currentCity.postValue(result.data.cityName)
                }
                is Result.Error -> {
                    _message.postValue(result.message)
                }
                else -> {}
            }
            _isLoading.postValue(false)
        }
    }

    /** Refresh current city's weather */
    fun refresh() {
        _currentCity.value?.let { fetchWeather(it) }
    }

    /** Clear message after it has been shown */
    fun clearMessage() {
        _message.value = null
    }
}
