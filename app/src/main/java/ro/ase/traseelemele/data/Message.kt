package ro.ase.traseelemele.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val message: String,
    val status: String
)

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: Message)

    @Update
    suspend fun update(message: Message)

    @Query("SELECT * FROM messages")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'Received'")
    fun getReceivedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'Sent via WebSocket' OR status = 'Sent'")
    fun getSentCount(): Flow<Int>

    @Query("SELECT * FROM messages WHERE status = 'Received' ORDER BY id ASC")
    fun getReceivedMessagesAsc(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE status = 'Sent via WebSocket' OR status = 'Sent' ORDER BY id DESC")
    fun getSentMessagesDesc(): Flow<List<Message>>
}
