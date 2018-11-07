package network.o3.o3wallet.MultiWallet.Activate


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import network.o3.o3wallet.R

class EncryptExistingKeySuccessFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.multiwallet_existing_encryption_success, container, false)
    }


}
