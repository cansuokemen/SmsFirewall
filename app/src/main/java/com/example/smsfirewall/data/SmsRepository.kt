package com.example.smsfirewall.data

import com.example.smsfirewall.data.local.SmsDao
import com.example.smsfirewall.data.local.SmsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository @Inject constructor(
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

    fun getByStatusNot(status: String): Flow<List<SmsEntity>> {
        return smsDao.getByStatusNot(status)
    }

    suspend fun deleteByStatusBefore(status: String, beforeTimestamp: Long): Int {
        return smsDao.deleteByStatusBefore(status, beforeTimestamp)
    }

    suspend fun getAllByStatus(status: String): List<SmsEntity> {
        return smsDao.getAllByStatus(status)
    }

    suspend fun delete(sms: SmsEntity) {
        smsDao.delete(sms)
    }

    suspend fun deleteAll(messages: List<SmsEntity>) {
        smsDao.deleteAll(messages)
    }
}
