package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_profile")
data class CompanyProfile(
    @PrimaryKey val id: String = "active",
    val name: String = "نور الطاقة ش.م.م",
    val sector: String = "المرافق والخدمات العامة",
    val sharesOutstanding: Double = 100_000_000.0,
    val currentPrice: Double = 42.50,
    val currency: String = "EGP",
    val fiscalYearEnd: String = "31 ديسمبر",
    val description: String = "شركة رائدة في مجال توليد ونقل الطاقة الكهربائية والحلول المستدامة."
)

@Entity(tableName = "income_statement")
data class IncomeStatement(
    @PrimaryKey val id: String = "active",
    val revenue: Double = 0.0,
    val cogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val opex: Double = 0.0,
    val ebit: Double = 0.0,
    val interestExpense: Double = 0.0,
    val tax: Double = 0.0,
    val netIncome: Double = 0.0
)

@Entity(tableName = "balance_sheet")
data class BalanceSheet(
    @PrimaryKey val id: String = "active",
    val cash: Double = 0.0,
    val receivables: Double = 0.0,
    val inventory: Double = 0.0,
    val currentAssets: Double = 0.0,
    val ppe: Double = 0.0,
    val totalAssets: Double = 0.0,
    val currentLiabilities: Double = 0.0,
    val longTermDebt: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val equity: Double = 0.0
)

@Entity(tableName = "cash_flow")
data class CashFlow(
    @PrimaryKey val id: String = "active",
    val cfo: Double = 0.0,
    val cfi: Double = 0.0,
    val cff: Double = 0.0,
    val capex: Double = 0.0
)

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String = "",
    val description: String = "",
    val debitJson: String = "",
    val creditJson: String = "",
    val ledgerImpactJson: String = "",
    val finImpactJson: String = "",
    val notes: String = ""
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String = "RAW", // RAW or FINISHED
    val name: String = "",
    val quantity: Double = 0.0,
    val unitCost: Double = 0.0,
    val totalValue: Double = 0.0
)

@Entity(tableName = "saved_analysis")
data class SavedAnalysis(
    @PrimaryKey val id: String = "active",
    val ratiosJson: String = "",
    val forecastJson: String = "",
    val dcfJson: String = "",
    val comparablesJson: String = "",
    val sensitivityJson: String = "",
    val summaryText: String = "",
    val finalTargetPrice: Double = 0.0,
    val recommendation: String = "",
    val upsidePct: Double = 0.0,
    val keyAssumptions: String = "",
    val notes: String = ""
)

@Entity(tableName = "saved_report")
data class SavedReport(
    @PrimaryKey val id: String = "active",
    val businessDescription: String = "",
    val industryOverview: String = "",
    val investmentSummary: String = "",
    val valuationSummary: String = "",
    val financialAnalysis: String = "",
    val keyRisks: String = "",
    val recommendation: String = "",
    val targetPrice: String = "",
    val currentPrice: String = "",
    val tableJson: String = ""
)

// --- Helper JSON structures (for Serialization / Deserialization if needed) ---

data class ExtractedFinancialsResponse(
    val company_name: String? = null,
    val fiscal_year: String? = null,
    val currency: String? = null,
    val income_statement: IncomeStatement? = null,
    val balance_sheet: BalanceSheet? = null,
    val cash_flow: CashFlow? = null,
    val shares_outstanding: Double? = null,
    val market_price_if_available: Double? = null,
    val notes: String? = null
)

data class RatioItem(
    val year: String? = "",
    val current_ratio: Double? = null,
    val quick_ratio: Double? = null,
    val gross_margin: Double? = null,
    val operating_margin: Double? = null,
    val net_margin: Double? = null,
    val roe: Double? = null,
    val roa: Double? = null,
    val debt_to_equity: Double? = null
)

data class ForecastItem(
    val year: String? = "",
    val revenue: Double? = null,
    val cogs: Double? = null,
    val gross_profit: Double? = null,
    val opex: Double? = null,
    val ebit: Double? = null,
    val net_income: Double? = null
)

data class FcffItem(
    val year: String? = "",
    val fcff: Double? = null
)

data class DcfValuation(
    val wacc: Double? = null,
    val terminal_growth: Double? = null,
    val fcff_projection: List<FcffItem>? = emptyList(),
    val enterprise_value: Double? = null,
    val equity_value: Double? = null,
    val target_price_dcf: Double? = null
)

data class PeerMultiples(
    val P_E: Double? = null,
    val EV_EBITDA: Double? = null,
    val P_B: Double? = null
)

data class ComparablesValuation(
    val peer_multiples: PeerMultiples? = null,
    val implied_price: Double? = null
)

data class SensitivityItem(
    val wacc: Double? = null,
    val terminal_growth: Double? = null,
    val implied_price: Double? = null
)

data class ValuationData(
    val dcf: DcfValuation? = null,
    val comparables: ComparablesValuation? = null,
    val sensitivity: List<SensitivityItem>? = emptyList()
)

data class ResearchResponse(
    val ratios: List<RatioItem>? = emptyList(),
    val forecast: List<ForecastItem>? = emptyList(),
    val valuation: ValuationData? = null,
    val financial_analysis_summary: String? = null,
    val final_target_price: Double? = null,
    val recommendation: String? = null,
    val upside_downside_pct: Double? = null,
    val key_assumptions: List<String>? = emptyList(),
    val notes: String? = null
)

data class AccountEntry(
    val account: String? = "",
    val amount: Double? = 0.0
)

data class JournalEntryData(
    val date: String? = "",
    val description: String? = "",
    val debit: List<AccountEntry>? = emptyList(),
    val credit: List<AccountEntry>? = emptyList()
)

data class LedgerImpactItem(
    val account: String? = "",
    val debit_total: Double? = 0.0,
    val credit_total: Double? = 0.0,
    val new_balance: Double? = 0.0
)

data class FinancialStatementImpact(
    val income_statement: String? = null,
    val balance_sheet: String? = null,
    val cash_flow: String? = null
)

data class JournalResponse(
    val journal_entry: JournalEntryData? = null,
    val ledger_impact: List<LedgerImpactItem>? = emptyList(),
    val financial_statement_impact: FinancialStatementImpact? = null,
    val notes: String? = null
)

data class InventoryResponse(
    val raw_materials: List<InventoryItem>? = emptyList(),
    val finished_goods: List<InventoryItem>? = emptyList(),
    val valuation_method: String? = null,
    val cogs_period: Double? = null,
    val ending_inventory_value: Double? = null
)

data class ReportSections(
    val business_description: String? = "",
    val industry_overview: String? = "",
    val investment_summary: String? = "",
    val valuation_summary: String? = "",
    val financial_analysis: String? = "",
    val key_risks: String? = ""
)

data class FinancialSummaryRow(
    val metric: String? = "",
    val y1: String? = "",
    val y2: String? = "",
    val y3: String? = ""
)

data class FinalReportResponse(
    val sections: ReportSections? = null,
    val recommendation: String? = null,
    val target_price: String? = null,
    val current_price: String? = null,
    val financial_summary_table: List<FinancialSummaryRow>? = emptyList()
)
