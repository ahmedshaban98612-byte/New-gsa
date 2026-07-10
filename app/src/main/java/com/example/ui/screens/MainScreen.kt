package com.example.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.viewmodel.AppViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    
    // --- Collect States ---
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val income by viewModel.incomeStatement.collectAsStateWithLifecycle()
    val balance by viewModel.balanceSheet.collectAsStateWithLifecycle()
    val cashFlow by viewModel.cashFlow.collectAsStateWithLifecycle()
    val journals by viewModel.journalEntries.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val analysis by viewModel.savedAnalysis.collectAsStateWithLifecycle()
    val report by viewModel.savedReport.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadingTask by viewModel.loadingTask.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val customKey by viewModel.customApiKey.collectAsStateWithLifecycle()

    // --- Show Toast on success/error ---
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSuccess()
        }
    }

    // --- PDF File picker launcher ---
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadAndProcessPdf(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "د",
                                color = MaterialTheme.colorScheme.background,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "دفتر المحاسبة الذكية والبحوث الاستثمارية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    var showKeyDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showKeyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = "تهيئة مفتاح API",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (showKeyDialog) {
                        var tempKey by remember { mutableStateOf(customKey ?: "") }
                        AlertDialog(
                            onDismissRequest = { showKeyDialog = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.setCustomApiKey(tempKey)
                                        showKeyDialog = false
                                        Toast.makeText(context, "تم حفظ مفتاح API المخصص بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("حفظ")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showKeyDialog = false }) {
                                    Text("إلغاء")
                                }
                            },
                            title = { Text("مفتاح API المخصص (Claude / Gemini)") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "لتجنب انقطاع الخدمة، يمكنك إدخال مفتاح Anthropic Claude (يبدأ بـ sk-) أو مفتاح Gemini الخاص بك مباشرة هنا:",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedTextField(
                                        value = tempKey,
                                        onValueChange = { tempKey = it },
                                        placeholder = { Text("sk-or AIza...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // One Single Connected Scrollable Layout (Mandatory Constraint)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Intro Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "منصة التحليل والتقييم والقيود المحاسبية الذكية مدمجة بالذكاء الاصطناعي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 1. بيانات الشركة (Company Profile)
                item {
                    ProfileSection(
                        profile = profile,
                        onSave = { updated -> viewModel.updateProfile(updated) }
                    )
                }

                // 2. رفع القوائم المالية (PDF Upload / Local Extraction)
                item {
                    PdfUploadSection(
                        onUploadClick = { pdfPickerLauncher.launch("application/pdf") },
                        companyName = profile.name
                    )
                }

                // 3. القوائم المالية القابلة للتعديل (Financial Statements)
                item {
                    FinancialStatementsSection(
                        income = income,
                        balance = balance,
                        cashFlow = cashFlow,
                        onSave = { inc, bal, cf ->
                            viewModel.updateIncomeStatement(inc)
                            viewModel.updateBalanceSheet(bal)
                            viewModel.updateCashFlow(cf)
                            Toast.makeText(context, "تم حفظ القوائم المالية وتحديث الميزانية بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 4. التحليل المالي والتقييم (Analysis & Valuation)
                item {
                    AnalysisSection(
                        analysis = analysis,
                        profile = profile,
                        onAnalyzeClick = { viewModel.runResearchSpreadsheetAnalysis() }
                    )
                }

                // 5. اليومية والأستاذ (Journal & Ledger)
                item {
                    JournalSection(
                        entries = journals,
                        onAddEntry = { desc -> viewModel.processJournalEntry(desc) },
                        onManualEntry = { date, desc, drAcc, drAmt, crAcc, crAmt ->
                            viewModel.addManualJournalEntry(date, desc, drAcc, drAmt, crAcc, crAmt)
                        },
                        onClearAll = { viewModel.clearAllJournalEntries() }
                    )
                }

                // 6. المخزون (Inventory Section)
                item {
                    InventorySection(
                        items = inventory,
                        onAddItem = { type, name, qty, cost ->
                            viewModel.addInventoryItem(type, name, qty, cost)
                        },
                        onDeleteItem = { id -> viewModel.deleteInventoryItem(id) },
                        onRunValuation = { method -> viewModel.runInventoryValuation(method) }
                    )
                }

                // 7. التقرير النهائي والـ PDF Export (CFA Report)
                item {
                    FinalReportSection(
                        report = report,
                        profile = profile,
                        onGenerate = { viewModel.generateFinalReport() },
                        onExportPdf = { rpt ->
                            PdfExporter.exportReportAsPdf(context, rpt, profile)
                        }
                    )
                }
            }

            // Global Loading Overlay with Arabic indicators
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = loadingTask ?: "جاري معالجة البيانات...",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "يرجى الانتظار، يتطلب التحليل بضع ثوانٍ للوصول لنتائج متزنة...",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 1: Profile UI
// ==========================================
@Composable
fun ProfileSection(profile: CompanyProfile, onSave: (CompanyProfile) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    
    var name by remember(profile) { mutableStateOf(profile.name) }
    var sector by remember(profile) { mutableStateOf(profile.sector) }
    var shares by remember(profile) { mutableStateOf(profile.sharesOutstanding.toString()) }
    var price by remember(profile) { mutableStateOf(profile.currentPrice.toString()) }
    var currency by remember(profile) { mutableStateOf(profile.currency) }
    var fye by remember(profile) { mutableStateOf(profile.fiscalYearEnd) }
    var desc by remember(profile) { mutableStateOf(profile.description) }

    EditorialCard(
        title = "بيانات ملف الشركة",
        subtitle = "الملف التعريفي النشط لتقدير القيمة العادلة والأبحاث",
        icon = Icons.Default.Business
    ) {
        if (!isEditing) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("اسم الشركة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(profile.name, style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("القطاع الاستثماري:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(profile.sector, style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("عدد الأسهم المصدرة (مليون):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(profile.sharesOutstanding.toString(), style = MaterialTheme.typography.labelSmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("سعر السهم الحالي:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("${profile.currentPrice} ${profile.currency}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("نهاية السنة المالية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(profile.fiscalYearEnd, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("وصف النشاط:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(profile.description, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("edit_profile_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل ملف البيانات")
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشركة") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("القطاع") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shares,
                        onValueChange = { shares = it },
                        label = { Text("الأسهم (مليون)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("السعر الحالي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("العملة") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fye,
                        onValueChange = { fye = it },
                        label = { Text("نهاية السنة المالية") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("وصف نشاط الشركة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val updated = CompanyProfile(
                                name = name,
                                sector = sector,
                                sharesOutstanding = shares.toDoubleOrNull() ?: profile.sharesOutstanding,
                                currentPrice = price.toDoubleOrNull() ?: profile.currentPrice,
                                currency = currency,
                                fiscalYearEnd = fye,
                                description = desc
                            )
                            onSave(updated)
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ")
                    }
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 2: PDF Upload
// ==========================================
@Composable
fun PdfUploadSection(onUploadClick: () -> Unit, companyName: String) {
    EditorialCard(
        title = "رفع القوائم المالية الذكي",
        subtitle = "يدعم استخراج البيانات المالية آليًا من مستندات الـ PDF محليًا لدمجها بالقوائم",
        icon = Icons.Default.CloudUpload
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "اختر ملف القوائم المالية (PDF)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "سيقوم التطبيق بقراءة أرقام الميزانية وحسابات الأرباح للشركة ($companyName)",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUploadClick,
                modifier = Modifier.testTag("upload_pdf_btn")
            ) {
                Text("رفع وتحليل الملف")
            }
        }
    }
}

// ==========================================
// SECTION 3: Editable Financial Statements
// ==========================================
@Composable
fun FinancialStatementsSection(
    income: IncomeStatement,
    balance: BalanceSheet,
    cashFlow: CashFlow,
    onSave: (IncomeStatement, BalanceSheet, CashFlow) -> Unit
) {
    var revenue by remember(income) { mutableStateOf(income.revenue.toString()) }
    var cogs by remember(income) { mutableStateOf(income.cogs.toString()) }
    var grossProfit by remember(income) { mutableStateOf(income.grossProfit.toString()) }
    var opex by remember(income) { mutableStateOf(income.opex.toString()) }
    var ebit by remember(income) { mutableStateOf(income.ebit.toString()) }
    var netIncome by remember(income) { mutableStateOf(income.netIncome.toString()) }

    var cash by remember(balance) { mutableStateOf(balance.cash.toString()) }
    var receivables by remember(balance) { mutableStateOf(balance.receivables.toString()) }
    var inventoryValue by remember(balance) { mutableStateOf(balance.inventory.toString()) }
    var currentAssets by remember(balance) { mutableStateOf(balance.currentAssets.toString()) }
    var ppe by remember(balance) { mutableStateOf(balance.ppe.toString()) }
    var totalAssets by remember(balance) { mutableStateOf(balance.totalAssets.toString()) }
    var currentLiab by remember(balance) { mutableStateOf(balance.currentLiabilities.toString()) }
    var longTermDebt by remember(balance) { mutableStateOf(balance.longTermDebt.toString()) }
    var totalLiab by remember(balance) { mutableStateOf(balance.totalLiabilities.toString()) }
    var equity by remember(balance) { mutableStateOf(balance.equity.toString()) }

    var cfo by remember(cashFlow) { mutableStateOf(cashFlow.cfo.toString()) }
    var cfi by remember(cashFlow) { mutableStateOf(cashFlow.cfi.toString()) }
    var cff by remember(cashFlow) { mutableStateOf(cashFlow.cff.toString()) }
    var capex by remember(cashFlow) { mutableStateOf(cashFlow.capex.toString()) }

    EditorialCard(
        title = "القوائم المالية (خلايا إكسل التفاعلية)",
        subtitle = "اضبط الأرقام مباشرة، وسيتم تحديث التحليلات والتقييمات الاستثمارية فورًا",
        icon = Icons.Default.GridOn
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // 3.1 Income Statement Panel
            Text("١. قائمة الدخل (Income Statement)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "الإيرادات", value = revenue, onValueChange = { revenue = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "تكلفة المبيعات", value = cogs, onValueChange = { cogs = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "مجمل الربح", value = grossProfit, onValueChange = { grossProfit = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "المصاريف التشغيلية", value = opex, onValueChange = { opex = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "الربح قبل الفائدة والضريبة (EBIT)", value = ebit, onValueChange = { ebit = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "صافي الدخل", value = netIncome, onValueChange = { netIncome = it }, modifier = Modifier.weight(1f))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // 3.2 Balance Sheet Panel
            Text("٢. الميزانية العمومية (Balance Sheet)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "النقدية وشبه النقدية", value = cash, onValueChange = { cash = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "العملاء / المدينون", value = receivables, onValueChange = { receivables = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "المخزون", value = inventoryValue, onValueChange = { inventoryValue = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "إجمالي الأصول المتداولة", value = currentAssets, onValueChange = { currentAssets = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "الأصول الثابتة (PPE)", value = ppe, onValueChange = { ppe = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "إجمالي الأصول", value = totalAssets, onValueChange = { totalAssets = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "الالتزامات المتداولة", value = currentLiab, onValueChange = { currentLiab = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "الديون طويلة الأجل", value = longTermDebt, onValueChange = { longTermDebt = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "إجمالي الالتزامات", value = totalLiab, onValueChange = { totalLiab = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "حقوق الملكية", value = equity, onValueChange = { equity = it }, modifier = Modifier.weight(1f))
                }

                // Balance equation indicator (Assets = Liabilities + Equity)
                val computedAssets = totalAssets.toDoubleOrNull() ?: 0.0
                val computedLiabAndEquity = (totalLiab.toDoubleOrNull() ?: 0.0) + (equity.toDoubleOrNull() ?: 0.0)
                val diff = computedAssets - computedLiabAndEquity
                val isBalanced = Math.abs(diff) < 0.1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBalanced) "✓ الميزانية متزنة" else "⚠ ميزانية غير متزنة (الأصول ≠ الخصوم + حقوق الملكية)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الفارق: $diff",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // 3.3 Cash Flow Panel
            Text("٣. قائمة التدفقات النقدية (Cash Flow Statement)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "التدفق من التشغيل (CFO)", value = cfo, onValueChange = { cfo = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "التدفق من الاستثمار (CFI)", value = cfi, onValueChange = { cfi = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniEditableCell(label = "التدفق من التمويل (CFF)", value = cff, onValueChange = { cff = it }, modifier = Modifier.weight(1f))
                    MiniEditableCell(label = "الإنفاق الرأسمالي (CapEx)", value = capex, onValueChange = { capex = it }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val inc = IncomeStatement(
                        revenue = revenue.toDoubleOrNull() ?: income.revenue,
                        cogs = cogs.toDoubleOrNull() ?: income.cogs,
                        grossProfit = grossProfit.toDoubleOrNull() ?: income.grossProfit,
                        opex = opex.toDoubleOrNull() ?: income.opex,
                        ebit = ebit.toDoubleOrNull() ?: income.ebit,
                        interestExpense = income.interestExpense,
                        tax = income.tax,
                        netIncome = netIncome.toDoubleOrNull() ?: income.netIncome
                    )
                    val bal = BalanceSheet(
                        cash = cash.toDoubleOrNull() ?: balance.cash,
                        receivables = receivables.toDoubleOrNull() ?: balance.receivables,
                        inventory = inventoryValue.toDoubleOrNull() ?: balance.inventory,
                        currentAssets = currentAssets.toDoubleOrNull() ?: balance.currentAssets,
                        ppe = ppe.toDoubleOrNull() ?: balance.ppe,
                        totalAssets = totalAssets.toDoubleOrNull() ?: balance.totalAssets,
                        currentLiabilities = currentLiab.toDoubleOrNull() ?: balance.currentLiabilities,
                        longTermDebt = longTermDebt.toDoubleOrNull() ?: balance.longTermDebt,
                        totalLiabilities = totalLiab.toDoubleOrNull() ?: balance.totalLiabilities,
                        equity = equity.toDoubleOrNull() ?: balance.equity
                    )
                    val cf = CashFlow(
                        cfo = cfo.toDoubleOrNull() ?: cashFlow.cfo,
                        cfi = cfi.toDoubleOrNull() ?: cashFlow.cfi,
                        cff = cff.toDoubleOrNull() ?: cashFlow.cff,
                        capex = capex.toDoubleOrNull() ?: cashFlow.capex
                    )
                    onSave(inc, bal, cf)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_statements_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ وتحديث الميزانية")
            }
        }
    }
}

@Composable
fun MiniEditableCell(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), maxLines = 1)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFBF9F4),
                unfocusedContainerColor = Color(0xFFFBF9F4)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==========================================
// SECTION 4: Financial Analysis & Valuation
// ==========================================
@Composable
fun AnalysisSection(analysis: SavedAnalysis?, profile: CompanyProfile, onAnalyzeClick: () -> Unit) {
    EditorialCard(
        title = "التحليل المالي والتقييم الاستثماري",
        subtitle = "تقييم التدفقات النقدية المخصومة (DCF) والـ WACC ومقارنات الأسواق",
        icon = Icons.Default.TrendingUp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            if (analysis == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("لا يوجد تحليل نشط حاليًا لمستندات ${profile.name}.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onAnalyzeClick, modifier = Modifier.testTag("run_analysis_btn")) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إجراء التحليل والتقييم عبر الذكاء الاصطناعي")
                    }
                }
            } else {
                // Main Recommendations Card
                val isBuy = analysis.recommendation.lowercase().contains("buy")
                val isSell = analysis.recommendation.lowercase().contains("sell")
                val badgeColor = if (isBuy) Color(0xFFE8F5E9) else if (isSell) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                val textColor = if (isBuy) Color(0xFF2E7D32) else if (isSell) Color(0xFFC62828) else Color(0xFFEF6C00)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("التوصية الاستثمارية النهائية", style = MaterialTheme.typography.bodySmall, color = textColor, fontWeight = FontWeight.Bold)
                        Text(
                            text = analysis.recommendation.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "معدل العائد المتوقع: ${analysis.upsidePct}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("السعر المستهدف العادل", style = MaterialTheme.typography.bodySmall, color = textColor)
                        Text(
                            text = "${analysis.finalTargetPrice} ${profile.currency}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "السعر الحالي: ${profile.currentPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }

                // Financial Analysis summary text
                Text("ملخص التحليل المالي والربحية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(analysis.summaryText, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

                // 4.1 Ratios Grid
                Text("المؤشرات المالية التاريخية (Historical Ratios)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                val ratiosArr = remember(analysis.ratiosJson) {
                    try {
                        JSONArray(analysis.ratiosJson)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (ratiosArr == null) {
                    Text("تعذر عرض المؤشرات: خطأ في صيغة البيانات", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                } else {
                    Column(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Headers
                        Row(modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(6.dp)) {
                            Text("السنة", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("السيولة", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("العائد Gross", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("صافي الهامش", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("العائد ROE", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("الرافعة المالي", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        for (i in 0 until ratiosArr.length()) {
                            val obj = ratiosArr.getJSONObject(i)
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(obj.optString("year"), modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("current_ratio"), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("gross_margin"), modifier = Modifier.width(85.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("net_margin"), modifier = Modifier.width(85.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("roe"), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("debt_to_equity"), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 4.2 Pro-Forma Forecast
                Text("التوقعات المستقبلية (Pro-Forma Forecast)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                val forecastArr = remember(analysis.forecastJson) {
                    try {
                        JSONArray(analysis.forecastJson)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (forecastArr == null) {
                    Text("تعذر عرض التوقعات: خطأ في صيغة البيانات", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                } else {
                    Column(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Headers
                        Row(modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(6.dp)) {
                            Text("السنة المتوقعة", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("الإيرادات المتوقعة", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("مجمل الأرباح", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("صافي الدخل المتوقع", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        for (i in 0 until forecastArr.length()) {
                            val obj = forecastArr.getJSONObject(i)
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(obj.optString("year"), modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("revenue"), modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("gross_profit"), modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall)
                                Text(obj.optString("net_income"), modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 4.3 DCF Details
                Text("تفاصيل ميزان التدفقات المخصومة (DCF)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                val dcfObj = remember(analysis.dcfJson) {
                    try {
                        JSONObject(analysis.dcfJson)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (dcfObj == null) {
                    Text("تعذر عرض DCF: خطأ في صيغة البيانات", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("معدل WACC", style = MaterialTheme.typography.bodySmall)
                                Text(dcfObj.optString("wacc", "12.5%"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("نمو نهائي Terminal", style = MaterialTheme.typography.bodySmall)
                                Text(dcfObj.optString("terminal_growth", "3%"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("قيمة المنشأة Enterprise", style = MaterialTheme.typography.bodySmall)
                                Text(dcfObj.optString("enterprise_value", "15,200M"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("القيمة العادلة Equity", style = MaterialTheme.typography.bodySmall)
                                Text(dcfObj.optString("equity_value", "11,000M"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4.4 Comparables
                Text("مقارنات الشركات المثيلة (Market Peers Multiples)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                val compObj = remember(analysis.comparablesJson) {
                    try {
                        JSONObject(analysis.comparablesJson)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (compObj == null) {
                    Text("تعذر عرض المقارنات: خطأ في صيغة البيانات", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                } else {
                    val multiples = compObj.optJSONObject("peer_multiples") ?: JSONObject()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("P/E Multi", style = MaterialTheme.typography.bodySmall)
                                Text(multiples.optString("P_E", "8.2x"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("EV/EBITDA", style = MaterialTheme.typography.bodySmall)
                                Text(multiples.optString("EV_EBITDA", "5.8x"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("P/B Multi", style = MaterialTheme.typography.bodySmall)
                                Text(multiples.optString("P_B", "1.2x"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("السعر الضمني من الشركات المثيلة: ${compObj.optString("implied_price", "")} ${profile.currency}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                // 4.5 Sensitivity Matrix
                Text("تحليل الحساسية الثنائي (Sensitivity: WACC × Growth)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                val sensArr = remember(analysis.sensitivityJson) {
                    try {
                        JSONArray(analysis.sensitivityJson)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (sensArr == null) {
                    Text("تعذر عرض مصفوفة الحساسية: خطأ في الصيغة", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("WACC", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("Growth", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("السعر المتوقع", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f))
                        }
                        for (i in 0 until minOf(sensArr.length(), 6)) {
                            val item = sensArr.getJSONObject(i)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.optString("wacc"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                Text(item.optString("terminal_growth"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                Text("${item.optString("implied_price")} ${profile.currency}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            }
                        }
                    }
                }

                // Recalculate button
                OutlinedButton(
                    onClick = onAnalyzeClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديث وإعادة تشغيل التقييم والـ DCF")
                }
            }
        }
    }
}

// ==========================================
// SECTION 5: Journal & Ledger
// ==========================================
@Composable
fun JournalSection(
    entries: List<JournalEntry>,
    onAddEntry: (String) -> Unit,
    onManualEntry: (String, String, String, Double, String, Double) -> Unit,
    onClearAll: () -> Unit
) {
    var arabicDesc by remember { mutableStateOf("") }
    
    // Manual Input details
    var showManualForm by remember { mutableStateOf(false) }
    var mDate by remember { mutableStateOf("10-07-2026") }
    var mDesc by remember { mutableStateOf("") }
    var mDrAcc by remember { mutableStateOf("") }
    var mDrAmt by remember { mutableStateOf("") }
    var mCrAcc by remember { mutableStateOf("") }
    var mCrAmt by remember { mutableStateOf("") }

    EditorialCard(
        title = "دفتر اليومية والأستاذ الترحيلي",
        subtitle = "اكتب المعاملة باللغة العربية البسيطة، وسيحولها الذكاء الاصطناعي لقيد مزدوج متزن",
        icon = Icons.Default.Book
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            
            // AI translation box
            Text("إدخال المعاملة باللغة الطبيعية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = arabicDesc,
                onValueChange = { arabicDesc = it },
                placeholder = { Text("مثال: شراء سيارة نقل للشركة بقيمة 500,000 جنيه كاش من الخزينة") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (arabicDesc.isNotBlank()) {
                            onAddEntry(arabicDesc)
                            arabicDesc = ""
                        }
                    },
                    modifier = Modifier.weight(1.5f).testTag("journal_submit_btn")
                ) {
                    Text("تحليل وترحيل القيد آليًا")
                }
                
                OutlinedButton(
                    onClick = { showManualForm = !showManualForm },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showManualForm) "إخفاء اليدوي" else "إدخال يدوي")
                }
            }

            if (showManualForm) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("قيد يدوي كلاسيكي (Double Entry)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = mDate, onValueChange = { mDate = it }, label = { Text("التاريخ") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = mDesc, onValueChange = { mDesc = it }, label = { Text("الوصف") }, modifier = Modifier.weight(2f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = mDrAcc, onValueChange = { mDrAcc = it }, label = { Text("الحساب المدين (Dr)") }, modifier = Modifier.weight(1.5f))
                            OutlinedTextField(value = mDrAmt, onValueChange = { mDrAmt = it }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = mCrAcc, onValueChange = { mCrAcc = it }, label = { Text("الحساب الدائن (Cr)") }, modifier = Modifier.weight(1.5f))
                            OutlinedTextField(value = mCrAmt, onValueChange = { mCrAmt = it }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                val dr = mDrAmt.toDoubleOrNull() ?: 0.0
                                val cr = mCrAmt.toDoubleOrNull() ?: 0.0
                                if (mDesc.isNotBlank() && mDrAcc.isNotBlank() && mCrAcc.isNotBlank()) {
                                    onManualEntry(mDate, mDesc, mDrAcc, dr, mCrAcc, cr)
                                    mDesc = ""
                                    mDrAcc = ""
                                    mDrAmt = ""
                                    mCrAcc = ""
                                    mCrAmt = ""
                                    showManualForm = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تسجيل القيد يدويًا في النظام")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // History Log of Journal Entries
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("سجل القيود المحاسبية المدخلة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (entries.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("تصفير السجل", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (entries.isEmpty()) {
                Text("لا توجد أي قيود مسجلة في هذه الجلسة حتى الآن.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    entries.forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entry.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(entry.date, style = MaterialTheme.typography.labelSmall)
                                }
                                
                                // Debits & Credits
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                        .padding(6.dp)
                                ) {
                                    val debArr = remember(entry.debitJson) {
                                        try { JSONArray(entry.debitJson) } catch (e: Exception) { null }
                                    }
                                    val credArr = remember(entry.creditJson) {
                                        try { JSONArray(entry.creditJson) } catch (e: Exception) { null }
                                    }
                                    if (debArr == null || credArr == null) {
                                        Text("خطأ عرض تفاصيل القيد", style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        for (i in 0 until debArr.length()) {
                                            val obj = debArr.getJSONObject(i)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("من حـ/ ${obj.optString("account")}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                                Text(obj.optString("amount"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        for (i in 0 until credArr.length()) {
                                            val obj = credArr.getJSONObject(i)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("إلى حـ/ ${obj.optString("account")}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), modifier = Modifier.padding(start = 12.dp))
                                                Text(obj.optString("amount"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (entry.notes.isNotBlank()) {
                                    Text("ملاحظات: ${entry.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 6: Inventory Section
// ==========================================
@Composable
fun InventorySection(
    items: List<InventoryItem>,
    onAddItem: (String, String, Double, Double) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onRunValuation: (String) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var itemType by remember { mutableStateOf("RAW") }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("") }
    var itemCost by remember { mutableStateOf("") }

    EditorialCard(
        title = "إدارة المخزون والتكلفة للشركة",
        subtitle = "تتبع خامات ومخرجات التصنيع وحساب تكلفة البضاعة المباعة COGS آليًا",
        icon = Icons.Default.Inventory
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("الأصناف والمخزونات الحالية:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { isAdding = !isAdding }) {
                    Icon(if (isAdding) Icons.Default.Close else Icons.Default.Add, contentDescription = "صنف جديد")
                }
            }

            if (isAdding) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("صنف مخزون جديد", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = itemType == "RAW", onClick = { itemType = "RAW" })
                                Text("خامات RAW", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = itemType == "FINISHED", onClick = { itemType = "FINISHED" })
                                Text("بضاعة تامة FINISHED", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("اسم الصنف") }, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("الكمية") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = itemCost, onValueChange = { itemCost = it }, label = { Text("تكلفة الوحدة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                val qty = itemQty.toDoubleOrNull() ?: 0.0
                                val cost = itemCost.toDoubleOrNull() ?: 0.0
                                if (itemName.isNotBlank() && qty > 0 && cost > 0) {
                                    onAddItem(itemType, itemName, qty, cost)
                                    itemName = ""
                                    itemQty = ""
                                    itemCost = ""
                                    isAdding = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إضافة الصنف للمخزون")
                        }
                    }
                }
            }

            // Tables of items
            if (items.isEmpty()) {
                Text("لا توجد أصناف مخزون مضافة حاليًا.", style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .background(if (item.type == "RAW") Color(0xFFFBF9F4) else Color(0xFFF5F1E5))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (item.type == "RAW") Color(0xFFB07F35) else Color(0xFF2F4538))
                                    )
                                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("الكمية: ${item.quantity} | تكلفة الوحدة: ${item.unitCost}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${item.totalValue}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onDeleteItem(item.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف صنف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // Valuation controls
            Text("تقييم وحساب تكلفة البضاعة المباعة COGS:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onRunValuation("FIFO") },
                    modifier = Modifier.weight(1f).testTag("fifo_btn")
                ) {
                    Text("حساب بطريقة FIFO")
                }
                Button(
                    onClick = { onRunValuation("Weighted Average") },
                    modifier = Modifier.weight(1f).testTag("wavg_btn")
                ) {
                    Text("المتوسط المرجح")
                }
            }
        }
    }
}

// ==========================================
// SECTION 7: Final CFA Report Section
// ==========================================
@Composable
fun FinalReportSection(
    report: SavedReport?,
    profile: CompanyProfile,
    onGenerate: () -> Unit,
    onExportPdf: (SavedReport) -> Unit
) {
    EditorialCard(
        title = "تقرير البحوث المعتمد (CFA Portfolio Challenge)",
        subtitle = "تقرير شامل يتكامل مع تحليل الأرقام والـ DCF وصياغة سيناريوهات المخاطر والتقييم الاستثماري",
        icon = Icons.Default.Description
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            if (report == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("لم يتم توليد التقرير النهائي للشركة (${profile.name}) بعد.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onGenerate, modifier = Modifier.testTag("generate_report_btn")) {
                        Icon(Icons.Default.Article, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد التقرير الاستثماري بأسلوب CFA")
                    }
                }
            } else {
                // Headline recommendation summary
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("توصية CFA Challenge المعتمدة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = report.recommendation.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (report.recommendation.lowercase().contains("buy")) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("السعر المستهدف العادل:", style = MaterialTheme.typography.bodySmall)
                            Text(report.targetPrice, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سعر السهم الحالي بالسوق:", style = MaterialTheme.typography.bodySmall)
                            Text(report.currentPrice, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Subsections layout (connected Scrollable text blocks)
                CfaSectionBlock(title = "١. وصف النشاط وأساس الاستثمار (Business Description)", text = report.businessDescription)
                CfaSectionBlock(title = "٢. نظرة عامة على القطاع (Industry Overview)", text = report.industryOverview)
                CfaSectionBlock(title = "٣. ملخص الاستثمار وقيمته (Investment Summary)", text = report.investmentSummary)
                CfaSectionBlock(title = "٤. ملخص التقييم والـ DCF (Valuation Summary)", text = report.valuationSummary)
                CfaSectionBlock(title = "٥. التحليل المالي والأداء (Financial Analysis)", text = report.financialAnalysis)
                CfaSectionBlock(title = "٦. تحليل المخاطر الرئيسية والفرص (Key Risks)", text = report.keyRisks)

                // Financial Summary Table inside report
                Text("٧. جدول الملخص المالي السريع (Financial Summary)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                val tableParsed = remember(report.tableJson) {
                    runCatching { JSONArray(report.tableJson) }
                }
                if (tableParsed.isSuccess) {
                    val tableArray = tableParsed.getOrThrow()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Headers
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)).padding(4.dp)) {
                            Text("البند المالي", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("الماضي", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("الحالي", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("المستهدف", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        for (i in 0 until tableArray.length()) {
                            val rowObj = tableArray.getJSONObject(i)
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                Text(rowObj.optString("metric"), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall)
                                Text(rowObj.optString("val1"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                Text(rowObj.optString("val2"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                Text(rowObj.optString("val3"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    Log.e("FinalReportSection", "Error displaying table: ${tableParsed.exceptionOrNull()?.message}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // EXPORT PDF BUTTON
                Button(
                    onClick = { onExportPdf(report) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_pdf_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير التقرير النهائي كملف PDF معتمد")
                }
            }
        }
    }
}

@Composable
fun CfaSectionBlock(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ==========================================
// CORE GRAPHICAL CONTAINER: EditorialCard
// ==========================================
@Composable
fun EditorialCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editorial_card_${title.replace(" ", "_")}"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}
