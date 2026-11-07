package com.example.darhoney.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.darhoney.R
import com.example.darhoney.data.model.Restaurant
import com.example.darhoney.databinding.FragmentHomeBinding
import com.example.darhoney.ui.MainViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.adapter.RestaurantAdapter
import com.example.darhoney.ui.restaurant.AddRestaurantActivity
import com.example.darhoney.ui.restaurant.RestaurantDetailActivity
import com.example.darhoney.ui.search.SearchActivity
import com.example.darhoney.utils.applyStatusBarPadding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { ViewModelFactory.instance }
    private lateinit var adapter: RestaurantAdapter
    private var coupleId: String = ""
    private var allRestaurants = listOf<Restaurant>()
    private var currentSort = SortOption.NAME

    private enum class SortOption {
        NAME, RATING, DATE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coupleId = arguments?.getString(ARG_COUPLE_ID) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.headerBar.applyStatusBarPadding()

        adapter = RestaurantAdapter { restaurant ->
            startActivity(Intent(requireContext(), RestaurantDetailActivity::class.java).apply {
                putExtra(RestaurantDetailActivity.EXTRA_COUPLE_ID, coupleId)
                putExtra(RestaurantDetailActivity.EXTRA_RESTAURANT_ID, restaurant.id)
            })
        }

        binding.recyclerView.adapter = adapter

        viewModel.restaurants.observe(viewLifecycleOwner) { restaurants ->
            allRestaurants = restaurants
            applySort()
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddRestaurantActivity::class.java).apply {
                putExtra(AddRestaurantActivity.EXTRA_COUPLE_ID, coupleId)
            })
        }

        binding.btnSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java).apply {
                putExtra(SearchActivity.EXTRA_COUPLE_ID, coupleId)
            })
        }

        binding.btnSort.setOnClickListener { anchor ->
            showSortMenu(anchor)
        }
    }

    private fun showSortMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, MENU_SORT_NAME, 0, getString(R.string.sort_by_name))
            menu.add(0, MENU_SORT_RATING, 1, getString(R.string.sort_by_rating))
            menu.add(0, MENU_SORT_DATE, 2, getString(R.string.sort_by_date))
            menu.add(0, MENU_VIEW_RANKINGS, 3, getString(R.string.view_rankings))
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_SORT_NAME -> {
                        currentSort = SortOption.NAME
                        applySort()
                        true
                    }
                    MENU_SORT_RATING -> {
                        currentSort = SortOption.RATING
                        applySort()
                        true
                    }
                    MENU_SORT_DATE -> {
                        currentSort = SortOption.DATE
                        applySort()
                        true
                    }
                    MENU_VIEW_RANKINGS -> {
                        openRankings()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun applySort() {
        val sorted = when (currentSort) {
            SortOption.NAME -> allRestaurants.sortedBy { it.name.lowercase() }
            SortOption.RATING -> allRestaurants.sortedByDescending { it.averageRating }
            SortOption.DATE -> allRestaurants.sortedByDescending { it.latestVisit?.date ?: 0L }
        }
        adapter.submitList(sorted)
        binding.emptyView.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openRankings() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, RankingsFragment.newInstance(coupleId))
            .addToBackStack("rankings")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COUPLE_ID = "couple_id"
        private const val MENU_SORT_NAME = 1
        private const val MENU_SORT_RATING = 2
        private const val MENU_SORT_DATE = 3
        private const val MENU_VIEW_RANKINGS = 4

        fun newInstance(coupleId: String) = HomeFragment().apply {
            arguments = Bundle().apply { putString(ARG_COUPLE_ID, coupleId) }
        }
    }
}
