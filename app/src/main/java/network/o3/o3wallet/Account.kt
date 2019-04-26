package network.o3.o3wallet

import neoutils.Neoutils
import neoutils.Neoutils.generateFromWIF
import neoutils.Wallet
import network.o3.o3wallet.Crypto.Decryptor
import network.o3.o3wallet.Crypto.EncryptedSettingsRepository
import network.o3.o3wallet.Crypto.EncryptedSettingsRepository.setProperty
import network.o3.o3wallet.Crypto.Encryptor
import java.security.MessageDigest

/**
 * Created by drei on 11/22/17.
 */

object Account {
    private var wallet: Wallet? = null
    private var sharedSecretPieceOne: String? = null

    //this keypair is used for device level auth services
    private var o3KeyPair: Wallet? = null

    private fun sha256(input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
                .getInstance("SHA-256")
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }
        return result.toString()
    }

    private fun storeEncryptedKeyOnDevice() {
        val wif = wallet!!.wif
        val encryptor = Encryptor()
        val alias = "O3 Key"
        val encryptedWIF = encryptor.encryptText(alias, wif)!!

        val iv = encryptor.getIv()!!
        setProperty(alias, encryptedWIF.toHex(), iv, O3Wallet.appContext!!)
    }

    fun isEncryptedWalletPresent(): Boolean {
        val alias = "O3 Key"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
        if (storedVal.data == null) {
            return false
        }
        val storedEncryptedWIF = storedVal.data?.hexStringToByteArray()
        if (storedEncryptedWIF == null || storedEncryptedWIF.size == 0 || !Decryptor().keyStoreEntryExists("O3 Key")) {
            return false
        }
        return true
    }

    fun createO3Keypair() {
        val encryptor = Encryptor()
        val alias = "O3AuthKey"
        val o3auth = Neoutils.newWallet()
        val encryptedPass = encryptor.encryptText(alias, o3auth.privateKey.toHex())!!

        val iv = encryptor.getIv()!!
        setProperty(alias, encryptedPass.toHex(), iv, O3Wallet.appContext!!)
    }

    fun restoreO3KeyPair() {
        val alias = "O3AuthKey"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
        val storedEncryptedPass = storedVal.data?.hexStringToByteArray()!!
        val storedIv = storedVal.iv!!
        val decrypted = Decryptor().decrypt(alias, storedEncryptedPass , storedIv)
        o3KeyPair = Neoutils.generateFromPrivateKey(decrypted)
    }

    fun getO3AuthPubKey(): String? {
        if (o3KeyPair != null) {
            return o3KeyPair!!.publicKey.toHex()
        }

        val alias = "O3AuthKey"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
        if (storedVal.data == null) {
            return null
        }

        val storedEncryptedPass = storedVal.data?.hexStringToByteArray()
        if (storedEncryptedPass == null || storedEncryptedPass.size == 0 || !Decryptor().keyStoreEntryExists("O3AuthKey")) {
            return null
        }

        restoreO3KeyPair()
        return o3KeyPair!!.publicKey.toHex()
    }

    fun getO3AuthPrivKey(): String? {
        if (o3KeyPair != null) {
            return o3KeyPair!!.privateKey.toHex()
        }

        val alias = "O3AuthKey"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
        if (storedVal.data == null) {
            return null
        }

        val storedEncryptedPass = storedVal.data?.hexStringToByteArray()
        if (storedEncryptedPass == null || storedEncryptedPass.size == 0 || !Decryptor().keyStoreEntryExists("O3AuthKey")) {
            return null
        }

        restoreO3KeyPair()
        return o3KeyPair!!.privateKey.toHex()
    }

    fun storeDefaultNep6Pass(password: String) {
        val encryptor = Encryptor()
        val alias = "Default NEP6 Pass"
        val encryptedPass = encryptor.encryptText(alias, password)!!

        val iv = encryptor.getIv()!!
        setProperty(alias, encryptedPass.toHex(), iv, O3Wallet.appContext!!)
    }

    fun isDefaultEncryptedNEP6PassPresent(): Boolean {
        val alias = "Default NEP6 Pass"
        val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
        if (storedVal.data == null) {
            return false
        }
        val storedEncryptedPass = storedVal.data?.hexStringToByteArray()
        if (storedEncryptedPass == null || storedEncryptedPass.size == 0 || !Decryptor().keyStoreEntryExists("Default NEP6 Pass")) {
            return false
        }
        return true
    }

    // A generic key pass is a key value store for keys into a keystore file
    // Currently O3 supports One keystore file type which is NEP6
    // However there maybe alternative file types in the future
    // the key value mapping will look something like
    // {"NEP6.2f3475895f3a334245ba91ee3f994392a89dc2b1b6074d086ad1606b19a3dcba: SuperSecurePassword"}

    // Where NEP6 is the file type and 2f3475895f3a334245ba91ee3f994392a89dc2b1b6074d086ad1606b19a3dcba is
    // double hashed primary key identifier of the file. In the NEP6 file, the primary identifier is the NEO address
    // this double hash was generated from AKzUziiiv9vHj8hX3bYQFVUktk36u6C5w3
    // In another file type it could be a generic public key
    fun storeGenericKeyPassOnDevice(file: String, keyIdentifier: String, password: String) {
        val encryptor = Encryptor()
        val alias = file + "." + sha256(sha256(keyIdentifier))
        val encryptedPass = encryptor.encryptText(alias, password)!!

        val iv = encryptor.getIv()!!
        setProperty(alias, encryptedPass.toHex(), iv, O3Wallet.appContext!!)
    }

    fun getStoredPassForNEP6Entry(keyIdentifier: String): String {
        val alias = "NEP6." + sha256(sha256(keyIdentifier))
        val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
        val storedEncryptedPass = storedVal.data?.hexStringToByteArray()!!
        val storedIv = storedVal.iv!!
        val decrypted = Decryptor().decrypt(alias, storedEncryptedPass , storedIv)
        return decrypted
    }

    fun isStoredPasswordForNep6KeyPresent(keyIdentifier: String): Boolean {
        val alias = "NEP6." + sha256(sha256(keyIdentifier))
        val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
        if (storedVal.data == null) {
            return false
        }
        val storedEncryptedPassword = storedVal.data?.hexStringToByteArray()
        if (storedEncryptedPassword == null || storedEncryptedPassword.size == 0 || !Decryptor().keyStoreEntryExists(alias)) {
            return false
        }
        return true
    }

    fun deleteStoredNEP6PasswordEntry(keyIdentifier: String){
        val alias = "NEP6." + sha256(sha256(keyIdentifier))
        EncryptedSettingsRepository.removeProperty(alias, O3Wallet.appContext!!)
    }

    fun restoreWalletFromDevice() {
        if (isDefaultEncryptedNEP6PassPresent()) {
            val alias = "Default NEP6 Pass"
            val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
            val storedEncryptedPass = storedVal.data?.hexStringToByteArray()!!
            val storedIv = storedVal.iv!!
            val decrypted = Decryptor().decrypt(alias, storedEncryptedPass , storedIv)
            val defaultAccount = NEP6.getFromFileSystem().accounts.find { it.isDefault }!!
            wallet = Neoutils.neP2DecryptToWallet(defaultAccount.key!!, decrypted)
        } else {
            ///O3 Key is the old key version that was used
            val alias = "O3 Key"
            val storedVal = EncryptedSettingsRepository.getProperty(alias, O3Wallet.appContext!!)
            val storedEncryptedWIF = storedVal.data?.hexStringToByteArray()!!
            val storedIv = storedVal.iv!!
            val decrypted = Decryptor().decrypt(alias, storedEncryptedWIF, storedIv)
            wallet = generateFromWIF(decrypted)
        }
    }

    //This allows for setting of the address temporarily
    // while the long running decryption of NEP6 is taking pl;ace
    fun accountSetAddress(address: String) {
        wallet?.address = address
        wallet?.wif = null
        wallet?.privateKey = null
        wallet?.publicKey = null
        wallet?.hashedSignature = null
    }


    fun deleteKeyFromDevice() {
        val alias = "O3 Key"
        setProperty(alias, "", kotlin.ByteArray(0), O3Wallet.appContext!!)
    }

    fun deleteNEP6PassFromDevice() {
        val alias = "Default NEP6 Pass"
        setProperty(alias, "", kotlin.ByteArray(0), O3Wallet.appContext!!)
    }
    
    fun fromWIF(wif: String): Boolean{
        //Java does not support multiple return values from function.
        //The function that is generated by go mobile bind. If the second return value is Error type, it will throw the generic exception
        try {
            wallet = generateFromWIF(wif)
        } catch (e: Exception) {
            return false
        }

        storeEncryptedKeyOnDevice()
        return true
    }

    fun fromEncryptedKey(encryptedKey: String, password: String): Boolean{
        //Java does not support multiple return values from function.
        //The function that is generated by go mobile bind. If the second return value is Error type, it will throw the generic exception
        try {
            wallet = Neoutils.neP2DecryptToWallet(encryptedKey, password)
        } catch (e: Exception) {
            return false
        }

        storeEncryptedKeyOnDevice()
        return true
    }

    fun getWallet(): Wallet {
        if (wallet != null) {
            return wallet!!
        } else {
            restoreWalletFromDevice()
            return wallet!!
        }
    }

    fun clearWallet() {
        wallet = null
    }
}
