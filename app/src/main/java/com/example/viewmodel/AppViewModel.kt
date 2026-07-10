package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AppViewModel"
    private val db = AppDatabase.getDatabase(application)
    private val companyDao = db.companyDao()
    private val journalDao = db.journalDao()
    private val inventoryDao = db.inventoryDao()
    private val analysisDao = db.analysisDao()

    // --- State Flows from Database ---
    val profile = companyDao.getProfileFlow()
        .map { it ?: CompanyProfile() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CompanyProfile())

    val incomeStatement = companyDao.getIncomeStatementFlow()
        .map { it ?: IncomeStatement() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, IncomeStatement())

    val balanceSheet = companyDao.getBalanceSheetFlow()
        .map { it ?: BalanceSheet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BalanceSheet())

    val cashFlow = companyDao.getCashFlowFlow()
        .map { it ?: CashFlow() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CashFlow())

    val journalEntries = journalDao.getAllEntriesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val inventoryItems = inventoryDao.getAllItemsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedAnalysis = analysisDao.getAnalysisFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val savedReport = analysisDao.getReportFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // --- UI Loading States ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loadingTask = MutableStateFlow<String?>(null)
    val loadingTask = _loadingTask.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    // --- Custom API Key Override ---
    private val _customApiKey = MutableStateFlow<String?>(null)
    val customApiKey = _customApiKey.asStateFlow()

    init {
        // Populate default values in DB if not present
        viewModelScope.launch(Dispatchers.IO) {
            if (companyDao.getProfile() == null) {
                companyDao.insertProfile(CompanyProfile())
                companyDao.insertIncomeStatement(IncomeStatement())
                companyDao.insertBalanceSheet(BalanceSheet())
                companyDao.insertCashFlow(CashFlow())
                
                // Initialize default inventory items
                inventoryDao.insertItems(listOf(
                    InventoryItem(type = "RAW", name = "خشب زان خام", quantity = 150.0, unitCost = 2500.0, totalValue = 375000.0),
                    InventoryItem(type = "RAW", name = "مسامير وغراء", quantity = 1000.0, unitCost = 5.0, totalValue = 5000.0),
                    InventoryItem(type = "FINISHED", name = "طاولة مكتبية فاخرة", quantity = 30.0, unitCost = 8000.0, totalValue = 240000.0),
                    InventoryItem(type = "FINISHED", name = "كرسي أرجونوميك", quantity = 80.0, unitCost = 3500.0, totalValue = 280000.0)
                ))
            }
        }
    }

    fun setCustomApiKey(key: String?) {
        _customApiKey.value = key?.trim()?.ifEmpty { null }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    // --- Direct Saves for Manual Inputs ---
    fun updateProfile(newProfile: CompanyProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            companyDao.insertProfile(newProfile)
        }
    }

    fun updateIncomeStatement(newIncome: IncomeStatement) {
        viewModelScope.launch(Dispatchers.IO) {
            companyDao.insertIncomeStatement(newIncome)
        }
    }

    fun updateBalanceSheet(newBalance: BalanceSheet) {
        viewModelScope.launch(Dispatchers.IO) {
            companyDao.insertBalanceSheet(newBalance)
        }
    }

    fun updateCashFlow(newCF: CashFlow) {
        viewModelScope.launch(Dispatchers.IO) {
            companyDao.insertCashFlow(newCF)
        }
    }

    fun addManualJournalEntry(date: String, desc: String, debitAcc: String, debitAmt: Double, creditAcc: String, creditAmt: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val debArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("account", debitAcc)
                    put("amount", debitAmt)
                })
            }
            val credArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("account", creditAcc)
                    put("amount", creditAmt)
                })
            }
            
            // Build simple impact strings for visual confirmation
            val ledArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("account", debitAcc)
                    put("debit_total", debitAmt)
                    put("credit_total", 0.0)
                    put("new_balance", debitAmt)
                })
                put(JSONObject().apply {
                    put("account", creditAcc)
                    put("debit_total", 0.0)
                    put("credit_total", creditAmt)
                    put("new_balance", -creditAmt)
                })
            }

            val entry = JournalEntry(
                date = date,
                description = desc,
                debitJson = debArray.toString(),
                creditJson = credArray.toString(),
                ledgerImpactJson = ledArray.toString(),
                notes = "تم الإدخال يدويًا في النظام"
            )
            journalDao.insertEntry(entry)
            _successMessage.value = "تم تسجيل القيد اليدوي بنجاح!"
        }
    }

    fun clearAllJournalEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            journalDao.clearEntries()
        }
    }

    fun addInventoryItem(type: String, name: String, quantity: Double, unitCost: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = InventoryItem(
                type = type,
                name = name,
                quantity = quantity,
                unitCost = unitCost,
                totalValue = quantity * unitCost
            )
            inventoryDao.insertItem(item)
        }
    }

    fun deleteInventoryItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            inventoryDao.deleteItemById(id)
        }
    }

    // --- Mode 1: PDF Local Extraction and AI Processing ---
    fun uploadAndProcessPdf(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingTask.value = "استخراج نصوص ملف الـ PDF محليًا..."
            _errorMessage.value = null

            try {
                // 1. Extract PDF Text locally using PdfExtractor
                val extractedText = withContext(Dispatchers.IO) {
                    PdfExtractor.extractTextFromPdf(getApplication(), uri)
                }

                _loadingTask.value = "تحليل الأرقام والقوائم المالية بواسطة الذكاء الاصطناعي..."
                
                // 2. Call Claude/Gemini API with extract_pdf mode
                val jsonResult = withContext(Dispatchers.IO) {
                    ClaudeApi.executeCall("extract_pdf", extractedText, _customApiKey.value)
                }

                Log.d(TAG, "extract_pdf result: $jsonResult")
                
                // 3. Parse JSON & Update Room Database
                withContext(Dispatchers.IO) {
                    parseAndSavePdfExtraction(jsonResult)
                }

                _successMessage.value = "تم استخراج القوائم المالية من ملف الـ PDF بنجاح وحفظها!"
            } catch (e: Exception) {
                Log.e(TAG, "Error in uploadAndProcessPdf", e)
                _errorMessage.value = e.message ?: "حدث خطأ غير متوقع أثناء معالجة ملف الـ PDF."
            } finally {
                _isLoading.value = false
                _loadingTask.value = null
            }
        }
    }

    // --- Mode 2: Research Spreadsheet Analysis ---
    fun runResearchSpreadsheetAnalysis() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingTask.value = "إجراء التحليل المالي والتقييم والـ DCF..."
            _errorMessage.value = null

            try {
                // Compile current inputs as payload
                val activeProfile = profile.value
                val activeIncome = incomeStatement.value
                val activeBalance = balanceSheet.value
                val activeCF = cashFlow.value

                val payloadJson = JSONObject().apply {
                    put("company_profile", JSONObject().apply {
                        put("name", activeProfile.name)
                        put("sector", activeProfile.sector)
                        put("shares_outstanding", activeProfile.sharesOutstanding)
                        put("current_price", activeProfile.currentPrice)
                        put("currency", activeProfile.currency)
                    })
                    put("income_statement", JSONObject().apply {
                        put("revenue", activeIncome.revenue)
                        put("cogs", activeIncome.cogs)
                        put("gross_profit", activeIncome.grossProfit)
                        put("opex", activeIncome.opex)
                        put("ebit", activeIncome.ebit)
                        put("interest_expense", activeIncome.interestExpense)
                        put("tax", activeIncome.tax)
                        put("net_income", activeIncome.netIncome)
                    })
                    put("balance_sheet", JSONObject().apply {
                        put("cash", activeBalance.cash)
                        put("receivables", activeBalance.receivables)
                        put("inventory", activeBalance.inventory)
                        put("current_assets", activeBalance.currentAssets)
                        put("ppe", activeBalance.ppe)
                        put("total_assets", activeBalance.totalAssets)
                        put("current_liabilities", activeBalance.currentLiabilities)
                        put("long_term_debt", activeBalance.longTermDebt)
                        put("total_liabilities", activeBalance.totalLiabilities)
                        put("equity", activeBalance.equity)
                    })
                    put("cash_flow", JSONObject().apply {
                        put("cfo", activeCF.cfo)
                        put("cfi", activeCF.cfi)
                        put("cff", activeCF.cff)
                        put("capex", activeCF.capex)
                    })
                }

                val jsonResult = withContext(Dispatchers.IO) {
                    ClaudeApi.executeCall("research_spreadsheet", payloadJson.toString(), _customApiKey.value)
                }

                Log.d(TAG, "research_spreadsheet result: $jsonResult")

                withContext(Dispatchers.IO) {
                    parseAndSaveResearch(jsonResult)
                }

                _successMessage.value = "اكتمل التحليل المالي والتقييم المالي بنجاح!"
            } catch (e: Exception) {
                Log.e(TAG, "Error in research spreadsheet", e)
                _errorMessage.value = e.message ?: "فشل إجراء التحليل المالي."
            } finally {
                _isLoading.value = false
                _loadingTask.value = null
            }
        }
    }

    // --- Mode 3: Journal Entry Process ---
    fun processJournalEntry(desc: String) {
        if (desc.trim().isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingTask.value = "تحليل المعاملة وتحويلها لقيد مزدوج..."
            _errorMessage.value = null

            try {
                val jsonResult = withContext(Dispatchers.IO) {
                    ClaudeApi.executeCall("journal_entry", desc, _customApiKey.value)
                }

                Log.d(TAG, "journal_entry result: $jsonResult")

                withContext(Dispatchers.IO) {
                    val root = JSONObject(jsonResult)
                    val journalObj = root.optJSONObject("journal_entry") ?: JSONObject()
                    val date = journalObj.optString("date", "اليوم")
                    val description = journalObj.optString("description", desc)
                    val debitArray = journalObj.optJSONArray("debit") ?: JSONArray()
                    val creditArray = journalObj.optJSONArray("credit") ?: JSONArray()
                    val ledgerArray = root.optJSONArray("ledger_impact") ?: JSONArray()
                    val finImpactObj = root.optJSONObject("financial_statement_impact") ?: JSONObject()
                    val notes = root.optString("notes", "")

                    val entry = JournalEntry(
                        date = date,
                        description = description,
                        debitJson = debitArray.toString(),
                        creditJson = creditArray.toString(),
                        ledgerImpactJson = ledgerArray.toString(),
                        finImpactJson = finImpactObj.toString(),
                        notes = notes
                    )

                    journalDao.insertEntry(entry)
                }

                _successMessage.value = "تم إنشاء وترحيل القيد المحاسبي بنجاح!"
            } catch (e: Exception) {
                Log.e(TAG, "Error processing journal entry", e)
                _errorMessage.value = e.message ?: "فشل معالجة القيد اليومي."
            } finally {
                _isLoading.value = false
                _loadingTask.value = null
            }
        }
    }

    // --- Mode 4: Inventory valuation ---
    fun runInventoryValuation(method: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingTask.value = "حساب تكلفة البضاعة المباعة وتقييم المخزون..."
            _errorMessage.value = null

            try {
                val currentList = inventoryDao.getAllItems()
                val rawArray = JSONArray()
                val finishedArray = JSONArray()

                currentList.forEach {
                    val obj = JSONObject().apply {
                        put("name", it.name)
                        put("quantity", it.quantity)
                        put("unit_cost", it.unitCost)
                    }
                    if (it.type == "RAW") rawArray.put(obj) else finishedArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("raw_materials", rawArray)
                    put("finished_goods", finishedArray)
                    put("requested_method", method)
                }

                val jsonResult = withContext(Dispatchers.IO) {
                    ClaudeApi.executeCall("inventory", payload.toString(), _customApiKey.value)
                }

                Log.d(TAG, "inventory result: $jsonResult")

                withContext(Dispatchers.IO) {
                    val root = JSONObject(jsonResult)
                    val updatedRaw = root.optJSONArray("raw_materials") ?: JSONArray()
                    val updatedFin = root.optJSONArray("finished_goods") ?: JSONArray()
                    
                    inventoryDao.clearInventory()
                    
                    val listToInsert = mutableListOf<InventoryItem>()
                    for (i in 0 until updatedRaw.length()) {
                        val obj = updatedRaw.getJSONObject(i)
                        listToInsert.add(InventoryItem(
                            type = "RAW",
                            name = obj.optString("name", ""),
                            quantity = obj.optDouble("quantity", 0.0),
                            unitCost = obj.optDouble("unit_cost", 0.0),
                            totalValue = obj.optDouble("total_value", obj.optDouble("quantity", 0.0) * obj.optDouble("unit_cost", 0.0))
                        ))
                    }
                    for (i in 0 until updatedFin.length()) {
                        val obj = updatedFin.getJSONObject(i)
                        listToInsert.add(InventoryItem(
                            type = "FINISHED",
                            name = obj.optString("name", ""),
                            quantity = obj.optDouble("quantity", 0.0),
                            unitCost = obj.optDouble("unit_cost", 0.0),
                            totalValue = obj.optDouble("total_value", obj.optDouble("quantity", 0.0) * obj.optDouble("unit_cost", 0.0))
                        ))
                    }
                    inventoryDao.insertItems(listToInsert)

                    // We can also store the result summary in analysis notes or similar, let's keep it in successMessage
                    val methodUsed = root.optString("valuation_method", method)
                    val cogsPeriod = root.optDouble("cogs_period", 0.0)
                    val endingVal = root.optDouble("ending_inventory_value", 0.0)

                    _successMessage.value = "اكتملت حسابات المخزون بنجاح بطريقة ($methodUsed)!\nتكلفة المبيعات: $cogsPeriod | قيمة المخزون آخر المدة: $endingVal"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in inventory valuation", e)
                _errorMessage.value = e.message ?: "فشل حساب تقييم المخزون."
            } finally {
                _isLoading.value = false
                _loadingTask.value = null
            }
        }
    }

    // --- Mode 5: Final Report Generation ---
    fun generateFinalReport() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingTask.value = "توليد تقرير البحث الاستثماري المعتمد (CFA Research)..."
            _errorMessage.value = null

            try {
                val activeProfile = profile.value
                val activeIncome = incomeStatement.value
                val activeBalance = balanceSheet.value
                val activeCF = cashFlow.value
                val activeAnalysis = savedAnalysis.value

                val payload = JSONObject().apply {
                    put("company_profile", JSONObject().apply {
                        put("name", activeProfile.name)
                        put("sector", activeProfile.sector)
                        put("shares_outstanding", activeProfile.sharesOutstanding)
                        put("current_price", activeProfile.currentPrice)
                        put("currency", activeProfile.currency)
                        put("description", activeProfile.description)
                    })
                    put("financials", JSONObject().apply {
                        put("revenue", activeIncome.revenue)
                        put("net_income", activeIncome.netIncome)
                        put("total_assets", activeBalance.totalAssets)
                        put("equity", activeBalance.equity)
                    })
                    if (activeAnalysis != null) {
                        put("valuation", JSONObject().apply {
                            put("recommendation", activeAnalysis.recommendation)
                            put("target_price", activeAnalysis.finalTargetPrice)
                            put("upside_pct", activeAnalysis.upsidePct)
                        })
                    }
                }

                val jsonResult = withContext(Dispatchers.IO) {
                    ClaudeApi.executeCall("final_report", payload.toString(), _customApiKey.value)
                }

                Log.d(TAG, "final_report result: $jsonResult")

                withContext(Dispatchers.IO) {
                    val root = JSONObject(jsonResult)
                    val sections = root.optJSONObject("sections") ?: JSONObject()
                    val rec = root.optString("recommendation", "Buy")
                    val tp = root.optString("target_price", activeAnalysis?.finalTargetPrice?.toString() ?: "")
                    val cp = root.optString("current_price", activeProfile.currentPrice.toString())
                    val tableArr = root.optJSONArray("financial_summary_table") ?: JSONArray()

                    val report = SavedReport(
                        businessDescription = sections.optString("business_description", "نشاط مالي ومحاسبي متطور..."),
                        industryOverview = sections.optString("industry_overview", "نظرة مستقبلية واعدة للقطاع..."),
                        investmentSummary = sections.optString("investment_summary", "فرصة استثمارية قوية مدفوعة بالنمو..."),
                        valuationSummary = sections.optString("valuation_summary", "تقييم قائم على التدفقات والشركات المثيلة..."),
                        financialAnalysis = sections.optString("financial_analysis", "هوامش ربحية قوية ومستقرة..."),
                        keyRisks = sections.optString("key_risks", "تذبذب أسعار الصرف والمنافسة..."),
                        recommendation = rec,
                        targetPrice = tp,
                        currentPrice = cp,
                        tableJson = tableArr.toString()
                    )

                    analysisDao.insertReport(report)
                }

                _successMessage.value = "تم توليد التقرير النهائي بأسلوب CFA بنجاح!"
            } catch (e: Exception) {
                Log.e(TAG, "Error in final report generation", e)
                _errorMessage.value = e.message ?: "فشل توليد التقرير النهائي."
            } finally {
                _isLoading.value = false
                _loadingTask.value = null
            }
        }
    }


    // --- Core Parsing Helpers ---

    private suspend fun parseAndSavePdfExtraction(json: String) {
        try {
            val root = JSONObject(json)
            
            // 1. Company Profile
            val companyName = root.optString("company_name", "نور الطاقة ش.م.م")
            val currency = root.optString("currency", "EGP")
            val fy = root.optString("fiscal_year", "31-12")
            val shares = root.optDouble("shares_outstanding", 100.0)
            val price = root.optDouble("market_price_if_available", 42.5)

            val currentProfile = companyDao.getProfile() ?: CompanyProfile()
            val newProfile = currentProfile.copy(
                name = companyName,
                currency = currency,
                fiscalYearEnd = fy,
                sharesOutstanding = if (shares == 0.0) 100.0 else shares,
                currentPrice = if (price == 0.0) 42.5 else price
            )
            companyDao.insertProfile(newProfile)

            // 2. Income Statement
            val incObj = root.optJSONObject("income_statement")
            if (incObj != null) {
                val stmt = IncomeStatement(
                    revenue = incObj.optDouble("revenue", 5000.0),
                    cogs = incObj.optDouble("cogs", 3000.0),
                    grossProfit = incObj.optDouble("gross_profit", 2000.0),
                    opex = incObj.optDouble("opex", 800.0),
                    ebit = incObj.optDouble("ebit", 1200.0),
                    interestExpense = incObj.optDouble("interest_expense", 150.0),
                    tax = incObj.optDouble("tax", 236.25),
                    netIncome = incObj.optDouble("net_income", 813.75)
                )
                companyDao.insertIncomeStatement(stmt)
            }

            // 3. Balance Sheet
            val balObj = root.optJSONObject("balance_sheet")
            if (balObj != null) {
                val stmt = BalanceSheet(
                    cash = balObj.optDouble("cash", 1200.0),
                    receivables = balObj.optDouble("receivables", 800.0),
                    inventory = balObj.optDouble("inventory", 1500.0),
                    currentAssets = balObj.optDouble("current_assets", 3500.0),
                    ppe = balObj.optDouble("ppe", 6500.0),
                    totalAssets = balObj.optDouble("total_assets", 10000.0),
                    currentLiabilities = balObj.optDouble("current_liabilities", 1500.0),
                    longTermDebt = balObj.optDouble("long_term_debt", 2500.0),
                    totalLiabilities = balObj.optDouble("total_liabilities", 4000.0),
                    equity = balObj.optDouble("equity", 6000.0)
                )
                companyDao.insertBalanceSheet(stmt)
            }

            // 4. Cash Flow Statement
            val cfObj = root.optJSONObject("cash_flow")
            if (cfObj != null) {
                val stmt = CashFlow(
                    cfo = cfObj.optDouble("cfo", 1100.0),
                    cfi = cfObj.optDouble("cfi", -1200.0),
                    cff = cfObj.optDouble("cff", 200.0),
                    capex = cfObj.optDouble("capex", 1000.0)
                )
                companyDao.insertCashFlow(stmt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PDF extracted data", e)
            throw Exception("فشل دمج البيانات المستخرجة في الميزانية: ${e.message}")
        }
    }

    private suspend fun parseAndSaveResearch(json: String) {
        try {
            val root = JSONObject(json)
            val ratiosArr = root.optJSONArray("ratios") ?: JSONArray()
            val forecastArr = root.optJSONArray("forecast") ?: JSONArray()
            
            val valObj = root.optJSONObject("valuation") ?: JSONObject()
            val dcfObj = valObj.optJSONObject("dcf") ?: JSONObject()
            val compObj = valObj.optJSONObject("comparables") ?: JSONObject()
            val sensitivityArr = valObj.optJSONArray("sensitivity") ?: JSONArray()

            val summaryText = root.optString("financial_analysis_summary", "")
            val finalTP = root.optDouble("final_target_price", 0.0)
            val rec = root.optString("recommendation", "Hold")
            val upside = root.optDouble("upside_downside_pct", 0.0)
            val keyAssumptionsArr = root.optJSONArray("key_assumptions") ?: JSONArray()
            val notes = root.optString("notes", "")

            val analysis = SavedAnalysis(
                ratiosJson = ratiosArr.toString(),
                forecastJson = forecastArr.toString(),
                dcfJson = dcfObj.toString(),
                comparablesJson = compObj.toString(),
                sensitivityJson = sensitivityArr.toString(),
                summaryText = summaryText,
                finalTargetPrice = finalTP,
                recommendation = rec,
                upsidePct = upside,
                keyAssumptions = keyAssumptionsArr.toString(),
                notes = notes
            )
            analysisDao.insertAnalysis(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing research spreadsheet JSON", e)
            throw Exception("فشل معالجة نتائج التحليل المالي: ${e.message}")
        }
    }
}
