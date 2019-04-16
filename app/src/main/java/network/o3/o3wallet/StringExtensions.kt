package network.o3.o3wallet

import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import java.math.BigDecimal
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


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

fun String.decimalNoGrouping(): String {
    var replaced = this.replace(DecimalFormat().decimalFormatSymbols.groupingSeparator.toString(), "")
    replaced = replaced.replace(DecimalFormat().decimalFormatSymbols.decimalSeparator.toString(), ".")
    return replaced
}

fun Editable.decimalNoGrouping(): String {
    var replaced = this.toString().replace(DecimalFormat().decimalFormatSymbols.groupingSeparator.toString(), "")
    replaced = replaced.replace(DecimalFormat().decimalFormatSymbols.decimalSeparator.toString(), ".")
    return replaced
}

fun CharSequence.decimalNoGrouping(): String {
    var replaced = this.toString().replace(DecimalFormat().decimalFormatSymbols.groupingSeparator.toString(), "")
    replaced = replaced.replace(DecimalFormat().decimalFormatSymbols.decimalSeparator.toString(), ".")
    return replaced
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

fun String.hasMaxDecimals(decimals: Int): Boolean {
    var replaced = this.replace(DecimalFormat().decimalFormatSymbols.groupingSeparator.toString(), "")
    replaced = replaced.replace(DecimalFormat().decimalFormatSymbols.decimalSeparator.toString(), ".")
    val decimalIndex = replaced.indexOf(".")
    if (decimalIndex == -1) {
        return false
    }
    val substr = replaced.substringAfter(".")
    return substr.count() >= decimals
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

