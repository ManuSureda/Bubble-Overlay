import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alq.bubbleoverlay.dao.Bubble

@Dao
interface BubbleDao {
    @Insert
    suspend fun insert(bubble: Bubble)

    @Query("SELECT * FROM bubbles")
    suspend fun getAllBubbles(): List<Bubble>

    @Query("SELECT * FROM bubbles WHERE id = :bubbleId")
    suspend fun getBubbleById(bubbleId: Int): Bubble?

    @Update
    suspend fun update(bubble: Bubble)

    @Delete
    suspend fun delete(bubble: Bubble)
}