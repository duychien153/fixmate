package com.duychien.fixmate.core.common

object Constants {
    const val BASE_URL = "https://dummyjson.com/"
    const val DATABASE_NAME = "fixmate.db"
    const val SETTINGS_DATASTORE_NAME = "fixmate_settings"
    const val SYNC_WORK_NAME = "fixmate_task_sync"

    /** Page size used when paging through the remote /todos endpoint. */
    const val PAGE_SIZE = 30

    /** Upper bound of demo tasks pulled from the remote API on refresh. */
    const val MAX_REMOTE_TASKS = 60

    /** DummyJSON has no auth, so every task created locally is attributed to this demo user. */
    const val DEFAULT_USER_ID = 1L

    const val MIN_TITLE_LENGTH = 3
}
