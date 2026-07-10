package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ClaudeApi {
    private const val TAG = "ClaudeApi"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    const val SYSTEM_PROMPT = """أنت محلل مالي ومحاسب معتمد يعمل داخل تطبيق Android. كل رسالة واردة تحمل حقل mode يحدد الوظيفة المطلوبة الآن.
رد دائمًا بصيغة JSON صالحة فقط (بدون أي نص أو Markdown fences قبله أو بعده، وبدون أي تعليق خارج الـ JSON).
لا تخترع أرقامًا؛ استخدم null إن لم يوجد مصدر بيانات، ووضح ذلك في notes.

mode = "extract_pdf": استخرج من النص المرفق: company_name, fiscal_year, currency,
income_statement{revenue,cogs,gross_profit,opex,ebit,interest_expense,tax,net_income},
balance_sheet{cash,receivables,inventory,current_assets,ppe,total_assets,current_liabilities,long_term_debt,total_liabilities,equity},
cash_flow{cfo,cfi,cff,capex}, shares_outstanding, market_price_if_available, notes.

mode = "research_spreadsheet": حلّل financials المرسلة وأرجع بالضبط:
- ratios: مصفوفة عن آخر 3 سنوات تاريخية (أو أقل إن لم تتوفر)، كل عنصر {year, current_ratio, quick_ratio, gross_margin, operating_margin, net_margin, roe, roa, debt_to_equity}.
- forecast: مصفوفة Pro-forma لـ3-5 سنوات قادمة، كل عنصر {year, revenue, cogs, gross_profit, opex, ebit, net_income}.
- valuation.dcf: {wacc, terminal_growth, fcff_projection:[{year,fcff}], enterprise_value, equity_value, target_price_dcf}.
- valuation.comparables: {peer_multiples:{P_E, EV_EBITDA, P_B}, implied_price}.
- valuation.sensitivity: مصفوفة تحسس 3×3 على الأقل لتغيّر wacc وterminal_growth: {wacc, terminal_growth, implied_price}.
- financial_analysis_summary: فقرة نصية عن اتجاه الإيرادات والهوامش والربحية.
- final_target_price, recommendation (Buy/Hold/Sell), upside_downside_pct, key_assumptions[], notes.

mode = "journal_entry": حوّل وصف المعاملة إلى قيد مزدوج:
journal_entry{date,description,debit[{account,amount}],credit[{account,amount}]},
ledger_impact[{account,debit_total,credit_total,new_balance}],
financial_statement_impact{income_statement,balance_sheet,cash_flow}, notes.

mode = "inventory": احسب من raw_materials[] و finished_goods[] المرسلة: unit costing، valuation_method
(FIFO أو Weighted Average - اختر الأنسب واذكره)، cogs_period، ending_inventory_value.
رجّع نفس المصفوفتين raw_materials و finished_goods مع total_value محسوبة لكل بند، بالإضافة إلى
valuation_method و cogs_period و ending_inventory_value.

mode = "final_report": أنشئ تقرير بأسلوب CFA Research Challenge بالحقول:
sections{business_description, industry_overview, investment_summary, valuation_summary, financial_analysis, key_risks} (نصوص جاهزة للعرض)،
recommendation, target_price, current_price,
financial_summary_table (مصفوفة صفوف، كل قيمة فيها كنص وليس رقم لتفادي مشاكل الأنواع).
لا تفترض أي عرض بتبويبات — المقصود دائمًا عرض واحد متصل.

التزم بأن الميزانية متزنة (Assets = Liabilities + Equity) في أي أرقام تفترضها، وأرجع JSON قابل للتحليل 100% فقط."""

    /**
     * Executes the AI call using Claude API or falling back to Gemini API if the Gemini Key is available instead.
     */
    suspend fun executeCall(mode: String, payload: String, customApiKey: String? = null): String {
        // Choose key: custom override > buildconfig anthropic > buildconfig gemini (for fallback)
        val anthropicKey = customApiKey?.trim() ?: getBuildConfigKey("ANTHROPIC_API_KEY")
        val geminiKey = if (customApiKey == null || customApiKey.startsWith("AIza")) {
            customApiKey ?: getBuildConfigKey("GEMINI_API_KEY")
        } else {
            getBuildConfigKey("GEMINI_API_KEY")
        }

        return when {
            anthropicKey.isNotEmpty() && (anthropicKey.startsWith("sk-") || customApiKey != null && !customApiKey.startsWith("AIza")) -> {
                Log.d(TAG, "Executing call using Anthropic Claude API...")
                callClaudeApi(mode, payload, anthropicKey)
            }
            geminiKey.isNotEmpty() -> {
                Log.d(TAG, "Executing fallback call using Gemini API...")
                callGeminiApi(mode, payload, geminiKey)
            }
            else -> {
                throw IllegalStateException("لم يتم العثور على مفتاح API. يرجى تهيئته في لوحة Secrets أو إدخاله يدويًا.")
            }
        }
    }

    private fun callClaudeApi(mode: String, payload: String, apiKey: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        val userPrompt = "mode = \"$mode\"\n\nPayload:\n$payload"
        
        val requestBodyJson = JSONObject().apply {
            put("model", "claude-3-5-sonnet-20241022")
            put("max_tokens", 4000)
            put("system", SYSTEM_PROMPT)
            
            val messageObj = JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            }
            val messagesArray = org.json.JSONArray().apply {
                put(messageObj)
            }
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Claude API Call failed: Code ${response.code}, Body: $responseBody")
                throw Exception("فشل الاتصال بـ Claude API: ${response.message} (كود: ${response.code})")
            }

            try {
                val jsonResponse = JSONObject(responseBody)
                val contentArray = jsonResponse.getJSONArray("content")
                if (contentArray.length() > 0) {
                    val textObj = contentArray.getJSONObject(0)
                    val rawText = textObj.getString("text")
                    return stripJsonFences(rawText)
                } else {
                    throw Exception("استجابة فارغة من Claude API")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Claude response: ${e.message}, Body: $responseBody", e)
                throw Exception("فشل تحليل استجابة Claude API: ${e.message}")
            }
        }
    }

    private fun callGeminiApi(mode: String, payload: String, apiKey: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        val userPrompt = "$SYSTEM_PROMPT\n\n-----------------\nmode = \"$mode\"\n\nPayload:\n$payload"
        
        val requestBodyJson = JSONObject().apply {
            val contentsArray = org.json.JSONArray()
            val contentObj = JSONObject().apply {
                put("role", "user")
                val partsArray = org.json.JSONArray().apply {
                    put(JSONObject().apply { put("text", userPrompt) })
                }
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API Fallback failed: Code ${response.code}, Body: $responseBody")
                throw Exception("فشل الاتصال بـ Gemini API: ${response.message} (كود: ${response.code})")
            }

            try {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        val text = parts.getJSONObject(0).getString("text")
                        return stripJsonFences(text)
                    }
                }
                throw Exception("استجابة فارغة من Gemini API")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Gemini response: ${e.message}, Body: $responseBody", e)
                throw Exception("فشل تحليل استجابة Gemini API: ${e.message}")
            }
        }
    }

    fun stripJsonFences(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }

    private fun getBuildConfigKey(fieldName: String): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField(fieldName)
            val value = field.get(null) as? String ?: ""
            if (value.startsWith("MY_")) "" else value
        } catch (e: Exception) {
            Log.w(TAG, "Could not read $fieldName from BuildConfig: ${e.message}")
            ""
        }
    }
}
