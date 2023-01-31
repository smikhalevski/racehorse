package com.example.myapplication

import com.google.gson.Gson
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

        val a = Gson().fromJson("{\"hey\":456,\"wow\":\"rrr\",\"baz\":\"ttt\"}", Foo::class.java)

        assertEquals(4, 2 + 2)
    }
}



class Foo {
    val hey = 123
    val wow = "asd"
}