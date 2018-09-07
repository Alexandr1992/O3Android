package network.o3.o3wallet

import android.content.Context
import android.content.res.Configuration
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.text.Spanned
import android.text.InputFilter
import java.math.BigDecimal
import java.util.regex.Pattern
import android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
import android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL
import android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL
import android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
import java.security.MessageDigest


/**
 * Created by drei on 12/7/17.
 */
fun Double.formattedBTCString() : String {
    return "%.8f".format(this) + "BTC"
}

fun Double.formattedFiatString() : String {
    val formatter = NumberFormat.getCurrencyInstance()
    formatter.currency = Currency.getInstance(PersistentStore.getCurrency().toUpperCase())
    if (this < 0.0099999999999999) {
        formatter.maximumFractionDigits = 4
    } else {
        formatter.maximumFractionDigits = 2
    }

    return formatter.format(this)
}

fun Double.formattedPercentString(): String {
    return  "%.2f".format(this) + "%"
}

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

fun Double.removeTrailingZeros(): String {
    var formatter = NumberFormat.getNumberInstance()
    formatter.maximumFractionDigits = 8
    return  formatter.format(this)

    /* var doubleString = this.format(8)
    if (this % 1.0 == 0.0) {
        return this.format(0)
    }
    for (x in doubleString.length - 1 downTo  0) {
        if (doubleString[x] == '0') {
            doubleString = doubleString.removeSuffix("0")
        }
    }*/
   // return doubleString
}

fun String.toSafeDecimal(): BigDecimal {
    return BigDecimal(this)
}

fun BigDecimal.fromSafeMemory(decimals: Int): BigDecimal {
    return this.divide(BigDecimal(Math.pow(10.0, decimals.toDouble())), decimals, BigDecimal.ROUND_HALF_UP)
}

fun BigDecimal.toSafeMemory(decimals: Int): Long {
    return this.multiply(BigDecimal(Math.pow(10.0, decimals.toDouble()))).toLong()
}



enum class CurrencyType {
    BTC, FIAT
}

fun Double.formattedCurrencyString(currency: CurrencyType): String {
    return when(currency) {
        CurrencyType.BTC -> this.formattedBTCString()
        CurrencyType.FIAT -> this.formattedFiatString()
    }
}

fun String.transactionToID(): String {
    val firstHash = this.hexStringToByteArray().Hash256().hexStringToByteArray().toHex()
    return firstHash.hexStringToByteArray().Hash256().hexStringToByteArray().reversedArray().toHex()
}

fun ByteArray.Hash256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(this)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

fun Date.intervaledString(interval: String): String {
    var dateFormat = ""
    if (interval == "6H" || interval == "24H") {
        dateFormat = "hh:mm"
    } else {
        dateFormat = "MMM d, hh:mm"
    }
    val dateFormatter = SimpleDateFormat(dateFormat)
    return "since " + dateFormatter.format(this)
}



class DecimalDigitsInputFilter(digitsBeforeZero: Int, digitsAfterZero: Int) : InputFilter {

    internal var mPattern: Pattern

    init {
        mPattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?")
    }

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {

        val matcher = mPattern.matcher(dest)
        return if (!matcher.matches()) "" else null
    }
}

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

