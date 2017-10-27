package com.rockchip.tutk.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * Created by waha on 2017/2/22.
 */

public class CrcUtils {
    private static String TAG = "CrcUtils";
    private static int FWK_CONTROLLER_BUFSIZE = 1024 * 4;
    private static long[] crc_table = new long[256];

    private static void init_crc_table() {
        long c;
        int i, j;
        Log.v(TAG, "init_crc_table");
        for (i = 0; i < 256; i++) {
            c = getUnsignedIntt(i);
            for (j = 0; j < 8; j++) {
                if ((c & 1) > 0) {
                    c = 0xedb88320L ^ (c >> 1);
                } else {
                    c = c >> 1;
                }
            }
            crc_table[i] = c;
        }
        Log.v(TAG, "init_crc_table done!\n");
    }

    private static long crc32(long crc, byte[] buffer, int size) {
        int i;
        for (i = 0; i < size; i++) {
            crc = crc_table[(int) ((crc ^ buffer[i]) & 0xff)] ^ (crc >> 8);
        }
        return crc;
    }

    public static long calc_img_crc_with_api(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "file name is NULL");
            return 0;
        }
        CheckedInputStream cis = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            cis = new CheckedInputStream(fis, new CRC32());
            byte[] buffer = new byte[128];
            while (cis.read(buffer) >= 0) {
            }
            long crc = cis.getChecksum().getValue();
            return crc;
        } catch (Exception e) {
            Log.e(TAG, "calc_img_crc_with_api happen error: " + fileName);
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fis = null;
            }
            if (null != cis) {
                try {
                    cis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                cis = null;
            }
        }
        return 0;
    }

    public static long calc_img_crc(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "file name is NULL");
            return 0;
        }

        init_crc_table();

        long crc = getUnsignedIntt(0xFFFFFFFF);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            byte[] buffer = new byte[FWK_CONTROLLER_BUFSIZE];

            int len = -1;
            while ((len = fis.read(buffer)) != -1) {
                crc = crc32(crc, buffer, len);
            }
            return crc;
        } catch (Exception e) {
            Log.e(TAG, "calc_img_crc happen error: " + fileName);
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fis = null;
            }
        }
        return 0;
    }

    private static long getUnsignedIntt(int data) {
        long temp = data & 0x0FFFFFFFFL;
        return temp;
    }

}
