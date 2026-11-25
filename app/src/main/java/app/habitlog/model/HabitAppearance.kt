package app.habitlog.model

// IDs are stored in habits and backup files, so the picker can be reordered independently.
enum class HabitColor(val id: Int, val label: String, val argb: Long) {
    CHERRY(11, "Cherry", 0xFFF47387),
    CORAL(1, "Coral", 0xFFF29498),
    TANGERINE(13, "Tangerine", 0xFFF29159),
    APRICOT(12, "Apricot", 0xFFF5AB81),
    AMBER(4, "Amber", 0xFFF3C277),
    GOLD(14, "Gold", 0xFFEFD064),
    LEMON(15, "Lemon", 0xFFECE77D),
    OLIVE(7, "Olive", 0xFFB7CD79),
    LIME(16, "Lime", 0xFFA5D46F),
    FERN(17, "Fern", 0xFF7DB778),
    EMERALD(18, "Emerald", 0xFF51BB89),
    MINT(2, "Mint", 0xFF6DCAA8),
    TEAL(19, "Teal", 0xFF4CC3BE),
    AQUA(6, "Aqua", 0xFF65C9DA),
    OCEAN(20, "Ocean", 0xFF64A8DA),
    BLUE(3, "Blue", 0xFF77B7FA),
    SAPPHIRE(21, "Sapphire", 0xFF7297E7),
    PERIWINKLE(8, "Periwinkle", 0xFF8B9AF6),
    LILAC(0, "Lilac", 0xFFAA9BFC),
    PLUM(9, "Plum", 0xFFC286DF),
    FUCHSIA(10, "Fuchsia", 0xFFE783CC),
    ROSE(5, "Rose", 0xFFF29ECF),
    SAND(23, "Sand", 0xFFD8BB9D),
    SILVER(22, "Silver", 0xFFBFC9D9);

    companion object {
        private val byId = entries.associateBy(HabitColor::id)
        val suggestedOrder = entries.sortedBy(HabitColor::id)

        fun fromId(id: Int): HabitColor = byId[id] ?: LILAC
        fun containsId(id: Int): Boolean = id in byId
    }
}

enum class IconGroup(val label: String) {
    DAILY("Everyday"),
    MOVEMENT("Movement"),
    HEALTH("Wellbeing"),
    FOCUS("Study and work"),
    HOME("Home and hobbies"),
}

// The first release used IDs 0 through 7 for its eight icons.
enum class HabitIcon(val id: Int, val label: String, val group: IconGroup) {
    TARGET(0, "Target", IconGroup.DAILY),
    TIMER(7, "Timer", IconGroup.DAILY),
    MORNING(8, "Morning", IconGroup.DAILY),
    ALARM(9, "Alarm", IconGroup.DAILY),
    CALENDAR(10, "Calendar", IconGroup.DAILY),
    CHECKLIST(11, "Checklist", IconGroup.DAILY),
    COFFEE(12, "Coffee", IconGroup.DAILY),
    SHOPPING(13, "Shopping", IconGroup.DAILY),
    SAVINGS(14, "Budget", IconGroup.DAILY),
    PEOPLE(15, "Friends", IconGroup.DAILY),
    WORKOUT(1, "Workout", IconGroup.MOVEMENT),
    WALKING(6, "Walking", IconGroup.MOVEMENT),
    RUNNING(16, "Running", IconGroup.MOVEMENT),
    CYCLING(17, "Cycling", IconGroup.MOVEMENT),
    HIKING(18, "Hiking", IconGroup.MOVEMENT),
    SWIMMING(19, "Swimming", IconGroup.MOVEMENT),
    BASKETBALL(20, "Basketball", IconGroup.MOVEMENT),
    SOCCER(21, "Soccer", IconGroup.MOVEMENT),
    TENNIS(22, "Tennis", IconGroup.MOVEMENT),
    MARTIAL_ARTS(23, "Martial arts", IconGroup.MOVEMENT),
    WATER(3, "Water", IconGroup.HEALTH),
    MINDFULNESS(4, "Mindfulness", IconGroup.HEALTH),
    SLEEP(5, "Sleep", IconGroup.HEALTH),
    MEDICATION(24, "Medication", IconGroup.HEALTH),
    HEART(25, "Heart", IconGroup.HEALTH),
    MEALS(26, "Meals", IconGroup.HEALTH),
    HEALTH(27, "Health", IconGroup.HEALTH),
    SELF_CARE(28, "Self-care", IconGroup.HEALTH),
    HANDWASHING(29, "Handwashing", IconGroup.HEALTH),
    SHOWER(30, "Shower", IconGroup.HEALTH),
    READING(2, "Reading", IconGroup.FOCUS),
    JOURNAL(31, "Journal", IconGroup.FOCUS),
    STUDY(32, "Studying", IconGroup.FOCUS),
    WORK(33, "Work", IconGroup.FOCUS),
    CODE(34, "Coding", IconGroup.FOCUS),
    LANGUAGE(35, "Language", IconGroup.FOCUS),
    IDEAS(36, "Ideas", IconGroup.FOCUS),
    LAPTOP(37, "Laptop", IconGroup.FOCUS),
    MATH(38, "Math", IconGroup.FOCUS),
    CLEANING(39, "Cleaning", IconGroup.HOME),
    LAUNDRY(40, "Laundry", IconGroup.HOME),
    GARDENING(41, "Gardening", IconGroup.HOME),
    PETS(42, "Pets", IconGroup.HOME),
    MUSIC(43, "Music", IconGroup.HOME),
    ART(44, "Art", IconGroup.HOME),
    PHOTO(45, "Photography", IconGroup.HOME),
    PUZZLES(46, "Puzzles", IconGroup.HOME),
    HOUSE(47, "Home", IconGroup.HOME);

    companion object {
        private val byId = entries.associateBy(HabitIcon::id)

        fun fromId(id: Int): HabitIcon = byId[id] ?: TARGET
        fun containsId(id: Int): Boolean = id in byId
    }
}
