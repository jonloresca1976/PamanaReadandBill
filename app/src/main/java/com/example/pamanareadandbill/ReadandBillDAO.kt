package com.example.pamanareadandbill

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.w3c.dom.Text

@Dao
interface CustomerDao {

    //---------------------------------------------------------
    // Customer Information related queries
    //---------------------------------------------------------
    @Insert
    abstract fun insertCustomer(customer: CustomerInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCustomers(CustomerInfo: List<CustomerInfo>)

    @Query("SELECT * FROM CustomerInfo ORDER BY read_seqn")
    public abstract suspend fun getCustomers(): List<CustomerInfo>

    @Query("SELECT * FROM CustomerInfo ORDER BY read_seqn")
    fun getAllCustomers(): Flow<List<CustomerInfo>>

    @Query("DELETE FROM CustomerInfo")
    fun deleteAll(): Int
    //---------------------------------------------------------

    /*
     @Query("SELECT * FROM Users WHERE user_name = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?
     */
}

@Dao
interface HistoryDao {
    //---------------------------------------------------------
    // Account histories related queries
    //---------------------------------------------------------
    @Insert
    abstract fun insertHistory(history: ReadHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistories(ReadHistory: List<ReadHistory>)

    @Query("SELECT * FROM ReadHistory ORDER BY srvc_nmbr, read_date")
    public abstract suspend fun getHistories(): List<ReadHistory>

    @Query( "SELECT * FROM ReadHistory WHERE srvc_nmbr = :srvc_nmbr ORDER BY read_date")
    public abstract suspend fun getHistory(srvc_nmbr: String): List<ReadHistory>

    @Query("DELETE FROM ReadHistory")
    fun deleteAllHistories(): Int
}

@Dao
interface FindingsDao {
    //---------------------------------------------------------
    // Field Findings related queries
    //---------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFldFindings(FieldFindings: List<FieldFindings>)

    @Query("SELECT * FROM FieldFindings ORDER BY finding_desc")
    public abstract suspend fun getFldFindings(): List<FieldFindings>

    @Query("DELETE FROM FieldFindings")
    fun deleteAllFldFindings(): Int
    //---------------------------------------------------------
}


@Dao
interface WaterRatesDao {
    //---------------------------------------------------------
    // Water Rates related queries
    //---------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWaterRates(WaterRates: List<WaterRates>)

    @Query("SELECT * FROM WaterRates ORDER BY acct_code")
    public abstract suspend fun getAllWaterRates(): List<WaterRates>

    @Query("SELECT * FROM WaterRates WHERE acct_code = :acct_code LIMIT 1")
    public abstract suspend fun getWaterRates(acct_code: String): WaterRates?

    @Query("DELETE FROM WaterRates")
    fun deleteAllWaterRates(): Int
}

@Dao
interface MeterReadingDao {
    //---------------------------------------------------------
    // Meter Reading related queries
    //---------------------------------------------------------
    @Insert
    suspend fun insertMeterReading(meterReading: MeterReading)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeterReading(meterReading: List<MeterReading>)

    @Query("SELECT * FROM MeterReading ORDER BY srvc_nmbr, read_date")
    fun getAllMeterReading(): Flow<List<MeterReading>>

    @Query("SELECT * FROM MeterReading WHERE srvc_nmbr = :srvc_nmbr LIMIT 1")
    suspend fun getMeterReading(srvc_nmbr: String): MeterReading?

    @Query("UPDATE MeterReading SET pres_rdng = :pres_rdng, " +
            "consume = :consume, " +
            "peso_value = :peso_value, " +
            "field_findings = :findings, " +
            "remarks = :remarks " +
            "WHERE srvc_nmbr = :srvc_nmbr")
    suspend fun updateMeterReading(srvc_nmbr: String, pres_rdng: Int, consume: Int, peso_value: Double,
            findings: String, remarks: String)

    @Query("DELETE FROM MeterReading")
    suspend fun deleteAllMeterReading(): Int

}
