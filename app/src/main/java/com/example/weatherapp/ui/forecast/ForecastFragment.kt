package com.example.weatherapp.ui.forecast

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.databinding.FragmentForecastBinding
import com.example.weatherapp.viewmodel.WeatherViewModel

/**
 * FORECAST FRAGMENT — Shows the 7-day forecast as a scrollable list
 *
 * Uses the shared WeatherViewModel — data is already loaded by HomeFragment.
 * The RecyclerView + ForecastAdapter displays the list of ForecastDay objects.
 */
class ForecastFragment : Fragment() {

    private var _binding: FragmentForecastBinding? = null
    private val binding get() = _binding!!

    // Same ViewModel instance shared with HomeFragment
    private val viewModel: WeatherViewModel by activityViewModels()
    private lateinit var forecastAdapter: ForecastAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        forecastAdapter = ForecastAdapter()
        binding.recyclerViewForecast.apply {
            adapter = forecastAdapter
            // LinearLayoutManager: one item per row, vertical scroll
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true) // Optimisation: item size doesn't change
        }
    }

    private fun observeViewModel() {
        // Observe the forecast LiveData — RecyclerView updates automatically via submitList
        viewModel.forecast.observe(viewLifecycleOwner) { forecastList ->
            forecastAdapter.submitList(forecastList)

            // Update the toolbar subtitle with the city name
            viewModel.currentCity.value?.let { city ->
                binding.textViewForecastTitle.text = "7-Day Forecast — $city"
            }

            // Show/hide empty state
            binding.textViewEmpty.visibility =
                if (forecastList.isEmpty()) View.VISIBLE else View.GONE
        }

        // Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
