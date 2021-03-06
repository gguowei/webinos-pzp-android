/*******************************************************************************
 *  Code contributed to the webinos project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Copyright 2013 Sony Mobile Communications
 * 
 ******************************************************************************/

package org.webinos.android.impl.nfc;

import java.util.ArrayList;
import java.util.List;

import org.meshpoint.anode.AndroidContext;
import org.meshpoint.anode.module.IModule;
import org.meshpoint.anode.module.IModuleContext;
import org.webinos.api.DeviceAPIError;
import org.webinos.api.nfc.NdefRecord;
import org.webinos.api.nfc.NfcEventListener;
import org.webinos.api.nfc.NfcModule;
import org.webinos.api.nfc.NfcTag;
import org.webinos.api.nfc.NfcTagTechnology;
import org.webinos.api.nfc.NfcTagTechnologyNdef;
import org.webinos.android.impl.nfc.NfcManager.NfcDiscoveryListener;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;

public class NfcAnodeModule extends NfcModule implements IModule, NfcDiscoveryListener {

  private static String TAG = NfcAnodeModule.class.getName();

  private IModuleContext moduleContext;
  private Context androidContext;

  private NfcAdapter mNfcAdapter;
  
  protected NfcManager nfcMgr;
  protected NfcEventListener mNfcEventListener;

  @Override
  public Object startModule(IModuleContext ctx) {
    Log.v(TAG, "startModule");
    this.moduleContext = ctx;
    this.androidContext = ((AndroidContext) ctx).getAndroidContext();

    mNfcAdapter = NfcAdapter.getDefaultAdapter(androidContext);
    
    nfcMgr = NfcManager.getInstance(androidContext);

    return this;
  }

  @Override
  public void stopModule() {
    Log.v(TAG, "stopModule");
  }
  
  @Override
  public void setListener(NfcEventListener listener) {
    mNfcEventListener = listener;
    if (mNfcEventListener != null) {
      nfcMgr.addListener(this);
    } else {
      nfcMgr.removeListener(this);
    }
  }

  @Override
  public void addTextTypeFilter() {
    checkNfcAvailability();
    nfcMgr.addTextTypeFilter();
  }

  @Override
  public void addUriTypeFilter(String scheme) {
    checkNfcAvailability();
    nfcMgr.addUriTypeFilter(scheme);
  }

  @Override
  public void addMimeTypeFilter(String mimeType) {
    checkNfcAvailability();
    nfcMgr.addMimeTypeFilter(mimeType);
  }

  @Override
  public void removeTextTypeFilter() {
    checkNfcAvailability();
    nfcMgr.removeTextTypeFilter();
  }

  @Override
  public void removeUriTypeFilter(String scheme) {
    checkNfcAvailability();
    nfcMgr.removeUriTypeFilter(scheme);
  }

  @Override
  public void removeMimeTypeFilter(String mimeType) {
    checkNfcAvailability();
    nfcMgr.addMimeTypeFilter(mimeType);
  }

  private void checkNfcAvailability() {
    if (!isNfcAvailable()) {
      throw new NfcError(DeviceAPIError.NOT_SUPPORTED_ERR,
          "NFC is not supported on this device");
    }
  }

  @Override
  public void shareTag(NdefRecord[] ndefMessage) {
    checkNfcPushAvailability();
    setSharedTag(ndefMessage);
  }

  @Override
  public void unshareTag() {
    checkNfcPushAvailability();
    setSharedTag(null);
  }

  private void checkNfcPushAvailability() {
    if (!isNfcAvailable()) {
      throw new NfcError(DeviceAPIError.NOT_SUPPORTED_ERR,
          "NFC push is not supported on this device");
    }
  }

  @Override
  public boolean isNfcAvailable() {
    return (mNfcAdapter != null && mNfcAdapter.isEnabled());
  }
  
  @Override
  public boolean isNfcPushAvailable() {
    return isNfcAvailable();
  }

  @Override
  public void onTagDiscovered(Object tag) {
    if (tag instanceof Tag) {
      Tag androidTag = (Tag) tag;
      NfcTag event = new NfcTag();
      event.tagId = androidTag.getId();
      List<NfcTagTechnology> techList = new ArrayList<NfcTagTechnology>();
      for (String tech : androidTag.getTechList()) {
        if (tech.equals(Ndef.class.getName())) {
          android.nfc.tech.Ndef ndefTech = android.nfc.tech.Ndef
              .get(androidTag);
          techList.add(new NfcTagTechnologyNdefImpl(this.env, ndefTech));
        }
      }
      // event.techList = new NfcTagTechnology[techList.size()];
      // techList.toArray(event.techList);
      if (techList.size() > 0) {
        event.tech = (NfcTagTechnologyNdef) techList.get(0);
      }
      if (mNfcEventListener != null) {
        mNfcEventListener.handleEvent(event);
      }
    }
  }

  private void setSharedTag(NdefRecord[] ndefMessage) {
    nfcMgr
        .setSharedTag(NfcTagTechnologyNdefImpl.createNdefMessage(ndefMessage));
  }

  @Override
  public void launchScanningActivity(boolean autoDismiss) {
    nfcMgr.launchScanningActivity(autoDismiss);
  }
}
