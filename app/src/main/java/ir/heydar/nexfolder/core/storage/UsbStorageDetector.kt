package ir.heydar.nexfolder.core.storage

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.storage.StorageVolume

/**
 * Distinguishes USB/OTG flash drives from removable SD cards by matching
 * [StorageManager] volumes against connected USB mass-storage devices.
 */
object UsbStorageDetector {

    /**
     * Returns true when [volume] looks like a USB OTG drive: removable volume whose
     * path lives under /mnt/media_rw with a matching connected USB mass-storage device.
     */
    fun isUsbVolume(context: Context, volume: StorageVolume, volumePath: String): Boolean {
        if (!volume.isRemovable) return false
        val usbDevices = connectedMassStorageDevices(context)
        if (usbDevices.isEmpty()) return false
        // Heuristic: OTG mounts appear under /mnt/media_rw/<uuid>; SD cards usually
        // carry an SD-matching description. Compare volume descriptions first.
        val description = volume.getDescription(context)?.lowercase() ?: ""
        if (description.contains("usb")) return true
        // Fall back to mount-path naming used for OTG drives.
        return volumePath.contains("/mnt/media_rw") &&
            usbDevices.any { device -> volume.uuid?.contains(device.serialNumber ?: "") == true }
    }

    /** Connected USB devices that advertise mass-storage class, if any. */
    fun connectedMassStorageDevices(context: Context): List<UsbDevice> {
        val manager = context.getSystemService(UsbManager::class.java) ?: return emptyList()
        return try {
            manager.deviceList.values.filter { device ->
                device.deviceClass == UsbConstants.USB_CLASS_MASS_STORAGE ||
                    (device.deviceClass == 0 && (0 until device.interfaceCount).any { index ->
                        device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
                    })
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
