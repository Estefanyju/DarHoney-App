package com.example.darhoney.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.darhoney.databinding.FragmentTimelineBinding
import com.example.darhoney.ui.MainViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.adapter.TimelineAdapter
import com.example.darhoney.utils.applyStatusBarPadding

class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { ViewModelFactory.instance }
    private lateinit var adapter: TimelineAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.headerBar.applyStatusBarPadding()
        adapter = TimelineAdapter()
        binding.recyclerView.adapter = adapter

        viewModel.restaurants.observe(viewLifecycleOwner) { restaurants ->
            val entries = restaurants.flatMap { restaurant ->
                restaurant.visits.map { visit ->
                    TimelineAdapter.TimelineEntry(
                        restaurant = restaurant,
                        visitDate = visit.date,
                        rating = visit.ratings.average(),
                        comments = visit.memories.ifBlank { visit.comments },
                        photoUrl = visit.photoUrls.firstOrNull()
                    )
                }
            }.sortedByDescending { it.visitDate }
            adapter.submitList(entries)
            binding.emptyView.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(@Suppress("UNUSED_PARAMETER") coupleId: String) = TimelineFragment()
    }
}
