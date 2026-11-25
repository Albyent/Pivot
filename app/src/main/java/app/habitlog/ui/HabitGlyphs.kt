package app.habitlog.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Shower
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.habitlog.model.HabitColor
import app.habitlog.model.HabitIcon

internal val HabitColor.tint: Color
    get() = Color(argb)

internal val HabitIcon.glyph: ImageVector
    get() = when (this) {
        HabitIcon.TARGET -> Icons.Outlined.TrackChanges
        HabitIcon.TIMER -> Icons.Outlined.Timer
        HabitIcon.MORNING -> Icons.Outlined.WbSunny
        HabitIcon.ALARM -> Icons.Outlined.Alarm
        HabitIcon.CALENDAR -> Icons.Outlined.CalendarMonth
        HabitIcon.CHECKLIST -> Icons.Outlined.Checklist
        HabitIcon.COFFEE -> Icons.Outlined.LocalCafe
        HabitIcon.SHOPPING -> Icons.Outlined.ShoppingBasket
        HabitIcon.SAVINGS -> Icons.Outlined.Savings
        HabitIcon.PEOPLE -> Icons.Outlined.Group
        HabitIcon.WORKOUT -> Icons.Outlined.FitnessCenter
        HabitIcon.WALKING -> Icons.AutoMirrored.Outlined.DirectionsWalk
        HabitIcon.RUNNING -> Icons.AutoMirrored.Outlined.DirectionsRun
        HabitIcon.CYCLING -> Icons.AutoMirrored.Outlined.DirectionsBike
        HabitIcon.HIKING -> Icons.Outlined.Hiking
        HabitIcon.SWIMMING -> Icons.Outlined.Pool
        HabitIcon.BASKETBALL -> Icons.Outlined.SportsBasketball
        HabitIcon.SOCCER -> Icons.Outlined.SportsSoccer
        HabitIcon.TENNIS -> Icons.Outlined.SportsTennis
        HabitIcon.MARTIAL_ARTS -> Icons.Outlined.SportsMartialArts
        HabitIcon.WATER -> Icons.Outlined.WaterDrop
        HabitIcon.MINDFULNESS -> Icons.Outlined.SelfImprovement
        HabitIcon.SLEEP -> Icons.Outlined.Bedtime
        HabitIcon.MEDICATION -> Icons.Outlined.Medication
        HabitIcon.HEART -> Icons.Outlined.FavoriteBorder
        HabitIcon.MEALS -> Icons.Outlined.Restaurant
        HabitIcon.HEALTH -> Icons.Outlined.MonitorHeart
        HabitIcon.SELF_CARE -> Icons.Outlined.Spa
        HabitIcon.HANDWASHING -> Icons.Outlined.CleanHands
        HabitIcon.SHOWER -> Icons.Outlined.Shower
        HabitIcon.READING -> Icons.AutoMirrored.Outlined.MenuBook
        HabitIcon.JOURNAL -> Icons.Outlined.EditNote
        HabitIcon.STUDY -> Icons.Outlined.School
        HabitIcon.WORK -> Icons.Outlined.WorkOutline
        HabitIcon.CODE -> Icons.Outlined.Code
        HabitIcon.LANGUAGE -> Icons.Outlined.Translate
        HabitIcon.IDEAS -> Icons.Outlined.Lightbulb
        HabitIcon.LAPTOP -> Icons.Outlined.LaptopMac
        HabitIcon.MATH -> Icons.Outlined.Calculate
        HabitIcon.CLEANING -> Icons.Outlined.CleaningServices
        HabitIcon.LAUNDRY -> Icons.Outlined.LocalLaundryService
        HabitIcon.GARDENING -> Icons.Outlined.Yard
        HabitIcon.PETS -> Icons.Outlined.Pets
        HabitIcon.MUSIC -> Icons.Outlined.MusicNote
        HabitIcon.ART -> Icons.Outlined.Palette
        HabitIcon.PHOTO -> Icons.Outlined.PhotoCamera
        HabitIcon.PUZZLES -> Icons.Outlined.Extension
        HabitIcon.HOUSE -> Icons.Outlined.Home
    }
