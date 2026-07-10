package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    private const val TAG = "PdfExporter"

    fun exportReportAsPdf(context: Context, report: SavedReport, profile: CompanyProfile) {
        try {
            Log.d(TAG, "Starting PDF export for report: ${report.recommendation}")
            val pdfDocument = PdfDocument()
            
            // Standard A4 Size: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            
            // 1. Draw Background Parchment Color
            paint.color = 0xFFF2ECDD.toInt()
            canvas.drawRect(0f, 0f, 595f, 842f, paint)

            // 2. Decorative Top Brass Line
            paint.color = 0xFFB07F35.toInt()
            canvas.drawRect(30f, 30f, 565f, 34f, paint)

            // 3. Header Text - Arabic Alignment (RTL)
            val titlePaint = TextPaint().apply {
                color = 0xFF2F4538.toInt()
                textSize = 22f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("دفتر — تقرير البحوث الاستثمارية والتحليل المالي", 50f, 65f, titlePaint)

            val subTitlePaint = TextPaint().apply {
                color = 0xFF2F4538.toInt()
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText("CFA Research Challenge Style Portfolio Report", 50f, 80f, subTitlePaint)

            // 4. Draw Metadata Header Box
            paint.color = 0xFFEAE3CE.toInt()
            canvas.drawRoundRect(30f, 95f, 565f, 155f, 8f, 8f, paint)

            val metaBoldPaint = TextPaint().apply {
                color = 0xFF2F4538.toInt()
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val metaBodyPaint = TextPaint().apply {
                color = 0xFF2F4538.toInt()
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            // Draw Profile Details (Arabic)
            canvas.drawText("الشركة: ${profile.name}", 45f, 115f, metaBoldPaint)
            canvas.drawText("القطاع: ${profile.sector}", 45f, 132f, metaBodyPaint)
            canvas.drawText("العملة: ${profile.currency}", 45f, 147f, metaBodyPaint)

            // Target Valuation Box
            paint.color = 0xFF2F4538.toInt()
            canvas.drawRoundRect(380f, 102f, 555f, 148f, 6f, 6f, paint)

            val targetPricePaint = TextPaint().apply {
                color = 0xFFF2ECDD.toInt()
                textSize = 13f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
            val targetLabelPaint = TextPaint().apply {
                color = 0xFFB07F35.toInt()
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            canvas.drawText("T. PRICE: ${report.targetPrice}", 390f, 120f, targetPricePaint)
            canvas.drawText("RECOMM: ${report.recommendation.uppercase()}", 390f, 132f, targetLabelPaint)
            canvas.drawText("CURRENT: ${report.currentPrice}", 390f, 144f, targetPricePaint)

            // 5. Draw Sections with automatic wrapping using StaticLayout
            var currentY = 175f

            val sectionTitlePaint = TextPaint().apply {
                color = 0xFF2F4538.toInt()
                textSize = 12f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val sectionBodyPaint = TextPaint().apply {
                color = 0xFF2F4538.toInt()
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            fun drawSection(title: String, body: String) {
                if (currentY > 750f) return // Avoid drawing off-page in simple 1-page sample (we can handle overflow if needed)
                
                // Section Title Divider
                paint.color = 0x222F4538.toInt()
                canvas.drawRect(30f, currentY, 565f, currentY + 1f, paint)
                
                currentY += 12f
                canvas.drawText(title, 30f, currentY, sectionTitlePaint)
                currentY += 6f

                // Body text wrapped to width 535 points
                val staticLayout = StaticLayout.Builder.obtain(
                    body, 0, body.length, sectionBodyPaint, 535
                )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(true)
                .build()

                canvas.save()
                canvas.translate(30f, currentY)
                staticLayout.draw(canvas)
                canvas.restore()

                currentY += staticLayout.height + 14f
            }

            drawSection("١. وصف النشاط وأساس الاستثمار (Business Description)", report.businessDescription)
            drawSection("٢. نظرة على القطاع والمنافسة (Industry Overview)", report.industryOverview)
            drawSection("٣. ملخص التحليل المالي والتقييم (Valuation Summary)", report.valuationSummary)
            drawSection("٤. المخاطر الاستثمارية الرئيسية (Key Investment Risks)", report.keyRisks)

            // 6. Draw Table at Bottom
            if (currentY < 720f) {
                paint.color = 0x152F4538.toInt()
                canvas.drawRect(30f, currentY, 565f, currentY + 18f, paint)
                
                val tableHeaderPaint = TextPaint().apply {
                    color = 0xFF2F4538.toInt()
                    textSize = 8f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                
                canvas.drawText("البند المالي (Metric)", 35f, currentY + 12f, tableHeaderPaint)
                canvas.drawText("السنة الماضية", 220f, currentY + 12f, tableHeaderPaint)
                canvas.drawText("الحالي", 340f, currentY + 12f, tableHeaderPaint)
                canvas.drawText("المستهدف", 460f, currentY + 12f, tableHeaderPaint)

                currentY += 20f

                try {
                    val tableArray = JSONArray(report.tableJson)
                    val tableCellPaint = TextPaint().apply {
                        color = 0xFF2F4538.toInt()
                        textSize = 8f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                        isAntiAlias = true
                    }

                    for (i in 0 until minOf(tableArray.length(), 4)) {
                        val rowObj = tableArray.getJSONObject(i)
                        val metric = rowObj.optString("metric", "")
                        val val1 = rowObj.optString("val1", "")
                        val val2 = rowObj.optString("val2", "")
                        val val3 = rowObj.optString("val3", "")

                        // Line divider
                        paint.color = 0x112F4538.toInt()
                        canvas.drawRect(30f, currentY + 12f, 565f, currentY + 13f, paint)

                        canvas.drawText(metric, 35f, currentY + 9f, tableCellPaint)
                        canvas.drawText(val1, 220f, currentY + 9f, tableCellPaint)
                        canvas.drawText(val2, 340f, currentY + 9f, tableCellPaint)
                        canvas.drawText(val3, 460f, currentY + 9f, tableCellPaint)

                        currentY += 14f
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rendering report table to PDF: ${e.message}")
                }
            }

            // Draw Footer Note
            val footerPaint = TextPaint().apply {
                color = 0xAA2F4538.toInt()
                textSize = 7f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("تم التوليد تلقائيًا بواسطة تطبيق 'دفتر' لتقييم الشركات والمحاسبة الذكية", 180f, 825f, footerPaint)

            pdfDocument.finishPage(page)

            // Save PDF file in cache
            val cacheFile = File(context.cacheDir, "Daftar_CFA_Research_Report.pdf")
            FileOutputStream(cacheFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            Log.d(TAG, "PDF successfully written to ${cacheFile.absolutePath}")

            // Share file with custom provider
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Daftar Equity Research Report - ${profile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة التقرير عبر").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting PDF", e)
            throw Exception("فشل تصدير التقرير النهائي كملف PDF: ${e.message}")
        }
    }
}
