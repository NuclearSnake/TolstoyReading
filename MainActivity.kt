package com.neoproduction.template

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException

class WordPair(var key: String?, var value: Int)

class MainActivity : AppCompatActivity() {
    private val TAG = "TolstoyReader"
    private lateinit var okHttp: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        okHttp = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url("http://az.lib.ru/t/tolstoj_lew_nikolaewich/text_0040.shtml")
            .build()

        getTolstoy(request, ::processInput)
    }

    private fun getTolstoy(request: Request, onTolstoy: (html: String?) -> Unit) {
        okHttp.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to read Tolstoy :'(")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val html = response.body?.string()
                onTolstoy(html)
            }
        })
    }

    private fun processInput(html: String?) {
        val index = html?.indexOf("<!----------- Собственно произведение --------------->")
        if (index == null || index < 0) {
            // Couldn't read html or failed to find the start of the book
            Log.e(TAG, "Failed to understand Tolstoy :'(")
            return
        }

        val text = html.substring(index)
        val readyText = preProcessText(text)
        val wordsMap = getWordsOccurrencesMap(readyText)
        val maxes = wordsMap.getMaxes(10)
        val maxesSorted = maxes.toList().sortedByDescending { (_, value) -> value }
        val b = StringBuilder()
        for (m in maxesSorted) {
            b.append(m.first)
            b.append(" -> ")
            b.append(m.second)
            b.append("\n")
        }
        Log.d(TAG, "Maxes: \n $b")
    }

    private fun preProcessText(text: String): String =
        text.replace("&nbsp;", " ")
            .replace("<dd>", "")


    /**
     * Returns hashmap with a structure 'word -> occurrences'
     */
    private fun getWordsOccurrencesMap(text: String): HashMap<String, Int> {
        val allWords = text.split("([.,!?:;'\"-]|\\s)+".toRegex())
        val wordsMap = hashMapOf<String, Int>()

        for (word in allWords) {
            // if no such key - then -1 to form 0 for initial occurrence
            wordsMap[word] = (wordsMap[word] ?: -1) + 1
        }
        return wordsMap
    }


    /**
     * Returns subMap of N maximum values for the given map
     */
    private fun Map<String, Int>.getMaxes(howMuch: Int): Map<String, Int> {
        if (howMuch < 1) {
            Log.e(TAG, "Can't get negative number of maxes!")
            return mapOf()
        }

        val maxes = mutableMapOf<String, Int>()
        var min = maxes.min()
        for ((nKey, nValue) in this) {
            if (maxes.size < howMuch) {
                maxes[nKey] = nValue

                // The new element could be the new min
                if (min.value > nValue) {
                    min.key = nKey
                    min.value = nValue
                }
            } else if (nValue > min.value) {
                // min.key must not be null here because map size is > 0
                maxes.remove(min.key)
                maxes[nKey] = nValue

                // Recalculate because we just excluded our last min
                min = maxes.min()
            }
        }

        return maxes
    }

    /**
     * Returns pair key+value that represents the minimum value for the given map
     */
    private fun Map<String, Int>.min(): WordPair {
        val minResult = WordPair(null, Int.MAX_VALUE)
        for ((key, value) in this) {
            if (value < minResult.value) {
                minResult.value = value
                minResult.key = key
            }
        }

        return minResult
    }
}
