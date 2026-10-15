package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A reusable packing list, saved from a trip and added to others. Per user. */
@Entity(
    tableName = "trip_packing_templates",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId")],
)
data class TripPackingTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
)

@Entity(
    tableName = "trip_packing_template_items",
    foreignKeys = [
        ForeignKey(entity = TripPackingTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId")],
)
data class TripPackingTemplateItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val name: String,
    val category: TripPackingCategory = TripPackingCategory.OTHER,
    val quantity: Int = 1,
)
