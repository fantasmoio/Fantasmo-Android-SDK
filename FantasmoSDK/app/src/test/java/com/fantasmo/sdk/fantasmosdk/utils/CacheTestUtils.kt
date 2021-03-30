package com.fantasmo.sdk.volley.utils

import com.android.volley.Cache
import com.android.volley.NetworkResponse
import java.lang.String
import java.util.*

class CacheTestUtils {

    /**
     * Makes a random cache entry.
     * @param data Data to use, or null to use random data
     * @param isExpired Whether the TTLs should be set such that this entry is expired
     * @param needsRefresh Whether the TTLs should be set such that this entry needs refresh
     */
    private fun makeRandomCacheEntry(
        data: ByteArray?, isExpired: Boolean, needsRefresh: Boolean
    ): Cache.Entry {
        val random = Random()
        val entry: Cache.Entry = Cache.Entry()
        if (data != null) {
            entry.data = data
        } else {
            entry.data = ByteArray(random.nextInt(1024))
        }
        entry.etag = String.valueOf(random.nextLong())
        entry.lastModified = random.nextLong()
        entry.ttl = if (isExpired) 0 else Long.MAX_VALUE
        entry.softTtl = if (needsRefresh) 0 else Long.MAX_VALUE
        return entry
    }

    /**
     * Like [.makeRandomCacheEntry] but
     * defaults to an unexpired entry.
     */
    fun makeRandomCacheEntry(data: ByteArray?): Cache.Entry {
        return makeRandomCacheEntry(data, isExpired = false, needsRefresh = false)
    }


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
        entry.etag = String.valueOf(random.nextLong())
        entry.lastModified = random.nextLong()
        entry.ttl = if (isExpired) 0 else Long.MAX_VALUE
        entry.softTtl = if (needsRefresh) 0 else Long.MAX_VALUE
        return entry
    }

    fun makeRandomCacheEntry(response: NetworkResponse?): Cache.Entry {
        return makeRandomCacheEntry(response, isExpired = false, needsRefresh = false)
    }
}