package com.fantasmo.sdk.utils

import com.android.volley.Cache
import com.android.volley.NetworkResponse
import java.util.*

class CacheTestUtils {

    /**
     * Makes a random cache entry.
     * @param response Data to use, or null to use random data
     * @param isExpired Whether the TTLs should be set such that this entry is expired
     * @param needsRefresh Whether the TTLs should be set such that this entry needs refresh
     */
    private fun makeRandomCacheEntry(
        response: NetworkResponse?, isExpired: Boolean, needsRefresh: Boolean
    ): Cache.Entry {
        val random = Random()
        val entry: Cache.Entry = Cache.Entry()
        if (response != null) {
            entry.data = response.data
        } else {
            entry.data = ByteArray(random.nextInt(1024))
        }
        entry.etag = random.nextLong().toString()
        entry.lastModified = random.nextLong()
        entry.ttl = if (isExpired) 0 else Long.MAX_VALUE
        entry.softTtl = if (needsRefresh) 0 else Long.MAX_VALUE
        return entry
    }

    fun makeRandomCacheEntry(response: NetworkResponse?): Cache.Entry {
        return makeRandomCacheEntry(response, isExpired = false, needsRefresh = false)
    }
}