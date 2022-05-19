package com.fantasmo.sdk.utilities.math

import androidx.annotation.Nullable
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.StringBuilder

internal class Preconditions {
    companion object{
        /**
         * Ensures that an object reference passed as a parameter to the calling method is not null.
         *
         * @param reference an object reference
         * @throws NullPointerException if `reference` is null
         */
        fun <T> checkNotNull(@Nullable reference: T?): T {
            if (reference == null) {
                throw NullPointerException()
            }
            return reference
        }

        /**
         * Ensures that an object reference passed as a parameter to the calling method is not null.
         *
         * @param reference an object reference
         * @param errorMessage the exception message to use if the check fails; will be converted to a
         * string using [String.valueOf]
         * @throws NullPointerException if `reference` is null
         */
        fun <T> checkNotNull(@Nullable reference: T?, errorMessage: Any): T {
            if (reference == null) {
                throw NullPointerException(errorMessage.toString())
            }
            return reference
        }

        /**
         * Ensures that `index` specifies a valid *element* in an array, list or string of size
         * `size`. An element index may range from zero, inclusive, to `size`, exclusive.
         *
         * @param index a user-supplied index identifying an element of an array, list or string
         * @param size the size of that array, list or string
         * @throws IndexOutOfBoundsException if `index` is negative or is not less than `size`
         * @throws IllegalArgumentException if `size` is negative
         */
        fun checkElementIndex(index: Int, size: Int) {
            checkElementIndex(index, size, "index")
        }

        /**
         * Ensures that `index` specifies a valid *element* in an array, list or string of size
         * `size`. An element index may range from zero, inclusive, to `size`, exclusive.
         *
         * @param index a user-supplied index identifying an element of an array, list or string
         * @param size the size of that array, list or string
         * @param desc the text to use to describe this index in an error message
         * @throws IndexOutOfBoundsException if `index` is negative or is not less than `size`
         * @throws IllegalArgumentException if `size` is negative
         */
        private fun checkElementIndex(index: Int, size: Int, desc: String) {
            // Carefully optimized for execution by hotspot (explanatory comment above)
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException(badElementIndex(index, size, desc))
            }
        }

        /**
         * Ensures the truth of an expression involving the state of the calling instance, but not
         * involving any parameters to the calling method.
         *
         * @param expression a boolean expression
         * @throws IllegalStateException if `expression` is false
         */
        fun checkState(expression: Boolean) {
            check(expression)
        }

        /**
         * Ensures the truth of an expression involving the state of the calling instance, but not
         * involving any parameters to the calling method.
         *
         * @param expression a boolean expression
         * @param errorMessage the exception message to use if the check fails; will be converted to a
         * string using [String.valueOf]
         * @throws IllegalStateException if `expression` is false
         */
        fun checkState(expression: Boolean, errorMessage: Any?) {
            check(expression) { errorMessage.toString() }
        }

        private fun badElementIndex(index: Int, size: Int, desc: String): String? {
            return when {
                index < 0 -> {
                    format("%s (%s) must not be negative", desc, index)
                }
                size < 0 -> {
                    throw IllegalArgumentException("negative size: $size")
                }
                else -> { // index >= size
                    format("%s (%s) must be less than size (%s)", desc, index, size)
                }
            }
        }

        /**
         * Substitutes each `%s` in `template` with an argument. These are matched by
         * position: the first `%s` gets `args[0]`, etc. If there are more arguments than
         * placeholders, the unmatched arguments will be appended to the end of the formatted message in
         * square braces.
         *
         * @param template a string containing 0 or more `%s` placeholders. null is treated as
         * "null".
         * @param args the arguments to be substituted into the message template. Arguments are converted
         * to strings using [String.valueOf]. Arguments can be null.
         */
        // Note that this is somewhat-improperly used from Verify.java as well.
        private fun format(template: String, vararg args: Any?): String? {
            // start substituting the arguments into the '%s' placeholders
            val builder = StringBuilder(
                template // null -> "null"
                    .length + 16 * args.size)
            var templateStart = 0
            var i = 0
            while (i < args.size) {
                val placeholderStart = template // null -> "null"
                    .indexOf("%s", templateStart)
                if (placeholderStart == -1) {
                    break
                }
                builder.append(
                    template // null -> "null", templateStart, placeholderStart
                )
                builder.append(args[i++])
                templateStart = placeholderStart + 2
            }
            builder.append(
                template // null -> "null", templateStart, template // null -> "null"
                    .length
            )

            // if we run out of placeholders, append the extra args in square braces
            if (i < args.size) {
                builder.append(" [")
                builder.append(args[i++])
                while (i < args.size) {
                    builder.append(", ")
                    builder.append(args[i++])
                }
                builder.append(']')
            }
            return builder.toString()
        }
    }
}