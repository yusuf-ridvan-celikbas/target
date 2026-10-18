package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MoneyKind { EXPENSE, INCOME }

/**
 * A spending or income category. Seeded ones carry a [systemKey] (translated for display while
 * [name] is null); renaming sets [name]. Hidden ones aren't offered for new entries.
 */
@Entity(
    tableName = "money_categories",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class MoneyCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val kind: MoneyKind,
    val systemKey: String? = null,
    val name: String? = null,
    val isHidden: Boolean = false,
    val orderIndex: Int = 0,
)

/** One income or expense; [date] is a day start. [recurringId] is set on entries a recurring item created. */
@Entity(
    tableName = "money_entries",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MoneyCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = MoneyRecurring::class, parentColumns = ["id"], childColumns = ["recurringId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("userId"), Index("categoryId"), Index("recurringId")],
)
data class MoneyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val kind: MoneyKind,
    val amountMinor: Long,
    val categoryId: Long? = null,
    val note: String? = null,
    val date: Long,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A monthly budget for one expense category, or for all spending when [categoryId] is null. */
@Entity(
    tableName = "money_budgets",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MoneyCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId"), Index("categoryId")],
)
data class MoneyBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val categoryId: Long? = null,
    val amountMinor: Long,
)

/**
 * Income or expense that repeats every month on [dayOfMonth] (clamped to short months), from
 * [startMonth] (year × 12 + month index) on; entries are created for months that have come.
 */
@Entity(
    tableName = "money_recurring",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MoneyCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("userId"), Index("categoryId")],
)
data class MoneyRecurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val kind: MoneyKind,
    val amountMinor: Long,
    val categoryId: Long? = null,
    val note: String? = null,
    val dayOfMonth: Int,
    val startMonth: Int,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Saving up for something: [savedMinor] grows as amounts are added. */
@Entity(
    tableName = "money_goals",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class MoneyGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val targetMinor: Long,
    val savedMinor: Long = 0,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
