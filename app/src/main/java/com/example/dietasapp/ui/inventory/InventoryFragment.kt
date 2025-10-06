package com.example.dietasapp.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.dietasapp.R
import com.example.dietasapp.databinding.FragmentInventoryBinding
import com.google.android.material.tabs.TabLayoutMediator

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: InventoryPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupFab()
    }

    private fun setupViewPager() {
        pagerAdapter = InventoryPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Conectar TabLayout con ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_animales)
                1 -> getString(R.string.tab_insumos)
                2 -> getString(R.string.tab_stock)
                else -> ""
            }
        }.attach()

        // Cambiar texto del FAB según el tab
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateFabText(position)
            }
        })
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            val currentTab = binding.viewPager.currentItem
            when (currentTab) {
                0 -> showAddAnimalDialog()
                1 -> showAddInsumoDialog()
                2 -> showAddStockDialog()
            }
        }
    }

    private fun updateFabText(position: Int) {
        binding.fabAdd.text = when (position) {
            0 -> getString(R.string.agregar_animal)
            1 -> getString(R.string.agregar_insumo)
            2 -> getString(R.string.agregar_stock)
            else -> ""
        }
    }

    private fun showAddAnimalDialog() {
        val fragment = childFragmentManager.fragments.firstOrNull {
            it is AnimalesTabFragment
        } as? AnimalesTabFragment
        fragment?.showAddDialog()
    }

    private fun showAddInsumoDialog() {
        val fragment = childFragmentManager.fragments.firstOrNull {
            it is InsumosTabFragment
        } as? InsumosTabFragment
        fragment?.showAddDialog()
    }

    private fun showAddStockDialog() {
        val fragment = childFragmentManager.fragments.firstOrNull {
            it is StockTabFragment
        } as? StockTabFragment
        fragment?.showAddDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Adapter para ViewPager2
    private class InventoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AnimalesTabFragment()
                1 -> InsumosTabFragment()
                2 -> StockTabFragment()
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
}