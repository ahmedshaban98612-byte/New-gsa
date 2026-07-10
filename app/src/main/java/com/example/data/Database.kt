package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM company_profile WHERE id = 'active' LIMIT 1")
    fun getProfileFlow(): Flow<CompanyProfile?>

    @Query("SELECT * FROM company_profile WHERE id = 'active' LIMIT 1")
    suspend fun getProfile(): CompanyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CompanyProfile)

    @Query("SELECT * FROM income_statement WHERE id = 'active' LIMIT 1")
    fun getIncomeStatementFlow(): Flow<IncomeStatement?>

    @Query("SELECT * FROM income_statement WHERE id = 'active' LIMIT 1")
    suspend fun getIncomeStatement(): IncomeStatement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeStatement(statement: IncomeStatement)

    @Query("SELECT * FROM balance_sheet WHERE id = 'active' LIMIT 1")
    fun getBalanceSheetFlow(): Flow<BalanceSheet?>

    @Query("SELECT * FROM balance_sheet WHERE id = 'active' LIMIT 1")
    suspend fun getBalanceSheet(): BalanceSheet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalanceSheet(statement: BalanceSheet)

    @Query("SELECT * FROM cash_flow WHERE id = 'active' LIMIT 1")
    fun getCashFlowFlow(): Flow<CashFlow?>

    @Query("SELECT * FROM cash_flow WHERE id = 'active' LIMIT 1")
    suspend fun getCashFlow(): CashFlow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashFlow(statement: CashFlow)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY id ASC")
    fun getAllEntriesFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY id ASC")
    suspend fun getAllEntries(): List<JournalEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries")
    suspend fun clearEntries()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items")
    fun getAllItemsFlow(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items")
    suspend fun getAllItems(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM inventory_items")
    suspend fun clearInventory()
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM saved_analysis WHERE id = 'active' LIMIT 1")
    fun getAnalysisFlow(): Flow<SavedAnalysis?>

    @Query("SELECT * FROM saved_analysis WHERE id = 'active' LIMIT 1")
    suspend fun getAnalysis(): SavedAnalysis?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: SavedAnalysis)

    @Query("SELECT * FROM saved_report WHERE id = 'active' LIMIT 1")
    fun getReportFlow(): Flow<SavedReport?>

    @Query("SELECT * FROM saved_report WHERE id = 'active' LIMIT 1")
    suspend fun getReport(): SavedReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SavedReport)
}

@Database(
    entities = [
        CompanyProfile::class,
        IncomeStatement::class,
        BalanceSheet::class,
        CashFlow::class,
        JournalEntry::class,
        InventoryItem::class,
        SavedAnalysis::class,
        SavedReport::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun journalDao(): JournalDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun analysisDao(): AnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daftar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
