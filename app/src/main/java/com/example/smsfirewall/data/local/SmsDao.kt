package com.example.smsfirewall.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: SmsEntity)

    @Query("SELECT * FROM sms_messages ORDER BY receivedAt DESC")
    fun getAll(): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_messages WHERE status = :status ORDER BY receivedAt DESC")
    fun getByStatus(status: String): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_messages WHERE status != :status ORDER BY receivedAt DESC")
    fun getByStatusNot(status: String): Flow<List<SmsEntity>>

    @Query("DELETE FROM sms_messages WHERE status = :status AND receivedAt < :beforeTimestamp")
    suspend fun deleteByStatusBefore(status: String, beforeTimestamp: Long): Int

    @Query("SELECT * FROM sms_messages WHERE status = :status")
    suspend fun getAllByStatus(status: String): List<SmsEntity>

    @Delete
    suspend fun delete(sms: SmsEntity)

    @Delete
    suspend fun deleteAll(messages: List<SmsEntity>)
}
