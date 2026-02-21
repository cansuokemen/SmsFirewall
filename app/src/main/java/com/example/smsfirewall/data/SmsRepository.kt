package com.example.smsfirewall.data

import com.example.smsfirewall.data.local.SmsDao
import com.example.smsfirewall.data.local.SmsEntity
import kotlinx.coroutines.flow.Flow

class SmsRepository(
    private val smsDao: SmsDao
) {
    suspend fun insert(sms: SmsEntity) {
        smsDao.insert(sms)
    }

    fun getAll(): Flow<List<SmsEntity>> {
        return smsDao.getAll()
    }

    fun getByStatus(status: String): Flow<List<SmsEntity>> {
        return smsDao.getByStatus(status)
    }

    suspend fun delete(sms: SmsEntity) {
        smsDao.delete(sms)
    }
}
