package com.example.smsfirewall.data

import com.example.smsfirewall.data.local.SmsDao
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.data.security.CryptoBox
import com.example.smsfirewall.filter.SmsStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository @Inject constructor(
    private val smsDao: SmsDao,
    private val cryptoBox: CryptoBox
) {
    suspend fun insert(sms: SmsEntity) {
        smsDao.insert(sms.encryptForStorage())
    }

    fun getAll(): Flow<List<SmsEntity>> {
        return smsDao.getAll().map { messages -> messages.map { it.decryptFromStorage() } }
    }

    fun getByStatus(status: String): Flow<List<SmsEntity>> {
        return smsDao.getByStatus(status).map { messages -> messages.map { it.decryptFromStorage() } }
    }

    fun getByStatusNot(status: String): Flow<List<SmsEntity>> {
        return smsDao.getByStatusNot(status).map { messages -> messages.map { it.decryptFromStorage() } }
    }

    suspend fun deleteByStatusBefore(status: String, beforeTimestamp: Long): Int {
        return smsDao.deleteByStatusBefore(status, beforeTimestamp)
    }

    suspend fun deleteAllByStatus(status: String): Int {
        return smsDao.deleteAllByStatus(status)
    }

    suspend fun getAllByStatus(status: String): List<SmsEntity> {
        return smsDao.getAllByStatus(status).map { it.decryptFromStorage() }
    }

    suspend fun delete(sms: SmsEntity) {
        smsDao.deleteById(sms.id)
    }

    suspend fun deleteAll(messages: List<SmsEntity>) {
        val ids = messages.map { it.id }.filter { it > 0L }
        if (ids.isNotEmpty()) smsDao.deleteByIds(ids)
    }

    suspend fun migratePlaintextSensitiveRows() {
        smsDao.getAllByStatuses(SENSITIVE_STATUSES)
            .filterNot { it.isStoredEncrypted() }
            .forEach { smsDao.insert(it.encryptForStorage()) }
    }

    private fun SmsEntity.encryptForStorage(): SmsEntity {
        if (status !in SENSITIVE_STATUSES || isStoredEncrypted()) return this
        return copy(
            sender = cryptoBox.encrypt(sender),
            body = cryptoBox.encrypt(body),
            reason = cryptoBox.encrypt(reason)
        )
    }

    private fun SmsEntity.decryptFromStorage(): SmsEntity {
        if (status !in SENSITIVE_STATUSES) return this
        return copy(
            sender = cryptoBox.decrypt(sender),
            body = cryptoBox.decrypt(body),
            reason = cryptoBox.decrypt(reason)
        )
    }

    private fun SmsEntity.isStoredEncrypted(): Boolean {
        return cryptoBox.isEncrypted(sender) &&
            cryptoBox.isEncrypted(body) &&
            cryptoBox.isEncrypted(reason)
    }

    private companion object {
        val SENSITIVE_STATUSES = listOf(SmsStatus.BLOCK, SmsStatus.TRASH)
    }
}
