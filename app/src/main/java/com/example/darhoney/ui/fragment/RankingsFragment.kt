package com.example.darhoney.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.darhoney.databinding.FragmentRankingsBinding
import com.example.darhoney.databinding.ItemRankingSectionBinding
import com.example.darhoney.ui.MainViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.adapter.RestaurantAdapter
import com.example.darhoney.utils.RankingsCalculator

class RankingsFragment : Fragment() {

    private var _binding: FragmentRankingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { ViewModelFactory.instance }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRankingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewModel.restaurants.observe(viewLifecycleOwner) { restaurants ->
            binding.containerRankings.removeAllViews()
            RankingsCalculator.generate(restaurants).forEach { ranking ->
                if (ranking.restaurants.isEmpty()) return@forEach
                val sectionBinding = ItemRankingSectionBinding.inflate(layoutInflater, binding.containerRankings, false)
                sectionBinding.tvTitle.text = "${ranking.emoji} ${ranking.title}"

                val adapter = RestaurantAdapter { }
                sectionBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                sectionBinding.recyclerView.adapter = adapter
                sectionBinding.recyclerView.isNestedScrollingEnabled = false
                adapter.submitList(ranking.restaurants)

                binding.containerRankings.addView(sectionBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(@Suppress("UNUSED_PARAMETER") coupleId: String) = RankingsFragment()
    }
}
