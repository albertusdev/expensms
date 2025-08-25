package dev.albertus.expensms.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sender_filters")
data class SenderFilter(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String, // User-friendly name like "OCBC Bank"
    @ColumnInfo(name = "filter_pattern") val filterPattern: String, // Simple string to match in sender
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: java.util.Date = java.util.Date(),
    @ColumnInfo(name = "updated_at") val updatedAt: java.util.Date = java.util.Date()
)

// Default sender filters that can be pre-populated
object DefaultSenderFilters {
    val defaults = listOf(
        SenderFilter(
            id = "ocbc",
            name = "OCBC Bank",
            filterPattern = "OCBC",
            isEnabled = true
        ),
        SenderFilter(
            id = "uob",
            name = "UOB Bank", 
            filterPattern = "UOB",
            isEnabled = true
        ),
        SenderFilter(
            id = "bca",
            name = "BCA Bank",
            filterPattern = "BCA",
            isEnabled = true
        ),
        SenderFilter(
            id = "mandiri",
            name = "Bank Mandiri",
            filterPattern = "MANDIRI",
            isEnabled = true
        ),
        SenderFilter(
            id = "bni",
            name = "BNI Bank",
            filterPattern = "BNI",
            isEnabled = true
        ),
        SenderFilter(
            id = "bri",
            name = "BRI Bank",
            filterPattern = "BRI",
            isEnabled = true
        ),
        SenderFilter(
            id = "cimb",
            name = "CIMB Bank",
            filterPattern = "CIMB",
            isEnabled = true
        ),
        SenderFilter(
            id = "danamon",
            name = "Bank Danamon",
            filterPattern = "DANAMON",
            isEnabled = true
        ),
        SenderFilter(
            id = "permata",
            name = "Bank Permata",
            filterPattern = "PERMATA",
            isEnabled = true
        ),
        SenderFilter(
            id = "hsbc",
            name = "HSBC Bank",
            filterPattern = "HSBC",
            isEnabled = true
        )
    )
}
