package com.example.weatherapp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.weatherapp.R
import com.example.weatherapp.data.remote.WeatherApiConfig
import com.example.weatherapp.databinding.FragmentHomeBinding
import com.example.weatherapp.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt

/**
 * HOME FRAGMENT — Shows current weather
 *
 * NEW FEATURE: GPS location using FusedLocationProviderClient (Google Play Services).
 * The user can tap a button to auto-detect their city instead of typing it.
 *
 * Permission flow:
 * 1. User taps GPS button
 * 2. App requests ACCESS_FINE_LOCATION at runtime (Android 6+ requirement)
 * 3. If granted → get last known location → send lat/lon to ViewModel
 * 4. ViewModel → Repository → Retrofit API → Room cache → LiveData → UI updates
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel — same instance used in ForecastFragment
    private val viewModel: WeatherViewModel by activityViewModels()

    /**
     * FusedLocationProviderClient — Google's recommended location API.
     * More accurate and battery-efficient than using GPS directly.
     * This is the NEW FEATURE not used in previous lectures.
     */
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    /**
     * ActivityResultContracts.RequestMultiplePermissions — modern API for runtime permissions.
     * Replaces the old onRequestPermissionsResult() method.
     * The lambda is called with a Map<permission, granted> after the user responds.
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permission granted — get the device location
                getDeviceLocation()
            }
            else -> {
                Snackbar.make(binding.root, "Location permission denied. Please search manually.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        observeViewModel()
        setupMenu()

        // GPS button: request permission then fetch location
        binding.buttonLocation.setOnClickListener {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        // Navigate to the 7-day forecast screen
        binding.buttonSeeForcast.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_forecast)
        }
    }

    /**
     * Get the last known device location using FusedLocationProviderClient.
     * @SuppressLint: we already checked the permission above.
     * lastLocation returns the most recent cached location (very fast, no GPS warm-up).
     */
    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Pass coordinates to ViewModel → Repository → Retrofit
                    viewModel.fetchWeatherByLocation(location.latitude, location.longitude)
                } else {
                    Snackbar.make(binding.root, "Could not get location. Try again outside.", Snackbar.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "Location error: ${it.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    /**
     * Observe LiveData from the ViewModel.
     * Each observer lambda runs on the MAIN thread whenever the data changes.
     */
    private fun observeViewModel() {
        // Current weather — update the UI whenever Room cache is refreshed
        viewModel.currentWeather.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                // City + country header
                binding.textViewCity.text = "${weather.cityName}, ${weather.country}"

                // Main temperature (rounded)
                binding.textViewTemp.text = "${weather.temperature.roundToInt()}°C"

                // Description capitalised
                binding.textViewDescription.text =
                    weather.description.replaceFirstChar { it.uppercase() }

                // Feels like
                binding.textViewFeelsLike.text = "Feels like ${weather.feelsLike.roundToInt()}°C"

                // Humidity and wind
                binding.textViewHumidity.text = "${weather.humidity}%"
                binding.textViewWind.text = "${weather.windSpeed} m/s"

                // Load weather icon with Glide from OpenWeatherMap CDN
                val iconUrl = "${WeatherApiConfig.ICON_BASE_URL}${weather.iconCode}@4x.png"
                Glide.with(this)
                    .load(iconUrl)
                    .into(binding.imageViewWeatherIcon)
            }
        }

        // Loading state — show/hide progress bar
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        // Messages (errors) — show as Snackbar
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }
    }

    /**
     * Toolbar menu with Search and Refresh options.
     * MenuProvider is the modern replacement for onCreateOptionsMenu().
     */
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_search -> {
                        // Show a dialog with an EditText for city name input
                        showSearchDialog()
                        true
                    }
                    R.id.action_refresh -> {
                        viewModel.refresh()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /** AlertDialog with an EditText — user types city name and taps Search */
    private fun showSearchDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter city name (e.g. Tbilisi)"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Search City")
            .setView(editText)
            .setPositiveButton("Search") { _, _ ->
                val city = editText.text.toString().trim()
                if (city.isNotEmpty()) {
                    viewModel.fetchWeather(city)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
