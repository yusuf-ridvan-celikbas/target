package com.ridvan.target.data.focustimer

import androidx.annotation.StringRes
import com.ridvan.target.R

/** The four Focus Timer moments that each get their own user-selectable sound. */
enum class FocusSoundEvent(val prefKey: String, @StringRes val labelRes: Int) {
    WORK_START("focus_sound_work_start", R.string.focus_sound_work_start),
    WORK_END("focus_sound_work_end", R.string.focus_sound_work_end),
    BREAK_START("focus_sound_break_start", R.string.focus_sound_break_start),
    BREAK_END("focus_sound_break_end", R.string.focus_sound_break_end),
}
