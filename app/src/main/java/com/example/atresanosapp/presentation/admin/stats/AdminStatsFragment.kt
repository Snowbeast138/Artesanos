package com.example.atresanosapp.presentation.admin.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.data.model.Producto
import com.example.atresanosapp.databinding.FragmentAdminStatsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AdminStatsFragment : Fragment() {
    private var _binding: FragmentAdminStatsBinding? = null
    private val binding get() = _binding!!

    private var allPedidos = listOf<Pedido>()
    private val usersMap = mutableMapOf<String, String>()
    private var allProductos = listOf<Producto>()
    
    private lateinit var buyerAdapter: BuyerAdapter
    
    // Filtros
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var selectedProductId: String? = null
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTabs()
        setupBuyersRecyclerView()
        setupDateFilter()
        setupSearch()
        
        loadData()
    }

    private fun setupTabs() {
        binding.tabLayoutStats.addTab(binding.tabLayoutStats.newTab().setText("Resumen General"))
        binding.tabLayoutStats.addTab(binding.tabLayoutStats.newTab().setText("Flujo de Efectivo"))
        binding.tabLayoutStats.addTab(binding.tabLayoutStats.newTab().setText("Análisis por Producto"))
        binding.tabLayoutStats.addTab(binding.tabLayoutStats.newTab().setText("Cancelaciones"))

        binding.tabLayoutStats.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.layoutTabGeneral.visibility = View.GONE
                binding.layoutTabFlujo.visibility = View.GONE
                binding.layoutTabProducto.visibility = View.GONE
                binding.layoutTabCancelaciones.visibility = View.GONE

                when (tab?.position) {
                    0 -> binding.layoutTabGeneral.visibility = View.VISIBLE
                    1 -> binding.layoutTabFlujo.visibility = View.VISIBLE
                    2 -> binding.layoutTabProducto.visibility = View.VISIBLE
                    3 -> binding.layoutTabCancelaciones.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupBuyersRecyclerView() {
        buyerAdapter = BuyerAdapter()
        binding.rvBuyers.layoutManager = LinearLayoutManager(context)
        binding.rvBuyers.adapter = buyerAdapter
    }

    private fun setupDateFilter() {
        binding.btnDateFilter.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Selecciona rango de fechas")
                .build()
                
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedStartDate = selection.first
                selectedEndDate = selection.second
                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                val start = sdf.format(Date(selection.first))
                val end = sdf.format(Date(selection.second))
                binding.btnDateFilter.text = "$start - $end"
                applyFilters()
            }
            
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupSearch() {
        binding.svClientSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query ?: ""
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })
    }

    private fun loadData() {
        val db = FirebaseFirestore.getInstance()
        
        db.collection("usuarios").get().addOnSuccessListener { usersQuery ->
            for (doc in usersQuery.documents) {
                usersMap[doc.id] = doc.getString("nombre") ?: ""
            }
            
            db.collection("productos").get().addOnSuccessListener { prodQuery ->
                allProductos = prodQuery.map { it.toObject(Producto::class.java).copy(id = it.id) }
                setupProductSpinner()
                
                db.collection("pedidos").get().addOnSuccessListener { pedQuery ->
                    allPedidos = pedQuery.map { it.toObject(Pedido::class.java).copy(id = it.id) }
                    applyFilters()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error cargando datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupProductSpinner() {
        val productNames = mutableListOf("Todos los Productos")
        productNames.addAll(allProductos.map { it.nombre })
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, productNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProducts.adapter = adapter
        
        binding.spinnerProducts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProductId = if (position == 0) null else allProductos[position - 1].id
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun normalizeString(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "").lowercase()
    }

    private fun applyFilters() {
        var filteredList = allPedidos
        
        // 1. Filtrar por búsqueda de cliente
        if (searchQuery.isNotBlank()) {
            val q = normalizeString(searchQuery)
            filteredList = filteredList.filter { 
                val realName = if (it.nombreCliente.isNotBlank()) it.nombreCliente else (usersMap[it.idUsuario] ?: "")
                normalizeString(realName).contains(q) || it.id.lowercase().contains(q)
            }
        }
        
        // 2. Filtrar por Rango de Fechas (usando fechaCreacion)
        if (selectedStartDate != null && selectedEndDate != null) {
            filteredList = filteredList.filter {
                val time = it.fechaCreacion.toDate().time
                time in selectedStartDate!!..selectedEndDate!!
            }
        }
        
        // 3. Filtrar por Producto (Si se selecciona uno, solo mostrar pedidos que lo contengan)
        if (selectedProductId != null) {
            filteredList = filteredList.filter { p ->
                p.productos.any { it.idProducto == selectedProductId }
            }
        }

        updateUI(filteredList)
    }

    private fun updateUI(pedidos: List<Pedido>) {
        val pedidosActivos = pedidos.filter { it.estado != com.example.atresanosapp.data.model.EstadoPedido.CANCELADO }
        val pedidosCancelados = pedidos.filter { it.estado == com.example.atresanosapp.data.model.EstadoPedido.CANCELADO }

        var totalNeto = 0.0
        val uniqueProducts = mutableSetOf<String>()
        val buyersMap = mutableMapOf<String, Int>()
        val demandMap = mutableMapOf<String, Int>()

        pedidosActivos.forEach { p ->
            totalNeto += p.costoNeto
            
            val realName = if (p.nombreCliente.isNotBlank()) p.nombreCliente else (usersMap[p.idUsuario] ?: "Desconocido")
            
            p.productos.forEach { prod ->
                // Métrica de productos únicos
                uniqueProducts.add(prod.idProducto)
                
                // Métrica para PieChart de demanda
                val currentDemand = demandMap[prod.nombre] ?: 0
                demandMap[prod.nombre] = currentDemand + prod.cantidad
                
                // Si hay un producto seleccionado, solo agregamos a los compradores de ese producto
                if (selectedProductId == null || prod.idProducto == selectedProductId) {
                    val currentBuyerCount = buyersMap[realName] ?: 0
                    buyersMap[realName] = currentBuyerCount + prod.cantidad
                }
            }
        }

        // Actualizar Resumen (Solo Pedidos Activos)
        binding.tvTotalPedidos.text = pedidosActivos.size.toString()
        binding.tvTotalNeto.text = "$${String.format("%.2f", totalNeto)}"

        // Actualizar Lista de Compradores (Tab 3)
        val buyerStats = buyersMap.map { BuyerStat(it.key, it.value) }
        buyerAdapter.submitList(buyerStats)

        // Graficar Top Productos (PieChart)
        setupPieChart(demandMap)
        
        // Graficar Flujo (BarChart)
        setupCashFlowChart(pedidosActivos)
        
        // --- Actualizar Tab de Cancelaciones ---
        var totalPerdido = 0.0
        val canceladoresMap = mutableMapOf<String, Int>()
        
        pedidosCancelados.forEach { p ->
            totalPerdido += p.costoNeto
            val cancelador = if (p.canceladoPor.isNotBlank()) p.canceladoPor else "Desconocido"
            val count = canceladoresMap[cancelador] ?: 0
            canceladoresMap[cancelador] = count + 1
        }
        
        binding.tvTotalPerdido.text = "$${String.format("%.2f", totalPerdido)}"
        binding.tvTotalPedidosCancelados.text = "${pedidosCancelados.size} pedidos cancelados"
        setupCancelacionesChart(canceladoresMap)
    }

    private fun setupCancelacionesChart(canceladoresMap: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorPalette = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3")
        
        var i = 0
        canceladoresMap.entries.sortedByDescending { it.value }.take(6).forEach {
            entries.add(PieEntry(it.value.toFloat(), it.key))
            colors.add(Color.parseColor(colorPalette[i % colorPalette.size]))
            i++
        }
        
        if (entries.isEmpty()) {
            binding.pieChartCancelaciones.clear()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE
        
        val data = PieData(dataSet)
        binding.pieChartCancelaciones.data = data
        binding.pieChartCancelaciones.description.isEnabled = false
        binding.pieChartCancelaciones.invalidate()
    }

    private fun setupPieChart(demandMap: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorPalette = listOf("#4CAF50", "#FF9800", "#2196F3", "#E91E63", "#9C27B0")
        
        var i = 0
        demandMap.entries.sortedByDescending { it.value }.take(5).forEach {
            entries.add(PieEntry(it.value.toFloat(), it.key))
            colors.add(Color.parseColor(colorPalette[i % colorPalette.size]))
            i++
        }
        
        if (entries.isEmpty()) {
            binding.pieChartTopProductos.clear()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE
        
        val data = PieData(dataSet)
        binding.pieChartTopProductos.data = data
        binding.pieChartTopProductos.description.isEnabled = false
        binding.pieChartTopProductos.invalidate()
    }

    private fun setupCashFlowChart(pedidos: List<Pedido>) {
        // Agrupar por fecha simple (MM/dd)
        val grouped = pedidos.groupBy {
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(it.fechaCreacion.toDate())
        }.toSortedMap()

        val entriesBruto = ArrayList<BarEntry>()
        val entriesAbono = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        var index = 0f
        grouped.forEach { (date, list) ->
            val totalBruto = list.sumOf { it.costoNeto }
            val totalAbonado = list.sumOf { it.montoAbonado }
            
            entriesBruto.add(BarEntry(index, totalBruto.toFloat()))
            entriesAbono.add(BarEntry(index, totalAbonado.toFloat()))
            labels.add(date)
            index += 1f
        }

        if (entriesBruto.isEmpty()) {
            binding.barChartFlujo.clear()
            return
        }

        val setBruto = BarDataSet(entriesBruto, "Deuda / Total")
        setBruto.color = Color.parseColor("#F44336") // Rojo
        
        val setAbono = BarDataSet(entriesAbono, "Abonado")
        setAbono.color = Color.parseColor("#4CAF50") // Verde

        val data = BarData(setBruto, setAbono)
        
        // Configurar el chart de barras agrupadas
        val groupSpace = 0.08f
        val barSpace = 0.03f
        val barWidth = 0.43f
        
        data.barWidth = barWidth
        binding.barChartFlujo.data = data
        binding.barChartFlujo.groupBars(0f, groupSpace, barSpace)
        
        val xAxis = binding.barChartFlujo.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.setCenterAxisLabels(true)
        xAxis.granularity = 1f
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = labels.size.toFloat()

        binding.barChartFlujo.description.isEnabled = false
        binding.barChartFlujo.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
