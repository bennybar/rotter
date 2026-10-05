package com.bennybarak.scoops.rotter_scoops.ui

// Ported from the Flutter build's app_en.arb / app_he.arb.

abstract class Strings {
    abstract val brandRotter: String
    abstract val newCommentsBadge: String
    abstract val colorTheme: String
    abstract val themeClassic: String
    abstract val themeRed: String
    abstract val themeBlue: String
    abstract val themeGreen: String
    abstract val themePurple: String
    abstract val themeGraphite: String
    abstract val themeLavender: String
    abstract val sortTitle: String
    abstract val appTitle: String
    abstract val tabScoops: String
    abstract val tabNewMessage: String
    abstract val tabSettings: String
    abstract val settingsTitle: String
    abstract val appearance: String
    abstract val theme: String
    abstract val themeSystem: String
    abstract val themeLight: String
    abstract val themeDark: String
    abstract val accentColor: String
    abstract val textSize: String
    abstract val threadSpacing: String
    abstract val predictiveBack: String
    abstract val predictiveBackHint: String
    abstract val language: String
    abstract val languageSystem: String
    abstract val hebrew: String
    abstract val english: String
    abstract val account: String
    abstract val signIn: String
    abstract val usernameLabel: String
    abstract val passwordLabel: String
    abstract val loginFailed: String
    abstract val loginError: String
    abstract val signOut: String
    abstract val signOutConfirm: String
    abstract val signedIn: String
    abstract val loginSubtitle: String
    abstract val newMessagePrompt: String
    abstract val newMessagePromptBody: String
    abstract val compose: String
    abstract val loadingError: String
    abstract val threadNotReady: String
    abstract val scoopRemoved: String
    abstract val removedBadge: String
    abstract val retry: String
    abstract fun replies(count: Int): String
    abstract val reply: String
    abstract val edit: String
    abstract val save: String
    abstract val send: String
    abstract val composeHint: String
    abstract val subjectHint: String
    abstract val bodyHint: String
    abstract val postFailed: String
    abstract val postSuccess: String
    abstract val markRead: String
    abstract val markUnread: String
    abstract val markAllRead: String
    abstract val markAllReadConfirm: String
    abstract val markedAllRead: String
    abstract val cancel: String
    abstract val newBadge: String
    abstract val today: String
    abstract val yesterday: String
    abstract val earlier: String
    abstract val searchScoops: String
    abstract val noResults: String
    abstract val op: String
    abstract val jumpToNewest: String
    abstract val nextComment: String
    abstract val myReply: String
    abstract val justNow: String
    abstract fun minutesAgo(n: Int): String
    abstract fun hoursAgo(n: Int): String
    abstract fun daysAgo(n: Int): String
    abstract val titleOnly: String
    abstract val emptyScoops: String
    abstract val filterMine: String
    abstract val noMyReplies: String
    abstract val sortBy: String
    abstract val sortLastComment: String
    abstract val sortPostTime: String
    abstract val userDetails: String
    abstract val memberPoints: String
    abstract val memberRaters: String
    abstract val memberPosts: String
    abstract val memberSince: String
    abstract fun userPostsInThread(count: Int): String
    abstract val about: String
    abstract val aboutBody: String
    abstract val filterTitle: String
    abstract val filterAll: String
    abstract val filterUnread: String
    abstract val filterNewComments: String
    abstract val filterSaved: String
    abstract val filterFollowing: String
    abstract val filterShowAll: String
    abstract val emptyUnread: String
    abstract val emptyNewComments: String
    abstract val emptySaved: String
    abstract val emptyFollowing: String
    abstract val refreshFailed: String
    abstract val saveScoop: String
    abstract val unsaveScoop: String
    abstract val share: String
    abstract val openOnRotter: String
    abstract val feedNewScoops: String
    abstract val feedUpdated: String
    abstract val follow: String
    abstract val unfollow: String
    abstract val commentNew: String
    abstract val backToReply: String
    abstract val previousNew: String
    abstract val nextNew: String
    abstract fun newOfCount(index: Int, count: Int): String
    abstract fun newRepliesCount(count: Int): String
    abstract val searchThread: String
    abstract val searchThreadHint: String
    abstract val previousMatch: String
    abstract val nextMatch: String
    abstract fun matchesCount(count: Int): String
    abstract val collapseAll: String
    abstract val expandAll: String
    abstract val resumeReading: String
    abstract val startTop: String
    abstract val navigationHint: String
    abstract val close: String
    abstract fun replyingTo(name: String): String
    abstract fun depthLevel(n: Int): String
    abstract val hideReplies: String
    abstract fun showReplies(count: Int): String
    abstract val discardDraftTitle: String
    abstract val discard: String
    abstract val keepEditing: String
    abstract val notSignedInError: String
    abstract val blockedError: String
    abstract val copy: String
    abstract val copied: String
    abstract val aiSection: String
    abstract val aiEnable: String
    abstract val aiKey: String
    abstract val aiKeyPlaceholder: String
    abstract val aiKeyStored: String
    abstract val aiSaveKey: String
    abstract val aiRemoveKey: String
    abstract val aiModel: String
    abstract val aiEndpoint: String
    abstract val aiAdvanced: String
    abstract val aiPrivacyNote: String
    abstract val aiSummary: String
    abstract val aiSummarize: String
    abstract val aiSummarizing: String
    abstract val aiRegenerate: String
    abstract val aiDisclaimer: String
    abstract val aiErrorTitle: String
    abstract val aiErrorNotConfigured: String
    abstract val aiErrorEmpty: String
    abstract val aiLanguage: String
    abstract val aiLanguageFollowApp: String
    abstract val aiLanguageHebrew: String
    abstract val aiLanguageEnglish: String
    abstract val aiStatusConfigured: String
    abstract val aiStatusOff: String
}

object StringsEn : Strings() {
    override val brandRotter = "Rotter"
    override val newCommentsBadge = "New comments"
    override val colorTheme = "Color theme"
    override val themeClassic = "Classic"
    override val themeRed = "Crimson"
    override val themeBlue = "Ocean"
    override val themeGreen = "Forest"
    override val themePurple = "Violet"
    override val themeGraphite = "Graphite"
    override val themeLavender = "Lavender"
    override val sortTitle = "Sort"
    override val appTitle = "Rotter Scoops"
    override val tabScoops = "Scoops"
    override val tabNewMessage = "New message"
    override val tabSettings = "Settings"
    override val settingsTitle = "Settings"
    override val appearance = "APPEARANCE"
    override val theme = "Theme"
    override val themeSystem = "Device"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val accentColor = "Accent color"
    override val textSize = "Text size"
    override val threadSpacing = "Thread spacing"
    override val predictiveBack = "Predictive back"
    override val predictiveBackHint = "Peek at the previous screen while swiping back"
    override val language = "Language"
    override val languageSystem = "Device"
    override val hebrew = "עברית"
    override val english = "English"
    override val account = "ACCOUNT"
    override val signIn = "Sign in"
    override val usernameLabel = "Username"
    override val passwordLabel = "Password"
    override val loginFailed = "Wrong username or password"
    override val loginError = "Sign-in failed — check your connection and try again"
    override val signOut = "Sign out"
    override val signOutConfirm = "Sign out of rotter? Your saved sign-in and the list of scoops you replied to will be removed from this device."
    override val signedIn = "Signed in"
    override val loginSubtitle = "Sign in to rotter.net to post"
    override val newMessagePrompt = "Sign in to post a new message"
    override val newMessagePromptBody = "Posting on Rotter requires a signed-in account. Sign in once and you can open a new thread right here."
    override val compose = "New thread"
    override val loadingError = "Couldn't load"
    override val threadNotReady = "This scoop isn't available yet — try again in a moment"
    override val scoopRemoved = "This scoop is no longer available — it was most likely removed"
    override val removedBadge = "Removed"
    override val retry = "Retry"
    override fun replies(count: Int) = when (count) {
            0 -> "No replies"
            1 -> "1 reply"
            else -> "$count replies"
        }
    override val reply = "Reply"
    override val edit = "Edit"
    override val save = "Save"
    override val send = "Send"
    override val composeHint = "Write your reply…"
    override val subjectHint = "Headline"
    override val bodyHint = "Write your scoop…"
    override val postFailed = "Couldn't post — check your connection and try again"
    override val postSuccess = "Posted"
    override val markRead = "Read"
    override val markUnread = "Unread"
    override val markAllRead = "Mark all read"
    override val markAllReadConfirm = "Mark all scoops as read?"
    override val markedAllRead = "All marked as read"
    override val cancel = "Cancel"
    override val newBadge = "New"
    override val today = "Today"
    override val yesterday = "Yesterday"
    override val earlier = "Earlier"
    override val searchScoops = "Search scoops"
    override val noResults = "No matching scoops"
    override val op = "OP"
    override val jumpToNewest = "Jump to newest"
    override val nextComment = "Next comment"
    override val myReply = "Your reply"
    override val justNow = "just now"
    override fun minutesAgo(n: Int) = "${n}m ago"
    override fun hoursAgo(n: Int) = "${n}h ago"
    override fun daysAgo(n: Int) = "${n}d ago"
    override val titleOnly = "(headline only)"
    override val emptyScoops = "No scoops yet"
    override val filterMine = "Scoops I replied to"
    override val noMyReplies = "You haven't replied to any scoops yet"
    override val sortBy = "Sort scoops by"
    override val sortLastComment = "Last comment"
    override val sortPostTime = "Post time"
    override val userDetails = "Member details"
    override val memberPoints = "Points"
    override val memberRaters = "Raters"
    override val memberPosts = "Posts"
    override val memberSince = "Member since"
    override fun userPostsInThread(count: Int) = when (count) {
            0 -> "No posts in this thread"
            1 -> "1 post in this thread"
            else -> "$count posts in this thread"
        }
    override val about = "ABOUT"
    override val aboutBody = "An unofficial reader for rotter.net scoops."
    override val filterTitle = "Show"
    override val filterAll = "All"
    override val filterUnread = "Unread"
    override val filterNewComments = "New comments"
    override val filterSaved = "Saved"
    override val filterFollowing = "Following"
    override val filterShowAll = "Show all scoops"
    override val emptyUnread = "Nothing unread"
    override val emptyNewComments = "No new comments"
    override val emptySaved = "No saved scoops yet"
    override val emptyFollowing = "No followed discussions yet"
    override val refreshFailed = "Couldn't refresh — showing previous results"
    override val saveScoop = "Save scoop"
    override val unsaveScoop = "Remove from saved"
    override val share = "Share"
    override val openOnRotter = "Open on rotter.net"
    override val feedNewScoops = "New scoops available"
    override val feedUpdated = "Scoop updates available"
    override val follow = "Follow discussion"
    override val unfollow = "Unfollow discussion"
    override val commentNew = "New since your last visit"
    override val backToReply = "Back to reply"
    override val previousNew = "Previous new comment"
    override val nextNew = "Next new comment"
    override fun newOfCount(index: Int, count: Int) = "$index of $count new"
    override fun newRepliesCount(count: Int) = "New replies: $count"
    override val searchThread = "Search discussion"
    override val searchThreadHint = "Text or username"
    override val previousMatch = "Previous match"
    override val nextMatch = "Next match"
    override fun matchesCount(count: Int) = "Matches: $count"
    override val collapseAll = "Collapse all discussions"
    override val expandAll = "Expand all discussions"
    override val resumeReading = "Resume reading"
    override val startTop = "Start at the top"
    override val navigationHint = "Use the floating arrows to move between discussions or jump to the latest reply."
    override val close = "Close"
    override fun replyingTo(name: String) = "Replying to $name"
    override fun depthLevel(n: Int) = "Nesting level $n"
    override val hideReplies = "Hide replies"
    override fun showReplies(count: Int) = when (count) {
            1 -> "Show 1 reply"
            else -> "Show $count replies"
        }
    override val discardDraftTitle = "Discard this draft?"
    override val discard = "Discard"
    override val keepEditing = "Keep editing"
    override val notSignedInError = "You're not signed in to rotter"
    override val blockedError = "rotter blocked the request — try again in a moment"
    override val copy = "Copy"
    override val copied = "Copied"
    override val aiSection = "AI summary"
    override val aiEnable = "Summarize threads"
    override val aiKey = "API key"
    override val aiKeyPlaceholder = "OpenAI API key"
    override val aiKeyStored = "Key stored — enter a new one to replace"
    override val aiSaveKey = "Save key"
    override val aiRemoveKey = "Remove"
    override val aiModel = "Model"
    override val aiEndpoint = "Endpoint"
    override val aiAdvanced = "Advanced"
    override val aiPrivacyNote = "Summarizing sends the post and its comments to the endpoint below. Your key is kept in the device's secure storage and is never sent anywhere else."
    override val aiSummary = "Summary"
    override val aiSummarize = "Summarize"
    override val aiSummarizing = "Summarizing…"
    override val aiRegenerate = "Regenerate"
    override val aiDisclaimer = "Written by a language model from the thread's own text. rotter posts are often unverified — check before relying on it."
    override val aiErrorTitle = "Couldn't summarize"
    override val aiErrorNotConfigured = "Add an OpenAI API key in Settings first"
    override val aiErrorEmpty = "The model returned nothing"
    override val aiLanguage = "Summary language"
    override val aiLanguageFollowApp = "Same as app"
    override val aiLanguageHebrew = "Hebrew"
    override val aiLanguageEnglish = "English"
    override val aiStatusConfigured = "Configured"
    override val aiStatusOff = "Not configured"
}

object StringsHe : Strings() {
    override val brandRotter = "רוטר"
    override val newCommentsBadge = "תגובות חדשות"
    override val colorTheme = "ערכת צבעים"
    override val themeClassic = "קלאסי"
    override val themeRed = "ארגמן"
    override val themeBlue = "אוקיינוס"
    override val themeGreen = "יער"
    override val themePurple = "סגול"
    override val themeGraphite = "גרפיט"
    override val themeLavender = "לבנדר"
    override val sortTitle = "מיון"
    override val appTitle = "רוטר סקופים"
    override val tabScoops = "סקופים"
    override val tabNewMessage = "הודעה חדשה"
    override val tabSettings = "הגדרות"
    override val settingsTitle = "הגדרות"
    override val appearance = "מראה"
    override val theme = "ערכת נושא"
    override val themeSystem = "מערכת"
    override val themeLight = "בהיר"
    override val themeDark = "כהה"
    override val accentColor = "צבע הדגשה"
    override val textSize = "גודל טקסט"
    override val threadSpacing = "ריווח באשכול"
    override val predictiveBack = "חזרה עם הצצה"
    override val predictiveBackHint = "הצצה למסך הקודם בזמן החלקה לאחור"
    override val language = "שפה"
    override val languageSystem = "מערכת"
    override val hebrew = "עברית"
    override val english = "English"
    override val account = "חשבון"
    override val signIn = "התחברות"
    override val usernameLabel = "שם משתמש"
    override val passwordLabel = "סיסמה"
    override val loginFailed = "שם משתמש או סיסמה שגויים"
    override val loginError = "ההתחברות נכשלה — בדקו את החיבור ונסו שוב"
    override val signOut = "התנתקות"
    override val signOutConfirm = "להתנתק מרוטר? פרטי ההתחברות השמורים ורשימת הסקופים שהגבת בהם יימחקו מהמכשיר."
    override val signedIn = "מחובר/ת"
    override val loginSubtitle = "התחברות ל-rotter.net כדי לפרסם"
    override val newMessagePrompt = "התחבר/י כדי לפרסם הודעה חדשה"
    override val newMessagePromptBody = "פרסום ברוטר מחייב חשבון מחובר. התחבר/י פעם אחת, ותוכל/י לפתוח אשכול חדש כאן."
    override val compose = "אשכול חדש"
    override val loadingError = "הטעינה נכשלה"
    override val threadNotReady = "הסקופ עדיין לא זמין — נסה שוב בעוד רגע"
    override val scoopRemoved = "הסקופ אינו זמין יותר — ככל הנראה הוסר"
    override val removedBadge = "הוסר"
    override val retry = "נסה שוב"
    override fun replies(count: Int) = when (count) {
            0 -> "אין תגובות"
            1 -> "תגובה אחת"
            else -> "$count תגובות"
        }
    override val reply = "תגובה"
    override val edit = "עריכה"
    override val save = "שמור"
    override val send = "שלח"
    override val composeHint = "כתוב/כתבי תגובה…"
    override val subjectHint = "כותרת"
    override val bodyHint = "כתוב/כתבי את הסקופ…"
    override val postFailed = "הפרסום נכשל — בדקו את החיבור ונסו שוב"
    override val postSuccess = "פורסם"
    override val markRead = "נקרא"
    override val markUnread = "לא נקרא"
    override val markAllRead = "סמן הכל כנקרא"
    override val markAllReadConfirm = "לסמן את כל הסקופים כנקראו?"
    override val markedAllRead = "הכל סומן כנקרא"
    override val cancel = "ביטול"
    override val newBadge = "חדש"
    override val today = "היום"
    override val yesterday = "אתמול"
    override val earlier = "מוקדם יותר"
    override val searchScoops = "חיפוש בסקופים"
    override val noResults = "לא נמצאו סקופים"
    override val op = "כותב"
    override val jumpToNewest = "לתגובה האחרונה"
    override val nextComment = "לתגובה הבאה"
    override val myReply = "התגובה שלך"
    override val justNow = "כעת"
    override fun minutesAgo(n: Int) = "לפני $n דק׳"
    override fun hoursAgo(n: Int) = "לפני $n שע׳"
    override fun daysAgo(n: Int) = "לפני $n ימ׳"
    override val titleOnly = "(כותרת בלבד)"
    override val emptyScoops = "אין סקופים עדיין"
    override val filterMine = "סקופים שהגבתי בהם"
    override val noMyReplies = "עדיין לא הגבת לאף סקופ"
    override val sortBy = "מיון סקופים לפי"
    override val sortLastComment = "תגובה אחרונה"
    override val sortPostTime = "זמן פרסום"
    override val userDetails = "פרטי חבר"
    override val memberPoints = "נקודות"
    override val memberRaters = "מדרגים"
    override val memberPosts = "הודעות"
    override val memberSince = "חבר מתאריך"
    override fun userPostsInThread(count: Int) = when (count) {
            0 -> "אין הודעות באשכול"
            1 -> "הודעה אחת באשכול"
            else -> "$count הודעות באשכול"
        }
    override val about = "אודות"
    override val aboutBody = "קורא לא רשמי לסקופים של rotter.net."
    override val filterTitle = "הצג"
    override val filterAll = "הכל"
    override val filterUnread = "לא נקראו"
    override val filterNewComments = "תגובות חדשות"
    override val filterSaved = "שמורים"
    override val filterFollowing = "במעקב"
    override val filterShowAll = "הצג את כל הסקופים"
    override val emptyUnread = "הכל נקרא"
    override val emptyNewComments = "אין תגובות חדשות"
    override val emptySaved = "אין עדיין סקופים שמורים"
    override val emptyFollowing = "אין עדיין דיונים במעקב"
    override val refreshFailed = "הרענון נכשל — מוצגות התוצאות הקודמות"
    override val saveScoop = "שמירת סקופ"
    override val unsaveScoop = "הסרה מהשמורים"
    override val share = "שיתוף"
    override val openOnRotter = "פתיחה ב-rotter.net"
    override val feedNewScoops = "סקופים חדשים זמינים"
    override val feedUpdated = "עדכונים לסקופים זמינים"
    override val follow = "עקוב אחר הדיון"
    override val unfollow = "הפסק לעקוב"
    override val commentNew = "חדש מאז הביקור האחרון"
    override val backToReply = "חזרה לתגובה"
    override val previousNew = "התגובה החדשה הקודמת"
    override val nextNew = "התגובה החדשה הבאה"
    override fun newOfCount(index: Int, count: Int) = "חדשות: $index מתוך $count"
    override fun newRepliesCount(count: Int) = "תגובות חדשות: $count"
    override val searchThread = "חיפוש בדיון"
    override val searchThreadHint = "טקסט או שם משתמש"
    override val previousMatch = "התוצאה הקודמת"
    override val nextMatch = "התוצאה הבאה"
    override fun matchesCount(count: Int) = "תוצאות: $count"
    override val collapseAll = "כווץ את כל הדיונים"
    override val expandAll = "הרחב את כל הדיונים"
    override val resumeReading = "המשך מהמקום שבו עצרת"
    override val startTop = "התחל מלמעלה"
    override val navigationHint = "החצים הצפים מאפשרים לעבור בין דיונים או לדלג לתגובה החדשה ביותר."
    override val close = "סגור"
    override fun replyingTo(name: String) = "בתגובה ל$name"
    override fun depthLevel(n: Int) = "עומק $n"
    override val hideReplies = "הסתר תגובות"
    override fun showReplies(count: Int) = when (count) {
            1 -> "הצג תגובה אחת"
            else -> "הצג $count תגובות"
        }
    override val discardDraftTitle = "למחוק את הטיוטה?"
    override val discard = "מחיקה"
    override val keepEditing = "המשך עריכה"
    override val notSignedInError = "אינך מחובר/ת לרוטר"
    override val blockedError = "רוטר חסם את הבקשה — נסו שוב בעוד רגע"
    override val copy = "העתקה"
    override val copied = "הועתק"
    override val aiSection = "סיכום AI"
    override val aiEnable = "סיכום אשכולות"
    override val aiKey = "מפתח API"
    override val aiKeyPlaceholder = "מפתח API של OpenAI"
    override val aiKeyStored = "מפתח שמור — הזינו חדש כדי להחליף"
    override val aiSaveKey = "שמירת מפתח"
    override val aiRemoveKey = "הסרה"
    override val aiModel = "מודל"
    override val aiEndpoint = "כתובת ה-API"
    override val aiAdvanced = "מתקדם"
    override val aiPrivacyNote = "סיכום שולח את ההודעה והתגובות לכתובת שלמטה. המפתח נשמר באחסון המאובטח של המכשיר ואינו נשלח לשום מקום אחר."
    override val aiSummary = "סיכום"
    override val aiSummarize = "סיכום"
    override val aiSummarizing = "מסכם…"
    override val aiRegenerate = "יצירה מחדש"
    override val aiDisclaimer = "נכתב על ידי מודל שפה מתוך טקסט האשכול. פרסומים ברוטר לרוב אינם מאומתים — בדקו לפני שתסתמכו על זה."
    override val aiErrorTitle = "הסיכום נכשל"
    override val aiErrorNotConfigured = "הוסיפו מפתח API של OpenAI בהגדרות"
    override val aiErrorEmpty = "המודל לא החזיר תשובה"
    override val aiLanguage = "שפת הסיכום"
    override val aiLanguageFollowApp = "כמו האפליקציה"
    override val aiLanguageHebrew = "עברית"
    override val aiLanguageEnglish = "אנגלית"
    override val aiStatusConfigured = "מוגדר"
    override val aiStatusOff = "לא מוגדר"
}
