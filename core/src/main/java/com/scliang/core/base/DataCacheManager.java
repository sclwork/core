package com.scliang.core.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/18.
 */
public final class DataCacheManager {
    private DataCacheManager() {
    }

    private static class SingletonHolder {
        private static final DataCacheManager INSTANCE = new DataCacheManager();
    }

    public static DataCacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // max cache size 5M
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 5;
    // cache
    private static final int DISK_CACHE_INDEX = 0;
    private DiskLruCache mDiskLruCache;

    /**
     * 使用ApplicationContext初始化DataCacheManager工具
     */
    public void init(BaseApplication application) {
        init(application, null);
    }

    /**
     * 使用ApplicationContext初始化DataCacheManager工具
     */
    public void init(BaseApplication application, String diskCachePath) {
        String cachePath;
        final Context context = application.getApplicationContext();
        boolean useInternal = !Permission.hasStoragePermission(new OnCheckPermissionImpl()) || TextUtils.isEmpty(diskCachePath);
        if (useInternal) {
            cachePath = context.getCacheDir().getPath();
        } else {
            cachePath = diskCachePath;
        }

        if (!TextUtils.isEmpty(cachePath)) {
            cachePath = cachePath + File.separator + "cache";
        }

        if (TextUtils.isEmpty(cachePath)) {
            return;
        }

        File cacheFile = new File(cachePath);
        if (!cacheFile.exists()) {
            cacheFile.mkdirs();
        }

        if (!cacheFile.exists()) {
            return;
        }

        if (cacheFile.getUsableSpace() <= DISK_CACHE_SIZE) {
            return;
        }

        try {
            mDiskLruCache = DiskLruCache.open(cacheFile,
                    getAppVersion(context), 1, DISK_CACHE_SIZE);
        } catch (Exception ignored) { }
    }

    public void putCache(String key, String value) {
        if (mDiskLruCache == null) {
            return;
        }

        OutputStream os = null;

        try {
            DiskLruCache.Editor editor = mDiskLruCache.edit(NUtils.md5(key));
            if (editor == null) {
                return;
            }

            os = editor.newOutputStream(DISK_CACHE_INDEX);
            os.write(value.getBytes());
            os.flush();

            editor.commit();
            mDiskLruCache.flush();

        } catch (Exception ignored) {
        } finally {
            if (os != null) {
                try { os.close(); }
                catch (IOException ignored) { }
            }
        }
    }

    public String getCache(String key) {
        if (mDiskLruCache == null) {
            return "";
        }

        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;

        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(NUtils.md5(key));
            if (snapshot != null) {
                fis = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
                bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                byte[] data = bos.toByteArray();
                return new String(data);
            }
        } catch (Exception ignored) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) { }
            } if (bos != null) {
                try { bos.close(); }
                catch (IOException ignored) { }
            }
        }

        return "";
    }

    private int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 1;
        }
    }
}
