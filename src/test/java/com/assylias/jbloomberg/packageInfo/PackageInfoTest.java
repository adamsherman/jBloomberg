/*
 * Copyright (C) 2012 - present by Yann Le Tallec.
 * Please see distribution for license.
 */
package com.assylias.jbloomberg.packageInfo;

import com.assylias.jbloomberg.BloombergException;
import com.assylias.jbloomberg.BloombergSession;
import com.assylias.jbloomberg.DataChangeEvent;
import com.assylias.jbloomberg.DataChangeListener;
import com.assylias.jbloomberg.DefaultBloombergSession;
import com.assylias.jbloomberg.HistoricalData;
import com.assylias.jbloomberg.HistoricalRequestBuilder;
import com.assylias.jbloomberg.IntradayBarData;
import com.assylias.jbloomberg.IntradayBarField;
import com.assylias.jbloomberg.IntradayBarRequestBuilder;
import com.assylias.jbloomberg.IntradayTickData;
import com.assylias.jbloomberg.IntradayTickField;
import com.assylias.jbloomberg.IntradayTickRequestBuilder;
import com.assylias.jbloomberg.RealtimeField;
import com.assylias.jbloomberg.ReferenceData;
import com.assylias.jbloomberg.ReferenceRequestBuilder;
import com.assylias.jbloomberg.RequestBuilder;
import com.assylias.jbloomberg.SubscriptionBuilder;
import com.assylias.jbloomberg.TypedObject;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups="unit")
public class PackageInfoTest {

    private static final BloombergSession session = new DefaultBloombergSession();

    @BeforeClass
    public void beforeClass() throws Exception {
        session.start();
    }

    @AfterClass
    public void afterClass() throws Exception {
        session.stop();
    }

    @Test
    public void test_ReferenceExample() throws Exception {
        RequestBuilder<ReferenceData> hrb = new ReferenceRequestBuilder(
                "SPX Index", "CRNCY_ADJ_PX_LAST")
                .addOverride("EQY_FUND_CRNCY", "JPY");
        ReferenceData result = session.submit(hrb).get();
        double priceInYen = result.forSecurity("SPX Index").forField("CRNCY_ADJ_PX_LAST").asDouble();
        System.out.println("SPX in Yen = " + priceInYen);
    }

    @Test
    public void test_HistoricalExample() throws Exception {
        RequestBuilder<HistoricalData> hrb = new HistoricalRequestBuilder("SPX Index",
                "PX_LAST", DateTime.now().minusDays(7),
                DateTime.now())
                .fill(HistoricalRequestBuilder.Fill.NIL_VALUE)
                .days(HistoricalRequestBuilder.Days.ALL_CALENDAR_DAYS);
        HistoricalData result = session.submit(hrb).get();
        Map<DateTime, TypedObject> data = result.forSecurity("SPX Index").forField("PX_LAST").get();
        for (Map.Entry<DateTime, TypedObject> e : data.entrySet()) {
            DateTime dt = e.getKey();
            double price = e.getValue().asDouble();
            System.out.println("[" + dt + "] " + price);
        }
    }

    @Test
    public void test_IntradayBarExample() throws Exception {
        RequestBuilder<IntradayBarData> hrb = new IntradayBarRequestBuilder("SPX Index",
                DateTime.now().minusDays(7),
                DateTime.now())
                .adjustSplits()
                .fillInitialBar()
                .period(1, TimeUnit.HOURS);
        IntradayBarData result = session.submit(hrb).get();
        Map<DateTime, TypedObject> data = result.forField(IntradayBarField.CLOSE).get();
        for (Map.Entry<DateTime, TypedObject> e : data.entrySet()) {
            DateTime dt = e.getKey();
            double price = e.getValue().asDouble();
            System.out.println("[" + dt + "] " + price);
        }
    }

    @Test
    public void test_IntradayTickExample() throws Exception {
        RequestBuilder<IntradayTickData> hrb = new IntradayTickRequestBuilder("SPX Index",
                DateTime.now().minusHours(2),
                DateTime.now())
                .includeBrokerCodes()
                .includeConditionCodes();
        IntradayTickData result = session.submit(hrb).get();
        Multimap<DateTime, TypedObject> data = result.forField(IntradayTickField.VALUE);
        for (Map.Entry<DateTime, TypedObject> e : data.entries()) {
            DateTime dt = e.getKey();
            double price = e.getValue().asDouble();
            System.out.println("[" + dt + "] " + price);
        }
    }

    @Test
    public void test_SubscriptionExample() throws InterruptedException, BloombergException {
        DataChangeListener lst = new DataChangeListener() {
            @Override
            public void dataChanged(DataChangeEvent e) {
                System.out.println(e);
            }
        };
        SubscriptionBuilder builder = new SubscriptionBuilder()
                .addSecurity("ESA Index")
                .addField(RealtimeField.LAST_PRICE)
                .addField(RealtimeField.ASK)
                .addField(RealtimeField.ASK_SIZE)
                .addListener(lst);
        session.subscribe(builder);
        Thread.sleep(3000);
    }
}
