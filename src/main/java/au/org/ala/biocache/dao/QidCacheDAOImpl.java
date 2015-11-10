/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.biocache.dao;

import au.org.ala.biocache.model.Qid;
import au.org.ala.biocache.util.QidMissingException;
import au.org.ala.biocache.util.QidSizeException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

/**
 * Manage cache of POST'ed search parameter q in memory and in db.
 *
 * @author Adam
 */
@Component("qidCacheDao")
public class QidCacheDAOImpl implements QidCacheDAO {

    private final Logger logger = Logger.getLogger(QidCacheDAOImpl.class);
    //max size of cached params in bytes
    @Value("${qid.cache.size.max:104857600}")
    long maxCacheSize;
    //min size of cached params in bytes
    @Value("${qid.cache.size.min:52428800}")
    long minCacheSize;
    //max single cacheable object size
    @Value("${qid.cache.largestCacheableSize:5242880}")
    long largestCacheableSize;
    //in memory store of params
    private ConcurrentHashMap<String, Qid> cache = new ConcurrentHashMap<String, Qid>();
    //counter and lock
    final private Object counterLock = new Object();
    private long cacheSize;
    private CountDownLatch counter;
    private long triggerCleanSize = minCacheSize + (maxCacheSize - minCacheSize) / 2;
    //thread for cache size limitation
    private Thread cacheCleaner;

    protected QidDAO qidDao = (QidDAO) au.org.ala.biocache.Config.getInstance(QidDAO.class);

    /**
     * init
     */
    public QidCacheDAOImpl() {
        counter = new CountDownLatch(1);

        cacheCleaner = new Thread() {

            @Override
            public void run() {
                try {
                    while (true) {
                        if (counter != null) counter.await();

                        synchronized (counterLock) {
                            cacheSize = minCacheSize;
                            counter = new CountDownLatch(1);
                        }

                        cleanCache();
                    }
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    logger.error("params cache cleaner stopping", e);
                }
            }
        };
        cacheCleaner.start();

        try {
            updateTriggerCleanSize();

            logger.info("maxCacheSize > " + maxCacheSize);
            logger.info("minCacheSize > " + minCacheSize);
        } catch (Exception e) {
            logger.error("cannot load qid.properties", e);
        }
    }

    /**
     * Store search params and return key.
     *
     * @param q            Search parameter q to store.
     * @param displayQ     Search display q to store.
     * @param wkt          wkt to store
     * @param bbox         bounding box to store as double array [min longitude, min latitude, max longitude, max latitude]
     * @param fqs          fqs to store
     * @param maxAge       -1 or expected qid life in ms
     * @param source       name of app that created this qid
     * @return id to retrieve stored value as long.
     */
    public String put(String q, String displayQ, String wkt, double[] bbox, String[] fqs, long maxAge, String source) throws QidSizeException {
        Qid qid = new Qid(null, q, displayQ, wkt, bbox, 0, fqs, maxAge, source);

        if (qid.size() > largestCacheableSize) {
            throw new QidSizeException(qid.size());
        }

        save(qid);

        while (!put(qid)) {
            //cache cleaner has been run, safe to try again
        }

        return qid.getRowKey();
    }

    /**
     * after adding an object to the cache update the cache size.
     *
     * @param qid
     * @return true if successful.
     */
    boolean put(Qid qid) {
        boolean runCleaner = false;
        synchronized (counterLock) {
            logger.debug("new cache size: " + cacheSize);
            if (cacheSize + qid.size() > maxCacheSize) {
                //run outside of counterLock
                runCleaner = true;
                logger.debug("not putting qid");
            } else {
                if (cacheSize + qid.size() > triggerCleanSize) {
                    counter.countDown();
                }

                cacheSize += qid.size();
                logger.debug("putting qid");
                cache.put(qid.getRowKey(), qid);
            }
        }

        if (runCleaner) {
            logger.debug("cleaning qid cache");
            cleanCache();
            return false;
        }

        return true;
    }

    /**
     * Retrive search parameter object
     *
     * @param key id returned by put as long.
     * @return search parameter q as String, or null if not in memory
     * or in file storage.
     */
    public Qid get(String key) throws QidMissingException {
        Qid obj = cache.get(key);

        if (obj == null) {
            obj = load(key);
        }

        if (obj != null) {
            obj.setLastUse(System.currentTimeMillis());
        }

        if (obj == null) {
            throw new QidMissingException(key);
        }

        return obj;
    }

    /**
     * Retrieves the ParamsCacheObject based on the supplied query string.
     *
     * @param query
     * @return
     * @throws Exception
     */
    public Qid getQidFromQuery(String query) throws QidMissingException {
        Qid qid = null;
        if (query.contains("qid:")) {
            Matcher matcher = QidCacheDAOImpl.qidPattern.matcher(query);

            if (matcher.find()) {
                String value = matcher.group();
                qid = get(value.substring(4));
            }
        }
        return qid;
    }

    /**
     * delete records from the cache to get cache size <= minCacheSize
     */
    synchronized void cleanCache() {
        updateTriggerCleanSize();
                
        if (cacheSize < triggerCleanSize) {
            return;
        }

        List<Entry<String, Qid>> entries = new ArrayList(cache.entrySet());

        //sort ascending by last use time
        Collections.sort(entries, new Comparator<Entry<String, Qid>>() {

            @Override
            public int compare(Entry<String, Qid> o1, Entry<String, Qid> o2) {
                long c = o1.getValue().getLastUse() - o2.getValue().getLastUse();
                return (c < 0) ? -1 : ((c > 0) ? 1 : 0);
            }
        });

        long size = 0;
        int numberRemoved = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (size + entries.get(i).getValue().size() > minCacheSize) {
                String key = entries.get(i).getKey();
                cache.remove(key);
                numberRemoved++;
            } else {
                size += entries.get(i).getValue().size();
            }
        }

        //adjust output size correctly
        synchronized (counterLock) {
            cacheSize = cacheSize - (minCacheSize - size);
            size = cacheSize;
        }
        logger.debug("removed " + numberRemoved + " cached qids, new cache size " + size);
    }

    /**
     * save a Qid to db
     *
     * @param value
     */
    void save(Qid value) {
        try {
            qidDao.put(value);
        } catch (Exception e) {
            logger.error("faild to save qid to db", e);
        }
    }

    /**
     * load db stored Qid
     *
     * @param key
     * @return
     * @throws au.org.ala.biocache.util.QidMissingException
     */
    Qid load(String key) throws QidMissingException {
        Qid value = null;
        try {
            value = qidDao.get(key);
        } catch (Exception e) {
            logger.error("failed to find qid:" + key, e);
            throw new QidMissingException(key);
        }

        return value;
    }

    public void setMaxCacheSize(long sizeInBytes) {
        maxCacheSize = sizeInBytes;
        updateTriggerCleanSize();
    }

    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMinCacheSize(long sizeInBytes) {
        minCacheSize = sizeInBytes;
        updateTriggerCleanSize();
    }

    public long getMinCacheSize() {
        return minCacheSize;
    }

    public void setLargestCacheableSize(long sizeInBytes) {
        largestCacheableSize = sizeInBytes;
    }

    public long getLargestCacheableSize() {
        return largestCacheableSize;
    }

    public long getSize() {
        return cacheSize;
    }

    /**
     * cache cleaner is triggered when the size of the cache is
     * half way between the min and max size.
     */
    void updateTriggerCleanSize() {
        triggerCleanSize = minCacheSize + (maxCacheSize - minCacheSize) / 2;
        logger.debug("triggerCleanSize=" + triggerCleanSize + " minCacheSize=" + minCacheSize + " maxCacheSize=" + maxCacheSize);
    }
}