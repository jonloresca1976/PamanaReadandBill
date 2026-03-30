package com.example.pamanareadandbill

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CustomerInfo")
data class CustomerInfo(

    @PrimaryKey
    val srvc_nmbr: String,

    val read_seqn: Int,
    val acct_nmbr: String,
    val cssr_name: String,
    val cssr_addr: String,
    val mtr_info: String,
    val prev_rdng: Int,
    val date_from: String,
    val amt_arr: Double,
    val prev_arr: Double,
    val amt_misc: Double,
    val amt_mat: Double,
    val amt_pdv: Double,
    val amt_aro: Double,
    val months: Int,
    val average: Int,
    val stat_code: String,
    val location: String,
    val s_citizen: String,
    val s_expire: String,
    val chk_sum: Double
)

@Entity(tableName = "MeterReading")
data class MeterReading(

    @PrimaryKey
    val srvc_nmbr: String,

    val read_date: String,
    val prev_rdng: Int,
    val pres_rdng: Int,
    val consume: Int,
    val peso_value: Double,
    val amt_rr: Double,
    val prev_arr: Double,
    val amt_others: Double,
    val field_findings: String,
    val remarks: String,
    val reader: String,
    val numb_tries: Int,
    val numb_print: Int,
    val read_time: String,
    val read_loc: String,
    val loc_update: String,
    val device_id: String
)

@Entity(tableName = "WaterRates")
data class WaterRates(

    @PrimaryKey
    val acct_code: String,

    val acct_desc: String,
    val low_lim1: Int,
    val high_lim1: Int,
    val amt1: Double,
    val low_lim2: Int,
    val high_lim2: Int,
    val amt2: Double,
    val low_lim3: Int,
    val high_lim3: Int,
    val amt3: Double,
    val low_lim4: Int,
    val high_lim4: Int,
    val amt4: Double,
    val low_lim5: Int,
    val high_lim5: Int,
    val amt5: Double
)

@Entity(tableName = "MeterReaders")
data class MeterReaders(

    @PrimaryKey
    val reader_id: String,

    val reader_name: String,
    val reader_pw: String,
    val device_id: String
)

@Entity(tableName = "FieldFindings")
data class FieldFindings(

    @PrimaryKey(autoGenerate = true)
    val finding_id: Int = 0,

    val finding_desc: String,
    val endorsed_to: String
)

@Entity(tableName = "ReadHistory")
data class ReadHistory(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val srvc_nmbr: String,
    val read_date: String,
    val pres_rdng: Int,
    val consume: Int,
    val remarks:  String,
    val reader: String
)

@Entity(tableName = "ProxAlerts")
data class ProxAlerts(

    @PrimaryKey(autoGenerate = true)
    val alert_id: Int,

    val alert_msg: String,
    val alert_loc: String,
    val alert_pic: String
)
